package fr.roadguard.app;
import android.webkit.WebView;
import android.webkit.WebSettings;
import java.net.URI;
/** Only packaged HTML may receive native application data and bridge access. */
final class LocalPagePolicy {
  private LocalPagePolicy(){}
  private static String asset(String url){
    try{URI u=new URI(url);if(!"file".equalsIgnoreCase(u.getScheme())||u.isOpaque()||u.getRawAuthority()!=null&&!u.getRawAuthority().isEmpty()||u.getRawQuery()!=null)return "";return u.getRawPath();}catch(Exception invalid){return "";}
  }
  static boolean referencePage(String url){return "/android_asset/reference_ui.html".equals(asset(url));}
  static boolean mapPage(String url){if(LocalMapAssets.mapPage(url))return true;String path=asset(url);return "/android_asset/map.html".equals(path)||"/android_asset/map_offline.html".equals(path)||"/android_asset/map_maplibre.html".equals(path);}
  static boolean browserLink(String url){try{URI u=new URI(url);return "https".equalsIgnoreCase(u.getScheme())&&u.getHost()!=null&&u.getUserInfo()==null&&(u.getPort()==-1||u.getPort()==443);}catch(Exception invalid){return false;}}
  static void configure(WebView view,boolean storage){
    WebSettings s=view.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(storage);
    // android_asset remains available; access to arbitrary file/content URLs is disabled.
    s.setAllowFileAccess(false);s.setAllowContentAccess(false);s.setAllowFileAccessFromFileURLs(false);s.setAllowUniversalAccessFromFileURLs(false);
    s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);s.setJavaScriptCanOpenWindowsAutomatically(false);s.setSupportMultipleWindows(false);
  }
}
