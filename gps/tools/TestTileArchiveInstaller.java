package fr.roadguard.app;
import java.io.*;import java.nio.file.*;import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;import java.util.zip.*;
public final class TestTileArchiveInstaller {
 static int checks;
 static byte[] PNG={(byte)137,80,78,71,13,10,26,10,0,0,0,13,73,72,68,82};
 static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
 static byte[] zip(String[] names,byte[][] bytes)throws Exception{ByteArrayOutputStream out=new ByteArrayOutputStream();try(ZipOutputStream z=new ZipOutputStream(out)){for(int i=0;i<names.length;i++){z.putNextEntry(new ZipEntry(names[i]));z.write(bytes[i]);z.closeEntry();}}return out.toByteArray();}
 static byte[] zip(String name,byte[] bytes)throws Exception{return zip(new String[]{name},new byte[][]{bytes});}
 static void install(byte[] data,File base)throws Exception{TileArchiveInstaller.install(new ByteArrayInputStream(data),base);}
 static void rejected(byte[] data,File base)throws Exception{try{install(data,base);throw new AssertionError("archive accepted");}catch(IOException expected){checks++;}}
 static void old(File base)throws Exception{base.mkdirs();Files.write(new File(base,"original.txt").toPath(),"preserve".getBytes("UTF-8"));}
 static void retained(File base)throws Exception{check(new String(Files.readAllBytes(new File(base,"original.txt").toPath()),"UTF-8").equals("preserve"),"old map lost");}
 static class Paused extends FilterInputStream {
  final CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);boolean first=true;
  Paused(byte[] bytes){super(new ByteArrayInputStream(bytes));}
  @Override public int read(byte[] bytes,int off,int count)throws IOException{if(first){first=false;entered.countDown();try{if(!release.await(5,TimeUnit.SECONDS))throw new IOException("fixture timeout");}catch(InterruptedException e){Thread.currentThread().interrupt();throw new InterruptedIOException();}}return super.read(bytes,off,count);}
 }
 public static void main(String[] args)throws Exception{
  Path temp=Files.createTempDirectory("rg-tile-installer-");File base=temp.resolve("tiles").toFile();ExecutorService worker=Executors.newSingleThreadExecutor();
  try{
   old(base);rejected(zip("12/1/1.png",new byte[]{1,2,3}),base);retained(base);
   rejected(zip("1/2/0.png",PNG),base);retained(base);
   rejected(zip(new String[]{"12/1/1.png","12/1/1.jpg"},new byte[][]{PNG,new byte[]{(byte)255,(byte)216,(byte)255}}),base);retained(base);
   rejected(zip("readme.txt",new byte[]{1}),base);retained(base);
   byte[] huge=new byte[2*1024*1024+1];System.arraycopy(PNG,0,huge,0,PNG.length);rejected(zip("12/1/1.png",huge),base);retained(base);
   byte[] valid=zip(new String[]{"tiles/10/1/2.png","12/3/4.jpg","../escape.png"},new byte[][]{PNG,new byte[]{(byte)255,(byte)216,(byte)255,1},PNG});
   AtomicInteger moves=new AtomicInteger();
   try{TileArchiveInstaller.install(new ByteArrayInputStream(valid),base,(from,to)->{if(moves.incrementAndGet()==2)throw new IOException("injected commit failure");Files.move(from.toPath(),to.toPath(),StandardCopyOption.ATOMIC_MOVE);});throw new AssertionError("failed commit accepted");}catch(IOException expected){checks++;}
   retained(base);check(moves.get()==3,"rollback not attempted");
   TileArchiveInstaller.Result result=TileArchiveInstaller.install(new ByteArrayInputStream(valid),base);
   check(result.count==2&&result.minZoom==10&&result.maxZoom==12,"tile count/zoom bounds lost");
   check(new File(base,"10/1/2.png").isFile()&&new File(base,"12/3/4.jpg").isFile(),"committed tiles missing");
   check(!temp.resolve("escape.png").toFile().exists(),"path traversal wrote outside staging");
   check(!new File(base,"original.txt").exists(),"successful replacement retained stale tile set");
   File backup=temp.resolve("tiles.backup").toFile();Files.move(base.toPath(),backup.toPath());TileArchiveInstaller.recover(base);check(base.isDirectory()&&!backup.exists(),"restart recovery failed");
   Paused pending=new Paused(zip("14/1/1.png",PNG));
   Future<Boolean> cancelled=worker.submit(()->{try{TileArchiveInstaller.install(pending,base);return false;}catch(IOException expected){return true;}});
   check(pending.entered.await(2,TimeUnit.SECONDS),"pending fixture never read");
   install(zip("15/1/1.png",PNG),base);pending.release.countDown();
   check(cancelled.get(2,TimeUnit.SECONDS),"older import replaced newer import");check(new File(base,"15/1/1.png").isFile()&&!new File(base,"14/1/1.png").exists(),"newest import not retained");
   Paused beforeClear=new Paused(zip("16/1/1.png",PNG));
   Future<Boolean> cleared=worker.submit(()->{try{TileArchiveInstaller.install(beforeClear,base);return false;}catch(IOException expected){return true;}});
   check(beforeClear.entered.await(2,TimeUnit.SECONDS),"clear fixture never read");TileArchiveInstaller.clear(base);beforeClear.release.countDown();
   check(cleared.get(2,TimeUnit.SECONDS)&&!base.exists()&&!backup.exists(),"clear resurrected an older map/import");
   install(zip("01/00/00.png",PNG),base);check(new File(base,"1/0/0.png").exists(),"valid tile coordinates not normalized");
   File abandoned=temp.resolve("tiles.pending-old-process").toFile();abandoned.mkdirs();Files.write(new File(abandoned,"old").toPath(),new byte[]{1});
   Paused alive=new Paused(zip("17/1/1.png",PNG));Future<Boolean> live=worker.submit(()->{try{TileArchiveInstaller.install(alive,base);return true;}catch(IOException error){return false;}});
   check(alive.entered.await(2,TimeUnit.SECONDS),"live cleanup fixture never read");TileArchiveInstaller.cleanAbandoned(base);check(!abandoned.exists(),"abandoned staging survived cleanup");alive.release.countDown();check(live.get(2,TimeUnit.SECONDS)&&new File(base,"17/1/1.png").exists(),"cleanup removed a live import");
   File[] leftovers=temp.toFile().listFiles((dir,name)->name.startsWith("tiles.pending-"));check(leftovers!=null&&leftovers.length==0,"failed imports left staging folders");
   System.out.println("PASS tile archive installer: "+checks+" checks; actual ZIP/files, header fixtures, injected rename failure; no Android image decoding");
  }finally{worker.shutdownNow();try(java.util.stream.Stream<Path> files=Files.walk(temp)){files.sorted(Comparator.reverseOrder()).forEach(p->{try{Files.delete(p);}catch(IOException error){throw new UncheckedIOException(error);}});}}
 }
}
