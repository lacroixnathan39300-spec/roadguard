</script><script>function setRoadguardNight(enabled){
 document.body.classList.toggle('rg-night',!!enabled);
 // La coloration CSS ne concerne que les images de tuiles : marqueurs, routes et controles gardent leurs couleurs.
}let map=L.map('map',{zoomControl:true,fadeAnimation:true,zoomAnimation:true,markerZoomAnimation:true}).setView([46.75,5.90],11);
// WebView may resize after the Android navigation panel is laid out.
// Recompute tile bounds and request missing tiles after every real resize.
let rgLastWidth=0,rgLastHeight=0;
function rgSyncMapSize(){
 const node=document.getElementById('map');if(!node||!map)return;
 const width=node.clientWidth,height=node.clientHeight;
 if(width<100||height<100)return;
 if(width!==rgLastWidth||height!==rgLastHeight){
  rgLastWidth=width;rgLastHeight=height;
  map.invalidateSize({pan:false,animate:false});
 }
}
window.addEventListener('resize',rgSyncMapSize);
window.addEventListener('orientationchange',()=>setTimeout(rgSyncMapSize,250));
if(window.ResizeObserver)new ResizeObserver(rgSyncMapSize).observe(document.getElementById('map'));
setInterval(rgSyncMapSize,1500);
setTimeout(()=>map.invalidateSize({pan:false}),350);
setTimeout(()=>map.invalidateSize({pan:false}),1200);
let rgFirstGpsFix=true;
let rgFilteredGps=null,rgFilteredAt=0;
function rgFilterGps(lat,lon,accuracy,speedKmh){
 const now=Date.now(),raw=[Number(lat),Number(lon)],acc=Math.max(0,Number(accuracy)||0);
 if(!rgFilteredGps){rgFilteredGps=raw;rgFilteredAt=now;return raw;}
 const elapsed=Math.max(.1,(now-rgFilteredAt)/1000),distance=map.distance(rgFilteredGps,raw);
 const maxTravel=Math.max(20,Math.max(0,Number(speedKmh)||0)/3.6*elapsed*2.4+Math.min(60,acc));
 // Reject implausible GPS teleportation while keeping genuine relocation possible after long gaps.
 if(elapsed<12&&distance>maxTravel)return rgFilteredGps;
 const alpha=distance>100||elapsed>8?1:Math.min(.72,Math.max(.22,elapsed/3));
 rgFilteredGps=[rgFilteredGps[0]+(raw[0]-rgFilteredGps[0])*alpha,rgFilteredGps[1]+(raw[1]-rgFilteredGps[1])*alpha];
 rgFilteredAt=now;return rgFilteredGps;
}

let rgTileFailures=0;let rgTileSuccesses=0;let rgTileRequests=0;let rgTileWarningAt=0;
const rgTileNotice=document.createElement('div');
rgTileNotice.id='rg-tile-notice';rgTileNotice.setAttribute('role','status');
rgTileNotice.style.cssText='display:none;position:absolute;top:110px;left:12px;right:12px;z-index:1100;background:#10273eea;color:#e7f8ff;border:1px solid #4a9dbc;border-radius:12px;padding:10px;font:12px Arial;pointer-events:none';
document.body.appendChild(rgTileNotice);
function rgTileStatus(msg){rgTileNotice.textContent=msg;rgTileNotice.style.display=msg?'block':'none';}

