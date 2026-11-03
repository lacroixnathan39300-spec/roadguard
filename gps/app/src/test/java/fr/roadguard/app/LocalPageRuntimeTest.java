package fr.roadguard.app;
import android.webkit.*;
import android.net.Uri;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={26,36},shadows={LocalPageRuntimeTest.CaptureWebView.class})
@LooperMode(LooperMode.Mode.PAUSED)
public class LocalPageRuntimeTest {
 @Implements(WebView.class) public static class CaptureWebView extends org.robolectric.shadows.ShadowWebView {
  static android.webkit.ValueCallback<String> engineCheck;
  @Implementation @Override protected void evaluateJavascript(String script,android.webkit.ValueCallback<String> callback){if("typeof updateGPS === 'function'".equals(script))engineCheck=callback;super.evaluateJavascript(script,callback);}
 }

 ActivityController<PhoneRuntimeTest.OfflineActivity> controller;PhoneRuntimeTest.OfflineActivity activity;
 @Before public void setup()throws Exception{
  Shadows.shadowOf((android.app.Application)RuntimeEnvironment.getApplication()).grantPermissions(android.Manifest.permission.ACCESS_FINE_LOCATION,android.Manifest.permission.ACCESS_COARSE_LOCATION);
  controller=Robolectric.buildActivity(PhoneRuntimeTest.OfflineActivity.class).setup();activity=controller.get();activity.initialLoad.get(15,java.util.concurrent.TimeUnit.SECONDS);Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
 }
 @After public void close(){controller.pause().stop().destroy();}
 @Test public void httpsMapOriginServesPackagedAssetsWithoutOpeningFileAccess()throws Exception{
  assertEquals(LocalMapAssets.page(false,false),Shadows.shadowOf(activity.map).getLastLoadedUrl());
  for(String name:new String[]{"map.html","map_maplibre.html","map_offline.html","vendor/maplibre-gl.js","vendor/maplibre-gl.css"}){
   String url=LocalMapAssets.ORIGIN+"/assets/"+name;
   WebResourceResponse response=activity.serveOfflineTile(Uri.parse(url));assertNotNull(response);assertEquals(200,response.getStatusCode());
   try(java.io.InputStream body=response.getData()){assertTrue(body.read()>=0);}
  }
  assertFalse(activity.map.getSettings().getAllowUniversalAccessFromFileURLs());
  assertFalse(activity.map.getSettings().getAllowFileAccess());
 }
 @Test public void onlyExactPackagedPagesCanReceiveApplicationState(){
  assertTrue(LocalPagePolicy.mapPage(LocalMapAssets.page(false,false)));assertTrue(LocalPagePolicy.mapPage(LocalMapAssets.page(false,true)));assertTrue(LocalPagePolicy.mapPage("file:///android_asset/map.html"));assertTrue(LocalPagePolicy.mapPage("file:///android_asset/map_maplibre.html#map"));assertTrue(LocalPagePolicy.referencePage("file:///android_asset/reference_ui.html"));
  for(String url:new String[]{"https://example.org/map.html","file://evil/android_asset/map.html","file:///android_asset/map.html.evil","file:///android_asset/%6dap.html","file:///android_asset/map.html?redirect=x","file:///storage/emulated/0/map.html","content://data/map.html","javascript:alert(1)",null}){assertFalse(url,LocalPagePolicy.mapPage(url));assertFalse(url,LocalPagePolicy.referencePage(url));}
 }
 @Test public void externalPagesCannotMarkMapReadyOrReceivePosition(){
  WebView map=activity.map;activity.mapReady=false;org.robolectric.shadows.ShadowWebView shadow=Shadows.shadowOf(map);String before=shadow.getLastEvaluatedJavascript();
  shadow.getWebViewClient().onPageFinished(map,"https://example.org/");assertFalse(activity.mapReady);assertEquals(before,shadow.getLastEvaluatedJavascript());
  assertTrue(shadow.getWebViewClient().shouldOverrideUrlLoading(map,"https://example.org/"));assertFalse(shadow.getWebViewClient().shouldOverrideUrlLoading(map,LocalMapAssets.page(false,false)));
 }
 @Test public void webSettingsBlockArbitraryFilesContentAndCrossFileAccess(){
  WebSettings settings=activity.map.getSettings();assertTrue(settings.getJavaScriptEnabled());assertFalse(settings.getAllowFileAccess());assertFalse(settings.getAllowContentAccess());assertFalse(settings.getAllowFileAccessFromFileURLs());assertFalse(settings.getAllowUniversalAccessFromFileURLs());assertEquals(WebSettings.MIXED_CONTENT_NEVER_ALLOW,settings.getMixedContentMode());
 }
 @Test public void bridgeFromReplacedScreenCannotNavigateCurrentScreen(){
  activity.selected="Paramètres";activity.render();WebView old=activity.referenceWebView;ReferenceUi.Bridge bridge=new ReferenceUi.Bridge(activity,old);
  activity.selected="Apparence";activity.render();bridge.action("screen","Carte");Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertEquals("Apparence",activity.selected);
 }
 @Test public void currentPackagedScreenBridgeStillNavigates(){
  activity.selected="Paramètres";activity.render();ReferenceUi.Bridge bridge=new ReferenceUi.Bridge(activity,activity.referenceWebView);bridge.action("screen","Carte");Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertEquals("Carte",activity.selected);
 }
 @Test public void onlyExplicitHttpsLinksMayBeHandedToBrowser(){
  assertTrue(LocalPagePolicy.browserLink("https://www.openstreetmap.org/copyright"));assertFalse(LocalPagePolicy.browserLink("intent://settings"));assertFalse(LocalPagePolicy.browserLink("https://user:secret@example.org/"));assertFalse(LocalPagePolicy.browserLink("http://example.org"));
 }
 @Test public void lateJavascriptEngineCallbackCannotChangeNewMapStatus(){
  WebView old=activity.map;Shadows.shadowOf(old).getWebViewClient().onPageFinished(old,LocalMapAssets.page(false,false));android.webkit.ValueCallback<String> result=CaptureWebView.engineCheck;assertNotNull(result);
  activity.render();String before=activity.gps.getText().toString();result.onReceiveValue("false");assertEquals(before,activity.gps.getText().toString());
 }

}
