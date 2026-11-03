// Exercise the shipped Leaflet camera and inverse rendered rotation, no network.
const fs=require('fs'),vm=require('vm'),assert=require('assert');
const geometry=require('../app/src/main/assets/camera_geometry.js').RoadguardCameraGeometry;
const html=fs.readFileSync(__dirname+'/../app/src/main/assets/map.html','utf8');
const camera=html.slice(html.indexOf('function rgVisualCamera('),html.indexOf('function rgVisualTick('));
let cases=0;
for(const [width,height] of [[393,852],[852,393],[1280,720]]){
 for(const bearing of [0,45,90,135,180,225,270,315,359]){
  for(const sheet of [0,height*.3,height*.6]){
   for(const anchor of [50,60,66,72,75]){
    const point=[1000.3,700.7];let center={x:1000,y:700},pans=0;
    const map={getSize:()=>({x:width,y:height}),getZoom:()=>15,project:p=>Array.isArray(p)?{x:p[0],y:p[1]}:p,
     unproject:p=>p,getCenter:()=>center,panTo(p){pans++;center={x:Math.round(p[0]),y:Math.round(p[1])};}};
    const ctx={RoadguardCameraGeometry:geometry,map,rgAutoZooming:false,rgUserCameraPaused:false,userCameraHoldUntil:0,
     rgVisualGps:{follow:true,lastCameraFrame:0,speed:70,lookAhead:true},rgHeadingCamera:{enabled:true,angle:bearing},
     rgSheetOcclusionPx:sheet,rgCameraAnchorPercent:anchor,rgCameraSmooth:{},rgSubpixelCamera:{x:0,y:0},rgApplyCameraTransform(){}};
    vm.createContext(ctx);vm.runInContext(camera,ctx);ctx.rgVisualCamera(point,100);
    const a=bearing*Math.PI/180,dx=point[0]-center.x,dy=point[1]-center.y;
    const x=width/2+dx*Math.cos(a)+dy*Math.sin(a)+ctx.rgSubpixelCamera.x;
    const y=height/2-dx*Math.sin(a)+dy*Math.cos(a)+ctx.rgSubpixelCamera.y;
    assert(Math.abs(x-width/2)<1e-7,'horizontal anchor at '+bearing+' degrees');
    assert(Math.abs(y-(height-sheet)*anchor/100)<1e-7,'vertical anchor and sheet');
    assert.deepEqual(point,[1000.3,700.7],'never mutate the GPS fix');
    ctx.rgUserCameraPaused=true;let before=pans;ctx.rgVisualCamera([2000,3000],200);
    assert.equal(pans,before,'manual exploration owns the camera');
    ctx.rgUserCameraPaused=false;ctx.userCameraHoldUntil=Date.now()+20000;
    ctx.rgVisualCamera([2000,3000],300);assert.equal(pans,before,'route overview hold');
    cases++;
   }
  }
 }
}
assert.equal(geometry.percent(NaN),66);assert.equal(geometry.percent(400),75);
assert.equal(geometry.percent(-50),50);
assert.deepEqual(geometry.offset(800,240,false,66),[0,-120]);
// Same fixed GPS point while heading changes: no additional lateral lag.
assert(html.includes('if(rgVisualGps.shown)rgVisualCamera(rgVisualGps.shown,now);'));
assert(!html.includes('resumeCamera();map.setView(center'),'recenter must preserve the configured anchor');
console.log('PASS camera anchor: '+cases+' projected cases, bearings, viewports, sheets, manual pause, immutable GPS');

// Actual heading animation must also pause during manual exploration.
const tick=html.slice(html.indexOf('function rgHeadingCameraTick('),html.indexOf('// Keep the GPU pane origin'));
const c={angle:0,target:90,enabled:true,frame:1,lastFrame:0};
const ctx={rgHeadingCamera:c,rgUserCameraPaused:true,rgVisualGps:{follow:true,shown:null},userCameraHoldUntil:0,rgApplyCameraTransform(){},headingMarker:null,lastHeading:0,requestAnimationFrame(){return 1;}};
vm.createContext(ctx);vm.runInContext(tick,ctx);ctx.rgHeadingCameraTick(100);assert.equal(c.angle,0,'manual mode also freezes map orientation');
ctx.rgUserCameraPaused=false;ctx.rgHeadingCameraTick(200);assert(c.angle>0,'heading animation resumes with following');
console.log('PASS manual heading: the shipped rotation loop freezes during exploration and resumes with tracking');
