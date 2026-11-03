/* Validated ephemeral exploration view, shared by online and local renderers. */
(function(root){
 function normalize(value){if(!value||value.paused!==true)return null;
  const lat=value.lat,lon=value.lon,zoom=value.zoom;
  if(!Number.isFinite(lat)||!Number.isFinite(lon)||!Number.isFinite(zoom)||Math.abs(lat)>90||Math.abs(lon)>180||zoom<0||zoom>22)return null;
  return {paused:true,lat,lon,zoom};
 }
 function capture(map,paused){if(!paused||!map||typeof map.getCenter!=='function')return null;const center=map.getCenter();return normalize({paused:true,lat:center.lat,lon:center.lng,zoom:map.getZoom()});}
 function viewport(value){
  if(!value||!Number.isFinite(value.lat)||!Number.isFinite(value.lon)||!Number.isFinite(value.zoom)||Math.abs(value.lat)>90||Math.abs(value.lon)>180||value.zoom<0||value.zoom>22)return null;
  return {lat:value.lat,lon:value.lon,zoom:value.zoom};
 }
 function readViewport(){try{return viewport(JSON.parse(root.localStorage.getItem('roadguard-map-viewport-v1')));}catch(unavailable){return null;}}
 let storedViewportAt=0,storedZoom;
 function saveViewport(map,bridge){try{
  if(!map||typeof map.getCenter!=='function')return;const center=map.getCenter(),value=viewport({lat:center.lat,lon:center.lng,zoom:map.getZoom()});if(!value)return;
  const now=Date.now();if(storedZoom===value.zoom&&now-storedViewportAt<1000)return;
  try{root.localStorage.setItem('roadguard-map-viewport-v1',JSON.stringify(value));}catch(unavailable){}
  if(bridge&&typeof bridge.storeViewport==='function')bridge.storeViewport(JSON.stringify(value));storedViewportAt=now;storedZoom=value.zoom;
 }catch(unavailable){}}
 let lastBridge,lastValue;
 function publish(map,paused,bridge){saveViewport(map,bridge);if(!bridge||typeof bridge.storeViewState!=='function')return;const view=capture(map,paused),value=JSON.stringify(view);if(bridge===lastBridge&&value===lastValue)return;lastBridge=bridge;lastValue=value;bridge.storeViewState(value);}
 root.RoadguardViewState={normalize,capture,publish,readViewport,saveViewport,normalizeViewport:viewport};
})(typeof module==='object'?module.exports:window);
