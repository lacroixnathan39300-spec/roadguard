package fr.roadguard.app;

import android.os.Looper;
import android.webkit.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

/** Verifies native preference/bridge integration; does not claim pixel rendering. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk={26,36},shadows={MapAppearanceRuntimeTest.CaptureWebView.class})
@LooperMode(LooperMode.Mode.PAUSED)
public class MapAppearanceRuntimeTest {
  @Implements(WebView.class) public static class CaptureWebView extends org.robolectric.shadows.ShadowWebView {
    static final List<String> scripts=new ArrayList<>();
    @Implementation @Override protected void evaluateJavascript(String script,ValueCallback<String> callback){scripts.add(script);if(script.startsWith("(function(){if(typeof map")){if(callback!=null)callback.onReceiveValue("[46.75,5.9,60000]");}else super.evaluateJavascript(script,callback);}
  }
  @Test public void eachLocalPackIsSavedAndSentToTheMapWithoutResettingTheChoice()throws Exception{
    org.robolectric.android.controller.ActivityController<PhoneRuntimeTest.OfflineActivity> controller=Robolectric.buildActivity(PhoneRuntimeTest.OfflineActivity.class).setup();
    try{
      PhoneRuntimeTest.OfflineActivity a=controller.get();a.initialLoad.get(30,TimeUnit.SECONDS);Shadows.shadowOf(Looper.getMainLooper()).idle();
      for(String pack:new String[]{"graphite","readable","original"}){
        a.selected="Apparence";a.render();ReferenceUi.Bridge bridge=new ReferenceUi.Bridge(a,a.referenceWebView);
        bridge.action("mapPack",pack);Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertEquals(pack,a.getPreferences(0).getString("map_appearance_pack",""));assertEquals(pack,ReferenceUi.data(a).getString("mapPack"));
        a.selected="Carte";a.render();a.mapReady=true;CaptureWebView.scripts.clear();a.applyMapTheme();
        assertTrue(CaptureWebView.scripts.stream().anyMatch(script->script.contains("setRoadguardMapPack(\""+pack+"\")")));
        a.selected="Apparence";a.render();new ReferenceUi.Bridge(a,a.referenceWebView).action("mapPack","unknown-pack");Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertEquals(pack,a.getPreferences(0).getString("map_appearance_pack",""));
      }
    }finally{CaptureWebView.scripts.clear();controller.pause().stop().destroy();}
  }
  @Test public void largeCatalogSendsOnlyOneBoundedMapSlice()throws Exception{
    org.robolectric.android.controller.ActivityController<PhoneRuntimeTest.OfflineActivity> controller=Robolectric.buildActivity(PhoneRuntimeTest.OfflineActivity.class).setup();
    try{
      PhoneRuntimeTest.OfflineActivity a=controller.get();a.initialLoad.get(30,TimeUnit.SECONDS);Shadows.shadowOf(Looper.getMainLooper()).idle();a.selected="Carte";a.render();a.mapReady=true;
      a.dangers.clear();for(int i=0;i<20000;i++)a.dangers.add(new MainActivity.Danger(46.75+(i%100)*.0001,5.9+(i/100)*.0001,"Radar fixe"));
      a.rebuildDangerSpatialIndex();CaptureWebView.scripts.clear();a.pushDangers();
      String script=CaptureWebView.scripts.stream().filter(x->x.startsWith("setDangers([" )).findFirst().orElse("");
      assertTrue(script.length()>0&&script.length()<70000);assertEquals(600,script.split("Radar fixe",-1).length-1);
    }finally{CaptureWebView.scripts.clear();controller.pause().stop().destroy();}
  }
  @Test public void mapPickerSelectsAVisiblePackWithoutLeavingTheMap()throws Exception{
    org.robolectric.android.controller.ActivityController<PhoneRuntimeTest.OfflineActivity> controller=Robolectric.buildActivity(PhoneRuntimeTest.OfflineActivity.class).setup();
    try{
      PhoneRuntimeTest.OfflineActivity a=controller.get();a.initialLoad.get(30,TimeUnit.SECONDS);Shadows.shadowOf(Looper.getMainLooper()).idle();a.selected="Carte";a.render();a.showMapPackPicker();
      android.app.AlertDialog dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertNotNull(dialog);dialog.getListView().performItemClick(dialog.getListView().getChildAt(1),1,1);
      assertEquals("readable",a.getPreferences(0).getString("map_appearance_pack",""));assertEquals("Carte",a.selected);
    }finally{controller.pause().stop().destroy();}
  }

  @Test public void everyMapEngineReceivesTheSelectedPack()throws Exception{
    org.robolectric.android.controller.ActivityController<PhoneRuntimeTest.OfflineActivity> controller=Robolectric.buildActivity(PhoneRuntimeTest.OfflineActivity.class).setup();
    try{
      PhoneRuntimeTest.OfflineActivity a=controller.get();a.initialLoad.get(30,TimeUnit.SECONDS);Shadows.shadowOf(Looper.getMainLooper()).idle();
      a.getPreferences(0).edit().putString("map_appearance_pack","readable").commit();
      for(int engine=0;engine<3;engine++){
        a.offlineView=engine==1;a.mapLibrePreview=engine==2;a.selected="Carte";a.render();a.mapReady=true;
        CaptureWebView.scripts.clear();a.applyMapTheme();
        assertTrue("engine "+engine,CaptureWebView.scripts.stream().anyMatch(script->script.contains("setRoadguardMapPack(\"readable\")")));
      }
    }finally{CaptureWebView.scripts.clear();controller.pause().stop().destroy();}
  }

  @Test public void sixPaletteChoicesPersistStyleAndExplicitDayNight()throws Exception{
    org.robolectric.android.controller.ActivityController<PhoneRuntimeTest.OfflineActivity> controller=Robolectric.buildActivity(PhoneRuntimeTest.OfflineActivity.class).setup();
    try{
      PhoneRuntimeTest.OfflineActivity a=controller.get();a.initialLoad.get(30,TimeUnit.SECONDS);Shadows.shadowOf(Looper.getMainLooper()).idle();
      for(String pack:new String[]{"graphite","readable","original"})for(String mode:new String[]{"day","night"}){
        a.getPreferences(0).edit().putBoolean("theme_auto",true).commit();
        a.selected="Apparence";a.render();new ReferenceUi.Bridge(a,a.referenceWebView).action("mapPalette",pack+":"+mode);
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertEquals(pack,a.getPreferences(0).getString("map_appearance_pack",""));
        assertEquals(mode.equals("night"),a.nightMap);
        assertEquals(mode.equals("night"),a.getPreferences(0).getBoolean("night_map",!a.nightMap));
        assertFalse(a.getPreferences(0).getBoolean("theme_auto",true));
        a.selected="Carte";a.render();a.mapReady=true;CaptureWebView.scripts.clear();a.applyMapTheme();
        assertTrue(CaptureWebView.scripts.stream().anyMatch(script->script.contains("setRoadguardMapPack(\""+pack+"\")")));
        assertTrue(CaptureWebView.scripts.stream().anyMatch(script->script.contains("setRoadguardNight("+(mode.equals("night")?"true":"false")+")")));
      }
    }finally{CaptureWebView.scripts.clear();controller.pause().stop().destroy();}
  }

}
