package fr.roadguard.app;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class TestCompletion {
  static final byte[] PNG=new byte[]{(byte)137,80,78,71,13,10,26,10,1,2,3};
  static final String URL="https://tile.openstreetmap.org/10/513/510.png";
  static void check(boolean b,String label){if(!b)throw new AssertionError(label);}
  static class Time implements VisibleTileCache.Clock {long now=1_700_000_000_000L;public long now(){return now;}}
  static class Network implements VisibleTileCache.Fetcher {int count;int status=200;String control="max-age=2";boolean fail;Map<String,String> last;
    public synchronized VisibleTileCache.Reply get(String u,Map<String,String> h)throws IOException{count++;last=new HashMap<>(h);if(fail)throw new IOException("simulated network loss");return new VisibleTileCache.Reply(status,PNG,new HashMap<String,String>(){{put("Cache-Control",control);put("ETag","\"test\"");}});}}
  static void cacheTests()throws Exception {
    File root=Files.createTempDirectory("rg-cache-").toFile();Time t=new Time();Network n=new Network();VisibleTileCache cache=new VisibleTileCache(root,1024*1024,t,n);
    check(cache.get(URL,true)!=null&&n.count==1,"initial tile");
    check(cache.get(URL.replace("tile.","a.tile."),true).cached&&n.count==1,"subdomains share cache");
    VisibleTileCache restarted=new VisibleTileCache(root,1024*1024,t,n);
    check(restarted.get(URL,false)!=null&&n.count==1,"survives process restart without network");
    t.now+=3000;n.status=304;check(cache.get(URL,true)!=null&&"\"test\"".equals(n.last.get("If-None-Match")),"conditional revalidation");
    t.now+=3000;check(cache.get(URL,false)!=null,"expired tile usable during outage");
    n.fail=true;check(cache.get(URL,true)!=null,"network error falls back to tile");
    check(cache.get("https://evil.example/10/1/1.png",true)==null,"unrelated source rejected");
    check(VisibleTileCache.canonical("https://tile.openstreetmap.org/1/2/0.png")==null,"out of bounds tile rejected");
    check(VisibleTileCache.canonical("https://user@tile.openstreetmap.org/1/0/0.png")==null,"credentials rejected");
    check(VisibleTileCache.canonical("https://tile.openstreetmap.org/1/0/0.png?x=1")==null,"query rejected");
    File noStore=Files.createTempDirectory("rg-nostore-").toFile();n=new Network();n.control="no-store";cache=new VisibleTileCache(noStore,1024*1024,t,n);cache.get(URL,true);check(cache.get(URL,false)==null&&cache.snapshot().files==0,"no-store respected");
    File must=Files.createTempDirectory("rg-must-").toFile();n=new Network();n.control="max-age=1, must-revalidate";cache=new VisibleTileCache(must,1024*1024,t,n);cache.get(URL,true);t.now+=2000;check(cache.get(URL,false)==null,"must-revalidate respected offline");
    File concurrent=Files.createTempDirectory("rg-concurrent-").toFile();n=new Network();n.control="max-age=600";final VisibleTileCache shared=new VisibleTileCache(concurrent,1024*1024,t,n);ExecutorService pool=Executors.newFixedThreadPool(8);List<Future<?>> pending=new ArrayList<>();for(int i=0;i<16;i++)pending.add(pool.submit(()->{try{check(shared.get(URL,true)!=null,"concurrent tile");}catch(IOException e){throw new RuntimeException(e);}}));for(Future<?> f:pending)f.get();pool.shutdown();check(n.count==1,"one network fetch for concurrent duplicate requests");
    File[] tile=concurrent.listFiles((d,name)->name.endsWith(".tile"));Files.write(tile[0].toPath(),new byte[]{0,1,2});check(shared.get(URL,false)==null,"corrupt file cannot masquerade as a map tile");
    File bounded=Files.createTempDirectory("rg-bounded-").toFile();n=new Network();cache=new VisibleTileCache(bounded,600,t,n);for(int x=0;x<32;x++)cache.get("https://tile.openstreetmap.org/10/"+x+"/0.png",true);check(cache.snapshot().bytes<=600,"cache evicts to budget");
  }
  static void navigationTests(){
    List<double[]> route=Arrays.asList(new double[]{0,0},new double[]{0,.01},new double[]{.01,.01});NavigationProgress model=new NavigationProgress(route);NavigationProgress.Position halfway=model.measure(0,.005,0);
    check(halfway!=null&&halfway.segment==0&&halfway.fraction>.49&&halfway.fraction<.51,"projection between vertices");
    check(halfway.remaining>1600&&halfway.remaining<1700,"remaining road distance, not nearest vertex");
    check(model.toStep(halfway,1)>550&&model.toStep(halfway,1)<565,"maneuver distance along route");
    NavigationProgress.Position later=model.measure(.005,.01,0);check(later.remaining<halfway.remaining&&model.remainingSeconds(later,600)<model.remainingSeconds(halfway,600),"distance and ETA progress together");
    check(model.measure(.00999,.01,0).arrived(),"arrival near final point");
    check(!model.measure(.01,.012,0).arrived(),"off-route location is not an arrival");
    check(NavigationProgress.departure("Départ : route principale"),"accented departure skipped");
    check("↱".equals(NavigationProgress.glyph("Tourner à droite")),"right maneuver icon");
    check("⚑".equals(NavigationProgress.glyph("Arrivée à destination")),"arrival icon");
    check(model.measure(Double.NaN,0,0)==null,"unknown GPS does not invent progress");
  }
  public static void main(String[] args)throws Exception{cacheTests();navigationTests();System.out.println("PASS: persistent tiles, TTL/ETag, offline fallback, corrupt data, concurrency, bounded cache, road progression and arrival.");}
}
