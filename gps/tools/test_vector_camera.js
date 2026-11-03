// Exercise the map's actual camera functions with a deterministic MapLibre adapter.
const fs=require('fs'),vm=require('vm'),assert=require('assert'),path=require('path');
const html=fs.readFileSync(path.join(__dirname,'../app/src/main/assets/map_maplibre.html'),'utf8');
const geometry=require('../app/src/main/assets/camera_geometry.js').RoadguardCameraGeometry;
const AutoZoom=require('../app/src/main/assets/camera_motion.js').RoadguardAutoZoom;
const calls=[];
const map={getContainer(){return{clientHeight:800};},getZoom(){return 15;},
  easeTo(opts){calls.push(['ease',opts]);},jumpTo(opts){calls.push(['jump',opts]);},fitBounds(bounds,opts){calls.push(['overview',opts]);},
  zoomIn(){calls.push(['zoomIn']);},zoomOut(){calls.push(['zoomOut']);}};
class Bounds{constructor(a){this.points=[a];}extend(p){this.points.push(p);return this;}}
const ctx={window:{},RoadguardViewState:require('../app/src/main/assets/map_view_state.js').RoadguardViewState,rgVectorAutoZoom:new AutoZoom(),document:{querySelector(){return {style:{}};}},map,ready:true,lastPos:null,followOn:true,manualCameraPaused:false,heading:0,route:[],
  RoadguardCameraGeometry:geometry,rgSheetOcclusionPx:0,rgCameraOptions:null,rgAnchorActive:false,rgCameraAnchorPercent:66,maplibregl:{LngLatBounds:Bounds},
  valid:(a,b)=>Number.isFinite(+a)&&Number.isFinite(+b),ensureGpsMarker(){},sync(){},rgShowFollowState(){},rgFollowPause:{style:{}},reportMapError(){}};
vm.createContext(ctx);
vm.runInContext(html.slice(html.indexOf('function rgCameraOffset()'),html.indexOf('// Marqueur DOM'))+html.slice(html.indexOf('function updateGPS('),html.indexOf('function setGPSVisible()')),ctx);
vm.runInContext('rgSetSheetOcclusion(240);updateGPS(46.7,6,true,90,5,true,50,true,true)',ctx);
assert.equal(calls.at(-1)[0],'jump','each GPS fix anchors the vehicle without animation lag');
assert(Math.abs(calls.at(-1)[1].offset[1]+30.4)<1e-9,'GPS anchored in visible screen above sheet');
assert.equal(calls.at(-1)[1].pitch,55);
const center=calls.at(-1)[1].center;
assert.deepEqual(Array.from(center),[6,46.7],'GPS centre stays geographic truth; screen offset supplies look-ahead');
vm.runInContext('manualCameraPaused=true;updateGPS(46.71,6.01,true,90,5,true,50,true,true);rgSetSheetOcclusion(300)',ctx);
assert.equal(calls.length,1,'GPS fixes and sheet changes do not steal manual exploration');
vm.runInContext('resumeCamera()',ctx);
assert.equal(calls.length,2);assert.equal(calls.at(-1)[1].center[0],6.01,'Resume uses newest fix');
assert.equal(calls.at(-1)[1].offset[1],-70);
vm.runInContext('route=[[6,46.7],[6.1,46.8]];showRouteOverview()',ctx);
assert.equal(calls.at(-1)[1].padding.bottom,365);
vm.runInContext('updateGPS(46.72,6.02,true,90,5,true,50,true,true)',ctx);
assert.equal(calls.length,3,'Route overview remains paused across GPS fixes');
vm.runInContext('resumeCamera();rgManualZoom(1)',ctx);
assert.equal(ctx.manualCameraPaused,true);assert.equal(calls.at(-1)[0],'zoomIn');
const beforeManualFix=calls.length;
vm.runInContext('updateGPS(46.73,6.03,true,90,5,true,50,true,true)',ctx);
assert.equal(calls.length,beforeManualFix,'Native zoom keeps follow paused');
assert(!/if\(lastPos\)map\.jumpTo/.test(html),'Style reload must not recenter a paused map');
assert(html.includes('if(lastPos&&followOn&&!manualCameraPaused){if(!rgCameraOptions)'));
assert.equal((html.match(/function updateGPS\(/g)||[]).length,1);
vm.runInContext('focusRoadguardPoint(46.8,6.1)',ctx);assert.equal(ctx.manualCameraPaused,true);assert.deepEqual(Array.from(calls.at(-1)[1].center),[6.1,46.8]);
console.log('MapLibre camera QA OK: sheet clearance, manual pause, latest-fix resume, route overview, style reload guard');

for(const bearing of [0,45,90,180,270]){vm.runInContext('resumeCamera();updateGPS(46.7,6,true,'+bearing+',5,true,50,true,true)',ctx);assert.deepEqual(Array.from(calls.at(-1)[1].center),[6,46.7]);assert.equal(calls.at(-1)[1].offset[0],0);}
ctx.sync=()=>{throw Error('Optional radar layer failure');};
const beforeFailure=calls.length;
vm.runInContext('updateGPS(46.8,6.2,true,90,5,true,50,true,true)',ctx);
assert.equal(calls.length,beforeFailure+1,'radar layer errors cannot prevent GPS camera tracking');
assert.deepEqual(Array.from(calls.at(-1)[1].center),[6.2,46.8]);
