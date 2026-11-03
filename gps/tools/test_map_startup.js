// Execute the complete packaged MapLibre script against an event-driven renderer.
// This verifies ordering and error recovery, not WebGL/Android rendering.
const assert=require('node:assert/strict'),fs=require('fs'),vm=require('vm'),path=require('path');
const assets=path.join(__dirname,'../app/src/main/assets');
function fixture({throwConstructor=false,optionalFailure=false,viewport=null,bootstrap=null}={}){
 const events={},notifications=[],jobs=new Map();let serial=0;
 const el=()=>({style:{setProperty(){}},className:'',textContent:'',setAttribute(){},appendChild(){},addEventListener(){}});
 const status=el(),document={getElementById:()=>status,createElement:el,body:el(),documentElement:el(),querySelector:()=>el()};
 class MapAdapter{
  constructor(opts){if(throwConstructor)throw Error('GPU unavailable');this.handlers={};this.calls=[];this.paintCalls=[];this.center=opts.center;this.zoom=opts.zoom;this.loaded=false;this.setStyle(opts.style);instance=this;}
  on(name,a,b){(this.handlers[name]??=[]).push({fn:b||a,once:false});return this;}
  once(name,fn){(this.handlers[name]??=[]).push({fn,once:true});return this;}
  emit(name,arg={}){if(name==='style.load')this.loaded=true;const list=[...(this.handlers[name]||[])];this.handlers[name]=(this.handlers[name]||[]).filter(x=>!x.once);for(const item of list)item.fn(arg);}
  setStyle(style){this.loaded=false;this.style=style;this.sources={};this.layers={};for(const [id,value]of Object.entries(style.sources))this.addSource(id,value);for(const layer of style.layers)this.layers[layer.id]=layer;}
  getStyle(){return this.style;}isStyleLoaded(){return this.loaded;}
  addControl(){} getContainer(){return {clientHeight:800};}getCanvas(){return {style:{},clientWidth:400,clientHeight:800};}
  addSource(id,s){this.sources[id]={...s,setData(data){this.data=data;},setTiles(tiles){this.tiles=tiles;}};}
  getSource(id){return this.sources[id];}getLayer(id){return this.layers[id];}addLayer(l){this.layers[l.id]=l;}
  setLayoutProperty(id,key,value){if(!this.loaded)throw Error('Style is not done loading');this.layers[id].layout??={};this.layers[id].layout[key]=value;}
  setPaintProperty(...args){this.paintCalls.push(args);}moveLayer(){}getBounds(){return {contains:()=>true};}project(){return {x:200,y:400};}
  getCenter(){return {lat:this.center[1],lng:this.center[0]};}getZoom(){return this.zoom;}jumpTo(options){if(options.center)this.center=options.center;this.calls.push(options);this.zoom=options.zoom??this.zoom;}
  easeTo(o){this.jumpTo(o);}hasImage(){return false;}addImage(){}triggerRepaint(){}
 }
 let instance;
 class Marker{setLngLat(){return this;}addTo(){return this;}}
 const ctx={localStorage:{getItem:()=>JSON.stringify(viewport),setItem(){}},console,Date,Number,Math,document,setTimeout(fn,ms){jobs.set(++serial,{fn,ms});return serial;},clearTimeout(id){jobs.delete(id);},addEventListener(n,fn){events[n]=fn;},
  RoadguardMap:{mapBootstrap(){return JSON.stringify(bootstrap||{});},engineState(...a){notifications.push(a);}},maplibregl:{Map:MapAdapter,Marker,AttributionControl:class{}},
  RoadguardRadarIcons:{variants:optionalFailure?['fixed']:[],describe:()=>({category:'radar'}),isRadar:()=>true,image(){if(optionalFailure)throw Error('Optional icons unavailable');}},
  RoadguardRadarClusters:{group:()=>[]}};
 ctx.window=ctx;vm.createContext(ctx);
 for(const file of ['map_packs/palette.js','map_lifecycle.js','camera_motion.js','map_view_state.js','camera_geometry.js'])vm.runInContext(fs.readFileSync(path.join(assets,file),'utf8'),ctx);
 const html=fs.readFileSync(path.join(assets,'map_maplibre.html'),'utf8');for(const m of html.matchAll(/<script\b[^>]*>([\s\S]*?)<\/script>/g))if(m[1].trim())vm.runInContext(m[1],ctx);
 return {ctx,notifications,jobs,events,get map(){return instance;},run:s=>vm.runInContext(s,ctx)};
}
let scenarios=0;
{
 const f=fixture();f.run('updateGPS(46.7,6,true,359,5,true,60,true,true)');f.map.emit('style.load');assert.equal(f.notifications[0][0],'ready');assert.equal(f.jobs.size,0);assert(f.map.calls.length);scenarios++;
 f.map.emit('dragstart',{originalEvent:{}});const count=f.map.calls.length;f.run('updateGPS(46.8,6.1,true,1,5,true,65,true,true)');assert.equal(f.map.calls.length,count);f.run('resumeCamera()');assert.equal(f.map.calls.at(-1).center[0],6.1);scenarios++;
 f.run('setVectorPmtiles("all_downloaded",14,true);setRoadguardNetworkState(false)'); // Network loss DURING style loading.
 assert.equal(f.run('rgNetworkOnline'),false);f.map.emit('style.load');assert.equal(f.map.getLayer('osm-online-layer').layout.visibility,'visible','cached raster remains under local vectors offline');
 f.run('setRoadguardNetworkState(true)');assert.equal(f.map.getLayer('osm-online-layer').layout.visibility,'visible');assert.equal(f.map.getLayer('water').layout.visibility,'visible');scenarios++;
 const before=f.map.calls.length;f.map.emit('dragstart',{originalEvent:{}});f.run('setRoadguardNetworkState(false);updateGPS(46.9,6.2,true,1,5,true,-1,true,true)');assert.equal(f.map.calls.length,before);assert(!f.notifications.some(n=>n[0]==='failed'));scenarios++;
}
{
 const f=fixture({throwConstructor:true});assert.equal(f.notifications.length,1);assert.equal(f.notifications[0][0],'failed');assert.match(f.notifications[0][1],/GPU unavailable/);assert.equal(f.jobs.size,0);scenarios++;
}
{
 const f=fixture();assert.equal(f.jobs.size,1);[...f.jobs.values()][0].fn();assert.equal(f.notifications[0][0],'failed');f.map.emit('style.load');assert.equal(f.notifications.length,1,'late style event must not resurrect a failed startup');scenarios++;
}
{
 const f=fixture();f.events.pagehide();assert.equal(f.jobs.size,0);scenarios++;
}
{
 const f=fixture();f.map.emit('style.load');f.run('setVectorPmtiles("all_downloaded",14,false)');f.map.emit('style.load');assert.equal(f.jobs.size,0);assert.equal(f.notifications.filter(n=>n[0]==='ready').length,2);scenarios++;
}
{
 const f=fixture();let now=20000;f.ctx.Date={now:()=>now};f.map.emit('style.load');
 f.run('updateGPS(46.7,6,true,0,5,true,69,true,true)');const initial=f.map.calls.at(-1).zoom;
 for(let i=0;i<20;i++){now+=1000;f.run('updateGPS(46.7,6,true,0,5,true,'+(i%2?69:71)+',true,true)');assert.equal(f.map.calls.at(-1).zoom,initial,'threshold jitter cannot pump zoom');}
 now+=1000;f.run('updateGPS(46.7,6,true,0,5,true,80,true,true)');now+=4000;f.run('updateGPS(46.7,6,true,0,5,true,80,true,true)');assert(f.map.calls.at(-1).zoom<initial);
 const stable=f.map.calls.at(-1).zoom;now+=20000;f.run('updateGPS(46.7,6,true,0,5,true,-1,true,true)');assert.equal(f.map.calls.at(-1).zoom,stable,'absent speed retains zoom');scenarios++;
}
{
 const f=fixture({optionalFailure:true});f.map.emit('style.load');assert.equal(f.notifications[0][0],'ready');assert.equal(f.jobs.size,0);assert(f.run('rgMapDiagnostics.errors.length')>0,'optional image error remains diagnosable');scenarios++;
}
{
 const f=fixture();f.run('restoreRoadguardView({paused:true,lat:47,lon:7,zoom:12})');f.map.emit('style.load');assert.equal(f.map.calls.at(-1).center[0],7);const count=f.map.calls.length;f.run('updateGPS(46,6,true,0,5,true,50,true,true)');assert.equal(f.map.calls.length,count,'restored manual view survives GPS');f.run('resumeCamera()');assert.equal(f.map.calls.at(-1).center[0],6);scenarios++;
}
{
 const f=fixture();f.map.emit('style.load');f.run('setRoadguardGpsState(true)');assert.equal(JSON.parse(f.run('getMapDiagnostics()')).gpsFresh,true);f.run('setRoadguardGpsState(false)');assert.equal(JSON.parse(f.run('getMapDiagnostics()')).gpsFresh,false);assert(f.map.paintCalls.some(c=>c[0]==='rg-position-point'&&c[1]==='circle-color'&&c[2]==='#8e979e'));f.run('setRoadguardGpsState(true)');assert(f.map.paintCalls.some(c=>c[0]==='rg-position-point'&&c[2]==='#20bfff'));scenarios++;
}
{const f=fixture({viewport:{lat:46.74826,lon:5.92218,zoom:17}});assert.equal(f.map.center[0],5.92218);assert.equal(f.map.zoom,17);f.map.emit('style.load');f.run('setVectorPmtiles("all_downloaded",14,false)');f.map.emit('style.load');assert(f.map.getSource('osm-online'),'offline vector style must retain a native-cache raster backdrop');assert.equal(f.map.getLayer('osm-online-layer').layout.visibility,'visible');scenarios++;}
console.log('PASS '+scenarios+' full-script startup/network/manual-camera scenarios (simulated renderer).');

{const f=fixture({bootstrap:{local:true,maxZoom:14,online:true,night:false,pack:'ocean'}});assert(f.map.style.sources.roads,'first style must already use local maps online');f.map.emit('style.load');const before=f.map.style;f.run('setVectorPmtiles("all_downloaded",14,true);setRoadguardNetworkState(false);setRoadguardNetworkState(true)');assert.strictEqual(f.map.style,before,'network restoration must not discard loaded vector tiles');assert.equal(f.map.getLayer('water').layout.visibility,'visible');f.run('setRoadguardMapPack("custom");setRoadguardCustomPalette({road:"#123456",water:"#2789ab",background:"#111222"})');assert(f.map.paintCalls.some(p=>p[0]==='roads'&&p[1]==='line-color'&&p[2]==='#123456'));assert.strictEqual(f.map.style,before,'palette edits must not reload tiles');console.log('PASS immediate local bootstrap online, local visibility on network return, custom paints without style reload');}
