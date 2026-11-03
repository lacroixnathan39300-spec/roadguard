// Cache only public static assets. Never cache identity, writes or member positions.
const VERSION='roadguard-shell-v1';
self.addEventListener('install',e=>{e.waitUntil(caches.open(VERSION).then(c=>c.addAll(['/','/favicon.svg','/vendor/leaflet/leaflet.js','/vendor/leaflet/leaflet.css'])));self.skipWaiting();});
self.addEventListener('activate',e=>{e.waitUntil(self.clients.claim());});
self.addEventListener('fetch',e=>{
 const u=new URL(e.request.url);if(e.request.method!=='GET'||u.origin!==self.location.origin||u.pathname.startsWith('/api/')||u.pathname.includes('chatgpt')||u.pathname==='/callback')return;
 // HTML and JS shells are public and contain no identity. API responses are intentionally excluded.
 if(e.request.mode==='navigate'&&u.pathname==='/')e.respondWith(fetch(e.request).then(r=>{if(r.ok){const copy=r.clone();caches.open(VERSION).then(c=>c.put('/',copy));}return r;}).catch(()=>caches.match('/').then(r=>r||new Response('Connectez-vous une première fois pour préparer ROADGUARD.',{headers:{'Content-Type':'text/plain; charset=utf-8'}}))));
 else if(u.pathname.startsWith('/_next/static/')||u.pathname.startsWith('/assets/')||u.pathname.startsWith('/vendor/')||u.pathname==='/roadguard-scene.png')e.respondWith(caches.match(e.request).then(r=>r||fetch(e.request).then(response=>{if(response.ok){const copy=response.clone();caches.open(VERSION).then(c=>c.put(e.request,copy));}return response;})));
});

self.addEventListener('message',e=>{if(e.data?.type!=='CACHE_PUBLIC_SHELL'||!Array.isArray(e.data.urls))return;const urls=e.data.urls.slice(0,50).filter(v=>{try{const u=new URL(v);return u.origin===self.location.origin&&(u.pathname.startsWith('/_next/static/')||u.pathname.startsWith('/vendor/'));}catch{return false;}});e.waitUntil(caches.open(VERSION).then(c=>Promise.allSettled(urls.map(u=>c.add(u)))));});
