const assert=require('node:assert/strict'),fs=require('fs'),vm=require('vm'),path=require('path');
const assets=path.join(__dirname,'../app/src/main/assets');
const source=fs.readFileSync(path.join(assets,'map_view_state.js'),'utf8'),saved=new Map();let at=10000;
function context(storage){const c={localStorage:storage,Date:{now:()=>at}};c.window=c;vm.createContext(c);vm.runInContext(source,c);return c;}
const storage={getItem:k=>saved.get(k)||null,setItem:(k,v)=>saved.set(k,v)},c=context(storage),view=c.RoadguardViewState;
let lat=46.74826,lon=5.92218,zoom=17;const updates=[],manual=[];
const map={getCenter:()=>({lat,lng:lon}),getZoom:()=>zoom},bridge={storeViewport:s=>updates.push(JSON.parse(s)),storeViewState:s=>manual.push(JSON.parse(s))};
view.publish(map,false,bridge);assert.equal(updates.length,1);assert.equal(manual[0],null,'viewport persistence must not pause GPS following');
assert.equal(context(storage).RoadguardViewState.readViewport().zoom,17);assert.equal(view.readViewport().lat,lat);
at++;lat+=.00001;view.publish(map,false,bridge);assert.equal(updates.length,1,'GPS persistence should be throttled');zoom=18;view.publish(map,false,bridge);assert.equal(updates.length,2,'a zoom change must be saved immediately');
const html=fs.readFileSync(path.join(assets,'map.html'),'utf8');
const start=html.indexOf('const rgStartViewport=RoadguardViewState.readViewport();'),end=html.indexOf('// Remove the Leaflet logo',start);let boot;
const bootCtx={RoadguardViewState:view,L:{map:()=>({setView:(center,z)=>{boot={lat:center[0],lon:center[1],zoom:z};return {};}})}};vm.createContext(bootCtx);vm.runInContext(html.slice(start,end),bootCtx);assert.equal(boot.zoom,18);assert.equal(boot.lon,lon,'cold offline boot must use the cached tile viewport');
for(const bad of [null,{}, {lat:91,lon:5,zoom:17},{lat:46,lon:5,zoom:23},{lat:46,lon:null,zoom:17}]){saved.set('roadguard-map-viewport-v1',JSON.stringify(bad));assert.equal(context(storage).RoadguardViewState.readViewport(),null);}
const blocked=context({getItem(){throw Error('storage unavailable');},setItem(){throw Error('storage unavailable');}});assert.equal(blocked.RoadguardViewState.readViewport(),null);let native=0;blocked.RoadguardViewState.publish(map,false,{storeViewport(){native++;}});assert.equal(native,1,'native persistence must remain available when DOM storage fails');
// Exercise the actual Leaflet coverage callback: cached/current and retained parent images.
const timers=new Map(),states=[],misses=[];let id=0;
const offline={RoadguardTileRecovery:require(path.join(assets,'tile_recovery.js')),setTimeout(fn){timers.set(++id,fn);return id;},clearTimeout(i){timers.delete(i);},document:{hidden:false,addEventListener(){}},window:{addEventListener(){},RoadguardMap:{offlineTilesMissing(...p){misses.push(p);}}},map:{hasLayer:()=>true,getCenter:()=>({lat:46.74826,lng:5.92218})},rgLightTiles:{_tiles:{},on(){},_update(){}},rgTileRequests:0,rgTileFailures:0,rgTileSuccesses:0,rgTileStatus:t=>states.push(t)};
vm.createContext(offline);vm.runInContext(html.slice(html.indexOf('let rgNetworkOnline=true;'),html.indexOf('rgLightTiles.addTo(map);')),offline);
function flush(){const jobs=[...timers.values()];timers.clear();jobs.forEach(fn=>fn());}
vm.runInContext('setRoadguardNetworkState(false)',offline);flush();assert.match(states.at(-1),/Des tuiles manquent/);assert.equal(misses.length,1);
offline.rgLightTiles._tiles.good={current:true,el:{complete:true,naturalWidth:256}};vm.runInContext('rgCheckOfflineCoverage()',offline);flush();assert.equal(states.at(-1),'');assert.equal(misses.length,1,'good cached image must not trigger destructive fallback');
offline.rgLightTiles._tiles.bad={current:true,el:{complete:true,naturalWidth:0}};vm.runInContext('rgCheckOfflineCoverage()',offline);flush();assert.equal(misses.length,2,'one loaded tile must not mask the missing remainder');
offline.rgLightTiles._tiles={parent:{current:false,active:true,el:{complete:true,naturalWidth:256}}};vm.runInContext('rgCheckOfflineCoverage()',offline);flush();assert.equal(states.at(-1),'','retained parent tile is valid coverage');
offline.rgLightTiles._tiles={broken:{current:true,el:{complete:true,naturalWidth:0}}};vm.runInContext('rgCheckOfflineCoverage()',offline);flush();assert.match(states.at(-1),/Des tuiles manquent/);
console.log('PASS offline regressions: cold boot retains actual viewport/zoom without pausing GPS, throttled/native persistence, invalid storage and real cached/parent/missing coverage callbacks; simulated map, no WebView');
