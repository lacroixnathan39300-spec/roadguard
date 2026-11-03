package fr.roadguard.app;
import java.io.*;import java.nio.file.*;import java.util.*;import java.util.concurrent.atomic.AtomicInteger;
public final class TestRasterTileSource {
 static int checks;static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
 static byte[] bytes(RasterTileSource.Tile tile)throws Exception{if(tile==null)return null;try(InputStream in=tile.body;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[1024];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return out.toByteArray();}}
 public static void main(String[] args)throws Exception{
  File dir=Files.createTempDirectory("offline-raster-").toFile(),imports=new File(dir,"imports"),cacheDir=new File(dir,"cache");AtomicInteger network=new AtomicInteger();byte[] png=TestTileCacheStandalone.png();String url="https://tile.openstreetmap.org/15/16900/11500.png";
  try{
   VisibleTileCache cache=new VisibleTileCache(cacheDir,1000000,0,()->10000L,(u,h)->{network.incrementAndGet();return new VisibleTileCache.Reply(200,png,Collections.singletonMap("cache-control","max-age=60"));},d->10000000);
   RasterTileSource source=new RasterTileSource(cache,imports);check(source.get(url,false)==null&&network.get()==0,"offline miss used Internet");check(Arrays.equals(bytes(source.get(url,true)),png)&&network.get()==1,"online cache capture");
   VisibleTileCache restarted=new VisibleTileCache(cacheDir,1000000,0,()->20000L,(u,h)->{throw new AssertionError("offline fetch");},d->10000000);
   RasterTileSource cold=new RasterTileSource(restarted,imports);check(Arrays.equals(bytes(cold.get(url,false)),png),"offline restart lost cached tile");
   File local=new File(imports,"15/16900/11501.png");local.getParentFile().mkdirs();Files.write(local.toPath(),png);String missing="https://tile.openstreetmap.org/15/16900/11501.png";check(Arrays.equals(bytes(cold.get(missing,false)),png),"imported XYZ not used when HTTP cache misses");
   Files.write(local.toPath(),new byte[16]);check(cold.get(missing,false)==null,"corrupt imported PNG treated as a map");
   byte[] jpeg=new byte[]{(byte)255,(byte)216,(byte)255,(byte)224,0,2,(byte)255,(byte)217};File jpg=new File(local.getParentFile(),"11501.jpg");Files.write(jpg.toPath(),jpeg);RasterTileSource.Tile tile=cold.get(missing,false);check(tile!=null&&tile.mime.equals("image/jpeg")&&Arrays.equals(bytes(tile),jpeg),"JPEG import fallback or MIME lost");
   try(RandomAccessFile big=new RandomAccessFile(jpg,"rw")){big.setLength(2L*1024*1024+1);}check(cold.get(missing,false)==null,"oversized local tile accepted");
   for(String bad:new String[]{"http://tile.openstreetmap.org/15/16900/11501.png","https://evil.test/15/16900/11501.png","https://tile.openstreetmap.org/15/../11501.png","https://tile.openstreetmap.org/15/16900/99999.png"})check(cold.get(bad,false)==null,"untrusted tile path resolved");
   RasterTileSource withoutCache=new RasterTileSource(null,imports);Files.write(local.toPath(),png);check(Arrays.equals(bytes(withoutCache.get(missing,false)),png),"local tile requires initialized HTTP cache");
   System.out.println("PASS raster source: "+checks+" checks, real cache/files and restart, offline makes zero external requests; JPEG fixture tests signatures only");
  }finally{try(java.util.stream.Stream<Path> files=Files.walk(dir.toPath())){files.sorted(Comparator.reverseOrder()).forEach(p->{try{Files.delete(p);}catch(IOException e){throw new UncheckedIOException(e);}});}}
 }
}
