package fr.roadguard.app;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.Locale;

/** Importer for an authorized, checksum-pinned radar CSV. No proprietary endpoint is assumed.
 * Call from a background executor; never on the UI thread. */
public final class AuthorizedRadarImporter {
  private AuthorizedRadarImporter() {}
  public static final class Source {
    public final String country, url, sha256;
    public final long maxBytes;
    public Source(String country,String url,String sha256,long maxBytes){
      this.country=country;this.url=url;this.sha256=sha256;this.maxBytes=maxBytes;
    }
  }
  public interface ProgressListener { void onProgress(long downloadedBytes, long totalBytes); }
  public static File downloadAndVerify(Source source, File destinationDirectory) throws Exception {
    return downloadAndVerify(source, destinationDirectory, null);
  }
  /** Progress callbacks run on the calling worker thread; marshal to UI thread when needed. */
  public static File downloadAndVerify(Source source, File destinationDirectory, ProgressListener progress) throws Exception {
    if(source==null||!("FR".equals(source.country)||"CH".equals(source.country))
        ||source.sha256==null||!source.sha256.matches("[a-fA-F0-9]{64}")
        ||source.maxBytes<1||source.maxBytes>100_000_000L||destinationDirectory==null)
      throw new IOException("Invalid authorized radar source metadata");
    URL url=new URL(source.url);
    if(!"https".equalsIgnoreCase(url.getProtocol())||url.getUserInfo()!=null||url.getRef()!=null)
      throw new IOException("HTTPS URL required");
    if(!destinationDirectory.isDirectory()&&!destinationDirectory.mkdirs())throw new IOException("Cannot create directory");
    File temporary=File.createTempFile("radars-", ".tmp", destinationDirectory);
    try {
      // Follow only ordinary HTTPS redirects. Never forward credentials or bypass
      // application-restricted endpoints. The final payload is still hash-verified.
      HttpURLConnection conn=null;
      URL current=url;
      for(int redirects=0;redirects<=5;redirects++){
        conn=(HttpURLConnection)current.openConnection();
        conn.setInstanceFollowRedirects(false);
        conn.setConnectTimeout(12000);conn.setReadTimeout(30000);
        conn.setRequestProperty("Accept-Encoding","identity");
        int response=conn.getResponseCode();
        if(response==301||response==302||response==303||response==307||response==308){
          if(redirects==5)throw new IOException("Too many radar download redirects");
          String location=conn.getHeaderField("Location");
          if(location==null||location.isEmpty())throw new IOException("Radar redirect without destination");
          URL next=new URL(current,location);
          if(!"https".equalsIgnoreCase(next.getProtocol())||next.getUserInfo()!=null||next.getRef()!=null)
            throw new IOException("Unsafe radar download redirect");
          conn.disconnect();current=next;continue;
        }
        break;
      }
      final HttpURLConnection responseConnection=conn;
      try {
        if(conn.getResponseCode()!=200)throw new IOException("Radar source HTTP "+conn.getResponseCode());
        String contentType=conn.getContentType();
        if(contentType!=null && contentType.toLowerCase(Locale.ROOT).contains("text/html"))
          throw new IOException("Radar endpoint returned a web page, not a radar database");
        long total=conn.getContentLengthLong();
        if(total>source.maxBytes)throw new IOException("Radar source too large");
        if(progress!=null)progress.onProgress(0,total);
        MessageDigest digest=MessageDigest.getInstance("SHA-256");
        long count=0;
        try(InputStream in=conn.getInputStream();OutputStream out=new FileOutputStream(temporary)){
          byte[] buffer=new byte[8192];int n;
          while((n=in.read(buffer))!=-1){count+=n;if(count>source.maxBytes)throw new IOException("Radar source too large");digest.update(buffer,0,n);out.write(buffer,0,n);if(progress!=null)progress.onProgress(count,total);}
        }
        StringBuilder hex=new StringBuilder();for(byte b:digest.digest())hex.append(String.format(Locale.ROOT,"%02x",b&255));
        if(!hex.toString().equalsIgnoreCase(source.sha256))throw new IOException("Radar checksum mismatch");
        try(BufferedReader reader=new BufferedReader(new InputStreamReader(new FileInputStream(temporary),"UTF-8"))){
          BundledRadarCatalog.read(reader,source.country);
        }
        File target=new File(destinationDirectory,"authorized_radars_"+source.country.toLowerCase(Locale.ROOT)+".csv");
        try{Files.move(temporary.toPath(),target.toPath(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
        catch(AtomicMoveNotSupportedException e){Files.move(temporary.toPath(),target.toPath(),StandardCopyOption.REPLACE_EXISTING);}
        return target;
      }finally{responseConnection.disconnect();}
    }finally{if(temporary.exists())temporary.delete();}
  }
}
