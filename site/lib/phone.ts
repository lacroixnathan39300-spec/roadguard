import {ApiError} from './errors.ts';
import {digest} from './devices.ts';
import {nativeIdentity} from './devices.ts';

type Db=Pick<D1Database,'prepare'|'batch'>;
type Owner={userId:string;email?:string}|null;
type Secrets={PHONE_HASH_SECRET?:string;ROADGUARD_ADMIN_USER_ID?:string;ROADGUARD_ADMIN_EMAIL?:string};
const isAdmin=(owner:Owner,secrets:Secrets)=>!!owner&&(!!secrets.ROADGUARD_ADMIN_USER_ID&&owner.userId===secrets.ROADGUARD_ADMIN_USER_ID||!!secrets.ROADGUARD_ADMIN_EMAIL&&owner.email?.toLowerCase()===secrets.ROADGUARD_ADMIN_EMAIL.toLowerCase());
const response=(data:unknown,status=200)=>Response.json(data,{status,headers:{'Cache-Control':'no-store','X-Content-Type-Options':'nosniff'}});
export const e164=(value:unknown)=>{if(typeof value!=='string'||!/^\+[1-9][0-9]{7,14}$/.test(value))throw new ApiError(400,'Saisissez le numéro au format international, par exemple +33612345678.');return value;};
export async function phoneHash(s:Secrets,phone:string){if(!s.PHONE_HASH_SECRET)throw new ApiError(503,'Enregistrement des numéros en préparation.');const key=await crypto.subtle.importKey('raw',new TextEncoder().encode(s.PHONE_HASH_SECRET),{name:'HMAC',hash:'SHA-256'},false,['sign']);const bytes=await crypto.subtle.sign('HMAC',key,new TextEncoder().encode(phone));return Array.from(new Uint8Array(bytes)).map(x=>x.toString(16).padStart(2,'0')).join('');}
async function limited(db:Db,key:string,now:number,max:number,windowMs:number){const slot=Math.floor(now/windowMs),id='phone:'+key+':'+slot;const row=await db.prepare('INSERT INTO rate_limits(key,hits,until) VALUES(?,1,?) ON CONFLICT(key) DO UPDATE SET hits=hits+1 WHERE hits<? RETURNING key').bind(id,(slot+1)*windowMs,max).first();if(!row)throw new ApiError(429,'Trop de tentatives. Réessayez plus tard.');}
async function body(req:Request){const length=Number(req.headers.get('content-length')||0);if(length>1024)throw new ApiError(413,'Requête trop longue.');const raw=await req.text();if(raw.length>1024)throw new ApiError(413,'Requête trop longue.');try{const parsed=JSON.parse(raw);if(!parsed||typeof parsed!=='object'||Array.isArray(parsed))throw Error();return parsed as Record<string,unknown>;}catch{throw new ApiError(400,'Requête invalide.');}}
export async function phoneApi(db:Db,req:Request,owner:Owner,path:string,s:Secrets,now:number):Promise<Response|null>{
 if(!path.startsWith('phone/'))return null;
 // The legacy SMS routes never issue an access token when SMS verification is disabled.
 if(path==='phone/start'||path==='phone/verify')throw new ApiError(410,'Inscription par SMS désactivée. Associez votre téléphone depuis le site.');
 if(path==='phone/me'&&req.method==='GET'){
  if(!owner)throw new ApiError(401,'Connectez-vous sur le site.');
  const account=await db.prepare('SELECT last_four,display_name,unlimited,trial_started FROM phone_accounts WHERE user_id=?').bind(owner.userId).first();
  return response({account:account||null});
 }
 if(path==='phone/claim'&&req.method==='POST'){
  if(!owner)throw new ApiError(401,'Connectez-vous sur le site.');
  const b=await body(req),phone=e164(b.phone),hash=await phoneHash(s,phone),name=String(b.displayName||'').trim().replace(/[\x00-\x1f<>]/g,'').slice(0,32);
  if(name.length<2)throw new ApiError(400,'Prénom ou pseudonyme requis (2 à 32 caractères).');
  const ip=await digest(req.headers.get('cf-connecting-ip')||'unknown');await limited(db,'claim-ip:'+ip,now,10,3600000);
  const mine=await db.prepare('SELECT phone_hash,last_four FROM phone_accounts WHERE user_id=?').bind(owner.userId).first<{phone_hash:string;last_four:string}>();
  if(mine){if(mine.phone_hash!==hash)throw new ApiError(409,'Un numéro est déjà enregistré pour ce compte. Contactez le propriétaire pour le modifier.');return response({ok:true,lastFour:mine.last_four});}
  // Unique constraints on phone_hash and user_id are the final protection against concurrent claims.
  const other=await db.prepare('SELECT user_id FROM phone_accounts WHERE phone_hash=?').bind(hash).first<{user_id:string}>();
  if(other)throw new ApiError(409,'Ce numéro est déjà enregistré. Contactez le propriétaire si c’est le vôtre.');
  try{await db.prepare('INSERT INTO phone_accounts(phone_hash,user_id,last_four,display_name,trial_started,created,updated) VALUES(?,?,?,?,?,?,?)').bind(hash,owner.userId,phone.slice(-4),name,now,now,now).run();}
  catch{throw new ApiError(409,'Ce numéro ou ce compte est déjà enregistré.');}
  await db.prepare('INSERT INTO preferences(user,value,updated) VALUES(?,?,?) ON CONFLICT(user) DO NOTHING').bind(owner.userId,JSON.stringify({displayName:name}),now).run();
  return response({ok:true,lastFour:phone.slice(-4)},201);
 }
 if(path==='phone/access'&&req.method==='GET'){
  if(!isAdmin(owner,s))throw new ApiError(403,'Administration réservée au propriétaire.');
  const rows=await db.prepare('SELECT p.last_four,p.display_name,p.unlimited,p.admin,p.trial_started,p.created,(SELECT count(DISTINCT other.user_id) FROM device_signals mine JOIN device_signals other ON mine.signal_hash=other.signal_hash WHERE mine.user_id=p.user_id AND other.user_id<>p.user_id) shared_device_accounts FROM phone_accounts p ORDER BY p.created DESC LIMIT 200').all();return response({accounts:rows.results});
 }
 if(path==='phone/access'&&req.method==='POST'){
  if(!isAdmin(owner,s))throw new ApiError(403,'Administration réservée au propriétaire.');
  const b=await body(req),phone=e164(b.phone),hash=await phoneHash(s,phone),unlimited=b.unlimited===true?1:0;
  const updated=await db.prepare('UPDATE phone_accounts SET unlimited=?,updated=? WHERE phone_hash=? RETURNING last_four').bind(unlimited,now,hash).first<{last_four:string}>();
  await db.prepare('INSERT INTO access_grants(phone_hash,last_four,unlimited,updated) VALUES(?,?,?,?) ON CONFLICT(phone_hash) DO UPDATE SET unlimited=excluded.unlimited,updated=excluded.updated').bind(hash,phone.slice(-4),unlimited,now).run();
  return response({ok:true,lastFour:updated?.last_four||phone.slice(-4),unlimited:!!unlimited});
 }
 if(path==='phone/admin'&&req.method==='POST'){
  if(!isAdmin(owner,s))throw new ApiError(403,'Administration réservée au propriétaire du site.');
  const b=await body(req),phone=e164(b.phone),hash=await phoneHash(s,phone);
  const account=await db.prepare('SELECT user_id FROM phone_accounts WHERE phone_hash=?').bind(hash).first<{user_id:string}>();
  if(!account)throw new ApiError(404,'Créez d’abord le compte dans l’application avec ce numéro.');
  const native=await db.prepare('SELECT user_id FROM native_credentials WHERE phone_hash=?').bind(hash).first<{user_id:string}>();
  if(!native||native.user_id!==account.user_id)throw new ApiError(409,'Ce numéro est associé à un ancien compte du site. Contactez le propriétaire pour migrer ce compte.');
  await db.prepare('UPDATE phone_accounts SET admin=1,unlimited=1,updated=? WHERE phone_hash=?').bind(now,hash).run();
  return response({ok:true,lastFour:phone.slice(-4)});
 }
 return null;
}

