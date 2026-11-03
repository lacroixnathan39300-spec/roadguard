import type {Radar} from './types';
export type InventoryManifest={schema:number;count:number;version:string;bytes:number;url:string;sha256:string};
export async function loadInventory(force=false){
 const cache=await caches.open('roadguard-data-v1').catch(()=>({match:async(_key:string)=>undefined,put:async(_key:string,_value:Response)=>{},keys:async()=>[] as Request[],delete:async(_key:string|Request)=>false}));
 const oldResponse=await cache.match('/data/manifest.json');
 let previous:InventoryManifest|null=null;try{previous=oldResponse?await oldResponse.json() as InventoryManifest:null;}catch{}
 let manifest:InventoryManifest,stale=false;
 try{const r=await fetch('/data/manifest.json',{cache:'no-cache'});if(!r.ok)throw Error();manifest=await r.json() as InventoryManifest;}
 catch{if(!previous)throw Error('Connectez-vous une première fois pour télécharger les données.');manifest=previous;stale=true;}
 const read=async(m:InventoryManifest,forceNetwork:boolean)=>{
  const cached=await cache.match(m.url);let response=cached;
  if(forceNetwork||!response){try{const r=await fetch(m.url);if(!r.ok)throw Error();response=r;}catch{if(!cached)throw Error('Le téléchargement de l’inventaire a échoué.');response=cached;stale=true;}}
  const bytes=await response!.arrayBuffer(),hash=await crypto.subtle.digest('SHA-256',bytes),hex=Array.from(new Uint8Array(hash)).map(x=>x.toString(16).padStart(2,'0')).join('');
  if(hex!==m.sha256)throw Error('Le fichier téléchargé est incomplet. Relancez la synchronisation.');
  return bytes;
 };
 let bytes:ArrayBuffer;
 try{bytes=await read(manifest,force);}catch(e){if(!previous||previous.url===manifest.url)throw e;manifest=previous;stale=true;bytes=await read(previous,false);}
 // Publish the manifest only after complete byte verification, preserving the last good version on failure.
 await cache.put(manifest.url,new Response(bytes,{headers:{'Content-Type':'application/json'}}));
 await cache.put('/data/manifest.json',Response.json(manifest));
 const keys=await cache.keys();for(const k of keys)if(new URL(k.url).pathname.startsWith('/data/radars-')&&!k.url.endsWith(manifest.url))await cache.delete(k);
 return {radars:JSON.parse(new TextDecoder().decode(bytes)) as Radar[],manifest,stale};
}
