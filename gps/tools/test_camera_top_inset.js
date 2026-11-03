const fs=require('fs'),vm=require('vm'),assert=require('assert');
const geometry=require('../app/src/main/assets/camera_geometry.js').RoadguardCameraGeometry;
const leaf=fs.readFileSync(__dirname+'/../app/src/main/assets/map.html','utf8');
const vector=fs.readFileSync(__dirname+'/../app/src/main/assets/map_maplibre.html','utf8');
const camera=leaf.slice(leaf.indexOf('function rgVisualCamera('),leaf.indexOf('function rgVisualTick('));
const offset=vector.slice(vector.indexOf('function rgCameraOffset('),vector.indexOf('function setCameraAnchor('));
let count=0;
for(const [width,height] of [[393,852],[800,320],[600,240]])for(const top of [0,52,140])for(const bottom of [0,80,120])for(const bearing of [0,45,90,180,270]){
 const point=[1000.3,700.7];let center={x:1000,y:700};
 const map={getSize:()=>({x:width,y:height}),getContainer:()=>({clientHeight:height}),getZoom:()=>15,project:p=>Array.isArray(p)?{x:p[0],y:p[1]}:p,unproject:p=>p,getCenter:()=>center,panTo(p){center={x:Math.round(p[0]),y:Math.round(p[1])};}};
 const ctx={window:{rgTopInsetPx:top},RoadguardCameraGeometry:geometry,map,rgAutoZooming:false,rgUserCameraPaused:false,userCameraHoldUntil:0,rgVisualGps:{follow:true,lastCameraFrame:0,speed:70,lookAhead:true},rgHeadingCamera:{enabled:true,angle:bearing},rgSheetOcclusionPx:bottom,rgCameraAnchorPercent:66,rgAnchorActive:true,rgCameraSmooth:{},rgSubpixelCamera:{x:0,y:0},rgApplyCameraTransform(){}};
 vm.createContext(ctx);vm.runInContext(camera+'\n'+offset,ctx);ctx.rgVisualCamera(point,100);
 const expected=geometry.offset(height,bottom,true,66,top),a=bearing*Math.PI/180,dx=point[0]-center.x,dy=point[1]-center.y;
 const x=width/2+dx*Math.cos(a)+dy*Math.sin(a)+ctx.rgSubpixelCamera.x;
 const y=height/2-dx*Math.sin(a)+dy*Math.cos(a)+ctx.rgSubpixelCamera.y;
 assert(Math.abs(x-width/2)<1e-7);assert(Math.abs(y-height/2-expected[1])<1e-7);
 assert.deepEqual(Array.from(ctx.rgCameraOffset()),expected,'vector and Leaflet use identical native insets');
 const b=Math.min(height*.6,bottom),t=Math.min(height-b,top);assert(y>=t-1e-7&&y<=height-b+1e-7,'GPS anchor outside unobscured interval');
 assert.deepEqual(point,[1000.3,700.7]);count++;
}
assert.deepEqual(geometry.offset(800,240,false,66),[0,-120],'legacy zero top inset');
console.log('PASS top/bottom camera insets: '+count+' projected Leaflet/vector cases; no WebGL rendering');