const rgHeadingCamera={enabled:false,angle:0,target:0,lastFrame:0,frame:0,lastRendered:null,lastAppliedX:null,lastAppliedY:null};
// ROADGUARD 11.12.4: a rotated Leaflet viewport exposes corners outside the
// original rectangular tile grid. Request only the tile footprint needed for
// the current bearing, plus a small safety margin; never bulk-prefetch OSM.
const rgOriginalTileBounds=L.GridLayer.prototype._getTiledPixelBounds;
L.GridLayer.prototype._getTiledPixelBounds=function(center){
 const bounds=rgOriginalTileBounds.call(this,center);
 if(!rgHeadingCamera||!rgHeadingCamera.enabled)return bounds;
 const radians=rgHeadingCamera.angle*Math.PI/180;
 const co=Math.abs(Math.cos(radians)),si=Math.abs(Math.sin(radians));
 const size=bounds.getSize();
 // Cover the entire rotated viewport at ANY heading before a turn begins.
 // Its diagonal is the minimum safe square bounding every rotation of the view.
 // This is viewport coverage, NOT speculative download of distant map areas.
 const diagonal=Math.hypot(size.x,size.y);
 const safety=this.options && this.options.rgLocalPack?1.14:1.035;
 const side=diagonal*safety;
 const dx=Math.max(0,(side-size.x)/2);
 const dy=Math.max(0,(side-size.y)/2);
 return L.bounds(bounds.min.subtract([dx,dy]),bounds.max.add([dx,dy]));
};
let rgLastTileBearingRefresh=null;
function rgRefreshRotatedTileCoverage(force){
 if(!map||!rgHeadingCamera.enabled)return;
 const angle=rgHeadingCamera.angle;
 const delta=rgLastTileBearingRefresh===null?360:Math.abs(((angle-rgLastTileBearingRefresh+540)%360)-180);
 if(!force&&delta<8)return;
 rgLastTileBearingRefresh=angle;
 for(const layer of [rgLightTiles,rgRasterLayer]){
  if(layer&&map.hasLayer(layer)&&typeof layer._update==='function')layer._update();
 }
}
const rgLightTiles=L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{
 maxZoom:19,keepBuffer:9,updateWhenIdle:false,updateWhenZooming:false,updateInterval:100,attribution:'&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
});
// On-demand cache only with explicit provider permission; no OSM background prefetch.
function setLicensedEmergencyMap(enabled){
 if(!rgLightTiles)return;
 const template=enabled?'https://roadguard.local/emergency/{z}/{x}/{y}.png':'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png';
 // Experimental x10 retention relative to 11.13.95 (60 -> 600), authorised source only.
 // The public OSM source keeps its previous setting (9): no added invisible-tile requests.
 rgLightTiles.options.keepBuffer=enabled?600:9;
 rgLightTiles.setUrl(template);
 rgTileStatus(enabled?'Cache cartographique autorisé activé':'Carte en ligne');
}
// 11.13.91: the status is about connectivity, never a guarantee of offline tiles.
let rgNetworkOnline=true;
function setRoadguardNetworkState(online){
 rgNetworkOnline=!!online;
 if(!rgNetworkOnline){
  rgTileStatus('RÉSEAU COUPÉ · seules les tuiles déjà en cache peuvent apparaître');
 }else if(rgTileSuccesses>0){
  rgTileStatus('');
 }else{
  rgTileStatus('RÉSEAU DISPONIBLE · chargement des cartes visibles');
 }
}
rgLightTiles.on('tileloadstart',()=>{rgTileRequests++;});
rgLightTiles.on('tileerror',()=>{rgTileFailures++;if(rgTileFailures>=8&&rgTileSuccesses===0&&Date.now()-rgTileWarningAt>15000){rgTileWarningAt=Date.now();rgTileStatus('Certaines tuiles sont indisponibles. Vérifie Internet ou utilise une carte hors ligne.');}});
rgLightTiles.on('tileload',()=>{rgTileSuccesses++;rgTileFailures=0;if(rgNetworkOnline)rgTileStatus('');});
// Prechargement 7x7 uniquement pour un pack PMTiles local : ne pas precharger
// les serveurs publics OpenStreetMap (politique d'utilisation des tuiles).
// Local PMTiles only. Keep the 20 km background ring at a modest zoom:
// loading a 20 km radius at street-level zoom would require thousands of tiles.
const rgPrefetch={lastAt:0,lastKey:'',generation:0,active:new Map(),seen:new Map(),ringAt:0,ringCenter:null,ringZoom:-1,queue:[],queueIndex:0};
function rgQueueLocalTiles(candidates){
 if(!rgRasterLayer)return;
 const generation=rgPrefetch.generation;
 // Prioritize new tiles; retain a bounded queue and only 3 concurrent decodes.
 rgPrefetch.queue=candidates.slice(0,220);
 rgPrefetch.queueIndex=0;
 function pump(){
  if(generation!==rgPrefetch.generation||!rgRasterLayer)return;
  while(rgPrefetch.active.size<3&&rgPrefetch.queueIndex<rgPrefetch.queue.length){
   const item=rgPrefetch.queue[rgPrefetch.queueIndex++],key=item.tileKey;
   if(rgPrefetch.active.has(key)||rgPrefetch.seen.has(key))continue;
   const img=new Image();rgPrefetch.active.set(key,img);
   const finish=(ok)=>{rgPrefetch.active.delete(key);rgPrefetch.seen.set(key,Date.now()-(ok?0:45000));pump();};
   img.onload=()=>finish(true);img.onerror=()=>finish(false);
   img.src=rgRasterLayer.getTileUrl({x:item.x,y:item.y,z:item.z});
  }
 }
 pump();
}
function rgWarm20km(lat,lon){
 if(!rgRasterLayer||!Number.isFinite(lat)||!Number.isFinite(lon))return;
 const now=Date.now(),radiusMeters=20000;
 // Low-detail coverage is useful for orientation; nearby detailed tiles are
 // still handled by Leaflet and rgWarmAhead. Never touch public OSM here.
 const z=Math.min(12,Math.max(8,Math.round(map.getZoom())-3));
 const moved=rgPrefetch.ringCenter?map.distance([lat,lon],rgPrefetch.ringCenter):Infinity;
 if(z===rgPrefetch.ringZoom&&moved<1500&&now-rgPrefetch.ringAt<45000)return;
 if(now-rgPrefetch.ringAt<10000)return;
 rgPrefetch.ringAt=now;rgPrefetch.ringCenter=[lat,lon];rgPrefetch.ringZoom=z;
 const n=2**z,latRad=lat*Math.PI/180;
 const cx=Math.floor((lon+180)/360*n);
 const cy=Math.floor((1-Math.asinh(Math.tan(latRad))/Math.PI)/2*n);
 const tileMeters=40075016.686*Math.max(.05,Math.cos(latRad))/n;
 const r=Math.min(18,Math.ceil(radiusMeters/tileMeters)+1);
 const candidates=[];
 for(let dy=-r;dy<=r;dy++)for(let dx=-r;dx<=r;dx++){
  const d=Math.hypot(dx*tileMeters,dy*tileMeters);
  if(d>radiusMeters+tileMeters*1.5)continue;
  const x=((cx+dx)%n+n)%n,y=cy+dy;
  if(y<0||y>=n)continue;
  const tileKey=z+'/'+x+'/'+y;
  if(rgPrefetch.seen.has(tileKey)||rgPrefetch.active.has(tileKey))continue;
  candidates.push({tileKey,x,y,z,priority:d});
 }
 candidates.sort((a,b)=>a.priority-b.priority);
 // 20 km is the geographical target, not a promise that missing pack data
 // exists. Bound each cycle to avoid slowing the phone down.
 rgQueueLocalTiles(candidates.slice(0,180));
}

