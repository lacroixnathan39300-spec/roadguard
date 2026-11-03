"""Execute the actual MainActivity request-handler bodies with URI/WebView response adapters.
Reads actual packaged HTML/JS/CSS, then real PMTiles bytes. No Android/WebGL emulation.
The preceding version must reproduce the missing HTTPS asset route (404).
"""
from pathlib import Path
import tempfile,subprocess,struct
root=Path(__file__).resolve().parents[1];src=root/'app/src/main/java/fr/roadguard/app'
def body(project):
 s=(project/'app/src/main/java/fr/roadguard/app/MainActivity.java').read_text()
 return s[s.index('  WebResourceResponse localTileNotFound()'):s.index('  // Installe une archive XYZ')]
wrapper='''package fr.roadguard.app;
import java.io.*;import java.net.*;import java.util.*;
class Uri {static Uri parse(String s){return new Uri(s);}final URI u;Uri(String s){u=URI.create(s);}String getHost(){return u.getHost();}String getScheme(){return u.getScheme();}String getPath(){return u.getPath();}public String toString(){return u.toString();}}
class WebResourceRequest {final Uri u;WebResourceRequest(String s){u=new Uri(s);}Uri getUrl(){return u;}String getMethod(){return "GET";}}
class WebResourceResponse {final String mime,encoding;final int status;final InputStream data;final Map<String,String> headers;
WebResourceResponse(String m,String e,InputStream d){this(m,e,200,"OK",new HashMap<>(),d);}
WebResourceResponse(String m,String e,int s,String text,Map<String,String> h,InputStream d){mime=m;encoding=e;status=s;data=d;headers=h;}}
class Pipeline {
final File files,assets;String mapConnectionMode="offline";VisibleTileCache visibleTileCache;boolean network=false,mapLibrePreview=true,vectorEngineDisabledForSession=false;DownloadedMapStore downloaded;
final java.util.concurrent.atomic.AtomicInteger localMapAssetsServed=new java.util.concurrent.atomic.AtomicInteger(),localMapTileRequests=new java.util.concurrent.atomic.AtomicInteger(),localMapTilesServed=new java.util.concurrent.atomic.AtomicInteger();String lastLocalMapFailure="";
Pipeline(File f,File a){files=f;assets=a;}
DownloadedMapStore downloadedMapStore(){if(downloaded==null)downloaded=new DownloadedMapStore(files,new File(files,"cache"));return downloaded;}File getFilesDir(){return files;}
Pipeline getAssets(){return this;}InputStream open(String path)throws IOException{return new FileInputStream(new File(assets,path));}
boolean mapNetworkAvailable(){return network;}
List<InstalledPmtilesMosaic.Region> installedMosaicRegions(){return InstalledPmtilesMosaic.installed(new File(files,"pmtiles"),new File(files,"offline-cells"));}
PmtilesReader reusablePmtilesReader(File f)throws IOException{return new PmtilesReader(f);}
HANDLERS
}
class Probe {
 static int checks;static void ok(boolean b,String text){checks++;if(!b)throw new AssertionError(text);}
 static byte[] read(WebResourceResponse r)throws IOException{try(InputStream in=r.data;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))>=0)out.write(b,0,n);return out.toByteArray();}}
 static WebResourceResponse request(Pipeline p,String url){return p.serveMapResource(new WebResourceRequest(url));}
 public static void main(String[] args)throws Exception{
  Pipeline p=new Pipeline(new File(args[0]),new File(args[1]));
  for(String name:new String[]{"map.html","map_maplibre.html","map_offline.html"}){
   URI page=URI.create("https://roadguard.local/assets/"+name);WebResourceResponse response=request(p,page.toString());
   ok(response!=null&&response.status==200,"packaged HTTPS page unavailable: "+name+" status="+(response==null?null:response.status));ok("text/html".equals(response.mime),"HTML MIME");ok("no-store".equals(response.headers.get("Cache-Control")),"updated assets must not remain stale");
   String html=new String(read(response),"UTF-8");ok(html.contains("<script"),"actual packaged HTML");
   java.util.regex.Matcher references=java.util.regex.Pattern.compile("(?:src|href)=\\\"([^\\\"]+[.](?:js|css))\\\"").matcher(html);
   int resources=0;while(references.find()){
    URI uri=page.resolve(references.group(1));ok(page.getHost().equals(uri.getHost())&&page.getScheme().equals(uri.getScheme()),"same origin subresource");
    WebResourceResponse asset=request(p,uri.toString());ok(asset!=null&&asset.status==200,"missing packaged script/style: "+uri);ok(read(asset).length>20,"actual bytes");resources++;
   }ok(resources>0,"dependencies exercised");
   URI tile=page.resolve("/pmtiles/all_downloaded/0/0/0.pbf");ok(page.getHost().equals(tile.getHost()),"same origin tile endpoint");
   WebResourceResponse vector=request(p,tile.toString());ok(vector.status==200,"vector route");ok(Arrays.equals(read(vector),new byte[]{26,0}),"actual decoded PMTiles bytes");
  }
  WebResourceResponse direct=request(p,"https://roadguard.local/pmtiles/local/0/0/0.pbf");ok(direct.status==200&&read(direct).length==2,"direct legacy file route");
  for(String suffix:new String[]{"../reference_ui.html","%2e%2e/reference_ui.html","vendor/../../reference_ui.html","map.html?x=1","reference_ui.html","private.csv","vendor/missing.js"}){
   WebResourceResponse bad=request(p,"https://roadguard.local/assets/"+suffix);ok(bad!=null&&bad.status==404,"unsafe or unavailable resource: "+suffix);read(bad);
  }
  ok(request(p,"https://example.org/map.html").status==503,"offline external network blocked");
  ok(request(p,"http://roadguard.local/assets/map.html").status==404,"HTTP must not serve packaged pages");
  ok(p.localMapAssetsServed.get()>10&&p.localMapTilesServed.get()==4,"observable pipeline counters");
  // Exercise the real store while Internet is reported available: downloaded bytes still win.
  p.network=true;p.mapConnectionMode="auto";
  WebResourceResponse onlineVector=request(p,"https://roadguard.local/pmtiles/all_downloaded/0/0/0.pbf");
  ok(onlineVector.status==200&&Arrays.equals(read(onlineVector),new byte[]{26,0}),"downloaded vector still used online");
  File imported=new File(p.files,"tiles/6/32/32.png");imported.getParentFile().mkdirs();
  try(FileOutputStream out=new FileOutputStream(imported)){out.write(DownloadedMapStore.TRANSPARENT);}
  p.mapLibrePreview=false;
  for(boolean network:new boolean[]{false,true}){
   p.network=network;p.mapConnectionMode=network?"auto":"offline";
   WebResourceResponse raster=request(p,"https://tile.openstreetmap.org/6/32/32.png");
   ok(raster!=null&&raster.status==200,"downloaded raster must load with network="+network);
   ok("image/png".equals(raster.mime),"downloaded raster MIME");
   ok(Arrays.equals(read(raster),DownloadedMapStore.TRANSPARENT),"downloaded raster bytes win before network");
  }
  p.network=false;p.mapConnectionMode="offline";
  WebResourceResponse missing=request(p,"https://tile.openstreetmap.org/6/31/32.png");
  ok(missing.status==503,"missing offline raster is explicit, not a fake successful tile");read(missing);
  System.out.println("PASS "+checks+" actual-handler checks: three HTTPS map pages, packaged scripts/styles, mosaic/direct vector bytes, origin consistency, offline network guard, asset protections and counters. URI/WebView response adapters; no Android renderer.");
 }
}
'''
with tempfile.TemporaryDirectory() as d:
 t=Path(d);files=t/'files';(files/'pmtiles').mkdir(parents=True)
 payload=bytes([26,0]);directory=bytes([1,0,1,2,1]);h=bytearray(127);h[:7]=b'PMTiles';h[7]=3;h[97]=h[98]=h[99]=1;h[101]=14
 for off,val in [(8,127),(16,len(directory)),(56,127+len(directory)),(64,len(payload))]:struct.pack_into('<Q',h,off,val)
 for off,val in [(102,-100000000),(106,400000000),(110,100000000),(114,500000000)]:struct.pack_into('<i',h,off,val)
 (files/'pmtiles/local.pmtiles').write_bytes(h+directory+payload)
 names=['LocalMapAssets','InstalledPmtilesMosaic','OfflineCellStore','LocalPmtilesResolver','PmtilesReader','NearbyOfflinePlanner','VisibleTileCache','RasterTileSource','DownloadedMapStore']
 sources=[src/(n+'.java') for n in names]+[root/'tools/jvm-support/android/util/Log.java']
 def run(project,baseline=False):
  target=t/('old' if baseline else 'new');target.mkdir();fixture=target/'Probe.java';fixture.write_text(wrapper.replace('HANDLERS',body(project)))
  subprocess.run(['java','com.sun.tools.javac.Main','--release','8','-d',str(target),*map(str,sources),str(fixture)],check=True)
  p=subprocess.run(['java','-cp',str(target),'fr.roadguard.app.Probe',str(files),str(root/'app/src/main/assets')],capture_output=True,text=True)
  if baseline:
   assert p.returncode!=0 and 'packaged HTTPS page unavailable' in p.stderr,p.stdout+p.stderr
   print('EXPECTED baseline 289 failure: the HTTPS map page returns 404 (no packaged asset route).')
  else:
   print(p.stdout,end='');assert p.returncode==0,p.stderr
 previous=root.with_name('ROADGUARD-11.14.289')
 if previous.exists():run(previous,True)
 run(root)
