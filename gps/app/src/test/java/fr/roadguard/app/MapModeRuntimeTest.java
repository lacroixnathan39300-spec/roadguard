package fr.roadguard.app;
import android.webkit.WebView;
import android.os.Looper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;
/** Native ownership regressions; queued for the final Android/Robolectric session. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk={26,36})
@LooperMode(LooperMode.Mode.PAUSED)
public class MapModeRuntimeTest {
 @Test public void selectingOfflineThenAutoKeepsTheLoadedWebView()throws Exception{
  org.robolectric.android.controller.ActivityController<PhoneRuntimeTest.OfflineActivity> controller=Robolectric.buildActivity(PhoneRuntimeTest.OfflineActivity.class).setup();
  try{PhoneRuntimeTest.OfflineActivity a=controller.get();a.initialLoad.get(15,java.util.concurrent.TimeUnit.SECONDS);Shadows.shadowOf(Looper.getMainLooper()).idle();
   WebView map=new WebView(a);a.map=map;a.mapReady=true;a.offlineView=false;a.mapLibrePreview=false;a.mapConnectionMode="auto";
   a.selectMapConnection("offline");assertSame(map,a.map);assertTrue(a.mapReady);assertEquals("offline",a.mapConnectionMode);
   a.selectMapConnection("auto");assertSame(map,a.map);assertTrue(a.mapReady);assertEquals("auto",a.mapConnectionMode);
  }finally{controller.pause().stop().destroy();}
 }
}