function rgWarmAhead(lat,lon,bearing,speedKmh){
 if(!rgRasterLayer||!Number.isFinite(Number(lat))||!Number.isFinite(Number(lon)))return;
 rgWarm20km(Number(lat),Number(lon));
 const speed=Number(speedKmh),dir=Number(bearing);
 if(!Number.isFinite(speed)||speed<12||!Number.isFinite(dir))return;
 const now=Date.now();if(now-rgPrefetch.lastAt<3000)return;
 const z=Math.round(map.getZoom());if(z<3||z>19)return;
 const distance=Math.min(850,Math.max(170,speed*5));
 const radians=dir*Math.PI/180;
 const aheadLat=Math.max(-85,Math.min(85,Number(lat)+Math.cos(radians)*distance/111320));
 const aheadLon=Number(lon)+Math.sin(radians)*distance/(111320*Math.max(.15,Math.cos(Number(lat)*Math.PI/180)));
 const projected=map.project([aheadLat,aheadLon],z),n=Math.pow(2,z);
 const x0=Math.floor(projected.x/256),y0=Math.floor(projected.y/256);
 const key=z+':'+x0+':'+y0;
 if(key===rgPrefetch.lastKey&&now-rgPrefetch.lastAt<11000)return;
 rgPrefetch.lastAt=now;rgPrefetch.lastKey=key;
 for(const [k,at] of rgPrefetch.seen)if(now-at>60000)rgPrefetch.seen.delete(k);
 // The record of recently handled tiles must remain bounded on multi-hour trips.
 while(rgPrefetch.seen.size>1200)rgPrefetch.seen.delete(rgPrefetch.seen.keys().next().value);
 const candidates=[];
 for(let dy=-3;dy<=3;dy++)for(let dx=-3;dx<=3;dx++){
  const x=((x0+dx)%n+n)%n,y=y0+dy;
  if(y<0||y>=n)continue;
  const tileKey=z+'/'+x+'/'+y;
  if(rgPrefetch.seen.has(tileKey)||rgPrefetch.active.has(tileKey))continue;
  // Centre et direction de marche avant couronne exterieure.
  candidates.push({tileKey,x,y,priority:dx*dx+dy*dy});
 }
 candidates.sort((a,b)=>a.priority-b.priority);
 const generation=rgPrefetch.generation;
 let index=0;
 function next(){
  if(generation!==rgPrefetch.generation||!rgRasterLayer)return;
  while(rgPrefetch.active.size<3&&index<candidates.length){
   const {tileKey,x,y}=candidates[index++];
   const img=new Image();rgPrefetch.active.set(tileKey,img);
   const cleanup=()=>{
    rgPrefetch.active.delete(tileKey);
    rgPrefetch.seen.set(tileKey,Date.now());next();
   };
   img.onload=cleanup;img.onerror=cleanup;
   img.src=rgRasterLayer.getTileUrl({x,y,z});
  }
 }
 next();
}
rgLightTiles.addTo(map);document.body.classList.add('rg-night');setTimeout(rgSyncMapSize,2000);
// 11.13.96: experimental x10 of 11.13.95 local tile retention.
// WARNING: potentially very high DOM/RAM use on long journeys.
// keepBuffer retains previously loaded DOM tiles; it is not a network prefetch radius.
const rgLocalTileRetention={normal:720,highZoom:360,highZoomFrom:17};
function rgTuneLocalTileRetention(){
 if(!rgRasterLayer||!map)return;
 const target=map.getZoom()>=rgLocalTileRetention.highZoomFrom
  ?rgLocalTileRetention.highZoom:rgLocalTileRetention.normal;
 if(rgRasterLayer.options.keepBuffer===target)return;
 rgRasterLayer.options.keepBuffer=target;
 // Let Leaflet prune naturally on the next view update, without forced requests.
}
map.on('zoomend',rgTuneLocalTileRetention);
let rgRasterLayer=null;
function setRasterPmtiles(id){
 rgPrefetch.generation++;rgPrefetch.lastKey='';rgPrefetch.lastAt=0;rgPrefetch.ringAt=0;rgPrefetch.ringCenter=null;rgPrefetch.ringZoom=-1;rgPrefetch.queue=[];rgPrefetch.queueIndex=0;rgPrefetch.seen.clear();
 for(const img of rgPrefetch.active.values()){img.onload=null;img.onerror=null;img.src='';}rgPrefetch.active.clear();
 if(rgRasterLayer){map.removeLayer(rgRasterLayer);rgRasterLayer=null;}
 if(!/^[A-Za-z0-9_-]{1,64}$/.test(id))return;
 rgRasterLayer=L.tileLayer('https://roadguard.local/pmtiles/'+id+'/{z}/{x}/{y}.png',{
   maxZoom:19,keepBuffer:720,rgLocalPack:true,updateWhenIdle:false,attribution:'Carte raster locale PMTiles · données du fournisseur du pack',errorTileUrl:''
 });
 rgRasterLayer.addTo(map);
 rgTuneLocalTileRetention();
}

