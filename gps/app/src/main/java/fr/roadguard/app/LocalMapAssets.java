package fr.roadguard.app;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/** Packaged map pages and resources share the HTTPS origin of local PMTiles.
 * HTTPS is a WebView origin here: requests are intercepted, never fetched by DNS. */
final class LocalMapAssets {
  static final String ORIGIN="https://roadguard.local";
  private static final String PREFIX="/assets/";
  interface Opener { InputStream open(String path)throws IOException; }
  static final class Resource {
    final String mime;final InputStream body;
    Resource(String mime,InputStream body){this.mime=mime;this.body=body;}
  }
  private LocalMapAssets(){}
  static String page(boolean offlineCanvas,boolean vector){
    return ORIGIN+PREFIX+(offlineCanvas?"map_offline.html":vector?"map_maplibre.html":"map.html");
  }
  static String path(String url){
    try{
      URI uri=new URI(url);
      if(!"https".equals(uri.getScheme())||!"roadguard.local".equals(uri.getHost())
          ||uri.getPort()!=-1||uri.getRawUserInfo()!=null||uri.getRawQuery()!=null||uri.getRawFragment()!=null)return null;
      String raw=uri.getRawPath();if(raw==null||!raw.startsWith(PREFIX))return null;
      String path=raw.substring(PREFIX.length());
      if(!path.matches("[A-Za-z0-9_-]+(?:[./-][A-Za-z0-9_-]+)*"))return null;
      for(String component:path.split("/"))if(component.equals(".")||component.equals(".."))return null;
      return mime(path)==null?null:path;
    }catch(Exception invalid){return null;}
  }
  static boolean mapPage(String url){
    String path=path(url);return "map.html".equals(path)||"map_offline.html".equals(path)||"map_maplibre.html".equals(path);
  }
  static String mime(String path){
    if(path==null)return null;
    if(path.endsWith(".html"))return path.equals("map.html")||path.equals("map_offline.html")||path.equals("map_maplibre.html")?"text/html":null;
    if(path.endsWith(".js"))return "application/javascript";
    if(path.endsWith(".css"))return "text/css";
    if(path.endsWith(".png"))return "image/png";
    if(path.endsWith(".jpg")||path.endsWith(".jpeg"))return "image/jpeg";
    if(path.endsWith(".webp"))return "image/webp";
    if(path.endsWith(".svg"))return "image/svg+xml";
    return null;
  }
  static Resource open(String url,Opener opener)throws IOException {
    String path=path(url);return path==null?null:new Resource(mime(path),opener.open(path));
  }
}
