package fr.roadguard.app;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.Locale;

/** One cancellable pinned transfer. Failures leave the previous archive untouched. */
final class RadarArchiveDownloader {
  interface Connections { HttpURLConnection open(URL url)throws IOException; }
  interface Progress { void update(long received,long expected); }
  static final class Cancelled extends IOException { Cancelled(){super("Téléchargement annulé");} }
  private final Connections connections;
  private volatile boolean cancelled;
  private boolean started,committed;
  private volatile HttpURLConnection active;
  RadarArchiveDownloader(){this(url->(HttpURLConnection)url.openConnection());}
  RadarArchiveDownloader(Connections connections){this.connections=connections;}
  void cancel(){
    HttpURLConnection connection;
    synchronized(this){if(committed)return;cancelled=true;connection=active;}
    if(connection!=null)connection.disconnect();
  }
  private void checkCancelled()throws Cancelled{
    if(cancelled||Thread.currentThread().isInterrupted())throw new Cancelled();
  }
  private synchronized void attach(HttpURLConnection connection)throws Cancelled{
    if(cancelled){connection.disconnect();throw new Cancelled();}active=connection;
  }
  File download(RadarDownloadSource source,File folder,Progress progress)throws IOException{
    synchronized(this){if(started)throw new IOException("Transfert déjà utilisé");started=true;}
    checkCancelled();
    if(source==null||!source.country.matches("[A-Z]{2}")||source.bytes<33
        ||source.bytes>RadarArchivePolicy.MAX_ARCHIVE_BYTES||!source.sha256.matches("[a-f0-9]{64}"))
      throw new IOException("Source radar non valide");
    if(!folder.isDirectory()&&!folder.mkdirs())throw new IOException("Dossier de téléchargement inaccessible");
    String cc=source.country.toLowerCase(Locale.ROOT);
    File pending=File.createTempFile(cc+"-camera-",".pending",folder);
    File target=new File(folder,cc+"-camera.sqlite.7z");
    HttpURLConnection connection=null;
    try{
      URL current=new URL(source.url);
      for(int redirects=0;redirects<=5;redirects++){
        checkCancelled();
        if(!RadarDownloadSource.allowed(current))throw new IOException("Destination de téléchargement non autorisée");
        connection=connections.open(current);attach(connection);
        connection.setInstanceFollowRedirects(false);connection.setConnectTimeout(15000);connection.setReadTimeout(15000);
        connection.setRequestProperty("Accept-Encoding","identity");
        connection.setRequestProperty("Accept","application/octet-stream,application/x-7z-compressed");
        int response=connection.getResponseCode();checkCancelled();
        if(response==301||response==302||response==303||response==307||response==308){
          String location=connection.getHeaderField("Location");connection.disconnect();active=null;
          if(location==null||redirects==5)throw new IOException("Trop de redirections ou destination absente");
          current=new URL(current,location);continue;
        }
        if(response!=200)throw new IOException("Serveur HTTP "+response+" (source indisponible)");
        break;
      }
      String contentType=connection.getContentType();
      if(contentType!=null&&contentType.toLowerCase(Locale.ROOT).contains("text/html"))
        throw new IOException("La source a renvoyé une page Web");
      long total=connection.getContentLengthLong();
      if(total>=0&&total!=source.bytes)throw new IOException("Taille différente de la release vérifiée");
      long done=0,lastProgress=0;
      if(progress!=null)progress.update(0,source.bytes);
      try(InputStream input=connection.getInputStream();FileOutputStream output=new FileOutputStream(pending)){
        byte[] buffer=new byte[16384];int count;
        while((count=input.read(buffer))!=-1){
          checkCancelled();
          if(count>source.bytes-done)throw new IOException("Taille maximale de la release dépassée");
          done+=count;output.write(buffer,0,count);long now=System.nanoTime();
          if(progress!=null&&(done==source.bytes||now-lastProgress>=100_000_000L)){
            progress.update(done,source.bytes);lastProgress=now;
          }
        }
        checkCancelled();output.getFD().sync();
      }
      if(done!=source.bytes)throw new IOException("Téléchargement incomplet : "+done+" / "+source.bytes+" octets");
      if(!source.verify(pending))throw new IOException("SHA-256 différent de la release vérifiée");
      RadarArchivePolicy.verify(pending);
      synchronized(this){
        checkCancelled();
        try{Files.move(pending.toPath(),target.toPath(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
        catch(AtomicMoveNotSupportedException unsupported){
          throw new IOException("Remplacement atomique indisponible ; archive précédente conservée",unsupported);
        }
        committed=true;
      }
      return target;
    }catch(IOException error){if(cancelled)throw new Cancelled();throw error;}
    finally{
      if(connection!=null)connection.disconnect();active=null;
      if(pending.exists())pending.delete();
    }
  }
}
