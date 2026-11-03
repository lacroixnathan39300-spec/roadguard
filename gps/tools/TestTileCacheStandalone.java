package fr.roadguard.app;





import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.zip.*;




public class TestTileCacheStandalone {
  final String url="https://tile.openstreetmap.org/15/16900/11500.png";
  File directory;AtomicLong clock;AtomicInteger requests;
  public void setup(){directory=tempDirectory();clock=new AtomicLong(1000);requests=new AtomicInteger();}
  static byte[] png()throws Exception{
    ByteArrayOutputStream out=new ByteArrayOutputStream();out.write(new byte[]{(byte)137,80,78,71,13,10,26,10});ByteArrayOutputStream h=new ByteArrayOutputStream();DataOutputStream header=new DataOutputStream(h);header.writeInt(1);header.writeInt(1);header.write(new byte[]{8,6,0,0,0});chunk(out,"IHDR",h.toByteArray());
    ByteArrayOutputStream raw=new ByteArrayOutputStream();try(DeflaterOutputStream compressed=new DeflaterOutputStream(raw)){compressed.write(new byte[]{0,0,0,0,(byte)255});}chunk(out,"IDAT",raw.toByteArray());chunk(out,"IEND",new byte[0]);return out.toByteArray();
  }
  static void chunk(ByteArrayOutputStream out,String type,byte[] data)throws Exception{DataOutputStream writer=new DataOutputStream(out);byte[] name=type.getBytes("US-ASCII");writer.writeInt(data.length);writer.write(name);writer.write(data);CRC32 crc=new CRC32();crc.update(name);crc.update(data);writer.writeInt((int)crc.getValue());}
  VisibleTileCache cache(VisibleTileCache.Fetcher fetch){return new VisibleTileCache(directory,1024*1024,clock::get,fetch);}
  public void completeTileSurvivesOfflineRestart()throws Exception{byte[] data=png();VisibleTileCache first=cache((u,h)->{requests.incrementAndGet();return new VisibleTileCache.Reply(200,data,Collections.singletonMap("cache-control","max-age=60"));});assertArrayEquals(data,first.get(url,true).bytes);VisibleTileCache restarted=cache((u,h)->{throw new AssertionError("offline must not fetch");});assertArrayEquals(data,restarted.get(url,false).bytes);assertEquals(1,requests.get());}
  public void truncatedPngDoesNotReplacePreviousGoodTile()throws Exception{byte[] data=png();AtomicBoolean broken=new AtomicBoolean(false);VisibleTileCache cache=cache((u,h)->new VisibleTileCache.Reply(200,broken.get()?Arrays.copyOf(data,data.length-12):data,Collections.singletonMap("cache-control","max-age=1")));assertNotNull(cache.get(url,true));clock.addAndGet(2000);broken.set(true);assertArrayEquals(data,cache.get(url,true).bytes);assertArrayEquals(data,cache.get(url,false).bytes);}
  public void corruptCrcAndHeaderOnlyPayloadAreRejected()throws Exception{byte[] data=png();data[45]^=1;VisibleTileCache cache=cache((u,h)->new VisibleTileCache.Reply(200,data,Collections.emptyMap()));assertNull(cache.get(url,true));assertEquals(0,cache.snapshot().files);VisibleTileCache header=cache((u,h)->new VisibleTileCache.Reply(200,Arrays.copyOf(pngUnchecked(),8),Collections.emptyMap()));assertNull(header.get(url,true));}
  byte[] pngUnchecked(){try{return png();}catch(Exception e){throw new RuntimeException(e);}}
  public void separatePhoneAndCarCachesShareSingleFetch()throws Exception{byte[] data=png();VisibleTileCache.Fetcher fetch=(u,h)->{requests.incrementAndGet();try{Thread.sleep(40);}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IOException(e);}return new VisibleTileCache.Reply(200,data,Collections.singletonMap("cache-control","max-age=60"));};VisibleTileCache phone=cache(fetch),car=cache(fetch);ExecutorService pool=Executors.newFixedThreadPool(2);try{Future<?> a=pool.submit(()->{try{assertNotNull(phone.get(url,true));}catch(IOException e){throw new RuntimeException(e);}}),b=pool.submit(()->{try{assertNotNull(car.get(url,true));}catch(IOException e){throw new RuntimeException(e);}});a.get(5,TimeUnit.SECONDS);b.get(5,TimeUnit.SECONDS);}finally{pool.shutdownNow();}assertEquals(1,requests.get());}
  public void etag304RevalidatesWithoutLosingBytes()throws Exception{byte[] data=png();AtomicBoolean first=new AtomicBoolean(true);VisibleTileCache cache=cache((u,h)->{if(first.getAndSet(false))return new VisibleTileCache.Reply(200,data,new HashMap<String,String>(){{put("etag","tile-v1");put("cache-control","max-age=1");}});assertEquals("tile-v1",h.get("If-None-Match"));return new VisibleTileCache.Reply(304,new byte[0],Collections.singletonMap("cache-control","max-age=60"));});assertArrayEquals(data,cache.get(url,true).bytes);clock.addAndGet(2000);assertArrayEquals(data,cache.get(url,true).bytes);assertEquals(1,cache.snapshot().files);}
  String tileUrl(int index){return "https://tile.openstreetmap.org/15/"+(16900+index)+"/11500.png";}
  VisibleTileCache sized(long limit,VisibleTileCache.Space space){return new VisibleTileCache(directory,limit,0,clock::get,(u,h)->new VisibleTileCache.Reply(200,pngUnchecked(),Collections.singletonMap("cache-control","max-age=60")),space);}
  public void fourGiBBudgetRetainsCacheLargerThanSignedInteger()throws Exception{
    assertEquals(4L*1024*1024*1024,new VisibleTileCache(directory).limitBytes());directory.mkdirs();File old=new File(directory,"old.tile");
    try(RandomAccessFile sparse=new RandomAccessFile(old,"rw")){sparse.setLength(3L*1024*1024*1024);}
    VisibleTileCache cache=sized(VisibleTileCache.DEFAULT_MAX_BYTES,d->10L*1024*1024*1024);
    assertNotNull(cache.get(url,true));assertTrue(old.exists());assertTrue(cache.snapshot().bytes>Integer.MAX_VALUE);assertTrue(cache.snapshot().bytes<=VisibleTileCache.DEFAULT_MAX_BYTES);
  }
  public void lowFreeSpaceKeepsLiveMapWorkingWithoutCachingNewTile()throws Exception{
    VisibleTileCache cache=new VisibleTileCache(directory,VisibleTileCache.DEFAULT_MAX_BYTES,512L*1024*1024,clock::get,(u,h)->new VisibleTileCache.Reply(200,pngUnchecked(),Collections.emptyMap()),d->100L*1024*1024);
    assertArrayEquals(png(),cache.get(url,true).bytes);assertEquals(0,cache.snapshot().files);assertNull(cache.get(url,false));
  }
  public void cleanupRemovesLeastRecentlyViewedAndKeepsBudgetBounded()throws Exception{
    VisibleTileCache seed=sized(100000,d->10000000);seed.get(tileUrl(0),true);long size=seed.snapshot().bytes;
    clock.addAndGet(1000);seed.get(tileUrl(1),true);clock.addAndGet(1000);seed.get(tileUrl(2),true);
    clock.addAndGet(1000);assertNotNull(seed.get(tileUrl(0),false));
    VisibleTileCache small=sized(size*3,d->10000000);clock.addAndGet(1000);small.get(tileUrl(3),true);
    assertTrue(small.snapshot().bytes<=size*3);assertNull(small.get(tileUrl(1),false));assertNotNull(small.get(tileUrl(0),false));assertNotNull(small.get(tileUrl(3),false));
  }
  public void phoneAndCarConcurrentWritesUseOneCapacity()throws Exception{
    VisibleTileCache phone=sized(600,d->10000000),car=sized(600,d->10000000);ExecutorService pool=Executors.newFixedThreadPool(4);List<Future<?>> jobs=new ArrayList<>();
    try{for(int i=0;i<24;i++){final int n=i;jobs.add(pool.submit(()->{try{assertNotNull((n%2==0?phone:car).get(tileUrl(n),true));}catch(IOException e){throw new RuntimeException(e);}}));}for(Future<?> job:jobs)job.get(10,TimeUnit.SECONDS);}finally{pool.shutdownNow();}
    assertTrue(phone.snapshot().bytes<=600);assertEquals(phone.snapshot().bytes,car.snapshot().bytes);assertTrue(phone.snapshot().files<24);
  }
  public void lowSpaceRefreshNeverDeletesPreviousGoodVersion()throws Exception{
    AtomicLong available=new AtomicLong(10000000);
    VisibleTileCache cache=new VisibleTileCache(directory,100000,1000,clock::get,(u,h)->new VisibleTileCache.Reply(200,pngUnchecked(),Collections.singletonMap("cache-control","max-age=1")),d->available.get());
    assertNotNull(cache.get(url,true));clock.addAndGet(2000);available.set(0);
    assertArrayEquals(png(),cache.get(url,true).bytes);assertArrayEquals(png(),cache.get(url,false).bytes);assertEquals(1,cache.snapshot().files);
  }
  public void externallyClearedCacheReconcilesSharedCounters()throws Exception{
    VisibleTileCache cache=sized(100000,d->10000000);cache.get(url,true);assertEquals(1,cache.snapshot().files);
    for(File file:directory.listFiles())assertTrue(file.delete());directory.setLastModified(directory.lastModified()+10000);
    assertEquals(0,cache.snapshot().files);assertEquals(0,cache.snapshot().bytes);assertNotNull(cache.get(url,true));assertEquals(1,cache.snapshot().files);
  }
  public void serverNoStoreRemovesPreviousTileAndUpdatesCounters()throws Exception{
    AtomicBoolean noStore=new AtomicBoolean(false);
    VisibleTileCache cache=new VisibleTileCache(directory,100000,0,clock::get,(u,h)->new VisibleTileCache.Reply(200,pngUnchecked(),Collections.singletonMap("cache-control",noStore.get()?"no-store":"max-age=1")),d->10000000);
    cache.get(url,true);assertEquals(1,cache.snapshot().files);clock.addAndGet(2000);noStore.set(true);assertNotNull(cache.get(url,true));assertEquals(0,cache.snapshot().files);assertEquals(0,cache.snapshot().bytes);
  }
  public void tileLargerThanBudgetIsDisplayedButNotStored()throws Exception{
    VisibleTileCache cache=sized(1,d->10000000);assertArrayEquals(png(),cache.get(url,true).bytes);assertEquals(0,cache.snapshot().files);assertTrue(cache.snapshot().bytes<=1);
  }

  static File tempDirectory(){try{return java.nio.file.Files.createTempDirectory("roadguard-tile-check-").toFile();}catch(IOException e){throw new RuntimeException(e);}}
  static void assertTrue(boolean value){if(!value)throw new AssertionError("Expected true");}
  static void assertNull(Object value){if(value!=null)throw new AssertionError("Expected null");}
  static void assertNotNull(Object value){if(value==null)throw new AssertionError("Expected value");}
  static void assertEquals(long a,long b){if(a!=b)throw new AssertionError(a+" != "+b);}
  static void assertEquals(String a,String b){if(!Objects.equals(a,b))throw new AssertionError(a+" != "+b);}
  static void assertArrayEquals(byte[] a,byte[] b){if(!Arrays.equals(a,b))throw new AssertionError("Tile bytes changed");}
  static void remove(File path){File[] files=path.listFiles();if(files!=null)for(File child:files)remove(child);path.delete();}
  public void transientFailureBackoffAndReconnectRetry()throws Exception{
    AtomicBoolean broken=new AtomicBoolean(true);
    VisibleTileCache cache=sized(100000,d->10000000);
    VisibleTileCache changing=new VisibleTileCache(directory,100000,0,clock::get,(u,h)->new VisibleTileCache.Reply(broken.get()?503:200,pngUnchecked(),Collections.emptyMap()),d->10000000);
    assertNull(changing.get(url,true));assertEquals(1,changing.snapshot().attempts);assertEquals(1,changing.snapshot().failures);
    assertTrue(changing.snapshot().lastFailure.contains("503"));assertNull(changing.get(url,true));assertEquals(1,changing.snapshot().attempts);
    broken.set(false);changing.retryNetworkFailures();assertNotNull(changing.get(url,true));assertEquals(2,changing.snapshot().attempts);assertEquals(1,changing.snapshot().downloads);assertEquals("",changing.snapshot().lastFailure);
  }
  public void cachedRestartCanHaveZeroNetworkDownloads()throws Exception{
    VisibleTileCache first=sized(100000,d->10000000);assertNotNull(first.get(url,true));
    VisibleTileCache second=new VisibleTileCache(directory,100000,0,clock::get,(u,h)->{throw new AssertionError("Cached tile must not hit network");},d->10000000);
    assertNotNull(second.get(url,true));assertEquals(0,second.snapshot().downloads);assertEquals(0,second.snapshot().attempts);assertEquals(1,second.snapshot().hits);assertEquals(1,second.snapshot().files);
  }
  public void mustRevalidateNeverServesStaleOffline()throws Exception{
    VisibleTileCache cache=new VisibleTileCache(directory,100000,0,clock::get,(u,h)->new VisibleTileCache.Reply(200,pngUnchecked(),Collections.singletonMap("cache-control","max-age=1,must-revalidate")),d->10000000);
    assertNotNull(cache.get(url,true));clock.addAndGet(2000);assertNull(cache.get(url,false));assertEquals(1,cache.snapshot().files);
  }
  public void invalidDestinationsNeverReachTransport()throws Exception{
    VisibleTileCache cache=new VisibleTileCache(directory,100000,0,clock::get,(u,h)->{throw new AssertionError("Invalid URL fetched");},d->10000000);
    for(String invalid:new String[]{"http://tile.openstreetmap.org/1/0/0.png","https://evil.example/1/0/0.png","https://tile.openstreetmap.org/1/2/0.png","https://tile.openstreetmap.org/1/0/0.png?x=1"})assertNull(cache.get(invalid,true));
    assertEquals(0,cache.snapshot().attempts);
  }
  public static void main(String[] args)throws Exception{
    int scenarios=0;
    for(java.lang.reflect.Method method:TestTileCacheStandalone.class.getDeclaredMethods()){
      if(!java.lang.reflect.Modifier.isPublic(method.getModifiers())||java.lang.reflect.Modifier.isStatic(method.getModifiers())||method.getParameterCount()!=0||method.getName().equals("setup"))continue;
      TestTileCacheStandalone test=new TestTileCacheStandalone();test.setup();try{method.invoke(test);scenarios++;}catch(java.lang.reflect.InvocationTargetException e){throw new AssertionError(method.getName(),e.getCause());}finally{remove(test.directory);}
    }
    System.out.println("PASS "+scenarios+" real-file tile-cache scenarios; HTTP transport injected; no Android runtime");
  }

}
