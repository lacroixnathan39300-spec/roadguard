package fr.roadguard.app;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.*;
public class TestMapConnection {
 static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
 public static void main(String[] args)throws Exception{
  File dir=Files.createTempDirectory("roadguard-map-mode-").toFile();AtomicLong now=new AtomicLong(1000);AtomicInteger fetches=new AtomicInteger();
  VisibleTileCache cache=new VisibleTileCache(dir,1024*1024,0,now::get,(url,headers)->{fetches.incrementAndGet();try{return new VisibleTileCache.Reply(200,TestTileCacheStandalone.png(),Collections.singletonMap("cache-control","max-age=10"));}catch(Exception e){throw new IOException(e);}},folder->Long.MAX_VALUE);
  String url="https://tile.openstreetmap.org/15/16900/11500.png",missing="https://tile.openstreetmap.org/15/16901/11500.png";
  check(cache.get(url,MapConnectionPolicy.allowsNetwork("auto",true))!=null,"Auto online supplies initial tile");check(fetches.get()==1,"one initial fetch");
  now.set(12000);check(cache.get(url,MapConnectionPolicy.allowsNetwork("auto",false))!=null,"Auto outage retains eligible cached tile");check(fetches.get()==1,"outage cannot fetch");
  check(cache.get(missing,MapConnectionPolicy.allowsNetwork("offline",true))==null,"manual offline missing area is unavailable");check(fetches.get()==1,"connected phone cannot bypass manual offline");
  check(cache.get(url,MapConnectionPolicy.allowsNetwork("auto",true))!=null,"Auto resumes after return of Internet");check(fetches.get()==2,"expired tile refreshed after reconnection");
  check(cache.get(missing,MapConnectionPolicy.allowsNetwork("online",false))==null,"online mode does not fabricate network access");check(fetches.get()==2,"no attempts without actual connection");
  check(cache.get(missing,MapConnectionPolicy.allowsNetwork("online",true))!=null,"explicit online allows new area after reconnection");check(fetches.get()==3,"new area fetched once");
  for(File f:dir.listFiles())f.delete();dir.delete();System.out.println("PASS map modes + real tile cache: outage, reconnect, manual offline, missing area, request counts");
 }
}
