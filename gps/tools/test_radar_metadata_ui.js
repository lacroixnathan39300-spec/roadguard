const fs=require('fs'),path=require('path'),vm=require('vm'),assert=require('node:assert/strict');
const html=fs.readFileSync(path.join(__dirname,'../app/src/main/assets/reference_ui.html'),'utf8');
const box={innerHTML:'',className:'',textContent:''};
const ctx={document:{getElementById:()=>box,addEventListener(){},querySelector:()=>null,documentElement:{style:{setProperty(){}}}},window:{Roadguard:{action(){}}}};
vm.createContext(ctx);for(const m of html.matchAll(/<script>([\s\S]*?)<\/script>/g))vm.runInContext(m[1],ctx);
function render(radar){ctx.window.roadguardUpdate(JSON.stringify({screen:'Radar',radars:[radar],alertIndex:0}));return box.innerHTML;}
const known=render({type:'Radar fixe',kind:'fixed',distance:'300 m',coords:'46, 6',sourceName:'<img src=x onerror=alert(1)>',sourceDirection:90,sourceReliability:'import_local'});
assert(known.includes('Orientation fournie'));assert(known.includes('90°'));assert(known.includes('import local'));assert(!known.includes('<img src=x'));assert(known.includes('&lt;img'));
const unknown=render({type:'Radar fixe',kind:'fixed',distance:'300 m',coords:'46, 6'});assert(!unknown.includes('Orientation fournie'));assert(!unknown.includes('Qualification de la source'));
console.log('PASS radar source details: known/unknown metadata, orientation wording and HTML escaping');
