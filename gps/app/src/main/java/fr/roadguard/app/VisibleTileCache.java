package fr.roadguard.app;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/** Bounded HTTP cache for tiles requested by the visible map, never an area downloader. */
final class VisibleTileCache {
  static final long DEFAULT_MAX_BYTES=4L*1024*1024*1024;
  static final long MIN_FREE_BYTES=512L*1024*1024;
  static final long FALLBACK_TTL=7L*24*60*60*1000;
  static final int MAX_TILE_BYTES=1024*1024;
  interface Clock { long now(); }
  interface Space { long available(File directory); }
  private static final class Budget {long bytes=-1,files=0,stamp=-1;}
  private static final java.util.concurrent.ConcurrentHashMap<String,Budget> BUDGETS=new java.util.concurrent.ConcurrentHashMap<>();
  interface Fetcher { Reply get(String url,Map<String,String> headers)throws IOException; }
  static final class Reply {
    final int status; final byte[] bytes; final Map<String,String> headers;
    Reply(int status,byte[] bytes,Map<String,String> headers){this.status=status;this.bytes=bytes;this.headers=new HashMap<>();if(headers!=null)for(Map.Entry<String,String> e:headers.entrySet())if(e.getKey()!=null&&e.getValue()!=null)this.headers.put(e.getKey().toLowerCase(Locale.ROOT),e.getValue());}
    String header(String key){String value=headers.get(key);return value==null?"":value;}
  }
  static final class Tile { final byte[] bytes; final boolean cached; Tile(byte[] bytes,boolean cached){this.bytes=bytes;this.cached=cached;} }
  static final class Snapshot {final long files,bytes,hits,downloads,misses,attempts,failures;final String lastFailure;Snapshot(long f,long b,long h,long d,long m,long a,long errors,String error){files=f;bytes=b;hits=h;downloads=d;misses=m;attempts=a;failures=errors;lastFailure=error;}}
  private static final class Entry {long expires,ttl;boolean revalidate;String etag,modified;byte[] bytes;}
  private final File directory; private final long maxBytes; private final Clock clock; private final Fetcher fetcher; private final long minFreeBytes; private final Space space; private final Budget budget;
  private static final Object[] LOCKS=new Object[64];
  static { for(int i=0;i<LOCKS.length;i++)LOCKS[i]=new Object(); }
  private final String lockScope;
  private final AtomicLong hits=new AtomicLong(),downloads=new AtomicLong(),misses=new AtomicLong(),attempts=new AtomicLong(),failures=new AtomicLong();
  private volatile String lastFailure="";
  void retryNetworkFailures(){retryAfter.clear();}
  private final Map<String,Long> retryAfter=Collections.synchronizedMap(new HashMap<>());
  VisibleTileCache(File directory){this(directory,DEFAULT_MAX_BYTES,System::currentTimeMillis,VisibleTileCache::fetch);}
  VisibleTileCache(File directory,long maxBytes,Clock clock,Fetcher fetcher){this(directory,maxBytes,MIN_FREE_BYTES,clock,fetcher,File::getUsableSpace);}
  VisibleTileCache(File directory,long maxBytes,long minFreeBytes,Clock clock,Fetcher fetcher,Space space){this.directory=directory;this.maxBytes=Math.max(0,maxBytes);this.minFreeBytes=Math.max(0,minFreeBytes);this.clock=clock;this.fetcher=fetcher;this.space=space;String scope;try{scope=directory.getCanonicalPath();}catch(IOException e){scope=directory.getAbsolutePath();}lockScope=scope;budget=BUDGETS.computeIfAbsent(scope,key->new Budget());}
  long limitBytes(){return maxBytes;}
  long reserveBytes(){return minFreeBytes;}
  static String canonical(String value){
    try{URI u=new URI(value);String host=u.getHost();if(!"https".equalsIgnoreCase(u.getScheme())||host==null||u.getUserInfo()!=null||u.getPort()!=-1||u.getQuery()!=null||u.getFragment()!=null)return null;
      if(!host.equalsIgnoreCase("tile.openstreetmap.org")&&!host.toLowerCase(Locale.ROOT).matches("[abc]\\.tile\\.openstreetmap\\.org"))return null;
      String path=u.getPath();if(path==null||!path.matches("/[0-9]{1,2}/[0-9]{1,7}/[0-9]{1,7}\\.png"))return null;
      String[] xyz=path.substring(1,path.length()-4).split("/");int z=Integer.parseInt(xyz[0]),x=Integer.parseInt(xyz[1]),y=Integer.parseInt(xyz[2]);
      if(z>19||x<0||y<0||x>=(1<<z)||y>=(1<<z))return null;
      return "https://tile.openstreetmap.org/"+z+"/"+x+"/"+y+".png";
    }catch(Exception invalid){return null;}
  }
  Tile get(String rawUrl,boolean online)throws IOException {
    String url=canonical(rawUrl);if(url==null)return null;
    synchronized(LOCKS[((lockScope+url).hashCode()&0x7fffffff)%LOCKS.length]){
      File file=new File(directory,key(url)+".tile");Entry old=read(file);long now=clock.now();
      if(old!=null&&(old.expires>now||!online&&!old.revalidate)){hits.incrementAndGet();file.setLastModified(now);return new Tile(old.bytes,true);}
      Long retry=retryAfter.get(url);
      if(!online||(retry!=null&&retry>now)){if(old!=null&&!old.revalidate){hits.incrementAndGet();return new Tile(old.bytes,true);}misses.incrementAndGet();return null;}
      Map<String,String> request=new HashMap<>();
      request.put("User-Agent","ROADGUARD/11.14.244 (Android personal navigation)");
      if(old!=null){if(!old.etag.isEmpty())request.put("If-None-Match",old.etag);if(!old.modified.isEmpty())request.put("If-Modified-Since",old.modified);}
      try{
        attempts.incrementAndGet();Reply reply=fetcher.get(url,request);
        if(reply.status==304&&old!=null){Entry entry=policy(reply,old.bytes,now,old.ttl);if(entry.etag.isEmpty())entry.etag=old.etag;if(entry.modified.isEmpty())entry.modified=old.modified;if(reply.header("cache-control").isEmpty())entry.revalidate=old.revalidate;if(reply.header("cache-control").toLowerCase(Locale.ROOT).contains("no-store"))remove(file);else write(file,entry);hits.incrementAndGet();return new Tile(entry.bytes,true);}
        if(reply.status==404||reply.status==410){remove(file);misses.incrementAndGet();return null;}
        if(reply.status!=200)throw new IOException("Serveur de tuiles : HTTP "+reply.status);
        if(!png(reply.bytes))throw new IOException("Tuile reçue incomplète ou invalide");
        Entry entry=policy(reply,reply.bytes,now,FALLBACK_TTL);
        if(reply.header("cache-control").toLowerCase(Locale.ROOT).contains("no-store")){remove(file);}else write(file,entry);
        retryAfter.remove(url);lastFailure="";downloads.incrementAndGet();return new Tile(entry.bytes,false);
      }catch(IOException networkError){
        failures.incrementAndGet();lastFailure=networkError.getMessage()!=null&&networkError.getMessage().startsWith("Serveur de tuiles : HTTP ")?networkError.getMessage():"Connexion aux tuiles indisponible ou réponse invalide";
        if(retryAfter.size()>4096)retryAfter.clear();retryAfter.put(url,now+30000L);
        if(old!=null&&!old.revalidate){hits.incrementAndGet();return new Tile(old.bytes,true);}
        misses.incrementAndGet();return null;
      }
    }
  }
  private Entry policy(Reply reply,byte[] bytes,long now,long fallback){
    Entry e=new Entry();e.bytes=bytes;e.etag=bounded(reply.header("etag"));e.modified=bounded(reply.header("last-modified"));
    String control=reply.header("cache-control").toLowerCase(Locale.ROOT);long ttl=fallback;long serverDate=httpDate(reply.header("date"),now);
    java.util.regex.Matcher max=java.util.regex.Pattern.compile("(?:^|,)\\s*max-age\\s*=\\s*\"?([0-9]+)").matcher(control);
    if(max.find()){try{ttl=Math.min(365L*24*3600,Long.parseLong(max.group(1)))*1000;}catch(NumberFormatException ignored){}}
    else if(!reply.header("expires").isEmpty()){
      try{java.text.SimpleDateFormat fmt=new java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz",Locale.US);ttl=Math.max(0,fmt.parse(reply.header("expires")).getTime()-serverDate);}catch(Exception ignored){}
    }
    long age=0;try{age=Math.min(365L*24*3600,Math.max(0,Long.parseLong(reply.header("age"))))*1000;}catch(NumberFormatException ignored){}
    age=Math.max(age,Math.max(0,now-serverDate));e.ttl=ttl;e.revalidate=control.contains("must-revalidate")||control.contains("no-cache");e.expires=now+Math.max(0,ttl-age);if(control.contains("no-cache"))e.expires=now;return e;
  }
  private static long httpDate(String value,long fallback){try{return new java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz",Locale.US).parse(value).getTime();}catch(Exception ignored){return fallback;}}
  private static String bounded(String v){return v.substring(0,Math.min(4096,v.length()));}
  private static int pngInt(byte[] b,int offset){return (b[offset]&255)<<24|(b[offset+1]&255)<<16|(b[offset+2]&255)<<8|(b[offset+3]&255);}
  private static boolean png(byte[] b){
    if(b==null||b.length<57||b.length>MAX_TILE_BYTES||(b[0]&255)!=137||b[1]!=80||b[2]!=78||b[3]!=71||b[4]!=13||b[5]!=10||b[6]!=26||b[7]!=10)return false;
    int offset=8;boolean header=false,data=false;
    while(offset<=b.length-12){int size=pngInt(b,offset);if(size<0||size>b.length-offset-12)return false;int type=pngInt(b,offset+4);
      if(!header){if(type!=0x49484452||size!=13)return false;int width=pngInt(b,offset+8),height=pngInt(b,offset+12);if(width<=0||height<=0||width>1024||height>1024)return false;header=true;}
      else if(type==0x49484452)return false;
      java.util.zip.CRC32 crc=new java.util.zip.CRC32();crc.update(b,offset+4,size+4);if(crc.getValue()!=(pngInt(b,offset+8+size)&0xffffffffL))return false;
      if(type==0x49444154)data=true;
      if(type==0x49454e44)return size==0&&data&&offset+12==b.length;
      offset+=size+12;
    }
    return false;
  }

  private Entry read(File file){
    if(!file.isFile())return null;
    try(DataInputStream in=new DataInputStream(new BufferedInputStream(new FileInputStream(file)))){
      if(in.readInt()!=0x52475431)throw new IOException("Cache format");Entry e=new Entry();e.expires=in.readLong();e.ttl=in.readLong();e.revalidate=in.readBoolean();e.etag=in.readUTF();e.modified=in.readUTF();int n=in.readInt();if(n<8||n>MAX_TILE_BYTES)throw new IOException("Cache size");e.bytes=new byte[n];in.readFully(e.bytes);if(!png(e.bytes)||in.read()!=-1)throw new IOException("Cache data");return e;
    }catch(Exception corrupt){remove(file);return null;}
  }
  private File[] tiles(){File[] files=directory.listFiles((d,n)->n.endsWith(".tile"));return files==null?new File[0]:files;}
  // One budget per directory: phone and car cannot each consume another 4 GiB.
  private void refreshBudget(){
    long stamp=directory.lastModified();if(budget.bytes>=0&&budget.stamp==stamp)return;
    File[] files=tiles();budget.bytes=0;for(File file:files)budget.bytes+=file.length();budget.files=files.length;budget.stamp=stamp;
  }
  private void remove(File file){synchronized(budget){long size=file.length();boolean existed=file.isFile();if(file.delete()&&existed&&budget.bytes>=0){budget.bytes=Math.max(0,budget.bytes-size);budget.files=Math.max(0,budget.files-1);budget.stamp=directory.lastModified();}}}
  private void writeHeader(DataOutputStream out,Entry e)throws IOException{out.writeInt(0x52475431);out.writeLong(e.expires);out.writeLong(e.ttl);out.writeBoolean(e.revalidate);out.writeUTF(e.etag);out.writeUTF(e.modified);out.writeInt(e.bytes.length);}
  private void write(File target,Entry e){
    synchronized(budget){File tmp=null;
      try{
        if(!directory.isDirectory()&&!directory.mkdirs())return;
        refreshBudget();ByteArrayOutputStream header=new ByteArrayOutputStream();try(DataOutputStream out=new DataOutputStream(header)){writeHeader(out,e);}
        long size=header.size()+(long)e.bytes.length;if(size>maxBytes)return;
        long old=target.length(),available=Math.max(0,space.available(directory));
        long forCapacity=budget.bytes-old+size>maxBytes?budget.bytes-old+size-maxBytes*9/10:0;
        long forSpace=Math.max(0,minFreeBytes+size-available);
        if(forCapacity>0||forSpace>0)trim(target,Math.max(forCapacity,forSpace));
        old=target.length();if(budget.bytes-old+size>maxBytes||space.available(directory)<minFreeBytes+size)return;
        boolean replacing=target.isFile();tmp=File.createTempFile("tile-",".part",directory);
        try(OutputStream out=new BufferedOutputStream(new FileOutputStream(tmp))){header.writeTo(out);out.write(e.bytes);}
        Files.move(tmp.toPath(),target.toPath(),StandardCopyOption.REPLACE_EXISTING);target.setLastModified(clock.now());
        budget.bytes=budget.bytes-old+target.length();if(!replacing)budget.files++;
      }catch(IOException ignored){/* A full disk must not stop live map display. */}
      finally{if(tmp!=null)tmp.delete();budget.stamp=directory.lastModified();}
    }
  }
  // Scan/sort only when cleanup is necessary, not every 32 downloaded tiles.
  private void trim(File protectedTile,long bytesToFree){
    File[] files=tiles();budget.bytes=0;for(File file:files)budget.bytes+=file.length();budget.files=files.length;
    Arrays.sort(files,Comparator.comparingLong(File::lastModified));long freed=0;
    for(File file:files){if(file.equals(protectedTile))continue;long size=file.length();if(file.delete()){budget.bytes=Math.max(0,budget.bytes-size);budget.files=Math.max(0,budget.files-1);freed+=size;}if(freed>=bytesToFree)break;}
    budget.stamp=directory.lastModified();
  }
  Snapshot snapshot(){synchronized(budget){refreshBudget();return new Snapshot(budget.files,budget.bytes,hits.get(),downloads.get(),misses.get(),attempts.get(),failures.get(),lastFailure);}}
  private static String key(String url){try{byte[] b=MessageDigest.getInstance("SHA-256").digest(url.getBytes("UTF-8"));StringBuilder s=new StringBuilder();for(byte v:b)s.append(String.format(Locale.ROOT,"%02x",v&255));return s.toString();}catch(Exception e){throw new IllegalStateException(e);}}
  private static Reply fetch(String url,Map<String,String> headers)throws IOException {
    HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(4000);c.setReadTimeout(5000);for(Map.Entry<String,String> h:headers.entrySet())c.setRequestProperty(h.getKey(),h.getValue());
    try{int status=c.getResponseCode();Map<String,String> response=new HashMap<>();for(Map.Entry<String,List<String>> h:c.getHeaderFields().entrySet())if(h.getKey()!=null&&h.getValue()!=null)response.put(h.getKey(),String.join(",",h.getValue()));
      byte[] bytes=new byte[0];if(status==200){if(c.getContentLengthLong()>MAX_TILE_BYTES)throw new IOException("Tile too large");try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(out.size()+n>MAX_TILE_BYTES)throw new IOException("Tile too large");out.write(buffer,0,n);}bytes=out.toByteArray();if(c.getContentLengthLong()>=0&&bytes.length!=c.getContentLengthLong())throw new IOException("Incomplete map tile");}}return new Reply(status,bytes,response);
    }finally{c.disconnect();}
  }
}
