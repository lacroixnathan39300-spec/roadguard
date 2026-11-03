(function(root){'use strict';
var ids=['graphite','readable','original','ocean','emerald','amber','custom'];
var keys=['background','water','landcover','building','road','roadOutline','motorway','rail','text','halo'];
var base={night:['#0a1422','#15579c','#163627','#26344d','#daeaff','#425471','#ffd550','#52627e','#edf4ff','#091322'],day:['#f5f2e9','#88c7e7','#bcd5aa','#cdd1d2','#ffffff','#aeb7b9','#f1bd56','#a4a9b0','#344153','#f8f6ed']};
function normalize(id){return ids.indexOf(id)>=0?id:'graphite';}
function resolve(theme,id,custom){id=normalize(id);var night=theme==='night',a=base[night?'night':'day'].slice(),p={};
if(id==='readable')a=night?['#0c1119','#244f78','#193a2c','#354352','#ffffff','#536278','#ffe081','#7b899a','#ffffff','#080c13']:['#fbfaf4','#9bd7ed','#c7dfb4','#cfd4d7','#ffffff','#8c979d','#efba53','#8b9298','#14212d','#ffffff'];
if(id==='original')a=night?['#14252f','#355878','#24482f','#394853','#c7d8ef','#526578','#e9c26b','#74868b','#eef5fa','#10202c']:['#e9eee7','#afd5e8','#bdd6ab','#d9d2c8','#ffffff','#adb5ae','#f2c16a','#a6a5a3','#34403b','#ffffff'];
keys.forEach(function(k,i){p[k]=a[i];});
if(id==='ocean'){p.background=night?'#071b2c':'#eef8ff';p.water=night?'#0a5c96':'#72c8f3';p.motorway=night?'#6de7ff':'#d8a343';}
if(id==='emerald'){p.background=night?'#0a211c':'#f0f8ef';p.water=night?'#17646a':'#8ed8d9';p.landcover=night?'#175139':'#abd79a';p.motorway=night?'#b3ee75':'#e4ad45';}
if(id==='amber'){p.background=night?'#211709':'#fff7e8';p.water=night?'#29444a':'#b5d6dc';p.road=night?'#ffdab0':'#fffdf6';p.motorway=night?'#ffa546':'#d88820';}
p.rasterSaturation=night?(id==='original'?-.04:id==='readable'?-.12:-.3):(id==='original'?0:id==='readable'?-.22:-.44);p.rasterContrast=night?.14:id==='original'?0:.08;p.rasterBrightness=night?.72:1;
p.rasterHue=id==='ocean'?20:id==='emerald'?325:id==='amber'?180:0;
if(id==='custom'&&custom&&typeof custom==='object'){keys.forEach(function(k){if(/^#[a-fA-F0-9]{6}$/.test(custom[k]||''))p[k]=custom[k].toLowerCase();});var h=Number(custom.rasterHue);p.rasterHue=Number.isFinite(h)?Math.max(0,Math.min(360,h)):0;}
return p;}
root.RoadguardPalette={ids:ids,keys:keys,normalize:normalize,resolve:resolve};if(typeof module!=='undefined')module.exports=root.RoadguardPalette;
})(typeof globalThis!=='undefined'?globalThis:this);
