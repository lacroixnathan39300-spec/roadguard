/* Screen-space GPS anchoring, independent of map bearing and geographic fix. */
(function(root){
 'use strict';
 function percent(value){return Number.isFinite(+value)?Math.max(50,Math.min(75,+value)):66;}
 function offset(height,occluded,active,anchorPercent,top){
  height=Number.isFinite(+height)?Math.max(0,+height):0;
  occluded=Number.isFinite(+occluded)?Math.max(0,Math.min(height*.6,+occluded)):0;
  top=Number.isFinite(+top)?Math.max(0,Math.min(height-occluded,+top)):0;
  return [0,top+(height-occluded-top)*(active?percent(anchorPercent)/100:.5)-height*.5];
 }
 // Inverse of the rendered map rotation R(-bearing). Keep screen X at zero.
 function center(point,bearing,screenOffset){
  const angle=(Number.isFinite(+bearing)?+bearing:0)*Math.PI/180;
  const x=screenOffset[0],y=screenOffset[1];
  return [point.x-x*Math.cos(angle)+y*Math.sin(angle),
          point.y-x*Math.sin(angle)-y*Math.cos(angle)];
 }
 root.RoadguardCameraGeometry={percent,offset,center};
})(typeof module==='object'?module.exports:window);
