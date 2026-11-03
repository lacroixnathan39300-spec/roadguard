package fr.roadguard.app;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.CRC32;

/** Offline transport tests. Synthetic 7z framing is NOT an extraction test. */
public final class TestRadarDownloads {
  static int scenarios,assertions;
  static void check(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
  static void u32(byte[] b,int at,long value){for(int i=0;i<4;i++)b[at+i]=(byte)(value>>>(8*i));}
  static void u64(byte[] b,int at,long value){for(int i=0;i<8;i++)b[at+i]=(byte)(value>>>(8*i));}
  static void startCrc(byte[] bytes){CRC32 crc=new CRC32();crc.update(bytes,12,20);u32(bytes,8,crc.getValue());}
  static byte[] fixture(){
    byte[] b=new byte[134],signature={0x37,0x7a,(byte)0xbc,(byte)0xaf,0x27,0x1c};
    System.arraycopy(signature,0,b,0,6);b[7]=4;
    Arrays.fill(b,32,132,(byte)7);b[132]=1;b[133]=0;
    u64(b,12,100);u64(b,20,2);CRC32 crc=new CRC32();crc.update(b,132,2);u32(b,28,crc.getValue());startCrc(b);return b;
  }
  static String sha(byte[] bytes)throws Exception{StringBuilder value=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(bytes))value.append(String.format(Locale.ROOT,"%02x",b&255));return value.toString();}
  static RadarDownloadSource source(byte[] bytes)throws Exception{return new RadarDownloadSource("LU",bytes.length,sha(bytes));}
  static final class Connection extends HttpURLConnection {
    final byte[] payload;boolean disconnected;int code=200;long length;String type="application/octet-stream",redirect;
    Runnable onRead;boolean failRead;CountDownLatch responseStarted,releaseResponse;
    Connection(byte[] bytes)throws Exception{super(new URL("https://github.com/test"));payload=bytes;length=bytes.length;}
    public int getResponseCode()throws IOException{
      if(responseStarted!=null){responseStarted.countDown();try{if(!releaseResponse.await(3,TimeUnit.SECONDS))throw new IOException("test timeout");}catch(InterruptedException e){throw new IOException(e);}throw new IOException("connection closed");}
      return code;
    }
    public long getContentLengthLong(){return length;}
    public String getContentType(){return type;}
    public String getHeaderField(String name){return "Location".equals(name)?redirect:null;}
    public InputStream getInputStream(){return new InputStream(){
      final ByteArrayInputStream input=new ByteArrayInputStream(payload);int reads;
      public int read()throws IOException{byte[] one=new byte[1];return read(one,0,1)<0?-1:one[0]&255;}
      @Override public synchronized int read(byte[] b,int off,int len)throws IOException{
        if(failRead&&reads++>=3)throw new SocketTimeoutException("simulated read timeout");
        if(onRead!=null)onRead.run();return input.read(b,off,Math.min(len,7));
      }
    };}
    public void connect(){}public boolean usingProxy(){return false;}
    public void disconnect(){disconnected=true;if(releaseResponse!=null)releaseResponse.countDown();}
  }
  static void noPending(File directory){File[] left=directory.listFiles((dir,name)->name.endsWith(".pending"));check(left!=null&&left.length==0,"pending file leaked");}
  static void reject(File directory,byte[] old,Connection connection,RadarDownloadSource source)throws Exception{
    File target=new File(directory,"lu-camera.sqlite.7z");Files.write(target.toPath(),old);
    try{new RadarArchiveDownloader(url->connection).download(source,directory,null);throw new AssertionError("bad transfer accepted");}
    catch(IOException expected){}
    check(Arrays.equals(old,Files.readAllBytes(target.toPath())),"previous archive changed on failure");
    check(connection.disconnected,"connection leaked");noPending(directory);scenarios++;
  }
  static void rejectStructure(File file,byte[] bytes)throws Exception{
    Files.write(file.toPath(),bytes);try{RadarArchivePolicy.verify(file);throw new AssertionError("bad structure accepted");}catch(IOException expected){}scenarios++;
  }
  public static void main(String[] args)throws Exception{
    File dir=Files.createTempDirectory("roadguard-download-test-").toFile();
    try{
      byte[] valid=fixture(),old=new byte[]{11,22,33};File target=new File(dir,"lu-camera.sqlite.7z");
      Connection good=new Connection(valid);RadarArchiveDownloader downloader=new RadarArchiveDownloader(url->good);
      File result=downloader.download(source(valid),dir,(done,total)->check(done>=0&&done<=total&&total==valid.length,"progress bound"));
      check(Arrays.equals(valid,Files.readAllBytes(result.toPath())),"successful atomic install");check(good.disconnected,"success disconnect");noPending(dir);downloader.cancel();check(target.isFile(),"late cancel removed committed file");scenarios++;
      Connection unknown=new Connection(valid);unknown.length=-1;
      new RadarArchiveDownloader(url->unknown).download(source(valid),dir,null);check(unknown.disconnected,"unknown length disconnect");scenarios++;
      Connection notFound=new Connection(valid);notFound.code=404;reject(dir,old,notFound,source(valid));
      Connection wrongLength=new Connection(valid);wrongLength.length=valid.length+1;reject(dir,old,wrongLength,source(valid));
      Connection html=new Connection(valid);html.type="text/html; charset=UTF-8";reject(dir,old,html,source(valid));
      Connection partial=new Connection(Arrays.copyOf(valid,valid.length-1));partial.length=-1;reject(dir,old,partial,source(valid));
      Connection oversized=new Connection(Arrays.copyOf(valid,valid.length+1));oversized.length=-1;reject(dir,old,oversized,source(valid));
      Connection timeout=new Connection(valid);timeout.failRead=true;reject(dir,old,timeout,source(valid));
      byte[] corrupt=valid.clone();corrupt[60]^=1;reject(dir,old,new Connection(corrupt),source(valid));
      byte[] invalid=valid.clone();invalid[0]=0;reject(dir,old,new Connection(invalid),source(invalid));
      Connection unsafe=new Connection(valid);unsafe.code=302;unsafe.redirect="http://github.com/test";reject(dir,old,unsafe,source(valid));
      Connection loop=new Connection(valid);loop.code=307;loop.redirect="https://github.com/loop";reject(dir,old,loop,source(valid));
      Connection redirect=new Connection(valid);redirect.code=302;redirect.redirect="https://release-assets.githubusercontent.com/test";Connection asset=new Connection(valid);
      final int[] opens={0};new RadarArchiveDownloader(url->opens[0]++==0?redirect:asset).download(source(valid),dir,null);
      check(opens[0]==2&&redirect.disconnected&&asset.disconnected,"redirect lifecycle");scenarios++;
      Connection cancelRead=new Connection(valid);RadarArchiveDownloader cancelled=new RadarArchiveDownloader(url->cancelRead);
      Files.write(target.toPath(),old);cancelRead.onRead=cancelled::cancel;
      try{cancelled.download(source(valid),dir,null);throw new AssertionError("cancelled read accepted");}catch(RadarArchiveDownloader.Cancelled expected){}
      check(Arrays.equals(old,Files.readAllBytes(target.toPath())),"cancel changed previous file");check(cancelRead.disconnected,"cancel disconnect");noPending(dir);scenarios++;
      RadarArchiveDownloader beforeStart=new RadarArchiveDownloader(url->{throw new AssertionError("network opened after cancel");});beforeStart.cancel();
      try{beforeStart.download(source(valid),dir,null);throw new AssertionError("pre-cancel accepted");}catch(RadarArchiveDownloader.Cancelled expected){}scenarios++;
      Connection finalRead=new Connection(valid);RadarArchiveDownloader beforeCommit=new RadarArchiveDownloader(url->finalRead);
      Files.write(target.toPath(),old);
      try{beforeCommit.download(source(valid),dir,(done,total)->{if(done==total)beforeCommit.cancel();});throw new AssertionError("cancel before commit accepted");}catch(RadarArchiveDownloader.Cancelled expected){}
      check(Arrays.equals(old,Files.readAllBytes(target.toPath())),"last-byte cancellation changed previous archive");noPending(dir);scenarios++;
      Connection waiting=new Connection(valid);waiting.responseStarted=new CountDownLatch(1);waiting.releaseResponse=new CountDownLatch(1);
      RadarArchiveDownloader activeCancel=new RadarArchiveDownloader(url->waiting);final Throwable[] failure={null};
      Thread worker=new Thread(()->{try{activeCancel.download(new RadarDownloadSource("LU",valid.length,uncheckedSha(valid)),dir,null);}catch(Throwable error){failure[0]=error;}});
      worker.setDaemon(true);worker.start();check(waiting.responseStarted.await(3,TimeUnit.SECONDS),"no active connection");activeCancel.cancel();worker.join(3000);
      check(!worker.isAlive()&&failure[0] instanceof RadarArchiveDownloader.Cancelled,"cancel must disconnect response wait");noPending(dir);scenarios++;
      File framing=new File(dir,"framing.7z");Files.write(framing.toPath(),valid);RadarArchivePolicy.verify(framing);scenarios++;
      byte[] bad=valid.clone();bad[12]^=1;rejectStructure(framing,bad);
      bad=valid.clone();bad[133]^=1;rejectStructure(framing,bad);
      bad=valid.clone();u64(bad,12,Long.MAX_VALUE);startCrc(bad);rejectStructure(framing,bad);
      bad=valid.clone();bad[19]=(byte)0x80;startCrc(bad);rejectStructure(framing,bad);
      bad=valid.clone();u64(bad,20,RadarArchivePolicy.MAX_NEXT_HEADER_BYTES+1);startCrc(bad);rejectStructure(framing,bad);
      rejectStructure(framing,Arrays.copyOf(valid,valid.length-1));
      for(String address:new String[]{"http://github.com/file","https://evil.test/file","https://github.com.evil.test/file","https://user@github.com/file","https://github.com:444/file","https://github.com/file#fragment"})check(!RadarDownloadSource.allowed(new URL(address)),"unsafe URL "+address);scenarios++;
      RadarCountryBatch queue=new RadarCountryBatch(new String[]{"FR","CH","DE"});check("FR".equals(queue.next()),"queue order");check(queue.next()==null,"parallel transfer offered");
      queue.finish("FR",RadarCountryBatch.Outcome.FAILED);check("CH".equals(queue.next()),"failure must not stop queue");queue.cancel();check(queue.next()==null,"cancelled queue advanced");queue.finish("CH",RadarCountryBatch.Outcome.CANCELLED);
      check(queue.count(RadarCountryBatch.Outcome.FAILED)==1&&queue.count(RadarCountryBatch.Outcome.CANCELLED)==2,"outcome summary");
      try{queue.finish("DE",RadarCountryBatch.Outcome.RECEIVED);throw new AssertionError("stale completion accepted");}catch(IllegalStateException expected){}scenarios++;
      RadarCountryBatch concurrent=new RadarCountryBatch(new String[]{"FR","CH"});ExecutorService pool=Executors.newFixedThreadPool(4);try{List<Future<String>> futures=new ArrayList<>();for(int i=0;i<8;i++)futures.add(pool.submit(concurrent::next));int offered=0;for(Future<String> f:futures)if(f.get()!=null)offered++;check(offered==1,"queue offered multiple simultaneous transfers");}finally{pool.shutdownNow();}scenarios++;
      System.out.println("PASS downloads/queue: "+scenarios+" scenarios, "+assertions+" assertions; mocked HTTP, real temporary files and atomic rename; no live network or 7z extraction");
    }finally{try(java.util.stream.Stream<Path> paths=Files.walk(dir.toPath())){paths.sorted(Comparator.reverseOrder()).forEach(p->{try{Files.deleteIfExists(p);}catch(IOException error){throw new java.io.UncheckedIOException(error);}});}}
  }
  static String uncheckedSha(byte[] bytes){try{return sha(bytes);}catch(Exception e){throw new RuntimeException(e);}}
}
