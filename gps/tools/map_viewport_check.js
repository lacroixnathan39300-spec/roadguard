function setRoadguardNight(enabled){
 document.body.classList.toggle('rg-night',!!enabled);
 // La coloration CSS ne concerne que les images de tuiles : marqueurs, routes et controles gardent leurs couleurs.
}let map=L.map('map',{zoomControl:true}).setView([46.75,5.90],11);
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
let rgTileFailures=0;let rgTileSuccesses=0;let rgTileRequests=0;let rgTileWarningAt=0;
const rgTileNotice=document.createElement('div');
rgTileNotice.id='rg-tile-notice';rgTileNotice.setAttribute('role','status');
rgTileNotice.style.cssText='display:none;position:absolute;top:110px;left:12px;right:12px;z-index:1100;background:#10273eea;color:#e7f8ff;border:1px solid #4a9dbc;border-radius:12px;padding:10px;font:12px Arial;pointer-events:none';
document.body.appendChild(rgTileNotice);
function rgTileStatus(msg){rgTileNotice.textContent=msg;rgTileNotice.style.display=msg?'block':'none';}

const rgLightTiles=L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{
 maxZoom:19,attribution:'&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
});
rgLightTiles.on('tileloadstart',()=>{rgTileRequests++;});
rgLightTiles.on('tileerror',()=>{rgTileFailures++;if(rgTileFailures>=8&&rgTileSuccesses===0&&Date.now()-rgTileWarningAt>15000){rgTileWarningAt=Date.now();rgTileStatus('Certaines tuiles sont indisponibles. Vérifie Internet ou utilise une carte hors ligne.');}});
rgLightTiles.on('tileload',()=>{rgTileSuccesses++;rgTileFailures=0;rgTileStatus('');});
rgLightTiles.addTo(map);document.body.classList.add('rg-night');setTimeout(rgSyncMapSize,2000);
let rgRasterLayer=null;
function setRasterPmtiles(id){
 if(rgRasterLayer){map.removeLayer(rgRasterLayer);rgRasterLayer=null;}
 if(!/^[A-Za-z0-9_-]{1,64}$/.test(id))return;
 rgRasterLayer=L.tileLayer('https://roadguard.local/pmtiles/'+id+'/{z}/{x}/{y}.png',{
   maxZoom:19,attribution:'Carte raster locale PMTiles · données du fournisseur du pack',errorTileUrl:''
 });
 rgRasterLayer.addTo(map);
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
function resumeCamera(){userCameraHoldUntil=0;lastCameraPan=0;if(pin)map.panTo(pin.getLatLng(),{animate:false});const el=document.getElementById('rg-camera');if(el)el.textContent='⌖ SUIVI GPS ACTIF';}
function smoothHeading(deg){
 const target=((Number(deg)||0)%360+360)%360;
 if(lastRenderedHeading===null){lastRenderedHeading=target;return target;}
 const delta=((target-lastRenderedHeading+540)%360)-180;
 lastRenderedHeading=((lastRenderedHeading+delta*.38)%360+360)%360;
 return lastRenderedHeading;
}
function updateGPS(lat,lon,follow,bearing,accuracy,headingUp,speedKmh,adaptiveZoom,lookAhead){
 if(!Number.isFinite(Number(lat))||!Number.isFinite(Number(lon))||Math.abs(lat)>90||Math.abs(lon)>180)return;
 // A stationary user also needs an initial camera recenter, even without motion.
 if(rgFirstGpsFix){rgFirstGpsFix=false;userCameraHoldUntil=0;map.invalidateSize({pan:false});if(follow)map.setView([lat,lon],Math.max(15,map.getZoom()),{animate:false});}
 const speedEl=document.getElementById('rg-speed-value');if(speedEl)speedEl.textContent=String(Math.max(0,Math.round(Number(speedKmh)||0)));const point=[lat,lon];updateRouteProgress(lat,lon);
 if(!pin)pin=L.circleMarker(point,{radius:8,color:'#ffffff',weight:3,fillColor:'#00bfff',fillOpacity:1}).addTo(map);else pin.setLatLng(point);
 const meters=Math.max(0,Math.min(2000,Number(accuracy)||0));
 if(meters>0){if(!accuracyRing)accuracyRing=L.circle(point,{radius:meters,color:'#50cfff',weight:1,opacity:.55,fillColor:'#20aeff',fillOpacity:.09,interactive:false}).addTo(map);else accuracyRing.setLatLng(point).setRadius(meters);}else if(accuracyRing){map.removeLayer(accuracyRing);accuracyRing=null;}
 if(!headingMarker){const icon=L.divIcon({className:'',html:'<div class="rg-arrow">▲</div>',iconSize:[36,36],iconAnchor:[18,18]});headingMarker=L.marker(point,{icon:icon,interactive:false,zIndexOffset:1000}).addTo(map);}else headingMarker.setLatLng(point);
 setHeading(smoothHeading(bearing),!!headingUp);
 const cameraText=document.getElementById('rg-camera');if(cameraText)cameraText.textContent=!follow?'◎ SUIVI GPS DÉSACTIVÉ':Date.now()<userCameraHoldUntil?'✋ EXPLORATION LIBRE · SUIVI EN PAUSE':'⌖ SUIVI GPS ACTIF';
 // Leaflet 1.9 has no native map rotation: heading-up changes the marker only.
 // Never rotate the entire tile container, which would break touch gestures and labels.
 if(follow && Date.now()>=userCameraHoldUntil){
  // Driving camera: wider view at higher speeds; throttle zoom changes to avoid jitter.
  const kmh=Math.max(0,Number(speedKmh)||0);
  const targetZoom=kmh>=100?14:kmh>=65?15:kmh>=30?16:17;
  const now=Date.now();
  // Camera anticipative: place the vehicle below center so more road is visible ahead.
  // Apply only while moving with a meaningful GPS bearing; never rotate Leaflet tiles.
  const ahead=!!lookAhead && kmh>=12 && Number.isFinite(Number(bearing)) && kmh<220;
  const viewCenter=(()=>{
    if(!ahead)return point;
    const distancePx=Math.min(map.getSize().y*.24,Math.max(45,kmh*1.1));
    const angle=Number(bearing)*Math.PI/180;
    const projected=map.project(point,map.getZoom());
    return map.unproject([projected.x+Math.sin(angle)*distancePx,projected.y-Math.cos(angle)*distancePx],map.getZoom());
  })();
  if(adaptiveZoom && now-lastCameraZoomChange>6500 && Math.abs(map.getZoom()-targetZoom)>=1){
    lastCameraZoomChange=now;
    map.setView(viewCenter,targetZoom,{animate:true,duration:.55});
  }else if(now-lastCameraPan>900 && map.distance(map.getCenter(),viewCenter)>Math.max(5,kmh*.12)){
    lastCameraPan=now;map.panTo(viewCenter,{animate:true,duration:.45});
  }
}
}
