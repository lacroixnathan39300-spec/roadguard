// Execute the actual map marker code with small map adapters, no network.
const fs=require('fs'),vm=require('vm'),assert=require('assert'),path=require('path');
const root=path.resolve(__dirname,'..'),assets=path.join(root,'app/src/main/assets');
let decodes=0;
class ImageAdapter {
  constructor(width,height){this.width=width;this.height=height;}
  set src(value){this.url=value;this.complete=true;this.naturalWidth=96;decodes++;if(this.onload)this.onload();}
}
const shared={Image:ImageAdapter};vm.createContext(shared);
vm.runInContext(fs.readFileSync(path.join(assets,'radar_icons.js'),'utf8'),shared);
const icons=shared.RoadguardRadarIcons;
for(const [label,variant] of [
  ['Contrôle routier','control'],['Lieu de contrôle mémorisé','control'],['Radar fixe · 80 km/h','fixed'],['Radar tourelle','turret'],['Radar urbain','urban'],
  ['Radar discriminant','discriminant'],['Radar mobile · lieu signalé, présence non confirmée','mobile'],
  ['Radar feu rouge','redlight'],['Radar tronçon début','average'],['Radar variable','variable'],
  ['Radar fixe · tunnel','tunnel'],['Radar passage à niveau','crossing'],
  ['Radar voie covoiturage','carpool'],['Zone de contrôle possible · présence non confirmée','danger'],
  ['Radar chantier','works'],['Accident','hazard']]){
  assert.equal(icons.describe(label).variant,variant,label);
  const svg=icons.svg(variant,96);assert(svg.includes('<svg')&&svg.includes('<title>'));
  assert(!/[\u{1F300}-\u{1FAFF}]/u.test(svg),'Emoji leaked into vector markup');
  assert(!/<image|<script|https?:\/\//.test(svg.replace('http://www.w3.org/2000/svg','')),'External or active resource');
  assert.equal(fs.readFileSync(path.join(assets,'radar-icons',variant+'.svg'),'utf8'),svg,'Export differs from shared source');
}
assert.equal(icons.variants.length,15);assert(icons.isRadar('Radar feu rouge'));assert(!icons.isRadar('Accident'));

// Leaflet: viewport, typed SVG markup, visibility, size and click popup retained.
const leaf=fs.readFileSync(path.join(assets,'map.html'),'utf8');
const markers=[];const group={addTo(){return this;},clearLayers(){markers.length=0;}};
const leaflet={RoadguardRadarIcons:icons,window:null,map:{getBounds(){return{pad(){return this;},contains(point){return point[0]<48;}};},getSize(){return{x:400,y:400};},latLngToContainerPoint(p){return{x:(p[1]-5)*100,y:(p[0]-46)*100};},getZoom(){return 10;},setView(){this.zoomed=true;},on(){}},
  L:{layerGroup(){return group;},divIcon(options){return options;},marker(position,options){return{bindPopup(popup){this.popup=popup;return this;},on(event,fn){this.click=fn;return this;},addTo(){markers.push(this);this.position=position;this.options=options;return this;}};}}};
leaflet.window=leaflet;vm.createContext(leaflet);
vm.runInContext(fs.readFileSync(path.join(assets,'radar_cluster.js'),'utf8'),leaflet);
vm.runInContext(leaf.slice(leaf.indexOf('let dangerMarkers='),leaf.indexOf('let routeLine=')),leaflet);
vm.runInContext("setDangers([[46,6,'Radar fixe · 80 km/h'],[47.5,6,'Radar tourelle'],[47,7,'Accident'],[49,6,'Radar fixe']])",leaflet);
assert.equal(markers.length,3);assert(markers[0].options.icon.html.includes('<svg'));
assert(markers[1].options.icon.html.includes('data-radar-kind="turret"'));assert(markers[0].popup.includes('80 km/h'));
vm.runInContext('setMapMarkerScale(0)',leaflet);assert(markers[0].options.icon.html.includes('scale(0.78)'));
vm.runInContext('setRadarMarkersVisible(false)',leaflet);assert.equal(markers.length,1);assert(markers[0].popup.includes('Accident'));
vm.runInContext("setRadarMarkersVisible(true);setDangers(Array.from({length:300},(_,i)=>[46+(i%20)*.005,6+Math.floor(i/20)*.005,'Radar fixe']))",leaflet);
assert(markers.length<10,'Dense scene must show clusters, not hundreds of markers');
assert(markers.some(m=>m.options.icon.html.includes('rg-cluster-badge')));
markers.find(m=>m.click).click();assert.equal(leaflet.map.zoomed,true,'Cluster click zooms');

// MapLibre: exact sync/registration code, typed image ids, scale and style reload.
const vector=fs.readFileSync(path.join(assets,'map_maplibre.html'),'utf8'),data={},registered=new Map(),layouts=[];
const map={getCanvas(){return{clientWidth:400,clientHeight:400};},getBounds(){return{contains(){return true;}};},project(p){return{x:(p[0]-5)*100,y:(p[1]-46)*100};},getSource(id){return{setData(value){data[id]=value;}};},getLayer(){return{};},setLayoutProperty(...args){layouts.push(args);},
  hasImage(id){return registered.has(id);},addImage(id,img,options){assert.equal(options.pixelRatio,2);registered.set(id,img);},triggerRepaint(){}};
const context={RoadguardRadarIcons:icons,ready:true,map,route:[],dangers:[],destination:null,lastPos:null,rgShowRadarMarkers:true,rgMapMarkerScale:1,
  valid:(a,b)=>Number.isFinite(+a)&&Number.isFinite(+b),setGPSVisible(){},reportMapError(e){throw e.error;}};
vm.createContext(context);vm.runInContext(fs.readFileSync(path.join(assets,'radar_cluster.js'),'utf8'),context);vm.runInContext(vector.slice(vector.indexOf('function dangerCategory(type)'),vector.indexOf('function setDestination(lat,lon)')),context);
vm.runInContext("setDangers([[46,6,'Radar mobile'],[47.1,6,'Radar passage à niveau'],[48.2,6,'Accident']]);registerRadarPictograms()",context);
assert.equal(registered.size,15);assert.equal(data['rg-alerts'].features[0].properties.icon,'rg-icon-mobile');
assert.equal(data['rg-alerts'].features[1].properties.icon,'rg-icon-crossing');
vm.runInContext('setMapMarkerScale(2)',context);assert.equal(layouts.at(-1)[1],'icon-size');assert.equal(layouts.at(-1)[2],1.3*.875);
vm.runInContext('setRadarMarkersVisible(false)',context);assert.equal(data['rg-alerts'].features.length,1);
registered.clear();vm.runInContext('registerRadarPictograms()',context);assert.equal(registered.size,15);assert.equal(decodes,15,'Same pictograms decoded repeatedly after style reload');
assert(vector.includes("layer('rg-alert-points',{type:'symbol'"));
console.log('Vector icon QA OK: 15 SVGs, classifier, Leaflet viewport/popups/scale/visibility, MapLibre registration/visibility/scale/style reload and image cache');
