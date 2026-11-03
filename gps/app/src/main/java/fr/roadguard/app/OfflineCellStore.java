package fr.roadguard.app;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Durable inventory of completed map sectors. Does not download map tiles. */
public final class OfflineCellStore {
  private final File root, index;
  private final TreeMap<String,Entry> entries=new TreeMap<>();
  public static final class Entry {
    public final String id, file, sha256;
    public final long bytes;
    Entry(String id,String file,long bytes,String sha256){this.id=id;this.file=file;this.bytes=bytes;this.sha256=sha256;}
  }
  public OfflineCellStore(File root) throws IOException {
    this.root=root; this.index=new File(root,"completed-sectors.tsv");
    if(!root.isDirectory()&&!root.mkdirs())throw new IOException("Cannot create map directory");
    recoverIndex();
    load();
  }
  /** Recover the last committed index after a crash between backup and replacement. */
  private void recoverIndex() throws IOException {
    File backup=new File(root,"completed-sectors.tsv.bak");
    if(!index.exists()&&backup.isFile()&&!backup.renameTo(index))
      throw new IOException("Cannot recover sector index backup");
    // A complete primary index always takes precedence over a stale backup.
    if(index.isFile()&&backup.isFile())backup.delete();
    File staleTmp=new File(root,"completed-sectors.tsv.tmp");
    if(index.isFile()&&staleTmp.isFile())staleTmp.delete();
  }
  private static boolean safe(String s){return s!=null&&s.matches("[A-Za-z0-9_.:-]{1,120}")&&!s.equals(".")&&!s.equals("..");}
  private void load() throws IOException {
    if(!index.isFile())return;
    try(BufferedReader r=new BufferedReader(new InputStreamReader(new FileInputStream(index),StandardCharsets.UTF_8))){
      String line;
      while((line=r.readLine())!=null){
        String[] p=line.split("\\t",-1);
        if(p.length!=4||!safe(p[0])||!safe(p[1])||!p[2].matches("[0-9]+")||!p[3].matches("[a-f0-9]{64}"))continue;
        try{entries.put(p[0],new Entry(p[0],p[1],Long.parseLong(p[2]),p[3]));}catch(NumberFormatException ignored){}
      }
    }
  }
  private File resolved(Entry e) throws IOException {
    File f=new File(root,e.file);
    if(!f.getCanonicalFile().getParentFile().equals(root.getCanonicalFile()))throw new IOException("Unsafe map path");
    return f;
  }
  private static String digest(File f) throws IOException {
    try{
      MessageDigest md=MessageDigest.getInstance("SHA-256");
      try(InputStream in=new BufferedInputStream(new FileInputStream(f))){byte[] buf=new byte[65536];int n;while((n=in.read(buf))!=-1)md.update(buf,0,n);}
      StringBuilder sb=new StringBuilder();for(byte b:md.digest())sb.append(String.format(Locale.ROOT,"%02x",b&255));return sb.toString();
    }catch(java.security.NoSuchAlgorithmException e){throw new IOException(e);}
  }
  /** Register only a fully written, verified sector file. */
  public synchronized void register(String id,String filename,String expectedSha256) throws IOException {
    if(!safe(id)||!safe(filename)||expectedSha256==null||!expectedSha256.matches("[a-fA-F0-9]{64}"))throw new IOException("Invalid sector metadata");
    Entry candidate=new Entry(id,filename,0,expectedSha256.toLowerCase(Locale.ROOT));
    File f=resolved(candidate);
    if(!f.isFile()||f.length()==0)throw new IOException("Missing or empty sector");
    if(!digest(f).equals(candidate.sha256))throw new IOException("Sector checksum mismatch");
    Entry previous=entries.put(id,new Entry(id,filename,f.length(),candidate.sha256));
    try{persist();}catch(IOException ex){
      if(previous==null)entries.remove(id);else entries.put(id,previous);
      throw ex;
    }
  }
  public synchronized boolean hasVerified(String id) throws IOException {
    Entry e=entries.get(id);if(e==null)return false;
    File f=resolved(e);
    if(f.isFile()&&f.length()==e.bytes&&digest(f).equals(e.sha256))return true;
    entries.remove(id);
    try{persist();}catch(IOException ex){entries.put(id,e);throw ex;}
    return false;
  }
  public synchronized List<String> missing(List<NearbyOfflinePlanner.Cell> cells) throws IOException {
    List<String> out=new ArrayList<>();for(NearbyOfflinePlanner.Cell c:cells)if(!hasVerified(c.id))out.add(c.id);return out;
  }
  /** Import a complete authorized map pack without loading it into RAM.
   * A failed copy/checksum never replaces an existing verified pack. */
  public synchronized void importVerified(String id,InputStream source,String expectedSha256,long maxBytes) throws IOException {
    if(!safe(id)||source==null||expectedSha256==null||!expectedSha256.matches("[a-fA-F0-9]{64}")||maxBytes<=0)
      throw new IOException("Invalid map import parameters");
    String filename=id+".pmtiles";
    File destination=resolved(new Entry(id,filename,0,""));
    File staging=resolved(new Entry(id,id+".download",0,""));
    File previous=resolved(new Entry(id,id+".previous",0,""));
    if(staging.exists()&&!staging.delete())throw new IOException("Cannot clear old staging file");
    try {
      MessageDigest md;
      try{md=MessageDigest.getInstance("SHA-256");}catch(java.security.NoSuchAlgorithmException e){throw new IOException(e);}
      long total=0;
      try(FileOutputStream out=new FileOutputStream(staging)){
        byte[] buffer=new byte[65536];int n;
        while((n=source.read(buffer))!=-1){
          if(n==0)continue;
          if(total>maxBytes-n)throw new IOException("Map exceeds permitted size");
          out.write(buffer,0,n);md.update(buffer,0,n);total+=n;
        }
        out.getFD().sync();
      }
      if(total==0)throw new IOException("Empty map pack");
      StringBuilder hex=new StringBuilder();for(byte b:md.digest())hex.append(String.format(Locale.ROOT,"%02x",b&255));
      if(!hex.toString().equalsIgnoreCase(expectedSha256))throw new IOException("Map checksum mismatch");
      if(previous.exists()&&!previous.delete())throw new IOException("Cannot clear previous map backup");
      boolean hadPrevious=destination.exists();
      if(hadPrevious&&!destination.renameTo(previous))throw new IOException("Cannot back up existing map");
      if(!staging.renameTo(destination)){
        if(hadPrevious)previous.renameTo(destination);
        throw new IOException("Cannot commit map pack");
      }
      try{register(id,filename,expectedSha256);}
      catch(IOException ex){
        if(!destination.delete())ex.addSuppressed(new IOException("Cannot remove unregistered map"));
        if(hadPrevious&&!previous.renameTo(destination))ex.addSuppressed(new IOException("Cannot restore previous map"));
        throw ex;
      }
      if(hadPrevious)previous.delete();
    }finally{if(staging.exists())staging.delete();}
  }
  /** Expose only a verified, completed pack; never a partial download. */
  public synchronized File verifiedFile(String id) throws IOException {
    if (!safe(id) || !hasVerified(id)) return null;
    Entry entry=entries.get(id);
    return entry==null?null:resolved(entry);
  }
  /** Fast discovery of a completed import. The checksum was checked at commit;
   * this length check is not a new checksum verification and is safe on the UI thread. */
  public synchronized File completedFile(String id) throws IOException {
    if(!safe(id))return null;
    Entry entry=entries.get(id);if(entry==null)return null;
    File file=resolved(entry);return file.isFile()&&file.length()==entry.bytes?file:null;
  }
  /** Sum of registered bytes; does not claim to verify all files. */
  public synchronized long registeredBytes(){long total=0;for(Entry e:entries.values())total+=e.bytes;return total;}
  public synchronized int count(){return entries.size();}
  private void persist() throws IOException {
    File tmp=new File(root,"completed-sectors.tsv.tmp");
    try(FileOutputStream fos=new FileOutputStream(tmp);Writer w=new OutputStreamWriter(fos,StandardCharsets.UTF_8)){
      for(Entry e:entries.values())w.write(e.id+"\t"+e.file+"\t"+e.bytes+"\t"+e.sha256+"\n");
      w.flush();fos.getFD().sync();
    }
    if(!tmp.renameTo(index)){
      // Do not delete a valid previous index unless a replacement is possible.
      File backup=new File(root,"completed-sectors.tsv.bak");
      if(index.exists()&&!index.renameTo(backup))throw new IOException("Index backup failed");
      if(!tmp.renameTo(index)){
        if(backup.exists())backup.renameTo(index);
        throw new IOException("Index update failed");
      }
      backup.delete();
    }
  }
}
