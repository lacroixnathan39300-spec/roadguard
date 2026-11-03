/* ROADGUARD 11.14.177: local rendering only; no account token or network calls in this asset. */
(function(){
 'use strict';
 window.rgAppearance={accent:'#ff304f',routeColor:'#329dff',cursor:'arrow'};
 const car='<svg viewBox="0 0 32 40" width="24" height="32" aria-hidden="true"><rect x="7" y="3" width="18" height="34" rx="6" fill="currentColor"/><path d="M10 12h12l-1 9H11z" fill="#17202b"/><path d="M10 27h12v4H10z" fill="#17202b"/><path d="M8 7h4m8 0h4" stroke="white" stroke-width="2"/></svg>';
 const arrow='<svg viewBox="0 0 32 40" width="27" height="33" aria-hidden="true"><path d="M16 3 29 35 16 28 3 35Z" fill="currentColor" stroke="white" stroke-width="1.4"/></svg>';
 function paintCursor(){
  const dot=window.rgAppearance.cursor==='dot',accent=window.rgAppearance.accent;
  if(typeof pin!=='undefined'&&pin&&pin.setStyle)pin.setStyle({fillColor:accent,opacity:dot?1:0,fillOpacity:dot?1:0,interactive:false});
  if(typeof accuracyRing!=='undefined'&&accuracyRing&&accuracyRing.setStyle)accuracyRing.setStyle({color:accent,fillColor:accent,opacity:.35,fillOpacity:.05});
  const el=typeof headingMarker!=='undefined'&&headingMarker&&headingMarker.getElement();const node=el&&el.querySelector('.rg-arrow');
  if(node){node.innerHTML=window.rgAppearance.cursor==='car'?car:arrow;node.style.color=window.rgAppearance.accent;node.style.display=window.rgAppearance.cursor==='dot'?'none':'flex';node.style.alignItems='center';node.style.justifyContent='center';node.style.background='transparent';node.style.border='0';node.style.boxShadow='none';}
 }
 window.setRoadguardAppearance=function(p){
  if(!p||!/^#[0-9a-f]{6}$/i.test(p.accent||'')||!/^#[0-9a-f]{6}$/i.test(p.routeColor||'')||!['arrow','car','dot'].includes(p.cursor))return;
  window.rgAppearance={accent:p.accent,routeColor:p.routeColor,cursor:p.cursor};const root=document.documentElement,red=parseInt(p.accent.slice(1,3),16),green=parseInt(p.accent.slice(3,5),16),blue=parseInt(p.accent.slice(5,7),16),light=.2126*red+.7152*green+.0722*blue;root.style.setProperty('--rg-accent',p.accent);root.style.setProperty('--rg-accent-border',light<60?'#9eb0c2':p.accent);root.style.setProperty('--rg-accent-ink',light>150?'#111820':'#ffffff');paintCursor();
  if(typeof routeLine!=='undefined'&&routeLine)routeLine.setStyle({color:p.routeColor});if(typeof routeGlow!=='undefined'&&routeGlow)routeGlow.setStyle({color:p.routeColor});
 };
 window.paintCommunityCursor=paintCursor;
 let members=[],clusters=[],nextExpiry=Infinity,layer=null;
 function paintMembers(){
  if(typeof L==='undefined'||typeof map==='undefined')return;if(!layer)layer=L.layerGroup().addTo(map);layer.clearLayers();const now=Date.now();nextExpiry=Infinity;
  for(const p of members){if(!Number.isFinite(p.lat)||!Number.isFinite(p.lng)||!(p.expires>now))continue;nextExpiry=Math.min(nextExpiry,p.expires);const marker=L.circleMarker([p.lat,p.lng],{radius:7,color:'#0e1822',weight:3,fillColor:'#36ce9c',fillOpacity:1}).addTo(layer);const text=document.createElement('div');text.textContent=String(p.alias||'Membre')+' · position approximative · '+new Date(p.updated).toLocaleTimeString('fr-FR',{hour:'2-digit',minute:'2-digit'});marker.bindPopup(text);}
  for(const p of clusters){if(!Number.isFinite(p.lat)||!Number.isFinite(p.lng)||!(p.expires>now)||!Number.isInteger(p.count)||p.count<=0)continue;nextExpiry=Math.min(nextExpiry,p.expires);L.marker([p.lat,p.lng],{icon:L.divIcon({className:'rg-member-group',html:String(p.count),iconSize:[34,34],iconAnchor:[17,17]})}).addTo(layer).on('click',()=>map.setView([p.lat,p.lng],Math.max(14,map.getZoom()+2)));}
 }
 window.setCommunityMembers=function(people,groups){members=Array.isArray(people)?people.slice(0,200):[];clusters=Array.isArray(groups)?groups.slice(0,200):[];paintMembers();};
 setInterval(()=>{if(Date.now()>=nextExpiry)paintMembers();},1000);
})();
(function(){let current='';window.setCommunityTileProvider=function(p){if(!p||typeof p.tileUrl!=='string'||!p.tileUrl.startsWith('https://')||!['{z}','{x}','{y}'].every(k=>p.tileUrl.includes(k))||p.tileUrl===current)return;if(typeof rgLightTiles==='undefined')return;current=p.tileUrl;if(map.attributionControl&&rgLightTiles.options.attribution)map.attributionControl.removeAttribution(rgLightTiles.options.attribution);rgLightTiles.options.attribution=p.osm?'&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors':String(p.tileAttribution||'').replace(/[<>"&]/g,'');if(map.attributionControl)map.attributionControl.addAttribution(rgLightTiles.options.attribution);rgLightTiles.setUrl(p.tileUrl);};})();
