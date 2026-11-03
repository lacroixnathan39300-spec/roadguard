/* ROADGUARD 11.14.168 — original vector road-control pictograms, no emoji/font dependency. */
(function (root) {
  'use strict';
  const ink='#17212b', light='#eef3f6', metal='#c4d0d9', dark='#344451', yellow='#ffd34b';
  const rect=(x,y,w,h,fill,rx=2)=>'<rect x="'+x+'" y="'+y+'" width="'+w+'" height="'+h+'" rx="'+rx+'" fill="'+fill+'"/>';
  const path=(d,fill,stroke='',width=2)=>'<path d="'+d+'" fill="'+fill+'"'+(stroke?' stroke="'+stroke+'" stroke-width="'+width+'" stroke-linecap="round" stroke-linejoin="round"':'')+'/>';
  const circle=(x,y,r,fill)=>'<circle cx="'+x+'" cy="'+y+'" r="'+r+'" fill="'+fill+'"/>';
  function stripe(x,y,w){
    return '<svg x="'+x+'" y="'+y+'" width="'+w+'" height="6" viewBox="0 0 '+w+' 6" overflow="hidden">'
      +rect(0,0,w,6,yellow,0)+path('M-5 6l4-6h4l-4 6zm9 0l4-6h4l-4 6zm9 0l4-6h4l-4 6zm9 0l4-6h4l-4 6z',ink)+'</svg>';
  }
  function cabinet(){
    return path('M23 11h22v39H23z',dark)+rect(19,10,22,39,light,3)+rect(22,14,16,10,ink,1.5)
      +rect(25,16,10,3,'#617f91',1)+rect(22,28,16,10,ink,1.5)+rect(25,30,10,3,'#617f91',1)
      +stripe(19,41,22)+rect(26,49,9,5,dark,1)+rect(22,53,18,3,ink,1);
  }
  function turret(){
    return rect(29,34,7,20,metal,1)+rect(24,53,17,3,ink,1)+path('M22 11h20l3 23H19z',ink)
      +rect(23,13,18,7,light,2)+rect(23,23,18,7,light,2)+rect(28,15,8,3,'#597c93',1)
      +rect(28,25,8,3,'#597c93',1)+rect(21,33,22,4,yellow,1);
  }
  function urban(){
    return rect(28,35,7,19,metal,1)+rect(22,53,19,3,ink,1)+rect(19,13,26,23,ink,5)
      +rect(22,16,20,5,light,1)+rect(22,25,20,7,light,2)+rect(27,27,10,3,'#597c93',1)
      +rect(19,34,26,4,yellow,1);
  }
  function mobile(){
    return path('M31 34L18 54m13-20l15 20m-15-20l1 21','none',light,3.5)
      +rect(17,17,28,21,ink,3)+path('M17 17l5-5h27v21l-4 5V17z',metal)
      +rect(20,20,12,6,light,1)+rect(35,21,7,9,'#5d8197',1)+stripe(18,30,26)
      +path('M48 18q7 5 0 11','none',light,2.5)+path('M52 14q10 10 0 20','none',light,2);
  }
  function control(){
    return path('M18 16l14-7 14 7-4 6H22z',ink)+rect(21,21,22,4,light,2)
      +circle(32,31,7,light)+path('M17 55v-8c0-5 7-9 15-9s15 4 15 9v8z',light)
      +path('M32 40v15','none',ink,3)+path('M44 34l10 4v9c0 5-5 8-10 10-5-2-10-5-10-10v-9z','#2063a4',light,2)
      +path('M39 45h10m-5-5v10','none',light,3);
  }
  function redlight(){
    return rect(29,43,5,12,metal,1)+rect(21,9,23,36,ink,6)
      +circle(32.5,17,5.2,'#ff6875')+circle(32.5,28,4.4,'#83603a')+circle(32.5,38,4.4,'#315f54')
      +path('M18 12h-4v7h4m29-7h4v7h-4m-29 4h-4v7h4m29-7h4v7h-4','none',light,2);
  }
  function average(){
    return path('M22 55l5-44h10l6 44z',ink)+path('M32 16v6m0 6v7m0 7v8','none',light,2.5)
      +path('M13 25V12h38v13M10 52V39h44v13','none',light,3.5)
      +rect(17,10,7,7,yellow,1)+rect(41,10,7,7,yellow,1)
      +rect(15,37,8,7,yellow,1)+rect(42,37,8,7,yellow,1);
  }
  function variable(){
    return path('M12 54V18h40v36','none',light,4)+rect(15,13,34,24,ink,4)
      +path('M23 30V20m-4 4l4-4 4 4m14-4v10m-4-4l4 4 4-4','none',light,2.8)
      +rect(20,40,24,4,yellow,1);
  }
  function tunnel(){
    return path('M11 54V32a21 21 0 0142 0v22z',light)+path('M18 54V32a14 14 0 0128 0v22z',ink)
      +path('M23 54l6-25h6l6 25z',dark)+path('M32 34v5m0 6v6','none',light,2.3)
      +rect(10,38,11,17,metal,1.5)+rect(12,41,7,5,ink,1)+rect(10,49,11,3,yellow,0);
  }
  function crossing(){
    return path('M30 55V12','none',light,4)+path('M19 13l23 17m-23 0l23-17','none',light,7)
      +path('M19 13l7 5m9 7l7 5m-23 0l7-5m9-7l7-5','none','#e44353',4)
      +rect(12,41,40,7,light,1)+path('M15 41l-3 7m14-7l-4 7m15-7l-4 7m15-7l-4 7','none','#e44353',4);
  }
  function carpool(){
    return path('M14 54l7-43h22l7 43z',ink)+path('M22 13l-6 39m26-39l6 39','none',light,2)
      +path('M32 23l8 9-8 9-8-9z','none',light,3)+rect(25,47,14,4,yellow,1);
  }
  function warning(){
    return path('M32 12L10 51h44z',light,ink,2)+rect(30,25,4,13,ink,1)+circle(32,44,2.5,ink);
  }
  function works(){
    return path('M25 13h14l9 37H16z',light)+path('M22 24h20l2 8H20zm-5 17h30l2 9H15z','#eb7d30')
      +rect(11,50,42,6,ink,2)+rect(27,13,10,5,light,1);
  }
  const records={
    control:{label:'Contrôle routier · police ou gendarmerie',color:'#438fd0',body:control},
    fixed:{label:'Radar fixe en cabine',color:'#ffbf32',body:cabinet},
    discriminant:{label:'Radar discriminant en cabine',color:'#e9aa2c',body:cabinet},
    turret:{label:'Radar tourelle',color:'#ffbf32',body:turret},
    urban:{label:'Radar urbain',color:'#ffbf32',body:urban},
    mobile:{label:'Radar mobile sur trépied',color:'#df5a50',body:mobile},
    redlight:{label:'Radar de feu rouge',color:'#bc3449',body:redlight},
    average:{label:'Contrôle de vitesse sur un tronçon',color:'#168bb6',body:average},
    variable:{label:'Radar à limitation variable',color:'#168d78',body:variable},
    tunnel:{label:'Radar dans un tunnel',color:'#6d58af',body:tunnel},
    crossing:{label:'Contrôle de passage à niveau',color:'#8b6950',body:crossing},
    carpool:{label:'Contrôle de voie de covoiturage',color:'#207f9e',body:carpool},
    danger:{label:'Zone de contrôle possible',color:'#d77b31',body:warning},
    works:{label:'Zone de travaux ou radar de chantier',color:'#d68c30',body:works},
    hazard:{label:'Danger sur la route',color:'#d3474b',body:warning}
  };
  function describe(type){
    const t=String(type||'').toLowerCase();let category='hazard',variant='hazard';
    if(t.includes('tunnel')||t.includes('tunel'))category=variant='tunnel';
    else if(t.includes('feu rouge')||t.includes('red light'))category=variant='redlight';
    else if(t.includes('vitesse moyenne')||t.includes('tronçon')||t.includes('average speed'))category=variant='average';
    else if(t.includes('mobile'))category=variant='mobile';
    else if(t.includes('variable')||t.includes('adaptatif'))category=variant='variable';
    else if(t.includes('passage à niveau')||t.includes('level crossing'))category=variant='crossing';
    else if(t.includes('zone dangereuse')||t.includes('zone radar')||t.includes('contrôle possible')||t.includes('controle possible'))category=variant='danger';
    else if(t.includes('travaux')||t.includes('chantier'))category=variant='works';
    else if(/controle routier|controle de police|police|gendarmerie|lieu de controle memorise/.test(t.normalize('NFD').replace(/[\u0300-\u036f]/g,'')))category=variant='control';
    else if(/radar|vitesse|contrôle|controle|speed camera/i.test(t)){
      category='fixed';variant=t.includes('tourelle')?'turret':t.includes('urbain')?'urban':t.includes('discriminant')?'discriminant':t.includes('covoiturage')?'carpool':'fixed';
    }
    return {category,variant,label:records[variant].label};
  }
  function svg(variant,size){
    const record=records[variant]||records.hazard;const dim=Math.max(16,Math.min(512,Number(size)||64));
    return '<svg xmlns="http://www.w3.org/2000/svg" width="'+dim+'" height="'+dim+'" viewBox="0 0 64 64" role="img" aria-label="'+record.label+'"><title>'+record.label+'</title>'
      +rect(2,2,60,60,record.color,15)+path('M17 2h30a15 15 0 0115 15v30a15 15 0 01-15 15H17A15 15 0 012 47V17A15 15 0 0117 2z','none','#f7fafc',3)
      +record.body()+'</svg>';
  }
  const images=Object.create(null),pending=Object.create(null);
  function image(variant,callback){
    const key=records[variant]?variant:'hazard';
    if(images[key]){callback(images[key]);return;}
    if(pending[key]){pending[key].push(callback);return;}
    pending[key]=[callback];const img=new Image(96,96);
    img.onload=function(){images[key]=img;const callbacks=pending[key]||[];delete pending[key];callbacks.forEach(fn=>fn(img));};
    img.onerror=function(){const callbacks=pending[key]||[];delete pending[key];callbacks.forEach(fn=>fn(null));};
    img.src='data:image/svg+xml;charset=utf-8,'+encodeURIComponent(svg(key,96));
  }
  function isRadar(type){return /radar|vitesse|contrôle|controle|speed camera|zone dangereuse|passage à niveau|level crossing/i.test(String(type||''));}
  root.RoadguardRadarIcons=Object.freeze({describe,svg,image,isRadar,variants:Object.freeze(Object.keys(records))});
})(typeof window==='undefined'?globalThis:window);