let dangerMarkers=L.layerGroup().addTo(map);function setDangers(items){dangerMarkers.clearLayers();items.forEach(d=>{const type=String(d[2]||'Danger');const radar=/radar|vitesse|contrôle|controle/i.test(type);const icon=L.divIcon({className:'rg-danger-icon',html:'<span class="rg-danger-badge '+(radar?'radar':'hazard')+'">'+(radar?'📷':'⚠')+'</span>',iconSize:[42,42],iconAnchor:[21,21]});L.marker([d[0],d[1]],{icon:icon}).bindPopup(type+' · signalement local').addTo(dangerMarkers);});}let routeLine=null,routeGlow=null,routeDone=null,routePoints=[];function setRoute(points){routePoints=points;routeProgressLast=0;resetRouteStatus();if(routeDone){map.removeLayer(routeDone);routeDone=null;}if(routeLine){map.removeLayer(routeLine);routeLine=null;}if(routeGlow){map.removeLayer(routeGlow);routeGlow=null;}if(points.length){routeGlow=L.polyline(points,{color:"#00b8ff",weight:15,opacity:.22,lineCap:"round",interactive:false}).addTo(map);routeLine=L.polyline(points,{color:"#2beaff",weight:6,opacity:1,lineCap:"round"}).addTo(map);routeDone=L.polyline([],{color:"#47738a",weight:6,opacity:.8,lineCap:"round",interactive:false}).addTo(map);map.fitBounds(routeLine.getBounds(),{padding:[65,65]});}}// Visual route progress: gray completed section, cyan road ahead.
// Nearest-segment projection uses a local equirectangular metric; this is display only.
let routeProgressLast=0;
function resetRouteStatus(){const e=document.getElementById('rg-route-status');if(e){e.style.display='none';e.classList.remove('warn');e.textContent='';}}
function metersBetween(a,b){const r=6371000,rad=Math.PI/180,dlat=(b[0]-a[0])*rad,dlon=(b[1]-a[1])*rad;const h=Math.sin(dlat/2)**2+Math.cos(a[0]*rad)*Math.cos(b[0]*rad)*Math.sin(dlon/2)**2;return 2*r*Math.asin(Math.min(1,Math.sqrt(h)));}
function showRouteStatus(distanceFromRoute,remaining,progress){
 const el=document.getElementById('rg-route-status');if(!el)return;
 el.style.display='block';el.classList.toggle('warn',distanceFromRoute>120);
 if(distanceFromRoute>120){el.innerHTML='⚠ HORS ITINÉRAIRE<strong>Écart '+Math.round(distanceFromRoute)+' m</strong>';return;}
 const fmt=remaining>=1000?(remaining/1000).toFixed(1)+' km':Math.round(remaining)+' m';
 el.innerHTML='◈ ITINÉRAIRE · '+Math.round(progress*100)+' %<strong>'+fmt+' restants</strong>';
}
function updateRouteProgress(lat,lon){
 if(!routeDone||routePoints.length<2)return;
 const cos=Math.cos(lat*Math.PI/180),scale=111320;
 let best=Infinity,index=-1,fraction=0;
 for(let i=0;i<routePoints.length-1;i++){
  const a=routePoints[i],b=routePoints[i+1];
  const ax=(a[1]-lon)*cos*scale,ay=(a[0]-lat)*scale;
  const bx=(b[1]-lon)*cos*scale,by=(b[0]-lat)*scale;
  const dx=bx-ax,dy=by-ay,den=dx*dx+dy*dy;
  const t=den>0?Math.max(0,Math.min(1,-(ax*dx+ay*dy)/den)):0;
  const dist=(ax+t*dx)**2+(ay+t*dy)**2;
  if(dist<best){best=dist;index=i;fraction=t;}
 }
 // Avoid marking distant roads completed when GPS is inaccurate or off route.
 if(index<0)return;
 const distanceFromRoute=Math.sqrt(best);
 if(distanceFromRoute>120){showRouteStatus(distanceFromRoute,0,0);return;}
 const progress=index+fraction;
 // Prevent small GPS jitter from repeatedly moving the completed line backwards.
 if(progress+0.3<routeProgressLast)return;
 routeProgressLast=Math.max(routeProgressLast,progress);
 const i=Math.min(routePoints.length-2,Math.floor(routeProgressLast));
 const t=Math.min(1,routeProgressLast-i);
 const a=routePoints[i],b=routePoints[i+1];
 const split=[a[0]+(b[0]-a[0])*t,a[1]+(b[1]-a[1])*t];
 routeDone.setLatLngs([...routePoints.slice(0,i+1),split]);
 let total=0,done=0;
 for(let j=0;j<routePoints.length-1;j++){
  const len=metersBetween(routePoints[j],routePoints[j+1]);total+=len;
  if(j<i)done+=len;else if(j===i)done+=metersBetween(routePoints[j],split);
 }
 showRouteStatus(distanceFromRoute,Math.max(0,total-done),total>0?done/total:0);
}
let destinationMarker=null;function setDestination(lat,lon){if(destinationMarker){map.removeLayer(destinationMarker);destinationMarker=null;}if(lat!==null&&lon!==null){destinationMarker=L.circleMarker([lat,lon],{radius:11,color:'#ffffff',weight:3,fillColor:'#ffb52e',fillOpacity:1}).bindPopup('Destination').addTo(map);}}let pin=null,accuracyRing=null,headingMarker=null,lastHeading=0;
function setHeading(deg,visible){lastHeading=((Number(deg)||0)%360+360)%360;const el=headingMarker&&headingMarker.getElement();if(el){const arrow=el.querySelector('.rg-arrow');if(arrow){arrow.style.transform='rotate('+lastHeading+'deg)';arrow.style.display=visible?'block':'none';}}const needle=document.getElementById('rg-needle');if(needle)needle.style.transform='rotate('+lastHeading+'deg)';const degrees=document.getElementById('rg-degrees');if(degrees)degrees.textContent=Math.round(lastHeading)+'°';}
let lastCameraZoomChange=0;
let lastCameraPan=0;
let userCameraHoldUntil=0;
let cameraGestureStart=0;
let lastRenderedHeading=null;
// Navigation heading-up: rotate map drawing panes, not Leaflet's mapPane
// (Leaflet owns mapPane transforms). Keep controls upright and interactive.

