const assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm');
const Recovery=require('../app/src/main/assets/tile_recovery.js');
let scenarios=0;
function clock(){let serial=0,now=0;const jobs=new Map();return {set(fn,delay){jobs.set(++serial,{fn,at:now+delay});return serial;},clear(id){jobs.delete(id);},next(){const item=[...jobs].sort((a,b)=>a[1].at-b[1].at)[0];if(!item)return false;jobs.delete(item[0]);now=item[1].at;item[1].fn();return true;},size(){return jobs.size;}};}
function fixture(){const c=clock(),requests=[],visible=new Set(['a','b']);const r=new Recovery({set:c.set,clear:c.clear,current:k=>visible.has(k),retry:keys=>requests.push(keys)});return {c,r,requests,visible};}
{
 const {c,r,requests}=fixture();r.failure('a');r.failure('a');assert.equal(c.size(),1);
 for(let i=0;i<4;i++)assert(c.next());assert.equal(c.next(),false);assert.equal(requests.length,4);assert(requests.every(keys=>keys.length===1&&keys[0]==='a'));scenarios++;
}
{
 const {c,r,requests}=fixture();r.failure('a');r.network(false);assert.equal(c.size(),0);r.network(true);assert(c.next());assert.equal(requests.length,1);scenarios++;
}
{
 const {c,r,requests}=fixture();r.failure('a');r.success('a');assert.equal(c.size(),0);assert.equal(requests.length,0);scenarios++;
}
{
 const {c,r,requests}=fixture();r.failure('a');r.failure('b');r.visibility(false);assert.equal(c.size(),0);r.visibility(true);c.next();assert.deepEqual(requests[0],['a','b']);scenarios++;
}
{
 const {c,r,requests,visible}=fixture();r.failure('a');visible.delete('a');c.next();assert.equal(requests.length,0);assert.equal(c.size(),0);scenarios++;
}
{
 const {c,r,requests}=fixture();r.failure('a');for(let i=0;i<4;i++)c.next();r.manual();c.next();assert.equal(requests.length,5);r.close();assert.equal(c.size(),0);r.failure('b');assert.equal(c.size(),0);scenarios++;
}
// Execute the actual map wiring, including tileunload during a retry.
{
 const c=clock(),events={},docEvents={},windowEvents={};let requests=0,broken=true;
 const healthy={current:true,el:{id:'healthy'}},tiles={healthy,bad:{current:true}};
 const layer={_tiles:tiles,on(name,fn){events[name]=fn;return this;},_tileCoordsToKey(coords){return coords.key;},_removeTile(key){delete tiles[key];events.tileunload({coords:{key}});},_update(){requests++;tiles.bad={current:true};events[broken?'tileerror':'tileload']({coords:{key:'bad'}});}};
 const context={RoadguardTileRecovery:Recovery,setTimeout:c.set,clearTimeout:c.clear,rgLightTiles:layer,map:{hasLayer:()=>true},rgTileRequests:0,rgTileFailures:0,rgTileSuccesses:0,rgTileStatus(){},document:{hidden:false,addEventListener(name,fn){docEvents[name]=fn;}},window:{addEventListener(name,fn){windowEvents[name]=fn;}}};
 vm.createContext(context);const html=fs.readFileSync('app/src/main/assets/map.html','utf8');
 vm.runInContext(html.slice(html.indexOf('let rgNetworkOnline=true;'),html.indexOf('rgLightTiles.addTo(map);')),context);
 events.tileerror({coords:{key:'bad'}});for(let i=0;i<4;i++)assert(c.next());assert.equal(requests,4);assert.equal(c.size(),0,'unload during retry must not reset retry budget');assert.equal(tiles.healthy,healthy);
 windowEvents.offline();broken=false;windowEvents.online();assert(c.next());assert.equal(requests,5);assert.equal(tiles.healthy,healthy);assert.equal(c.size(),0);scenarios++;
}
{
 const pack=require('../app/src/main/assets/map_packs/appearance.js');assert.equal(pack.normalize('readable'),'readable');assert.equal(pack.normalize('__proto__'),'graphite');assert.equal(pack.normalize('<script>'),'graphite');assert.equal(pack.normalize(null),'graphite');scenarios++;
 const attributes={};const context={document:{body:{setAttribute(key,value){attributes[key]=value;}}}};vm.createContext(context);vm.runInContext(fs.readFileSync('app/src/main/assets/map_packs/appearance.js','utf8'),context);vm.runInContext('setRoadguardMapPack("original")',context);assert.equal(attributes['data-rg-map-pack'],'original');scenarios++;
}
// Render shipped UI strings with supplied state and exercise its bridge actions.
{
 const html=fs.readFileSync('app/src/main/assets/reference_ui.html','utf8');const script=[...html.matchAll(/<script>([\s\S]*?)<\/script>/g)].map(x=>x[1]).join('\n');
 const box={innerHTML:'',className:'',textContent:''},calls=[];
 const context={document:{getElementById(){return box;},addEventListener(){},querySelector(){return null;},documentElement:{style:{setProperty(){}}}},window:{Roadguard:{action(name,value){calls.push([name,value]);}}}};vm.createContext(context);vm.runInContext(script,context);
 function render(state){vm.runInContext('window.roadguardUpdate('+JSON.stringify(JSON.stringify(state))+')',context);return box.innerHTML;}
 let output=render({screen:'Hors ligne',network:true,cacheFiles:278,cacheMb:4,cacheHits:204,cacheDownloads:0,cacheAttempts:2,cacheErrors:2,cacheLastError:'<img src=x onerror=alert(1)>'});
 assert(output.includes('278'));assert(output.includes('204'));assert(output.includes('2 tentatives réseau'));assert(output.includes('Le cache garde les zones déjà vues.'));assert(!output.includes('<img src=x'));assert(output.includes('Réessayer les tuiles manquantes'));scenarios++;
 output=render({screen:'Apparence',mapPack:'readable'});assert(output.includes('Pack graphique local'));assert(output.includes('Lisibilité'));assert(output.includes('Couleurs OSM'));assert(output.includes('routes restent fournies'));for(const pack of ['graphite','readable','original'])for(const mode of ['day','night'])assert(output.includes("mapPalette','"+pack+':'+mode));assert(output.includes('Mode automatique'));vm.runInContext('call("mapPack","readable");call("retryMapTiles","")',context);assert.deepEqual(calls,[['mapPack','readable'],['retryMapTiles','']]);scenarios++;
}
console.log('PASS '+scenarios+' tile recovery / local pack / rendered UI scenarios; actual shipped JS executed; no WebView or device claim');
