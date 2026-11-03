// Pure module: tested against SQLite with the same D1 query interface.
export const KINDS=['fixed','mobile','control','redlight','danger','accident','stopped','traffic','obstacle','works','other'] as const;
const PERMANENT=253402300799000, LIVE_CONTROL=2*3600000;
const lasting=(kind:string)=>['fixed','mobile','control','redlight'].includes(kind);
export const SITE_ORIGIN='https://roadguard-community.lacroixnathan39300.chatgpt.site';
import {ApiError} from './errors.ts';
export {ApiError} from './errors.ts';
import {deviceApi,nativeIdentity} from './devices.ts';
import {placesApi} from './places.ts';
type Db=Pick<D1Database,'prepare'|'batch'>;
type Row={id:string;kind:string;lat:number;lng:number;note:string;created:number;updated:number;expires:number;status:string;confirmations?:number;cleared?:number;flags?:number;lastConfirmed?:number;reporterName?:string};
export type Identity={userId:string}|null;
const fields='r.id,r.kind,r.lat,r.lng,r.note,r.created,r.updated,r.expires,r.status';
const scores=",(SELECT count(*) FROM votes v WHERE v.report_id=r.id AND v.value='seen') confirmations,(SELECT count(*) FROM votes v WHERE v.report_id=r.id AND v.value='gone') cleared,(SELECT count(*) FROM votes v WHERE v.report_id=r.id AND v.value='flag') flags,coalesce((SELECT max(v.updated) FROM votes v WHERE v.report_id=r.id AND v.value='seen'),r.created) lastConfirmed,coalesce((SELECT json_extract(p.value,'$.displayName') FROM preferences p WHERE p.user=r.owner),'') reporterName";
function requireUser(u:Identity){if(!u)throw new ApiError(401,'Connectez-vous pour contribuer.');return u.userId;}
function num(v:unknown,min:number,max:number,label:string){if(typeof v!=='number'||!Number.isFinite(v)||v<min||v>max)throw new ApiError(400,label+' invalide.');return v;}
function coords(b:Record<string,unknown>){return {lat:num(b.lat,-85,85,'Latitude'),lng:num(b.lng,-180,180,'Longitude')};}
function box(url:URL){
 const raw=url.searchParams.get('bbox');
 if(!raw||!/^-?[\d.]+,-?[\d.]+,-?[\d.]+,-?[\d.]+$/.test(raw))throw new ApiError(400,'Une zone bbox ouest,sud,est,nord est nécessaire.');
 const [west,south,east,north]=raw.split(',').map(Number);
 num(west,-180,180,'Ouest');num(east,-180,180,'Est');num(south,-85,85,'Sud');num(north,-85,85,'Nord');
 if(east<=west||north<=south||east-west>2||north-south>2)throw new ApiError(400,'Rapprochez la carte pour consulter une zone de moins de 2 degrés.');
 return {west,south,east,north};
}
function publicRow(r:Row){const {id,kind,lat,lng,note,created,updated,status,confirmations,cleared,flags}=r;const lastConfirmed=r.lastConfirmed??created;const expires=lasting(kind)?PERMANENT:r.expires;return {id,kind,lat,lng,note,created,updated,expires,status,confirmations:confirmations??0,cleared:cleared??0,flags:flags??0,lastConfirmed,reporterName:r.reporterName||'',presenceCurrent:!['mobile','control'].includes(kind)||Date.now()-lastConfirmed<LIVE_CONTROL};}
export function cleanPreferences(b:Record<string,unknown>){
 const choose=(key:string,allowed:string[],fallback:string)=>typeof b[key]==='string'&&allowed.includes(b[key] as string)?b[key] as string:fallback;
 const displayName=typeof b.displayName==='string'?b.displayName.trim().replace(/[\x00-\x1f<>]/g,'').slice(0,32):'';
 return {accent:choose('accent',['#ff304f','#329dff','#ff942e','#1acb91','#a575ff','#e5c36b'],'#ff304f'),theme:choose('theme',['night','day','auto'],'night'),cursor:choose('cursor',['arrow','car','dot'],'arrow'),voice:typeof b.voice==='string'?b.voice.slice(0,200):'',speech:b.speech===true,members:b.members!==false,routeColor:choose('routeColor',['#329dff','#ff304f','#1acb91','#a575ff'],'#329dff'),menuPhoto:b.menuPhoto!==false,displayName};
}
async function payload(req:Request){
 if(!req.headers.get('content-type')?.includes('application/json'))throw new ApiError(415,'Utilisez le format JSON.');
 if(Number(req.headers.get('content-length')||0)>4096)throw new ApiError(413,'Requête trop longue.');
 const t=await req.text();if(t.length>4096)throw new ApiError(413,'Requête trop longue.');
 try{const b=JSON.parse(t);if(!b||typeof b!=='object'||Array.isArray(b))throw Error();return b as Record<string,unknown>;}catch{throw new ApiError(400,'JSON invalide.');}
}
export async function handleApi(db:Db,req:Request,user:Identity,config:{tileUrl?:string;tileAttribution?:string;deviceHashSecret?:string}={}){
 const url=new URL(req.url),path=url.pathname.replace(/^\/api\/v1\/?/,'').replace(/\/$/,''),method=req.method,now=Date.now();
 const json=(v:unknown,status=200,h:Record<string,string>={})=>Response.json(v,{status,headers:{'Cache-Control':'no-store','X-Content-Type-Options':'nosniff',...h}});
 if(method==='OPTIONS')return new Response(null,{status:204,headers:{'Access-Control-Allow-Origin':'*','Access-Control-Allow-Methods':'GET, OPTIONS','Access-Control-Allow-Headers':'If-None-Match'}});
 if(!['GET','HEAD'].includes(method)){
  const origin=req.headers.get('origin');
  if(origin&&origin!==url.origin&&origin!==SITE_ORIGIN)throw new ApiError(403,'Origine non autorisée.');
  if(req.headers.get('sec-fetch-site')==='cross-site')throw new ApiError(403,'Origine non autorisée.');
 }
 const deviceResponse=await deviceApi(db,req,user,path,()=>payload(req),now,config.deviceHashSecret);if(deviceResponse)return deviceResponse;
 const publicRead=method==='GET'&&['health','config','places'].includes(path);
 if(!user&&!publicRead)user=await nativeIdentity(db,req,now);
 if(method==='GET'&&['reports','snapshot','sync','presence'].includes(path))requireUser(user);
 if(path==='places'&&method==='GET')return placesApi(db,req,now);
 if(path==='config'&&method==='GET'){let tileUrl=config.tileUrl||'https://tile.openstreetmap.org/{z}/{x}/{y}.png';if(!tileUrl.startsWith('https://')||!['{z}','{x}','{y}'].every(k=>tileUrl.includes(k)))tileUrl='https://tile.openstreetmap.org/{z}/{x}/{y}.png';return json({tileUrl,tileAttribution:config.tileAttribution||'© OpenStreetMap contributors',osm:tileUrl.includes('tile.openstreetmap.org/')},200,{'Cache-Control':'public, max-age=300'});}
 if(path==='health'&&method==='GET'){await db.prepare('SELECT 1 ok').first();return json({ok:true,schema:1,reportRefreshSeconds:120,presenceRefreshSeconds:600,presenceExpiresSeconds:1200,capacityTargetConcurrent:500000,capacityValidated:false});}
 if(path==='session'&&method==='GET'){
  if(!user)return json({authenticated:false,preferences:null,presence:null});
  const [p,ps,lease]=await db.batch([db.prepare('SELECT value FROM preferences WHERE user=?').bind(user.userId),db.prepare('SELECT alias,expires,updated FROM presence WHERE user=? AND expires>?').bind(user.userId,now),db.prepare('SELECT lease,expires FROM presence_sessions WHERE user=? AND expires>?').bind(user.userId,now)]);
  const account=await db.prepare('SELECT unlimited,trial_started FROM phone_accounts WHERE user_id=?').bind(user.userId).first<{unlimited:number;trial_started:number}>();
  return json({authenticated:true,preferences:p.results[0]?JSON.parse(String((p.results[0] as {value:string}).value)):null,presence:ps.results[0]??null,presenceLease:(lease.results[0] as {lease:string}|undefined)?.lease??null,unlimited:!!account?.unlimited,trialStarted:account?.trial_started??null});
 }
 if(path==='preferences'&&method==='PUT'){
  const uid=requireUser(user),input=await payload(req),b=cleanPreferences(input);
  if(typeof input.displayName!=='string'){
   const prior=await db.prepare('SELECT json_extract(value,\'$.displayName\') name FROM preferences WHERE user=?').bind(uid).first<{name:string}>();
   b.displayName=prior?.name||'';
  }
  await db.prepare('INSERT INTO preferences(user,value,updated) VALUES(?,?,?) ON CONFLICT(user) DO UPDATE SET value=excluded.value,updated=excluded.updated').bind(uid,JSON.stringify(b),now).run();return json({preferences:b});
 }
 if((path==='reports'||path==='snapshot')&&method==='GET'){
  const {west,south,east,north}=box(url),cursor=url.searchParams.get('cursor')||'';
  if(cursor.length>180)throw new ApiError(400,'Curseur invalide.');
  let afterLat=-90,afterLng=-180,afterId='';
  if(cursor){try{if(cursor.startsWith('p.')){const parts=JSON.parse(atob(cursor.slice(2).replace(/-/g,'+').replace(/_/g,'/')));afterLat=num(parts[0],-85,85,'Latitude');afterLng=num(parts[1],-180,180,'Longitude');if(typeof parts[2]!=='string'||parts[2].length>80)throw Error();afterId=parts[2];}else{const previous=await db.prepare('SELECT lat,lng FROM reports WHERE id=?').bind(cursor).first<{lat:number;lng:number}>();if(!previous)throw Error();afterLat=previous.lat;afterLng=previous.lng;afterId=cursor;}}catch{throw new ApiError(400,'Curseur invalide.');}}
  const result=await db.prepare(`SELECT ${fields}${scores} FROM reports r WHERE r.lat BETWEEN ? AND ? AND r.lng BETWEEN ? AND ? AND r.status='active' AND (r.expires>? OR r.kind IN ('fixed','mobile','control','redlight')) AND (r.lat,r.lng,r.id)>(?,?,?) ORDER BY r.lat,r.lng,r.id LIMIT 201`).bind(south,north,west,east,now,afterLat,afterLng,afterId).all<Row>();
  const more=result.results.length>200,rows=result.results.slice(0,200).map(publicRow),stamp=rows.map(r=>[r.id,r.updated,r.confirmations,r.cleared].join(':')).join('|');
  const hash=await crypto.subtle.digest('SHA-256',new TextEncoder().encode(stamp+':'+[west,south,east,north,cursor,String(more)].join(',')));
  const etag='W/"'+Array.from(new Uint8Array(hash)).map(x=>x.toString(16).padStart(2,'0')).join('')+'"';
  const h={'ETag':etag,'Cache-Control':'public, max-age=15, must-revalidate','Access-Control-Allow-Origin':'*','Access-Control-Expose-Headers':'ETag'};
  if(req.headers.get('if-none-match')===etag)return new Response(null,{status:304,headers:h});
  return json({schema:1,generatedAt:now,bbox:[west,south,east,north],reports:rows,nextCursor:more?'p.'+btoa(JSON.stringify([rows[rows.length-1].lat,rows[rows.length-1].lng,rows[rows.length-1].id])).replace(/\+/g,'-').replace(/\//g,'_').replace(/=/g,''):null,refreshSeconds:120},200,h);
 }
 if(path==='sync'&&method==='GET'){
  const {west,south,east,north}=box(url),since=Number(url.searchParams.get('since')??0);
  if(!Number.isSafeInteger(since)||since<0)throw new ApiError(400,'Version invalide.');
  const bounds=await db.prepare('SELECT (SELECT MIN(revision) FROM events) min,(SELECT MAX(revision) FROM events) max').first<{min:number|null;max:number|null}>();
  if(since&&bounds?.min&&since<bounds.min)return json({resetRequired:true},409);
  const upper=bounds?.max??0;if(since>upper)return json({resetRequired:true},409);
  const result=await db.prepare('SELECT revision,payload FROM events WHERE revision>? AND revision<=? AND lat BETWEEN ? AND ? AND lng BETWEEN ? AND ? ORDER BY revision LIMIT 201').bind(since,upper,south,north,west,east).all<{revision:number;payload:string}>();
  const more=result.results.length>200,page=result.results.slice(0,200);
  return json({schema:1,changes:page.map(r=>({revision:r.revision,...JSON.parse(r.payload)})),since:more?page[page.length-1].revision:upper,hasMore:more,refreshSeconds:120},200,{'Access-Control-Allow-Origin':'*'});
 }
 if(path==='reports'&&method==='POST'){
  const uid=requireUser(user),b=await payload(req),point=coords(b);
  if(!KINDS.includes(b.kind as typeof KINDS[number]))throw new ApiError(400,'Type de signalement invalide.');
  if(typeof b.note!=='string'||b.note.trim().length>180)throw new ApiError(400,'Le commentaire doit contenir au plus 180 caractères.');
  if(num(b.accuracy,0,10000,'Précision')>100)throw new ApiError(400,'Une position GPS à moins de 100 m est nécessaire.');
  const key=req.headers.get('idempotency-key');
  if(!key||!/^[0-9a-f-]{36}$/i.test(key))throw new ApiError(400,'Clé de signalement invalide.');
  const old=await db.prepare('SELECT owner FROM reports WHERE id=?').bind(key).first<{owner:string}>();
  if(old){if(old.owner!==uid)throw new ApiError(409,'Identifiant déjà utilisé.');return json({id:key,duplicate:true});}
  const kind=String(b.kind),expires=lasting(kind)?PERMANENT:now+7200000;
  const author=await db.prepare('SELECT json_extract(value,\'$.displayName\') name FROM preferences WHERE user=?').bind(uid).first<{name:string}>();
  const r={id:key,kind,...point,note:b.note.trim(),created:now,updated:now,expires,status:'active',reporterName:author?.name||''};
  const result=await db.batch([
   db.prepare("INSERT INTO reports(id,owner,kind,lat,lng,note,created,updated,expires,status) SELECT ?,?,?,?,?,?,?,?,?,? WHERE (SELECT count(*) FROM reports WHERE owner=? AND created>?)<5 RETURNING id").bind(key,uid,kind,point.lat,point.lng,r.note,now,now,expires,'active',uid,now-300000),
   db.prepare('INSERT INTO events(report_id,lat,lng,payload,created) SELECT ?,?,?,?,? WHERE changes()>0').bind(key,point.lat,point.lng,JSON.stringify({action:'upsert',report:r}),now)
  ]);
  if(!result[0].results.length)return json({error:'Limite de 5 signalements en 5 minutes atteinte.'},429,{'Retry-After':'300'});
  await db.prepare('DELETE FROM events WHERE revision IN (SELECT revision FROM events WHERE created<? LIMIT 200)').bind(now-604800000).run();
  await db.prepare('DELETE FROM devices WHERE id IN(SELECT id FROM devices WHERE user IS NOT NULL AND expires<? LIMIT 20)').bind(now).run();
  return json({report:publicRow(r)},201);
 }
 if(path==='me/reports'&&method==='GET'){
  const uid=requireUser(user),result=await db.prepare(`SELECT ${fields}${scores} FROM reports r WHERE r.owner=? ORDER BY r.created DESC LIMIT 100`).bind(uid).all<Row>();
  return json({reports:result.results.map(publicRow)});
 }
 const vote=path.match(/^reports\/([0-9a-f-]{36})\/vote$/i);
 if(vote&&method==='POST'){
  const uid=requireUser(user),b=await payload(req),id=vote[1];
  if(!['seen','gone','flag'].includes(String(b.value)))throw new ApiError(400,'Avis invalide.');
  const r=await db.prepare('SELECT owner,kind,expires,status FROM reports WHERE id=?').bind(id).first<{owner:string;kind:string;expires:number;status:string}>();
  if(!r||(r.expires<=now&&!lasting(r.kind))||r.status!=='active')throw new ApiError(404,'Ce signalement a expiré.');
  if(r.owner===uid)throw new ApiError(400,'Vous ne pouvez pas confirmer votre propre signalement.');
  const result=await db.batch([
   db.prepare('INSERT INTO votes(report_id,user,value,updated) VALUES(?,?,?,?) ON CONFLICT(report_id,user) DO UPDATE SET value=excluded.value,updated=excluded.updated WHERE votes.updated<? RETURNING report_id').bind(id,uid,b.value,now,now-30000),
   db.prepare("UPDATE reports SET expires=?,updated=? WHERE id=? AND kind IN ('fixed','mobile','control','redlight') AND ?='seen' AND EXISTS(SELECT 1 FROM votes WHERE report_id=? AND user=? AND value='seen' AND updated=?)").bind(PERMANENT,now,id,b.value,id,uid,now),
   db.prepare("INSERT INTO events(report_id,lat,lng,payload,created) SELECT r.id,r.lat,r.lng,CASE WHEN r.status!='active' THEN json_object('action','delete','id',r.id) ELSE json_object('action','upsert','report',json_object('id',r.id,'kind',r.kind,'lat',r.lat,'lng',r.lng,'note',r.note,'created',r.created,'updated',r.updated,'expires',r.expires,'status',r.status,'confirmations',(SELECT count(*) FROM votes v WHERE v.report_id=r.id AND v.value='seen'),'cleared',(SELECT count(*) FROM votes v WHERE v.report_id=r.id AND v.value='gone'))) END,? FROM reports r WHERE r.id=? AND EXISTS(SELECT 1 FROM votes WHERE report_id=? AND user=? AND updated=?)").bind(now,id,id,uid,now)
  ]);
  if(!result[0].results.length)return json({error:'Attendez 30 secondes avant de modifier votre avis.'},429,{'Retry-After':'30'});
  return json({ok:true});
 }
 const reportId=path.match(/^reports\/([0-9a-f-]{36})$/i);
 if(reportId&&method==='DELETE'){
  const uid=requireUser(user),id=reportId[1],row=await db.prepare('SELECT lat,lng FROM reports WHERE id=? AND owner=?').bind(id,uid).first<{lat:number;lng:number}>();
  if(!row)throw new ApiError(404,'Signalement introuvable.');
  await db.batch([db.prepare("UPDATE reports SET status='deleted',note='',updated=?,expires=? WHERE id=? AND owner=?").bind(now,now,id,uid),db.prepare('INSERT INTO events(report_id,lat,lng,payload,created) VALUES(?,?,?,?,?)').bind(id,row.lat,row.lng,JSON.stringify({action:'delete',id}),now)]);
  return json({ok:true});
 }
 if(path==='presence'&&method==='GET'){
  const {west,south,east,north}=box(url),zoom=Number(url.searchParams.get('zoom')||12);if(!Number.isFinite(zoom))throw new ApiError(400,'Zoom invalide.');
  if(zoom>=14){
   const result=await db.prepare('SELECT public_id id,alias,lat,lng,updated,expires FROM presence WHERE lat BETWEEN ? AND ? AND lng BETWEEN ? AND ? AND expires>? ORDER BY lat,lng,expires LIMIT 201').bind(south,north,west,east,now).all();
   return json({members:result.results.slice(0,200),clusters:[],truncated:result.results.length>200,refreshSeconds:600});
  }
  const grid=zoom>=11?.03:.1;
  const result=await db.prepare('SELECT round(lat/?) * ? lat,round(lng/?) * ? lng,count(*) count,min(expires) expires FROM presence WHERE lat BETWEEN ? AND ? AND lng BETWEEN ? AND ? AND expires>? GROUP BY round(lat/?),round(lng/?) LIMIT 201').bind(grid,grid,grid,grid,south,north,west,east,now,grid,grid).all();
  return json({members:[],clusters:result.results.slice(0,200),truncated:result.results.length>200,refreshSeconds:600});
 }
 if(path==='presence'&&method==='PUT'){
  const uid=requireUser(user),b=await payload(req),point=coords(b);
  if(b.consent!==true)throw new ApiError(400,'Activez volontairement la visibilité pour partager votre position.');
  num(b.accuracy,0,1000,'Précision GPS');
  let lease:string;
  if(b.start===true){lease=crypto.randomUUID();await db.prepare('INSERT INTO presence_sessions(user,lease,expires) VALUES(?,?,?) ON CONFLICT(user) DO UPDATE SET lease=excluded.lease,expires=excluded.expires').bind(uid,lease,now+2592000000).run();}
  else{const session=await db.prepare('SELECT lease FROM presence_sessions WHERE user=? AND expires>?').bind(uid,now).first<{lease:string}>();if(!session||typeof b.lease!=='string'||session.lease!==b.lease)throw new ApiError(409,'La visibilité a été désactivée ou reprise sur un autre appareil. Activez-la à nouveau volontairement.');lease=session.lease;}
  const lat=Math.round(point.lat/.003)*.003,lng=Math.round(point.lng/.003)*.003;
  const old=await db.prepare('SELECT updated FROM presence WHERE user=?').bind(uid).first<{updated:number}>();
  if(old&&now-old.updated<600000)return json({ok:true,lease,nextUpdate:old.updated+600000,expires:old.updated+1200000});
  const id=crypto.randomUUID(),alias='Conducteur '+id.slice(0,4).toUpperCase();
  const changed=await db.prepare('INSERT INTO presence(user,public_id,alias,lat,lng,updated,expires) SELECT ?,?,?,?,?,?,? WHERE EXISTS(SELECT 1 FROM presence_sessions WHERE user=? AND lease=? AND expires>?) ON CONFLICT(user) DO UPDATE SET public_id=excluded.public_id,alias=excluded.alias,lat=excluded.lat,lng=excluded.lng,updated=excluded.updated,expires=excluded.expires WHERE presence.updated<=? AND EXISTS(SELECT 1 FROM presence_sessions WHERE user=? AND lease=? AND expires>?) RETURNING user').bind(uid,id,alias,lat,lng,now,now+1200000,uid,lease,now,now-600000,uid,lease,now).first();
  if(!changed)throw new ApiError(409,'La visibilité a changé. Réessayez après vérification de vos réglages.');
  await db.prepare('DELETE FROM presence WHERE user IN (SELECT user FROM presence WHERE expires<? LIMIT 100)').bind(now).run();
  return json({ok:true,alias,lease,nextUpdate:now+600000,expires:now+1200000});
 }
 if(path==='presence'&&method==='DELETE'){
  const uid=requireUser(user);await db.batch([db.prepare('DELETE FROM presence_sessions WHERE user=?').bind(uid),db.prepare('DELETE FROM presence WHERE user=?').bind(uid)]);return json({ok:true});
 }
 throw new ApiError(404,'Cette ressource est introuvable.');
}
