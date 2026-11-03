import {ApiError} from './errors.ts';
import {digest,signalHash} from './devices.ts';
import {e164,phoneHash} from './phone.ts';

type Db=Pick<D1Database,'prepare'|'batch'>;
type Secrets={PHONE_HASH_SECRET?:string;ROADGUARD_ADMIN_BOOTSTRAP_PHONE?:string;ROADGUARD_ADMIN_BOOTSTRAP_SALT?:string;ROADGUARD_ADMIN_BOOTSTRAP_VERIFIER?:string};
type Credential={user_id:string;salt:string;verifier:string};
const response=(value:unknown,status=200)=>Response.json(value,{status,headers:{'Cache-Control':'no-store','X-Content-Type-Options':'nosniff','Referrer-Policy':'no-referrer'}});
const hex=(bytes:Uint8Array)=>Array.from(bytes).map(b=>b.toString(16).padStart(2,'0')).join('');
const base64=(bytes:Uint8Array)=>btoa(String.fromCharCode(...bytes));
async function verifier(password:string,salt:string){
 const material=await crypto.subtle.importKey('raw',new TextEncoder().encode(password),'PBKDF2',false,['deriveBits']);
 const bytes=Uint8Array.from(atob(salt),c=>c.charCodeAt(0));
 // The production Workers runtime caps a single PBKDF2 derivation at 100,000 iterations.
 return hex(new Uint8Array(await crypto.subtle.deriveBits({name:'PBKDF2',hash:'SHA-256',salt:bytes,iterations:100000},material,256)));
}
function equal(a:string,b:string){if(a.length!==b.length)return false;let diff=0;for(let i=0;i<a.length;i++)diff|=a.charCodeAt(i)^b.charCodeAt(i);return diff===0;}
async function bootstrapAdmin(s:Secrets,phone:string,password:string){
 if(!s.ROADGUARD_ADMIN_BOOTSTRAP_PHONE||phone!==s.ROADGUARD_ADMIN_BOOTSTRAP_PHONE)return false;
 if(!s.ROADGUARD_ADMIN_BOOTSTRAP_SALT||!s.ROADGUARD_ADMIN_BOOTSTRAP_VERIFIER)throw new ApiError(503,'Activation du compte administrateur en préparation.');
 return equal(await verifier(password,s.ROADGUARD_ADMIN_BOOTSTRAP_SALT),s.ROADGUARD_ADMIN_BOOTSTRAP_VERIFIER);
}
async function limited(db:Db,key:string,now:number,max:number,span:number){
 const slot=Math.floor(now/span),id='native-auth:'+key+':'+slot;
 const allowed=await db.prepare('INSERT INTO rate_limits(key,hits,until) VALUES(?,1,?) ON CONFLICT(key) DO UPDATE SET hits=hits+1 WHERE hits<? RETURNING key').bind(id,(slot+1)*span,max).first();
 if(!allowed)throw new ApiError(429,'Trop de tentatives. Réessayez plus tard.');
}
async function body(req:Request){
 if(Number(req.headers.get('content-length')||0)>2048)throw new ApiError(413,'Requête trop longue.');
 const raw=await req.text();if(raw.length>2048)throw new ApiError(413,'Requête trop longue.');
 try{const parsed=JSON.parse(raw);if(!parsed||typeof parsed!=='object'||Array.isArray(parsed))throw Error();return parsed as Record<string,unknown>;}catch{throw new ApiError(400,'Requête invalide.');}
}
function access(){const random=crypto.getRandomValues(new Uint8Array(32));return 'rg_'+base64(random).replace(/\+/g,'-').replace(/\//g,'_').replace(/=/g,'');}
export async function nativeAuthApi(db:Db,req:Request,path:string,secrets:Secrets,now:number):Promise<Response|null>{
 if(path!=='native-auth/register'&&path!=='native-auth/login')return null;
 if(req.method!=='POST')throw new ApiError(405,'Méthode non autorisée.');
 const data=await body(req),phone=e164(data.phone),password=data.password;
 if(typeof password!=='string'||password.length<12||password.length>128)throw new ApiError(400,'Utilisez un mot de passe de 12 à 128 caractères.');
 const hash=await phoneHash(secrets,phone),ip=await digest(req.headers.get('cf-connecting-ip')||'unknown');
 await limited(db,'ip:'+ip,now,30,3600000);
 await limited(db,'phone:'+hash,now,8,900000);
 const deviceName=typeof data.deviceName==='string'?data.deviceName.replace(/[\x00-\x1f<>]/g,'').slice(0,60):'Téléphone ROADGUARD';
 const deviceSignal=await signalHash(secrets.PHONE_HASH_SECRET,data.deviceSignal);
 const id=crypto.randomUUID(),credential=access(),expires=now+2592000000;
 if(path==='native-auth/register'){
  const isBootstrapPhone=phone===secrets.ROADGUARD_ADMIN_BOOTSTRAP_PHONE;
  const admin=isBootstrapPhone&&await bootstrapAdmin(secrets,phone,password);
  if(isBootstrapPhone&&!admin)throw new ApiError(403,'Mot de passe administrateur incorrect. Aucun compte n’a été créé.');
  const name=typeof data.displayName==='string'?data.displayName.trim().replace(/[\x00-\x1f<>]/g,'').slice(0,32):'';
  if(name.length<2)throw new ApiError(400,'Indiquez un prénom ou pseudonyme de 2 à 32 caractères.');
  const salt=base64(crypto.getRandomValues(new Uint8Array(16))),derived=await verifier(password,salt),user='native:'+crypto.randomUUID();
  const grant=await db.prepare('SELECT unlimited FROM access_grants WHERE phone_hash=?').bind(hash).first<{unlimited:number}>();
  try{await db.batch([
   db.prepare('INSERT INTO phone_accounts(phone_hash,user_id,last_four,display_name,unlimited,admin,trial_started,created,updated) VALUES(?,?,?,?,?,?,?,?,?)').bind(hash,user,phone.slice(-4),name,admin?1:grant?.unlimited||0,admin?1:0,now,now,now),
   db.prepare('INSERT INTO native_credentials(phone_hash,user_id,salt,verifier,created,updated) VALUES(?,?,?,?,?,?)').bind(hash,user,salt,derived,now,now),
   db.prepare('INSERT INTO devices(id,user,token_hash,name,created,pending_until,expires,signal_hash) VALUES(?,?,?,?,?,?,?,?)').bind(id,user,await digest(credential),deviceName||'Téléphone ROADGUARD',now,0,expires,deviceSignal),
   db.prepare('INSERT INTO preferences(user,value,updated) VALUES(?,?,?) ON CONFLICT(user) DO NOTHING').bind(user,JSON.stringify({displayName:name}),now)
  ]);}catch{throw new ApiError(409,'Ce numéro est déjà enregistré. Connectez-vous ou contactez le propriétaire pour un ancien compte créé sur le site.');}
  if(deviceSignal)await db.prepare('INSERT INTO device_signals(signal_hash,user_id,first_seen,last_seen) VALUES(?,?,?,?) ON CONFLICT(signal_hash,user_id) DO UPDATE SET last_seen=excluded.last_seen').bind(deviceSignal,user,now,now).run();
  return response({status:'approved',id,accessToken:credential,displayName:name,unlimited:admin||!!grant?.unlimited,admin,expires},201);
 }
 const existing=await db.prepare('SELECT user_id,salt,verifier FROM native_credentials WHERE phone_hash=?').bind(hash).first<Credential>();
 const candidate=await verifier(password,existing?.salt||'AAAAAAAAAAAAAAAAAAAAAA==');
 if(!existing||!equal(candidate,existing.verifier))throw new ApiError(401,'Numéro ou mot de passe incorrect.');
 const admin=await bootstrapAdmin(secrets,phone,password);
 if(admin)await db.prepare('UPDATE phone_accounts SET admin=1,unlimited=1,updated=? WHERE phone_hash=? AND user_id=?').bind(now,hash,existing.user_id).run();
 const count=await db.prepare('SELECT count(*) AS total FROM devices WHERE user=? AND expires>?').bind(existing.user_id,now).first<{total:number}>();
 if((count?.total||0)>=5)throw new ApiError(409,'Cinq téléphones sont déjà associés. Retirez un ancien téléphone depuis le compte ou contactez le propriétaire.');
 const account=await db.prepare('SELECT display_name,unlimited,admin FROM phone_accounts WHERE user_id=?').bind(existing.user_id).first<{display_name:string;unlimited:number;admin:number}>();
 await db.prepare('INSERT INTO devices(id,user,token_hash,name,created,pending_until,expires,signal_hash) VALUES(?,?,?,?,?,?,?,?)').bind(id,existing.user_id,await digest(credential),deviceName||'Téléphone ROADGUARD',now,0,expires,deviceSignal).run();
 if(deviceSignal)await db.prepare('INSERT INTO device_signals(signal_hash,user_id,first_seen,last_seen) VALUES(?,?,?,?) ON CONFLICT(signal_hash,user_id) DO UPDATE SET last_seen=excluded.last_seen').bind(deviceSignal,existing.user_id,now,now).run();
 return response({status:'approved',id,accessToken:credential,displayName:account?.display_name||'',unlimited:!!account?.unlimited,admin:!!account?.admin,expires});
}
