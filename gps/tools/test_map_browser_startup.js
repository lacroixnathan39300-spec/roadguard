/* Real Chromium/WebGL, actual bundled scripts and worker. Synthetic PBF fixture;
 * no Android WebView or downloaded-country coverage validation. */
'use strict';
const {chromium}=require('playwright');
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const assets=path.resolve(__dirname,'../app/src/main/assets');
(async()=>{
 const browser=await chromium.launch({headless:true,
  ...(process.env.ROADGUARD_CHROMIUM?{executablePath:process.env.ROADGUARD_CHROMIUM}:{}),
  args:['--no-sandbox','--use-gl=angle','--use-angle=swiftshader','--enable-unsafe-swiftshader']});
 try{
  for(const scenario of ['timer-failure','style-diff-failure','fixed']){
   const broken=scenario==='timer-failure';
   const page=await browser.newPage({viewport:{width:393,height:800}});
   const errors=[],states=[],tiles=[];
   page.on('pageerror',error=>errors.push(error.message));
   await page.exposeFunction('recordEngine',(state,detail)=>states.push({state,detail}));
   await page.addInitScript(()=>{window.RoadguardMap={engineState:(s,d)=>window.recordEngine(s,d)};});
   await page.route('**/*',async route=>{
    const u=new URL(route.request().url());
    if(u.hostname==='roadguard.local'&&u.pathname.startsWith('/assets/')){
     const file=path.join(assets,u.pathname.slice('/assets/'.length));
     if(fs.existsSync(file)&&fs.statSync(file).isFile()){
      if(scenario==='style-diff-failure'&&file.endsWith('/map_maplibre.html'))return route.fulfill({contentType:'text/html',body:fs.readFileSync(file,'utf8').replace('map.setStyle(style,{diff:false});','map.setStyle(style);')});
      if(broken&&file.endsWith('/map_lifecycle.js'))return route.fulfill({contentType:'application/javascript',body:fs.readFileSync(file,'utf8').replace('set.bind(root)','set').replace('clear.bind(root)','clear')});
      return route.fulfill({path:file});
     }
    }
    if(u.hostname==='roadguard.local'&&u.pathname.startsWith('/pmtiles/')){
     tiles.push(u.pathname);return route.fulfill({contentType:'application/x-protobuf',body:Buffer.from([26,0])});
    }
    // No external network in either scenario.
    return route.fulfill({status:503,body:''});
   });
   await page.goto('https://roadguard.local/assets/map_maplibre.html');
   if(broken){
    await page.waitForFunction(()=>document.readyState==='complete');
    assert(errors.some(e=>e.includes('Illegal invocation')),JSON.stringify(errors));
    assert(!states.some(s=>s.state==='ready'));assert.equal(tiles.length,0);
    console.log('PASS baseline 290: real browser reproduces Illegal invocation before map initialization, no vector requests.');
   }else{
    await page.waitForFunction(()=>typeof ready!=='undefined'&&ready);
    await page.evaluate(()=>{setRoadguardNetworkState(false);setVectorPmtiles('all_downloaded',14,false);updateGPS(46.7,5.9,true,0,5,false,0,false,false);});
    if(scenario==='style-diff-failure'){
     await page.waitForFunction(()=>rgMapDiagnostics.sourceReady);
     assert.equal(await page.evaluate(()=>ready),false);assert(tiles.length>0);
     console.log('PASS partial fix reproduces style diff stall: source loads but style.load readiness remains false.');
     await page.close();continue;
    }
    try{await page.waitForFunction(()=>ready&&activeVectorPack==='all_downloaded'&&rgMapDiagnostics.sourceReady,{},{timeout:10000});}catch(e){console.log({states,errors,tiles});console.log(await page.evaluate(()=>getMapDiagnostics()));throw e;}
    assert(states.some(s=>s.state==='ready'));assert(tiles.length>0,'Real worker must request vector tiles');assert.deepEqual(errors,[]);
    assert.equal(await page.evaluate(()=>map.getLayoutProperty('osm-online-layer','visibility')||'visible'),'visible');
    await page.evaluate(()=>rgEngineLifecycle.close());
    assert.deepEqual(errors,[]);
    console.log('PASS 291: real bundled MapLibre/WebGL and worker ready; '+tiles.length+' local vector requests, offline source loaded, cached raster retained, timer cancellation succeeds, no page error. Synthetic PBF, no Android/country data validation.');
   }
   await page.close();
  }
 }finally{await browser.close();}
})().catch(error=>{console.error(error);process.exitCode=1;});
