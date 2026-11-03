package fr.roadguard.app;

import android.location.LocationListener;
import android.os.Looper;
import java.util.concurrent.TimeUnit;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

/** Android integration coverage to run with Gradle/Robolectric; not replaced by the JVM adapters. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk={26,36})
@LooperMode(LooperMode.Mode.PAUSED)
public class RadarCatalogReloadRuntimeTest {
  ActivityController<PhoneRuntimeTest.OfflineActivity> controller;
  PhoneRuntimeTest.OfflineActivity activity;
  @Before public void open()throws Exception{
    controller=Robolectric.buildActivity(PhoneRuntimeTest.OfflineActivity.class).setup();activity=controller.get();
    activity.initialLoad.get(30,TimeUnit.SECONDS);Shadows.shadowOf(Looper.getMainLooper()).idle();
  }
  @After public void close(){controller.pause().stop().destroy();}
  void awaitReload()throws Exception{
    assertNotNull(activity.radarCatalogReload);activity.radarCatalogReload.get(30,TimeUnit.SECONDS);
    Shadows.shadowOf(Looper.getMainLooper()).idle();assertFalse(activity.radarCatalogReloadRunning);
  }
  @Test public void returnFromManagerReloadsCatalogAndPreservesCustomPoints()throws Exception{
    MainActivity.Danger personal=new MainActivity.Danger(46.75,5.9,"Danger personnel");activity.dangers.add(personal);
    int before=activity.bundledLoadedRadarCount;
    controller.pause();activity.getSharedPreferences("radar_country_visibility",0).edit().putBoolean("FR",false).commit();RadarCatalogRevision.changed(activity);
    controller.resume();awaitReload();
    assertTrue(activity.dangers.contains(personal));assertTrue(activity.bundledLoadedRadarCount<before);
    assertEquals(UpdatedRadarCatalog.active(activity,activity.getPreferences(0).getBoolean(BundledRadarCatalog.HISTORICAL_PREFERENCE,false),activity.getPreferences(0).getBoolean(BundledRadarCatalog.SWISS_HISTORICAL_PREFERENCE,true)).size(),activity.bundledLoadedRadarCount);
    assertTrue(activity.bundledLoadedFranceCount>0);assertTrue(activity.bundledLoadedSwissCount>0);
  }
  @Test public void unchangedRevisionDoesNotScheduleAnotherFullCatalogRead()throws Exception{
    RadarCatalogRevision.changed(activity);activity.requestRadarCatalogReload();awaitReload();
    java.util.concurrent.Future<?> previous=activity.radarCatalogReload;activity.requestRadarCatalogReload();assertSame(previous,activity.radarCatalogReload);
  }
  @Test public void activeReloadPreservesTheGpsSubscriptionAndDoesNotAppendDuplicates()throws Exception{
    activity.startGPS();LocationListener listener=activity.listener;
    RadarCatalogRevision.changed(activity);activity.requestRadarCatalogReload();awaitReload();int count=activity.dangers.size();
    RadarCatalogRevision.changed(activity);activity.requestRadarCatalogReload();awaitReload();
    assertEquals(count,activity.dangers.size());assertSame(listener,activity.listener);
  }
}
