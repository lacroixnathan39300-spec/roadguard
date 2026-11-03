const assert=require('assert'),fs=require('fs'),vm=require('vm');
const {RoadguardAutoZoom}=require('../app/src/main/assets/camera_motion.js');
let z=new RoadguardAutoZoom();assert.equal(z.update(29,17,20000),null);
for(let t=21000;t<50000;t+=1000)assert.equal(z.update(t%2000?29:32,17,t),null,'pas de pompage au seuil de 30');
assert.equal(z.update(40,17,51000),null);assert.equal(z.update(40,17,54500),16,'vitesse stable produit un seul changement');
assert.equal(z.update(25,16,60000),null,'hystérésis à la baisse');
assert.equal(z.update(-1,16,61000),null,'absence ne provoque pas de zoom');
assert.equal(z.update(20,16,62000),null);assert.equal(z.update(20,16,65500),null,'intervalle minimal');assert.equal(z.update(20,16,66500),17);
const source=fs.readFileSync(require('path').join(__dirname,'../app/src/main/assets/map.html'),'utf8');
let zoomEnd;const calls=[];const context={window:{},RoadguardAutoZoom,requestAnimationFrame:()=>1,Date,performance:{now:()=>0},map:{on:(event,fn)=>{if(event==='zoomend')zoomEnd=fn;},getZoom:()=>17,getCenter:()=>[46,5],flyTo:(...a)=>calls.push(a)}};
vm.createContext(context);vm.runInContext(source.slice(source.indexOf('let lastCameraZoomChange='),source.indexOf('let lastCameraPan=')),context);
let block=source.slice(source.indexOf('  const now=Date.now(),targetZoom='),source.indexOf('\n }\n rgVisualFix',source.indexOf('  const now=Date.now(),targetZoom=')));
context.Date={now:()=>20000};context.speedKmh=40;vm.runInContext('{'+block+'}',context);context.Date={now:()=>24000};vm.runInContext('{'+block+'}',context);assert.equal(calls.length,1);assert.equal(calls[0][2].animate,true);assert(calls[0][2].duration>=1);vm.runInContext('rgAutoZooming=false',context);
// A bearing of 100° inside markerPane rotated -80° must appear at +20°, not -60°.
const match=source.match(/if\(arrow\)arrow.style.transform='rotate\('\+([^;]+);/);assert(match);context.arrow={style:{}};context.lastHeading=100;context.c={enabled:true,angle:80};vm.runInContext("if(arrow)arrow.style.transform='rotate('+"+match[1]+';',context);assert.equal(context.arrow.style.transform,'rotate(100deg)');
const smooth=source.slice(source.indexOf('function smoothHeading('),source.indexOf('// Smooth the visual marker'));vm.runInContext('let lastRenderedHeading=359;'+smooth,context);let heading=vm.runInContext('smoothHeading(1)',context);assert(heading>359&&heading<361,'pas de tour complet au passage du nord');
for(const start of [1079, -721, 3599, -3601]){vm.runInContext('lastRenderedHeading='+start,context);const next=vm.runInContext('smoothHeading(1)',context);assert(Math.abs(next-start)<=70,'cap court même après plusieurs tours');}
console.log('PASS adaptive zoom: hysteresis/dwell/cadence, absent speed, real map flyTo animation and single heading rotation');
