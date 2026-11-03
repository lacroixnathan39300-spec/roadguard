const assert=require('node:assert/strict'),fs=require('fs'),vm=require('vm'),path=require('path');
const assets=path.join(__dirname,'../app/src/main/assets');
const html=fs.readFileSync(path.join(assets,'map.html'),'utf8');
const Recovery=require(path.join(assets,'tile_recovery.js'));
function fixture(native){
 const events={},timers=new Map();let next=0;
 const ctx={RoadguardTileRecovery:Recovery,setTimeout(fn){timers.set(++next,fn);return next;},clearTimeout(id){timers.delete(id);},document:{hidden:false,addEventListener(){}},window:{addEventListener(n,fn){events[n]=fn;}},map:{hasLayer:()=>true},rgLightTiles:{_tiles:{},on(){},_update(){}},rgTileRequests:0,rgTileFailures:0,rgTileSuccesses:0,rgTileStatus(){}};
 if(native)ctx.window.RoadguardMap={};vm.createContext(ctx);vm.runInContext(html.slice(html.indexOf('let rgNetworkOnline=true;'),html.indexOf('rgLightTiles.addTo(map);')),ctx);
 return {events,run:s=>vm.runInContext(s,ctx)};
}
const native=fixture(true);native.run('setRoadguardNetworkState(false)');native.events.online();assert.equal(native.run('rgNetworkOnline'),false,'browser online cannot override native offline');native.run('setRoadguardNetworkState(true)');native.events.offline();assert.equal(native.run('rgNetworkOnline'),true,'only native authority changes native mode');
const browser=fixture(false);browser.events.offline();assert.equal(browser.run('rgNetworkOnline'),false);browser.events.online();assert.equal(browser.run('rgNetworkOnline'),true);
console.log('PASS actual Leaflet native-authority and standalone-browser network event wiring');
