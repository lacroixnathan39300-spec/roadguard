package fr.roadguard.app;

import java.io.*;
import java.net.*;
import java.util.Locale;

/** Fetches one explicitly authorized, checksum-pinned map archive into OfflineCellStore.
 * This is a synchronous core, not an Android background worker or a map provider. */
public final class VerifiedMapDownloader {
  private VerifiedMapDownloader() {}
  public static final class Pack {
    public final String id, url, sha256;
    public final long maxBytes;
    public Pack(String id,String url,String sha256,long maxBytes){
      this.id=id;this.url=url;this.sha256=sha256;this.maxBytes=maxBytes;
    }
  }
  public static boolean download(OfflineCellStore store,Pack pack) throws IOException {
    return download(store,pack,false);
  }
  /** Only use allowLoopbackHttp in isolated local tests. */
  static boolean download(OfflineCellStore store,Pack pack,boolean allowLoopbackHttp) throws IOException {
    if(store==null||pack==null||pack.id==null||!pack.id.matches("[A-Za-z0-9_.:-]{1,120}")
        ||pack.id.equals(".")||pack.id.equals("..")||pack.sha256==null
        ||!pack.sha256.matches("[a-fA-F0-9]{64}")||pack.maxBytes<=0)
      throw new IOException("Invalid signed pack metadata");
    URL url=new URL(pack.url);
    boolean secure="https".equalsIgnoreCase(url.getProtocol());
    boolean loopback=allowLoopbackHttp&&"http".equalsIgnoreCase(url.getProtocol())
        &&("127.0.0.1".equals(url.getHost())||"localhost".equalsIgnoreCase(url.getHost()));
    if(!secure&&!loopback)throw new IOException("Map pack must use HTTPS");
    if(url.getUserInfo()!=null||url.getRef()!=null)throw new IOException("Unsafe URL");
    if(store.hasVerified(pack.id))return false;
    URLConnection raw=url.openConnection();
    if(!(raw instanceof HttpURLConnection))throw new IOException("HTTP(S) required");
    HttpURLConnection conn=(HttpURLConnection)raw;
    conn.setInstanceFollowRedirects(false);
    conn.setConnectTimeout(12000);conn.setReadTimeout(30000);
    conn.setRequestProperty("Accept-Encoding","identity");
    try {
      int code=conn.getResponseCode();
      if(code!=200)throw new IOException("Map server HTTP "+code+" (redirects disabled)");
      long announced=conn.getContentLengthLong();
      if(announced>pack.maxBytes)throw new IOException("Map exceeds size limit");
      try(InputStream in=new BufferedInputStream(conn.getInputStream())){
        store.importVerified(pack.id,in,pack.sha256.toLowerCase(Locale.ROOT),pack.maxBytes);
      }
      return true;
    }finally{conn.disconnect();}
  }
}