const rgSubpixelCamera={x:0,y:0};
function rgApplyCameraTransform(){
 const c=rgHeadingCamera,angle=c.enabled?c.angle:0;
 const x=rgSubpixelCamera.x,y=rgSubpixelCamera.y;
 if(c.lastRendered!==null&&Math.abs(c.lastRendered-angle)<.005&&c.lastAppliedX===x&&c.lastAppliedY===y)return;
 c.lastRendered=angle;c.lastAppliedX=x;c.lastAppliedY=y;
 const panes=[map.getPane('tilePane'),map.getPane('overlayPane'),map.getPane('shadowPane'),map.getPane('markerPane'),map.getPane('tooltipPane'),map.getPane('popupPane')];
 const mp=map.getPane('mapPane'),mpPos=L.DomUtil.getPosition(mp)||L.point(0,0),size=map.getSize();
 const origin=(size.x/2-mpPos.x)+'px '+(size.y/2-mpPos.y)+'px';
 const transform='translate3d('+x.toFixed(3)+'px,'+y.toFixed(3)+'px,0) rotate3d(0,0,1,'+(-angle).toFixed(4)+'deg)';
 for(const pane of panes){if(!pane)continue;pane.style.transformOrigin=origin;pane.style.transform=transform;}
}
function rgSetHeadingCamera(enabled,bearing,speed){
 const c=rgHeadingCamera,moving=Number(speed)>=8&&Number.isFinite(Number(bearing));
 c.enabled=!!enabled;
 if(!c.enabled)rgLastTileBearingRefresh=null;
 if(moving)c.target=((Number(bearing)%360)+360)%360;
 else if(!enabled)c.target=0;
 if(!c.frame)c.frame=requestAnimationFrame(rgHeadingCameraTick);
}
function rgHeadingCameraTick(now){
 const c=rgHeadingCamera;c.frame=0;
 const dt=c.lastFrame?Math.max(1,Math.min(65,now-c.lastFrame)):16.67;c.lastFrame=now;
 const target=c.enabled?c.target:0;
 const delta=((target-c.angle+540)%360)-180;
 // A time-based angular filter avoids heading jitter and frame-rate dependent jerks.
 // The max angular velocity also prevents sudden spins after noisy GPS bearings.
 const alpha=1-Math.exp(-dt/240);
 const maxStep=160*dt/1000;
 const step=Math.max(-maxStep,Math.min(maxStep,delta*alpha));
 if(Math.abs(delta)>.15)c.angle=((c.angle+step)%360+360)%360;
 else c.angle=target;
 rgApplyCameraTransform();
 // The 360-degree diagonal footprint is bearing-invariant; avoid re-requesting tiles each frame.
 const arrow=headingMarker&&headingMarker.getElement()&&headingMarker.getElement().querySelector('.rg-arrow');
 if(arrow)arrow.style.transform='rotate('+(lastHeading-(c.enabled?c.angle:0))+'deg)';
 if(Math.abs(delta)>.15)c.frame=requestAnimationFrame(rgHeadingCameraTick);
}
// Keep the GPU pane origin correct after a viewport resize or zoom.
map.on('resize zoomend moveend',function(){rgHeadingCamera.lastRendered=null;rgApplyCameraTransform();});
map.on('resize zoomend',function(){rgRefreshRotatedTileCoverage(true);});
// Manual gestures pause automatic tracking so the user can inspect the map.
map.on('dragstart zoomstart',function(e){
 if(e && e.originalEvent){userCameraHoldUntil=Date.now()+12000;cameraGestureStart=Date.now();}
});
function showRouteOverview(){
 if(!routeLine)return false;
 userCameraHoldUntil=Date.now()+20000;
 const bounds=routeLine.getBounds();
 if(!bounds.isValid())return false;
 map.fitBounds(bounds,{paddingTopLeft:[24,150],paddingBottomRight:[24,185],maxZoom:16,animate:true,duration:.65});
 const el=document.getElementById('rg-camera');if(el)el.textContent='▤ APERÇU ITINÉRAIRE · SUIVI EN PAUSE 20 S';
 return true;
}
function resumeCamera(){rgSubpixelCamera.x=0;rgSubpixelCamera.y=0;rgApplyCameraTransform();rgCameraSmooth.x=null;rgCameraSmooth.y=null;userCameraHoldUntil=0;lastCameraPan=0;if(pin)map.stop();if(rgVisualGps.shown)rgVisualCamera(rgVisualGps.shown,performance.now()+100);const el=document.getElementById('rg-camera');if(el)el.textContent='⌖ SUIVI GPS ACTIF';}
function smoothHeading(deg){
 const target=((Number(deg)||0)%360+360)%360;
 if(lastRenderedHeading===null){lastRenderedHeading=target;return target;}
 const delta=((target-lastRenderedHeading+540)%360)-180;
 lastRenderedHeading=((lastRenderedHeading+delta*.38)%360+360)%360;
 return lastRenderedHeading;
}
// Smooth the visual marker at display refresh rate; GPS coordinates remain authoritative.
// Never extrapolate past a fix or keep moving after GPS updates stop.
const rgCameraSmooth={x:null,y:null,lastAt:0,zoom:null};
const rgVisualGps={from:null,to:null,shown:null,started:0,duration:850,frame:0,follow:false,heading:0,speed:0,lookAhead:false,lastCameraFrame:0,lastFixAt:0,fixInterval:850};
function rgVisualCamera(point,now){
 if(!rgVisualGps.follow||Date.now()<userCameraHoldUntil||!map||!map.getSize().y)return;
 if(now-rgVisualGps.lastCameraFrame<12)return;
 rgVisualGps.lastCameraFrame=now;
 const kmh=rgVisualGps.speed;
 let center=point;
 if(rgVisualGps.lookAhead&&kmh>=12&&Number.isFinite(rgVisualGps.heading)){
  const distancePx=Math.min(map.getSize().y*.24,Math.max(45,kmh*1.1));
  const angle=rgVisualGps.heading*Math.PI/180;
  const projected=map.project(point,map.getZoom());
  center=map.unproject([projected.x+Math.sin(angle)*distancePx,projected.y-Math.cos(angle)*distancePx],map.getZoom());
 }
 if(rgHeadingCamera.enabled){
  // Position the vehicle about 73% down the viewport: extra road visible ahead.
  const px=map.project(point,map.getZoom());
  const shift=map.getSize().y*.23;
  const rad=rgHeadingCamera.angle*Math.PI/180;
  center=map.unproject([px.x+Math.sin(rad)*shift,px.y-Math.cos(rad)*shift],map.getZoom());
 }
 // Follow the interpolated position with a frame-rate-independent camera filter.
 // Leaflet snaps its map origin to whole pixels; damping prevents sudden multi-pixel jumps.
 const z=map.getZoom(),target=map.project(center,z),c=rgCameraSmooth;
 if(c.zoom!==z||c.x===null||now-c.lastAt>500){rgSubpixelCamera.x=0;rgSubpixelCamera.y=0;c.x=target.x;c.y=target.y;c.zoom=z;c.lastAt=now;}
 else {
  const dt=Math.max(1,Math.min(70,now-c.lastAt));
  const alpha=1-Math.exp(-dt/115);
  c.x+=(target.x-c.x)*alpha;c.y+=(target.y-c.y)*alpha;c.lastAt=now;
 }
 // Leaflet rounds the map origin to integer CSS pixels. Preserve the
 // fractional camera delta on the GPU rather than waiting for the next pixel.
 // Rebase Leaflet only when the offset becomes large; never queue pan animations.
 let current=map.project(map.getCenter(),z);
 let dx=c.x-current.x,dy=c.y-current.y;
 if(Math.hypot(dx,dy)>9){
  map.panTo(map.unproject([c.x,c.y],z),{animate:false});
  current=map.project(map.getCenter(),z);
  dx=c.x-current.x;dy=c.y-current.y;
 }
 // Offset the rendered map opposite to the desired center movement.
 // Rotate the offset into screen coordinates for heading-up navigation.
 const a=rgHeadingCamera.enabled?rgHeadingCamera.angle*Math.PI/180:0;
 const sx=-dx*Math.cos(a)-dy*Math.sin(a);
 const sy=dx*Math.sin(a)-dy*Math.cos(a);
 rgSubpixelCamera.x=Math.max(-10,Math.min(10,sx));
 rgSubpixelCamera.y=Math.max(-10,Math.min(10,sy));
 rgApplyCameraTransform();
}
function rgVisualTick(now){
 rgVisualGps.frame=0;
 if(!rgVisualGps.to)return;
 const elapsed=Math.max(0,now-rgVisualGps.started);
 const t=rgVisualGps.duration<=0?1:Math.min(1,elapsed/rgVisualGps.duration);
 // Linear interpolation prevents the visible slow-down / acceleration on each GPS fix.
 const ease=t;
 const a=rgVisualGps.from||rgVisualGps.to,b=rgVisualGps.to;
 const point=[a[0]+(b[0]-a[0])*ease,a[1]+(b[1]-a[1])*ease];
 rgVisualGps.shown=point;
 if(pin)pin.setLatLng(point);
 if(headingMarker)headingMarker.setLatLng(point);
 if(accuracyRing)accuracyRing.setLatLng(point);
 rgVisualCamera(point,now);
 if(t<1)rgVisualGps.frame=requestAnimationFrame(rgVisualTick);
}
function rgVisualFix(point,follow,bearing,speed,lookAhead){
 const now=performance.now();
 rgVisualGps.from=rgVisualGps.shown||point;
 rgVisualGps.to=point;
 // Estimate GPS cadence to spread the next movement across most of the fix interval.
 // Keep the marker at the latest measured fix; never extrapolate beyond it.
 if(rgVisualGps.lastFixAt){
  const interval=now-rgVisualGps.lastFixAt;
  if(interval>=200&&interval<=3500)rgVisualGps.fixInterval=rgVisualGps.fixInterval*.55+interval*.45;
 }
 rgVisualGps.lastFixAt=now;
 rgVisualGps.started=now;
 rgVisualGps.follow=!!follow;
 rgVisualGps.heading=Number(bearing);
 rgVisualGps.speed=Math.max(0,Number(speed)||0);
 rgVisualGps.lookAhead=!!lookAhead;
 // Never animate an implausibly large jump across the map.
 const jump=map.distance(rgVisualGps.from,point);
 rgVisualGps.duration=jump>100?0:Math.max(240,Math.min(1400,rgVisualGps.fixInterval*.94));
 if(rgVisualGps.frame)cancelAnimationFrame(rgVisualGps.frame);
 rgVisualGps.frame=requestAnimationFrame(rgVisualTick);
}
function updateGPS(lat,lon,follow,bearing,accuracy,headingUp,speedKmh,adaptiveZoom,lookAhead){
 if(!Number.isFinite(Number(lat))||!Number.isFinite(Number(lon))||Math.abs(lat)>90||Math.abs(lon)>180)return;
 const filtered=rgFilterGps(lat,lon,accuracy,speedKmh);lat=filtered[0];lon=filtered[1];
 // A stationary user also needs an initial camera recenter, even without motion.
 if(rgFirstGpsFix){rgSubpixelCamera.x=0;rgSubpixelCamera.y=0;rgCameraSmooth.x=null;rgCameraSmooth.y=null;rgFirstGpsFix=false;userCameraHoldUntil=0;map.invalidateSize({pan:false});if(follow)map.setView([lat,lon],Math.max(15,map.getZoom()),{animate:false});}
 if(follow && Date.now()>=userCameraHoldUntil)rgWarmAhead(lat,lon,bearing,speedKmh);
 const speedEl=document.getElementById('rg-speed-value');if(speedEl)speedEl.textContent=String(Math.max(0,Math.round(Number(speedKmh)||0)));const point=[lat,lon];updateRouteProgress(lat,lon);
 if(!pin)pin=L.circleMarker(point,{radius:8,color:'#ffffff',weight:3,fillColor:'#00bfff',fillOpacity:1}).addTo(map);
 const meters=Math.max(0,Math.min(2000,Number(accuracy)||0));
 if(meters>0){if(!accuracyRing)accuracyRing=L.circle(point,{radius:meters,color:'#50cfff',weight:1,opacity:.55,fillColor:'#20aeff',fillOpacity:.09,interactive:false}).addTo(map);else accuracyRing.setRadius(meters);}else if(accuracyRing){map.removeLayer(accuracyRing);accuracyRing=null;}
 if(!headingMarker){const icon=L.divIcon({className:'',html:'<div class="rg-arrow">▲</div>',iconSize:[36,36],iconAnchor:[18,18]});headingMarker=L.marker(point,{icon:icon,interactive:false,zIndexOffset:1000}).addTo(map);}
 setHeading(smoothHeading(bearing),!!headingUp);
 rgSetHeadingCamera(headingUp,bearing,speedKmh);
 const cameraText=document.getElementById('rg-camera');if(cameraText)cameraText.textContent=!follow?'◎ SUIVI GPS DÉSACTIVÉ':Date.now()<userCameraHoldUntil?'✋ EXPLORATION LIBRE · SUIVI EN PAUSE':'⌖ SUIVI GPS ACTIF';
 // Adaptive zoom is changed only on real GPS fixes, never every animation frame.
 if(follow && Date.now()>=userCameraHoldUntil && adaptiveZoom){
  const kmh=Math.max(0,Number(speedKmh)||0);
  const targetZoom=kmh>=100?14:kmh>=65?15:kmh>=30?16:17;
  const now=Date.now();
  if(now-lastCameraZoomChange>6500&&Math.abs(map.getZoom()-targetZoom)>=1){
   lastCameraZoomChange=now;rgSubpixelCamera.x=0;rgSubpixelCamera.y=0;rgCameraSmooth.x=null;rgCameraSmooth.y=null;map.stop();map.setZoom(targetZoom,{animate:false});
  }
 }
 rgVisualFix(point,follow,bearing,speedKmh,lookAhead);
}