export async function nativeAdminApi(db:Db,req:Request,path:string,s:Secrets,now:number):Promise<Response|null>{
 if(path!=='admin/me'&&path!=='admin/access')return null;
 const identity=await nativeIdentity(db,req,now);
 if(!identity)throw new ApiError(401,'Connectez-vous dans l’application.');
 const account=await db.prepare('SELECT admin FROM phone_accounts WHERE user_id=?').bind(identity.userId).first<{admin:number}>();
 if(path==='admin/me'&&req.method==='GET')return response({admin:!!account?.admin});
 if(!account?.admin)throw new ApiError(403,'Accès réservé à un compte administrateur approuvé.');
 if(path==='admin/access'&&req.method==='GET'){
  const accounts=await db.prepare('SELECT last_four,display_name,unlimited,admin,created FROM phone_accounts ORDER BY created DESC LIMIT 200').all();
  const pending=await db.prepare('SELECT last_four,unlimited,updated FROM access_grants ORDER BY updated DESC LIMIT 200').all();
  return response({accounts:accounts.results,pending:pending.results});
 }
 if(path==='admin/access'&&req.method==='POST'){
  const b=await body(req),phone=e164(b.phone),hash=await phoneHash(s,phone),unlimited=b.unlimited===true?1:0;
  await db.prepare('INSERT INTO access_grants(phone_hash,last_four,unlimited,updated) VALUES(?,?,?,?) ON CONFLICT(phone_hash) DO UPDATE SET unlimited=excluded.unlimited,updated=excluded.updated').bind(hash,phone.slice(-4),unlimited,now).run();
  await db.prepare('UPDATE phone_accounts SET unlimited=?,updated=? WHERE phone_hash=? AND admin=0').bind(unlimited,now,hash).run();
  return response({ok:true,lastFour:phone.slice(-4),unlimited:!!unlimited});
 }
 throw new ApiError(405,'Méthode non autorisée.');
}
