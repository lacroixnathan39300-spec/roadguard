const assert=require('node:assert/strict'),fs=require('fs'),path=require('path'),vm=require('vm');
const html=fs.readFileSync(path.join(__dirname,'../app/src/main/assets/map.html'),'utf8');
const attributes={},point=[],ring=[];
const ctx={document:{body:{setAttribute(k,v){attributes[k]=v;}}},pin:{setStyle:s=>point.push(s)},accuracyRing:{setStyle:s=>ring.push(s)}};
vm.createContext(ctx);vm.runInContext(html.slice(html.indexOf('let rgGpsFresh=false;'),html.indexOf('function updateGPS(')),ctx);
ctx.setRoadguardGpsState(false);assert.equal(attributes['data-rg-gps-fresh'],'false');assert.equal(point.at(-1).fillColor,'#8e979e');assert(ring.at(-1).opacity<.2);
ctx.setRoadguardGpsState(true);assert.equal(point.at(-1).fillColor,'#00bfff');assert.equal(point.at(-1).fillOpacity,1);assert.equal(attributes['data-rg-gps-fresh'],'true');
ctx.pin=null;ctx.accuracyRing=null;ctx.setRoadguardGpsState(false);
console.log('PASS actual Leaflet GPS fresh/stale style function with injected marker boundaries');
