// Android's zoom actions must pause follow consistently in every map engine.
const fs=require('fs'),vm=require('vm'),assert=require('assert'),path=require('path');
const root=path.join(__dirname,'..'),assets=path.join(root,'app/src/main/assets');
const native=fs.readFileSync(path.join(root,'app/src/main/java/fr/roadguard/app/MainActivity.java'),'utf8');
for(const call of ['rgManualZoom(1)','rgManualZoom(-1)'])assert(native.includes(call),call);
const leaflet=fs.readFileSync(path.join(assets,'map.html'),'utf8');
let zoom=0;const status={textContent:''};const ctx={rgUserCameraPaused:false,userCameraHoldUntil:10,
  document:{getElementById(){return status;}},map:{zoomIn(){zoom++;},zoomOut(){zoom--;}}};
vm.createContext(ctx);
vm.runInContext(leaflet.slice(leaflet.indexOf('function rgManualZoom(direction)'),leaflet.indexOf('function smoothHeading(deg)')),ctx);
vm.runInContext('rgManualZoom(1);rgManualZoom(-1)',ctx);
assert.equal(zoom,0);assert.equal(ctx.rgUserCameraPaused,true);assert.equal(ctx.userCameraHoldUntil,0);
assert(status.textContent.includes('SUIVI EN PAUSE'));
for(const name of ['map_maplibre.html','map_offline.html']){
  const map=fs.readFileSync(path.join(assets,name),'utf8');assert(map.includes('function rgManualZoom(direction)'));
}
console.log('Camera controls QA OK: native zoom dispatch, Leaflet pause, vector/local handlers');
