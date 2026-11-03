package fr.roadguard.app;
import android.os.Looper;
import java.util.List;
import java.util.concurrent.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

/** Native Android integration checks, not executed by the JVM-only script. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk={26,36})
@LooperMode(LooperMode.Mode.PAUSED)
public class StartupRuntimeTest {
  public static class SlowCatalogActivity extends MainActivity {
    static CountDownLatch entered,release;
    @Override public android.content.SharedPreferences getPreferences(int mode){return getSharedPreferences("MainActivity",mode);}
    @Override void initializeRoadguard(){
      // Exercise the real two-frame splash and initializer; isolate external HTTP.
      if(community!=null){community.destroy();community=null;}
      super.initializeRoadguard();
    }
    @Override List<BundledRadarCatalog.Point> loadPhoneRadarCatalog(boolean fr,boolean ch)throws java.io.IOException{
      entered.countDown();try{if(!release.await(10,TimeUnit.SECONDS))throw new java.io.IOException("Test timed out");}
      catch(InterruptedException e){Thread.currentThread().interrupt();throw new java.io.InterruptedIOException();}
      return super.loadPhoneRadarCatalog(fr,ch);
    }
  }
  @Test public void mapAndCustomPointsRemainAvailableWhileRadarWorkerIsBlocked()throws Exception{
    SlowCatalogActivity.entered=new CountDownLatch(1);SlowCatalogActivity.release=new CountDownLatch(1);
    org.robolectric.android.controller.ActivityController<SlowCatalogActivity> c=Robolectric.buildActivity(SlowCatalogActivity.class).setup();
    try{
      SlowCatalogActivity a=c.get();
      Shadows.shadowOf(Looper.getMainLooper()).idleFor(100,TimeUnit.MILLISECONDS);
      assertTrue(SlowCatalogActivity.entered.await(3,TimeUnit.SECONDS));
      assertNotNull(a.map);assertFalse(a.initialLoad.isDone());assertFalse(a.radarCatalogLoaded);
      assertNull(a.radarLoadingLabel);assertNotNull(a.mapConnectionSwitch);assertTrue(a.radarLoadStatus.contains("arrière-plan"));
      MainActivity.Danger custom=new MainActivity.Danger(46.7,5.9,"Objet sur la route");a.dangers.add(custom);
      android.webkit.WebView map=a.map;SlowCatalogActivity.release.countDown();a.initialLoad.get(20,TimeUnit.SECONDS);Shadows.shadowOf(Looper.getMainLooper()).idle();
      assertSame(map,a.map);assertTrue(a.radarCatalogLoaded);assertTrue(a.dangers.contains(custom));assertEquals("",a.radarLoadStatus);
    }finally{SlowCatalogActivity.release.countDown();c.pause().stop().destroy();}
  }
}
