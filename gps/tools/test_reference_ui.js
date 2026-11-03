const assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm');
const html=fs.readFileSync('app/src/main/assets/reference_ui.html','utf8');
const script=[...html.matchAll(/<script>([\s\S]*?)<\/script>/g)].map(x=>x[1]).join('\n');
let output='',calls=[];
const box={innerHTML:'',className:'',textContent:''};
const context={document:{getElementById(id){assert.equal(id,'app');return box;}},window:{Roadguard:{action(n,v){calls.push([n,v]);}}}};
vm.createContext(context);vm.runInContext(script,context);
const base={gps:true,night:true,auto:false,radars:[{index:0,type:'Radar fixe · 80 km/h',kind:'fixed',distance:'350 m',place:'Position de la base',coords:'46,7, 5,8',limit:'80'}],routeOptions:[{label:'Le plus rapide',summary:'12 min · 8,4 km'}],routeActive:0,destination:'Champagnole',routeReady:true,frCount:7325,chCount:1777,markerSize:'Normale',settings:{gps:true,radar:true,voice:true,night:true,markers:true,heading:true}};
for(const [page,must] of [['Accueil','ROULER INFORMÉ'],['Trajet','Démarrer l’itinéraire'],['Signaler','Continuer'],['Alertes','Radar fixe'],['Radar','350 m'],['Hors ligne','256 Mo'],['Paramètres','Annonces vocales'],['Apparence','Thème de la carte']]){
  vm.runInContext('window.roadguardUpdate('+JSON.stringify(JSON.stringify({...base,screen:page}))+')',context);
  assert(box.innerHTML.includes(must),page);
  assert(!box.innerHTML.includes('Télécharger un pays'),page);
}
vm.runInContext('window.roadguardUpdate('+JSON.stringify(JSON.stringify({...base,screen:'Signaler',community:{paired:true}}))+')',context);
assert(box.innerHTML.includes('Partager le signalement'),'connected users can share');
vm.runInContext('call("report","Radar fixe");call("toggle","markers")',context);
assert.deepEqual(calls,[['report','Radar fixe'],['toggle','markers']]);
vm.runInContext('window.roadguardUpdate('+JSON.stringify(JSON.stringify({...base,screen:'Alertes',radars:[{...base.radars[0],type:'<img src=x onerror=alert(1)>'}]}))+')',context);
assert(!box.innerHTML.includes('<img src=x onerror=alert(1)>'));
console.log('Reference UI: eight pages, real data slots, actions and escaping OK');
