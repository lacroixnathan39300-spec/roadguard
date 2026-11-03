
'use strict';
const status=document.getElementById('status');let activeVectorPack=null;let map=null,ready=false,lastPos=null,followOn=true,heading=0,route=[],dangers=[],destination=null;
function say(t,error){status.textContent=t;status.className=error?'error':'';}
function valid(lat,lon){return Number.isFinite(+lat)&&Number.isFinite(+lon)&&Math.abs(+lat)<=90&&Math.abs(+lon)<=180;}
function featurePoint(lon,lat,props){return {type:'Feature',geometry:{type:'Point',coordinates:[+lon,+lat]},properties:props||{}};}
function fc(features){return {type:'FeatureCollection',features};}
function put(id,data){if(ready&&map.getSource(id))map.getSource(id).setData(data);}
function sync(){if(!ready)return;put('rg-route',fc(route.length>1?[{type:'Feature',properties:{},geometry:{type:'LineString',coordinates:route}}]:[]));put('rg-alerts',fc(dangers.filter(d=>Array.isArray(d)?valid(d[0],d[1]):valid(d.lat,d.lon)).map(d=>Array.isArray(d)?featurePoint(d[1],d[0],{type:String(d[2]||'danger')}):featurePoint(d.lon,d.lat,{type:String(d.type||'danger')}))));put('rg-dest',fc(destination?[featurePoint(destination[0],destination[1])]:[]));put('rg-position',fc(lastPos?[featurePoint(lastPos[0],lastPos[1])]:[]));}
function parsePoints(items){return (Array.isArray(items)?items:[]).map(p=>Array.isArray(p)?[+p[1],+p[0]]:[+p.lon,+p.lat]).filter(p=>valid(p[1],p[0]));}
function setRoute(points){route=parsePoints(points);sync();}
function setDangers(items){dangers=Array.isArray(items)?items:[];sync();}
function setDestination(lat,lon){destination=valid(lat,lon)?[+lon,+lat]:null;sync();}
function showRouteOverview(){if(!ready||route.length<2)return;let bounds=route.reduce((b,p)=>b.extend(p),new maplibregl.LngLatBounds(route[0],route[0]));map.fitBounds(bounds,{padding:65,duration:500,maxZoom:15});followOn=false;}
function resumeCamera(){followOn=true;if(lastPos&&ready)map.easeTo({center:lastPos,duration:350});}
function resetView(){resumeCamera();}
function setHeading(deg,visible){if(Number.isFinite(+deg))heading=+deg;}
function updateGPS(lat,lon,follow,bearing,accuracy,headingUp,speedKmh,adaptiveZoom,lookAhead){if(!valid(lat,lon))return;lastPos=[+lon,+lat];followOn=!!follow;heading=Number.isFinite(+bearing)?+bearing:heading;sync();if(ready&&followOn){let speed=Number.isFinite(+speedKmh)?+speedKmh:0;let zoom=adaptiveZoom?(speed>95?13.6:speed>55?14.4:speed>20?15.3:16.1):map.getZoom();map.easeTo({center:lastPos,bearing:headingUp?heading:0,pitch:headingUp?55:45,zoom,duration:650,essential:true});}}
// Aperçu vectoriel expérimental : le moteur MapLibre est embarqué à la compilation.
// Les noms de source-layer suivent le schéma OpenMapTiles ; d'autres packs peuvent rester vides.
function setVectorPmtiles(id){
 if(!/^[A-Za-z0-9_-]{1,64}$/.test(id)){say('Identifiant PMTiles incorrect',true);return;}
 if(!map||!window.maplibregl){say('Moteur MapLibre local absent : exécuter tools/fetch_maplibre_vendor.py avant compilation',true);return;}
 const url='https://roadguard.local/pmtiles/'+id+'/{z}/{x}/{y}.pbf';
 const style={version:8,name:'ROADGUARD PMTiles vector preview',sources:{roads:{type:'vector',tiles:[url],minzoom:0,maxzoom:14}},
 layers:[
  {id:'bg',type:'background',paint:{'background-color':'#101c2a'}},
  {id:'water',type:'fill',source:'roads','source-layer':'water',paint:{'fill-color':'#193f5a'}},
  {id:'landcover',type:'fill',source:'roads','source-layer':'landcover',paint:{'fill-color':'#1d3840','fill-opacity':0.45}},
  {id:'building',type:'fill',source:'roads','source-layer':'building',paint:{'fill-color':'#324454','fill-opacity':0.8}},
  {id:'transportation',type:'line',source:'roads','source-layer':'transportation',paint:{'line-color':'#9ec4db','line-width':['interpolate',['linear'],['zoom'],6,0.4,14,3.2]}},
  {id:'roads',type:'line',source:'roads','source-layer':'road',paint:{'line-color':'#9ec4db','line-width':['interpolate',['linear'],['zoom'],6,0.4,14,3.2]}}
 ]};
 activeVectorPack=id;ready=false;
 map.setStyle(style);
 say('APERÇU VECTORIEL · tuiles PMTiles locales · moteur MapLibre embarqué si installé');
}
function setRoads(items){} // Les routes hors ligne sont gerees par l'ancien moteur.
function setOfflineRoads(items){}
function showRouteStatus(distanceFromRoute,remaining,progress){}
function updateRouteProgress(lat,lon){}
window.onerror=function(){say('Carte 3D indisponible : connexion ou ressources WebView. Appuyer sur 2D pour revenir.',true);};
try{if(!window.maplibregl)throw Error('MapLibre JS non charge');map=new maplibregl.Map({container:'map',style:{version:8,name:'ROADGUARD local startup',sources:{},layers:[{id:'background',type:'background',paint:{'background-color':'#101c2a'}}]},center:[5.9,46.7],zoom:11,pitch:45,bearing:0,attributionControl:true});map.on('style.load',()=>{if(map.getSource('rg-route'))return;map.addSource('rg-route',{type:'geojson',data:fc([])});map.addLayer({id:'rg-route-glow',type:'line',source:'rg-route',paint:{'line-color':'#00dfff','line-width':13,'line-opacity':0.2}});map.addLayer({id:'rg-route-line',type:'line',source:'rg-route',paint:{'line-color':'#46eeff','line-width':5}});for(let id of ['rg-alerts','rg-dest','rg-position'])map.addSource(id,{type:'geojson',data:fc([])});map.addLayer({id:'rg-alert-points',type:'circle',source:'rg-alerts',paint:{'circle-color':'#ffb64b','circle-radius':7,'circle-stroke-color':'#ffffff','circle-stroke-width':2}});map.addLayer({id:'rg-dest-point',type:'circle',source:'rg-dest',paint:{'circle-color':'#ffb64b','circle-radius':9}});map.addLayer({id:'rg-position-point',type:'circle',source:'rg-position',paint:{'circle-color':'#26dfff','circle-radius':9,'circle-stroke-color':'#ffffff','circle-stroke-width':3}});ready=true;say(activeVectorPack?'PMTILES · pack '+activeVectorPack+' · aperçu vectoriel expérimental':'MAPLIBRE · fond local vide · choisissez un pack vectoriel PMTiles');sync();if(lastPos)map.jumpTo({center:lastPos});});map.on('error',e=>{if(!ready)say('Carte vectorielle indisponible : vérifiez les fichiers MapLibre et le pack PMTiles.',true);});map.on('dragstart',()=>{followOn=false;});}catch(e){say('MapLibre indisponible. Repassez en 2D.',true);}
