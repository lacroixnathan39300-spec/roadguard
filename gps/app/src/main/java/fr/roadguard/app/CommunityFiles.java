package fr.roadguard.app;
import android.util.AtomicFile;
import java.io.*;
import java.util.concurrent.ConcurrentHashMap;
/** All readers and writers of the same community cache share an in-process lock. */
final class CommunityFiles {
  private static final ConcurrentHashMap<String,Object> locks=new ConcurrentHashMap<>();
  private static Object lock(File f)throws IOException{return locks.computeIfAbsent(f.getCanonicalPath(),k->new Object());}
  static byte[] read(File dir,String name,int max)throws IOException{File f=new File(dir,name);synchronized(lock(f)){try(InputStream in=new AtomicFile(f).openRead();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>max)throw new IOException("Cache trop grand");out.write(b,0,n);}return out.toByteArray();}}}
  static void write(File dir,String name,byte[] bytes)throws IOException{File f=new File(dir,name);synchronized(lock(f)){AtomicFile a=new AtomicFile(f);FileOutputStream out=null;try{out=a.startWrite();out.write(bytes);a.finishWrite(out);}catch(IOException e){if(out!=null)a.failWrite(out);throw e;}}}
  private CommunityFiles(){}
}
