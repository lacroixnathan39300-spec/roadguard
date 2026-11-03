"""Compile actual page and asset policy; WebSettings adapters record configuration only."""
from pathlib import Path
import subprocess,tempfile
root=Path(__file__).resolve().parents[1];src=root/'app/src/main/java/fr/roadguard/app'
with tempfile.TemporaryDirectory() as d:
 t=Path(d);a=t/'android/webkit';a.mkdir(parents=True)
 (a/'WebSettings.java').write_text('''package android.webkit;public class WebSettings {
 public static final int MIXED_CONTENT_NEVER_ALLOW=1;public boolean file,content,cross,universal,js,storage;public int mixed;
 public void setJavaScriptEnabled(boolean b){js=b;}public void setDomStorageEnabled(boolean b){storage=b;}
 public void setAllowFileAccess(boolean b){file=b;}public void setAllowContentAccess(boolean b){content=b;}
 public void setAllowFileAccessFromFileURLs(boolean b){cross=b;}public void setAllowUniversalAccessFromFileURLs(boolean b){universal=b;}
 public void setMixedContentMode(int m){mixed=m;}public void setJavaScriptCanOpenWindowsAutomatically(boolean b){}public void setSupportMultipleWindows(boolean b){}
}''')
 (a/'WebView.java').write_text('''package android.webkit;public class WebView{public WebSettings settings=new WebSettings();public WebSettings getSettings(){return settings;}}''')
 (t/'Probe.java').write_text('''package fr.roadguard.app;import java.io.*;import android.webkit.*;
class Probe{static int n;static void ok(boolean b,String label){n++;if(!b)throw new AssertionError(label);}
 public static void main(String[] args)throws Exception{
  for(boolean canvas:new boolean[]{false,true})for(boolean vector:new boolean[]{false,true}){String page=LocalMapAssets.page(canvas,vector);ok(page.startsWith("https://roadguard.local/assets/"),"HTTPS origin");ok(LocalPagePolicy.mapPage(page),"bridge recognizes actual map");ok(!LocalPagePolicy.referencePage(page),"reference bridge remains isolated");}
  String[] bad={null,"https://evil.test/assets/map.html","https://roadguard.local.evil.test/assets/map.html","https://roadguard.local@evil.test/assets/map.html","https://user@roadguard.local/assets/map.html","https://roadguard.local:443/assets/map.html","http://roadguard.local/assets/map.html","https://roadguard.local/assets/map.html?redirect=x","https://roadguard.local/assets/%6dap.html","https://roadguard.local/assets/../map.html","https://roadguard.local/assets/a/../../map.html","https://roadguard.local/assets/map.html.evil","https://roadguard.local/assets/reference_ui.html","https://roadguard.local/assets/private.csv","content://roadguard.local/assets/map.html","javascript:alert(1)"};
  for(String url:bad){ok(!LocalMapAssets.mapPage(url),"bad origin/path accepted");ok(LocalMapAssets.path(url)==null,"unsafe asset path");}
  int[] opened={0};ok(LocalMapAssets.open(bad[1],path->{opened[0]++;return new ByteArrayInputStream(new byte[0]);})==null,"invalid request must not open storage");ok(opened[0]==0,"opener called");
  WebView view=new WebView();LocalPagePolicy.configure(view,true);ok(view.settings.js&&view.settings.storage,"map scripts/persistence allowed");ok(!view.settings.file&&!view.settings.content&&!view.settings.cross&&!view.settings.universal,"all file access settings remain locked");ok(view.settings.mixed==WebSettings.MIXED_CONTENT_NEVER_ALLOW,"mixed content remains blocked");
  System.out.println("PASS "+n+" actual origin/bridge/policy checks, with recording WebSettings adapters; no Android runtime.");
 }}''')
 sources=[src/'LocalMapAssets.java',src/'LocalPagePolicy.java',*a.glob('*.java'),t/'Probe.java']
 subprocess.run(['java','com.sun.tools.javac.Main','--release','8','-d',str(t),*map(str,sources)],check=True)
 subprocess.run(['java','-cp',str(t),'fr.roadguard.app.Probe'],check=True)
