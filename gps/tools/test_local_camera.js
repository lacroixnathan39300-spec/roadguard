// Exercise the local Canvas map camera functions without a browser or network.
const fs=require('fs'),vm=require('vm'),assert=require('assert'),path=require('path');
const html=fs.readFileSync(path.join(__dirname,'../app/src/main/assets/map_offline.html'),'utf8');
const script=html.slice(html.indexOf('function pauseOfflineCamera()'),html.indexOf('function updateOfflineStatus()'))+html.slice(html.indexOf('function centerOnGps()'),html.indexOf('let last=null,pinch=null;'));
let redraws=0,overviewBounds=0;
const ctx={document:{getElementById(){return {style:{}};}},gps:null,route:[],roads:[],destination:null,trail:[],recordTrail:false,follow:true,
  manualCameraPaused:false,rgSheetOcclusionPx:0,scale:1,panX:0,panY:0,
  minLat:46.7,maxLat:46.8,minLon:5.8,maxLon:6,innerHeight:800,
  bounds(){overviewBounds++;this.minLat=46.6;this.maxLat=46.8;this.minLon=5.8;this.maxLon=6;},
  draw(){redraws++;},Date,Math,Number};
vm.createContext(ctx);vm.runInContext(script,ctx);
vm.runInContext('updateGPS(46.7,6,true)',ctx);
assert(Math.abs((ctx.minLon+ctx.maxLon)/2-6)<1e-9,'First fix centered');
vm.runInContext('rgSetSheetOcclusion(240)',ctx);
assert(ctx.gps[0]>(ctx.minLat+ctx.maxLat)/2,'Blue GPS point shifted above the panel');
vm.runInContext('pauseOfflineCamera()',ctx);
const boundsBefore=ctx.minLon;
vm.runInContext('updateGPS(46.71,6.01,true);setRoads([[[46.7,6]]])',ctx);
assert.equal(ctx.minLon,boundsBefore,'Manual exploration survives GPS and data updates');
assert.equal(ctx.gps[1],6.01,'GPS position continues updating');
vm.runInContext('resumeCamera()',ctx);
assert(Math.abs((ctx.minLon+ctx.maxLon)/2-6.01)<1e-9,'Resume uses latest fix');
vm.runInContext('zoomOffline(1.5)',ctx);assert.equal(ctx.manualCameraPaused,true);
vm.runInContext('resumeCamera();rgManualZoom(-1)',ctx);assert.equal(ctx.manualCameraPaused,true);
vm.runInContext('route=[[46.7,6],[46.8,6.1]];showRouteOverview()',ctx);
assert.equal(ctx.follow,false);assert(overviewBounds>1);
vm.runInContext('resetView()',ctx);assert.equal(ctx.follow,true);assert.equal(ctx.manualCameraPaused,false);
assert(html.includes('if(dx||dy){pauseOfflineCamera();panX+=dx;panY+=dy}'));
assert(html.includes('if(pinch&&pinch>0){pauseOfflineCamera();'));
console.log('Local camera QA OK: GPS follow, sheet, drag/zoom pause, data updates, route overview, recenter');
