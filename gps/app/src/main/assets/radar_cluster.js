/* Carte uniquement : les alertes GPS continuent d'utiliser tous les points. */
(function(root){
 'use strict';
 function kind(item){
  const label=String(item[2]||'').normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase()
   .split(' · ')[0].replace(/\s+/g,' ').trim();
  return label==='zone de danger'?'zone dangereuse':label;
 }
 function distance(a,b){
  const lat=(Number(a[0])+Number(b[0]))*Math.PI/360;
  return Math.hypot((a[0]-b[0])*111320,(a[1]-b[1])*111320*Math.cos(lat));
 }
 // Display only. Originals and their identities remain available to GPS alerts.
 // Compare representatives, not chains: two nearby distinct sites must not collapse transitively.
 function deduplicate(items){
  const unique=[];
  for(const item of items){
   if(!Number.isFinite(+item[0])||!Number.isFinite(+item[1]))continue;
   const label=kind(item);
   const same=unique.some(other=>{
    if(label!==kind(other))return false;
    if(+item[7]>0&&+other[7]>0&&+item[7]!==+other[7])return false;
    if(item[6]!=null&&other[6]!=null&&Math.abs(((+item[6]-other[6]+540)%360)-180)>1)return false;
    // Distinct community observations may be independent events, even at one site.
    if(item[4]&&other[4]&&item[4]!==other[4])return false;
    // Missing direction is insufficient evidence for merging two catalog cameras.
    const radius=item[8]&&other[8]?1:12;
    return distance(item,other)<=radius;
   });
   if(!same)unique.push(item);
  }
  return unique;
 }
 function group(items,project,width,height,cell){
  const bins=new Map(),step=Math.max(72,Number(cell)||96);
  for(const item of deduplicate(items)){
   const p=project(item);
   if(!p||!Number.isFinite(p.x)||!Number.isFinite(p.y)||p.x<0||p.y<0||p.x>width||p.y>height)continue;
   const key=Math.floor(p.x/step)+':'+Math.floor(p.y/step);
   let bin=bins.get(key);
   if(!bin){bin={items:[],x:0,y:0,lat:0,lon:0};bins.set(key,bin);}
   bin.items.push(item);bin.x+=p.x;bin.y+=p.y;
   bin.lat+=Number(item[0]);bin.lon+=Number(item[1]);
  }
  return [...bins.values()].map(bin=>{
   const count=bin.items.length;
   return {items:bin.items,count,x:bin.x/count,y:bin.y/count,lat:bin.lat/count,lon:bin.lon/count};
  });
 }
 root.RoadguardRadarClusters={group,deduplicate};
})(typeof window==='undefined'?globalThis:window);
