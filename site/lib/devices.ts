import {ApiError} from './errors.ts';
type Db=Pick<D1Database,'prepare'|'batch'>;
type User={userId:string}|null;
type Device={id:string;user:string|null;name:string;created:number;pending_until:number;expires:number;signal_hash:string|null};
export async function digest(value:string){const b=await crypto.subtle.digest('SHA-256',new TextEncoder().encode(value));return Array.from(new Uint8Array(b)).map(x=>x.toString(16).padStart(2,'0')).join('');}
export async function signalHash(secret:string|undefined,value:unknown){if(!secret||typeof value!=='string'||!/^([0-9a-f]{16}|[0-9a-f]{32})$/i.test(value))return null;const key=await crypto.subtle.importKey('raw',new TextEncoder().encode(secret),{name:'HMAC',hash:'SHA-256'},false,['sign']);const bytes=await crypto.subtle.sign('HMAC',key,new TextEncoder().encode('roadguard-android-id-v1:'+value.toLowerCase()));return Array.from(new Uint8Array(bytes)).map(x=>x.toString(16).padStart(2,'0')).join('');}
function token(req:Request){const t=req.headers.get('authorization')?.match(/^Bearer (rg_[A-Za-z0-9_-]{43})$/);return t?.[1]??null;}
async function tokenDevice(db:Db,req:Request){const t=token(req);return t?await db.prepare('SELECT id,user,name,created,pending_until,expires FROM devices WHERE token_hash=?').bind(await digest(t)).first<Device>():null;}
export async function nativeIdentity(db:Db,req:Request,now:number):Promise<User>{const d=await tokenDevice(db,req);return d?.user&&d.expires>now?{userId:d.user}:null;}
export async function deviceApi(db:Db,req:Request,browserUser:User,path:string,body:()=>Promise<Record<string,unknown>>,now:number,secret?:string):Promise<Response|null>{
 if(!path.startsWith('devices'))return null;
 const json=(v:unknown,status=200)=>Response.json(v,{status,headers:{'Cache-Control':'no-store','X-Content-Type-Options':'nosniff','Referrer-Policy':'no-referrer'}});
 if(path==='devices/enroll'&&req.method==='POST'){
  const b=await body();if(typeof b.name!=='string'||b.name.length<1||b.name.length>60)throw new ApiError(400,'Nom du téléphone invalide.');
  const ip=req.headers.get('cf-connecting-ip')||'not-provided',window=Math.floor(now/300000),key='enroll:'+window+':'+await digest(ip+':'+Math.floor(now/86400000));
  const limited=await db.prepare('INSERT INTO rate_limits(key,hits,until) VALUES(?,1,?) ON CONFLICT(key) DO UPDATE SET hits=hits+1 WHERE hits<10 RETURNING key').bind(key,(window+1)*300000).first();
  if(!limited)throw new ApiError(429,'Trop de demandes d’association. Réessayez dans cinq minutes.');
  const random=crypto.getRandomValues(new Uint8Array(32)),accessToken='rg_'+btoa(String.fromCharCode(...random)).replace(/\+/g,'-').replace(/\//g,'_').replace(/=/g,'');
  const id=crypto.randomUUID(),pendingUntil=now+600000,signal=await signalHash(secret,b.deviceSignal);
  await db.batch([
   db.prepare('INSERT INTO devices(id,user,token_hash,name,created,pending_until,expires,signal_hash) VALUES(?,NULL,?,?,?,?,0,?)').bind(id,await digest(accessToken),b.name.replace(/[\x00-\x1f]/g,''),now,pendingUntil,signal),
   db.prepare('DELETE FROM devices WHERE id IN(SELECT id FROM devices WHERE user IS NULL AND pending_until<? LIMIT 100)').bind(now),
   db.prepare('DELETE FROM rate_limits WHERE key IN(SELECT key FROM rate_limits WHERE until<? LIMIT 100)').bind(now)
  ]);
  return json({id,accessToken,code:id.slice(0,8).toUpperCase(),pendingUntil,approvalPath:'/?pair='+id},201);
 }
 if(path==='devices/session'&&req.method==='GET'){
  const d=await tokenDevice(db,req);if(!d)throw new ApiError(401,'Association introuvable ou révoquée.');
  if(d.user&&d.expires>now){const account=await db.prepare('SELECT display_name,unlimited FROM phone_accounts WHERE user_id=?').bind(d.user).first<{display_name:string;unlimited:number}>();return json({status:'approved',id:d.id,name:d.name,displayName:account?.display_name||'',unlimited:!!account?.unlimited,expires:d.expires});}
  if(!d.user&&d.pending_until>now)return json({status:'pending',id:d.id,pendingUntil:d.pending_until});
  throw new ApiError(401,'Cette association a expiré.');
 }
 if(path==='devices'&&req.method==='GET'){
  if(!browserUser)throw new ApiError(401,'Connectez-vous avec ChatGPT pour gérer vos appareils.');
  const r=await db.prepare('SELECT id,name,created,expires FROM devices WHERE user=? AND expires>? ORDER BY created DESC LIMIT 10').bind(browserUser.userId,now).all();return json({devices:r.results});
 }
 const match=path.match(/^devices\/([0-9a-f-]{36})(\/approve)?$/i);if(!match)return null;
 const id=match[1];
 if(!match[2]&&req.method==='GET'){
  const d=await db.prepare('SELECT id,name,pending_until,user FROM devices WHERE id=?').bind(id).first<Device>();
  if(!d||!d.user&&d.pending_until<=now)throw new ApiError(404,'Cette demande a expiré. Relancez l’association sur le téléphone.');
  return json({id,name:d.name,code:id.slice(0,8).toUpperCase(),status:d.user?'approved':'pending',pendingUntil:d.pending_until});
 }
 if(match[2]&&req.method==='POST'){
  if(!browserUser)throw new ApiError(401,'Connectez-vous avec ChatGPT pour autoriser ce téléphone.');
  const b=await body();if(b.consent!==true||b.code!==id.slice(0,8).toUpperCase())throw new ApiError(400,'Vérifiez le code affiché sur votre téléphone.');
  const account=await db.prepare('SELECT user_id FROM phone_accounts WHERE user_id=?').bind(browserUser.userId).first();
  if(!account)throw new ApiError(409,'Enregistrez d’abord votre numéro sur le site. Le numéro n’est pas vérifié par SMS.');
  const updated=await db.prepare('UPDATE devices SET user=?,expires=? WHERE id=? AND user IS NULL AND pending_until>? AND (SELECT count(*) FROM devices WHERE user=? AND expires>?)<5 RETURNING id').bind(browserUser.userId,now+2592000000,id,now,browserUser.userId,now).first();
  if(!updated)throw new ApiError(409,'Demande expirée, déjà traitée ou limite de cinq téléphones atteinte.');
  const device=await db.prepare('SELECT signal_hash FROM devices WHERE id=? AND user=?').bind(id,browserUser.userId).first<{signal_hash:string|null}>();
  if(device?.signal_hash)await db.prepare('INSERT INTO device_signals(signal_hash,user_id,first_seen,last_seen) VALUES(?,?,?,?) ON CONFLICT(signal_hash,user_id) DO UPDATE SET last_seen=excluded.last_seen').bind(device.signal_hash,browserUser.userId,now,now).run();
  return json({ok:true});
 }
 if(!match[2]&&req.method==='DELETE'){
  const current=await tokenDevice(db,req);if(!browserUser&&current?.id===id){await db.prepare('DELETE FROM devices WHERE id=?').bind(id).run();return json({ok:true});}const uid=browserUser?.userId||(current?.id===id&&current.expires>now?current.user:null);
  if(!uid)throw new ApiError(401,'Connectez-vous pour retirer cet appareil.');
  const deleted=await db.prepare('DELETE FROM devices WHERE id=? AND user=? RETURNING id').bind(id,uid).first();
  if(!deleted)throw new ApiError(404,'Téléphone introuvable.');
  return json({ok:true});
 }
 return null;
}
