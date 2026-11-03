package fr.roadguard.app;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/** Private XYZ archive replacement. The old folder survives every failure before commit. */
final class TileArchiveInstaller {
  interface Mover { void move(File from,File to)throws IOException; }
  private static final Mover ATOMIC=(from,to)->Files.move(from.toPath(),to.toPath(),StandardCopyOption.ATOMIC_MOVE);
  private static long generation;
  private static final Set<File> pendingFolders=new HashSet<>();
  static final class Result {
    final int count,minZoom,maxZoom;
    Result(int count,int minZoom,int maxZoom){this.count=count;this.minZoom=minZoom;this.maxZoom=maxZoom;}
  }
  private TileArchiveInstaller(){}
  private static File backup(File base){return new File(base.getParentFile(),base.getName()+".backup");}
  static synchronized void recover(File base)throws IOException{recover(base,ATOMIC);}
  private static void recover(File base,Mover mover)throws IOException{
    File backup=backup(base);
    if(!base.exists()&&backup.isDirectory())mover.move(backup,base);
  }
  static void clear(File base)throws IOException{
    List<File> obsolete=new ArrayList<>();
    try{
      synchronized(TileArchiveInstaller.class){generation++;for(File source:new File[]{backup(base),base})if(source.exists()){File trash=obsolete(base);ATOMIC.move(source,trash);obsolete.add(trash);}}
    }finally{for(File trash:obsolete)try{delete(trash);}catch(IOException deferred){}}
  }
  private static File obsolete(File base){return new File(base.getParentFile(),base.getName()+".obsolete-"+UUID.randomUUID());}
  static Result install(InputStream input,File base)throws IOException{return install(input,base,ATOMIC);}
  static Result install(InputStream input,File base,Mover mover)throws IOException{
    if(input==null)throw new IOException("Archive inaccessible");
    final long token;synchronized(TileArchiveInstaller.class){token=++generation;recover(base,mover);}
    File parent=base.getParentFile();if(!parent.isDirectory()&&!parent.mkdirs())throw new IOException("Stockage inaccessible");
    final File staging;synchronized(TileArchiveInstaller.class){check(token);staging=Files.createTempDirectory(parent.toPath(),base.getName()+".pending-").toFile();pendingFolders.add(staging);}
    int count=0,min=23,max=-1,entries=0;long total=0;Set<String> seen=new HashSet<>();List<File> obsolete=new ArrayList<>();
    try{
      try(ZipInputStream zip=new ZipInputStream(new BufferedInputStream(input))){
        ZipEntry entry;byte[] buffer=new byte[8192];
        while((entry=zip.getNextEntry())!=null){
          check(token);if(++entries>25000)throw new IOException("Trop d'entrées dans l'archive");
          String name=entry.getName();if(name.startsWith("tiles/"))name=name.substring(6);
          if(entry.isDirectory()||!name.matches("[0-9]{1,2}/[0-9]{1,9}/[0-9]{1,9}\\.(png|jpg|jpeg)")){
            int skipped;while((skipped=zip.read(buffer))!=-1){check(token);total+=skipped;if(total>100L*1024*1024)throw new IOException("Archive de tuiles trop volumineuse");}continue;
          }
          String[] xyz=name.split("/");int zoom=Integer.parseInt(xyz[0]);long x=Long.parseLong(xyz[1]),y=Long.parseLong(xyz[2].substring(0,xyz[2].indexOf('.')));
          if(zoom>22||x>=(1L<<zoom)||y>=(1L<<zoom))throw new IOException("Coordonnées de tuile invalides");
          if(!seen.add(zoom+"/"+x+"/"+y))throw new IOException("Tuile présente plusieurs fois");
          if(++count>20000)throw new IOException("Plus de 20 000 tuiles");
          String extension=name.substring(name.lastIndexOf('.'));name=zoom+"/"+x+"/"+y+extension;
          File target=new File(staging,name);if(!target.getCanonicalPath().startsWith(staging.getCanonicalPath()+File.separator))throw new IOException("Chemin de tuile invalide");
          if(!target.getParentFile().isDirectory()&&!target.getParentFile().mkdirs())throw new IOException("Dossier de tuile inaccessible");
          long bytes=0;byte[] signature=new byte[8];int signatureSize=0;
          try(FileOutputStream out=new FileOutputStream(target)){
            int n;while((n=zip.read(buffer))!=-1){check(token);bytes+=n;total+=n;if(bytes>2L*1024*1024||total>100L*1024*1024)throw new IOException("Archive de tuiles trop volumineuse");
              int copy=Math.min(n,signature.length-signatureSize);if(copy>0){System.arraycopy(buffer,0,signature,signatureSize,copy);signatureSize+=copy;}out.write(buffer,0,n);
            }
            if(!image(signature,signatureSize,name.endsWith("png")))throw new IOException("Image de tuile invalide");out.getFD().sync();
          }
          min=Math.min(min,zoom);max=Math.max(max,zoom);
        }
      }
      if(count==0)throw new IOException("Aucune tuile XYZ reconnue");
      synchronized(TileArchiveInstaller.class){
        check(token);recover(base,mover);File backup=backup(base);
        if(base.exists()&&!base.isDirectory())throw new IOException("Dossier de carte invalide");
        if(backup.exists()){File trash=obsolete(base);mover.move(backup,trash);obsolete.add(trash);}
        boolean previous=base.exists();if(previous)mover.move(base,backup);
        try{mover.move(staging,base);}
        catch(IOException failed){if(previous)try{mover.move(backup,base);}catch(IOException restore){failed.addSuppressed(restore);}throw failed;}
        // A failed cleanup must not turn a committed installation into a reported failure.
        if(backup.exists())try{File trash=obsolete(base);mover.move(backup,trash);obsolete.add(trash);}catch(IOException deferred){}
      }
      return new Result(count,min,max);
    }finally{if(staging.exists())try{delete(staging);}catch(IOException deferred){}synchronized(TileArchiveInstaller.class){pendingFolders.remove(staging);}for(File trash:obsolete)try{delete(trash);}catch(IOException deferred){}}
  }
  /** Worker-only cleanup. Live imports remain protected across Activity recreation. */
  static void cleanAbandoned(File base){
    List<File> stale=new ArrayList<>();
    synchronized(TileArchiveInstaller.class){File[] files=base.getParentFile().listFiles();if(files!=null)for(File file:files)if((file.getName().startsWith(base.getName()+".pending-")||file.getName().startsWith(base.getName()+".obsolete-"))&&!pendingFolders.contains(file))stale.add(file);}
    for(File file:stale){if(Thread.currentThread().isInterrupted())return;try{delete(file);}catch(IOException deferred){}}
  }
  private static synchronized void check(long token)throws InterruptedIOException{
    if(token!=generation||Thread.currentThread().isInterrupted())throw new InterruptedIOException("Import de tuiles annulé ou remplacé");
  }
  private static boolean image(byte[] data,int size,boolean png){
    if(png)return size>=8&&(data[0]&255)==137&&data[1]==80&&data[2]==78&&data[3]==71&&data[4]==13&&data[5]==10&&data[6]==26&&data[7]==10;
    return size>=3&&(data[0]&255)==255&&(data[1]&255)==216&&(data[2]&255)==255;
  }
  private static void delete(File root)throws IOException{
    if(!root.exists())return;
    try(java.util.stream.Stream<Path> paths=Files.walk(root.toPath())){
      Iterator<Path> iterator=paths.sorted(Comparator.reverseOrder()).iterator();while(iterator.hasNext()){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("Nettoyage annulé");Files.delete(iterator.next());}
    }
  }
}
