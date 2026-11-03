const assert=require('node:assert/strict'),fs=require('fs'),path=require('path'),vm=require('vm');
const assets=path.join(__dirname,'../app/src/main/assets'),View=require(path.join(assets,'map_view_state.js')).RoadguardViewState;
const html=fs.readFileSync(path.join(assets,'map.html'),'utf8');
const calls=[],stored=[],ctx={RoadguardViewState:View,rgUserCameraPaused:false,userCameraHoldUntil:900,rgSubpixelCamera:{x:10,y:20},rgCameraSmooth:{x:1,y:2},rgApplyCameraTransform(){},window:{RoadguardMap:{storeViewState:s=>stored.push(JSON.parse(s))}},map:{getCenter:()=>({lat:47,lng:7}),getZoom:()=>12,stop(){},setView(p,z){calls.push([p,z]);}}};
vm.createContext(ctx);const start=html.indexOf('function restoreRoadguardView(');vm.runInContext(html.slice(start,html.indexOf("map.on('moveend'",start)),ctx);
ctx.restoreRoadguardView({paused:true,lat:47,lon:7,zoom:12});assert(ctx.rgUserCameraPaused);assert.equal(ctx.userCameraHoldUntil,0);assert.equal(ctx.rgSubpixelCamera.x,0);assert.equal(calls[0][0][1],7);assert(stored[0].paused);
for(const invalid of [null,{}, {paused:false,lat:47,lon:7,zoom:12},{paused:true,lat:null,lon:7,zoom:12},{paused:true,lat:91,lon:7,zoom:12},{paused:true,lat:47,lon:7,zoom:Infinity}]){ctx.restoreRoadguardView(invalid);assert.equal(calls.length,1);}
assert.equal(View.capture(ctx.map,false),null);assert.deepEqual(View.capture(ctx.map,true),{paused:true,lat:47,lon:7,zoom:12});
// An inline body inside a script[src] is ignored by browsers: forbid losing restoration that way.
for(const m of html.matchAll(/<script\b([^>]*)>([\s\S]*?)<\/script>/g))if(/\bsrc=/.test(m[1]))assert.equal(m[2].trim(),'','external script must not contain ignored inline code');
console.log('PASS Leaflet view restore, invalid state rejection, explicit follow reset and external-script ordering');
