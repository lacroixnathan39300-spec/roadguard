import {ApiError} from './errors.ts';
type Db=Pick<D1Database,'prepare'>;
export async function placesApi(db:Db,request:Request,now:number){
 const q=new URL(request.url).searchParams.get('q')?.normalize('NFKC').trim().replace(/\s+/g,' ');
 if(!q||q.length<2||q.length>120)throw new ApiError(400,'Saisissez un lieu entre 2 et 120 caractères.');
 const key=q.toLocaleLowerCase('fr'),cached=await db.prepare('SELECT value FROM geocode_cache WHERE query=? AND expires>?').bind(key,now).first<{value:string}>();
 const json=(value:string)=>new Response(value,{headers:{'Content-Type':'application/json','Cache-Control':'public, max-age=86400','X-Content-Type-Options':'nosniff'}});
 if(cached)return json(cached.value);
 // One shared row arbitrates ALL isolates: never a per-browser or per-process limiter.
 const permit=await db.prepare("INSERT INTO rate_limits(key,hits,until) VALUES('geocoder-upstream',1,?) ON CONFLICT(key) DO UPDATE SET until=excluded.until WHERE rate_limits.until<=? RETURNING key").bind(now+1200,now).first();
 if(!permit)throw new ApiError(429,'La recherche est occupée. Réessayez dans quelques secondes ou saisissez des coordonnées.');
 try{
  const upstream=await fetch('https://nominatim.openstreetmap.org/search?format=json&limit=5&accept-language=fr&q='+encodeURIComponent(q),{headers:{'User-Agent':'ROADGUARD/1.1 (+https://roadguard-community.lacroixnathan39300.chatgpt.site)','Accept':'application/json'},signal:AbortSignal.timeout(8000)});
  if(!upstream.ok)throw Error();const raw=await upstream.json() as {lat:string;lon:string;display_name:string}[];
  const places=raw.slice(0,5).filter(x=>Number.isFinite(Number(x.lat))&&Number.isFinite(Number(x.lon))).map(x=>({lat:Number(x.lat),lng:Number(x.lon),name:String(x.display_name).slice(0,300)})),value=JSON.stringify({places,attribution:'© OpenStreetMap contributors'});
  await db.prepare('INSERT INTO geocode_cache(query,value,expires) VALUES(?,?,?) ON CONFLICT(query) DO UPDATE SET value=excluded.value,expires=excluded.expires').bind(key,value,now+2592000000).run();return json(value);
 }catch{throw new ApiError(503,'La recherche de lieux est indisponible. Essayez les coordonnées.');}
}
