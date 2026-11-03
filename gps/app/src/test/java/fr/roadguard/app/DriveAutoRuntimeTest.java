package fr.roadguard.app;
import android.Manifest;
import android.app.Application;
import android.content.*;
import android.content.pm.*;
import android.location.*;
import android.os.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.annotation.*;
import org.robolectric.shadows.*;
import java.time.Duration;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={26,36})
@LooperMode(LooperMode.Mode.PAUSED)
public class DriveAutoRuntimeTest {
  Application app;ServiceController<BackgroundAlertService> controller;BackgroundAlertService service;
  @Before public void setup(){app=RuntimeEnvironment.getApplication();Shadows.shadowOf(app).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.POST_NOTIFICATIONS);app.getSharedPreferences("MainActivity",0).edit().clear().putBoolean("floating_auto",true).putBoolean("monitor_armed",true).commit();ShadowSettings.setCanDrawOverlays(true);BackgroundAlertService.setRoadguardVisible(true);Shadows.shadowOf(Looper.getMainLooper()).idle();}
  @After public void close(){if(controller!=null)controller.destroy();BackgroundAlertService.setRoadguardVisible(true);Shadows.shadowOf(Looper.getMainLooper()).idle();}
  DriveAutoPolicy started(){DriveAutoPolicy p=new DriveAutoPolicy();p.update(0,21);p.update(1000,21);assertTrue(p.active(1000));return p;}
  @Test public void strictThresholdNeedsTwoDistinctReliableMeasurements(){DriveAutoPolicy p=new DriveAutoPolicy();p.update(0,20);p.update(1000,20);assertFalse(p.active(1000));p.update(2000,21);assertFalse(p.active(2000));p.update(2000,25);assertFalse(p.active(2000));p.update(3000,21);assertTrue(p.active(3000));}
  @Test public void stopRequiresWholeMinuteOfStationaryFreshFixes(){DriveAutoPolicy p=started();for(int t=2000;t<62000;t+=2000){p.update(t,0);assertTrue(p.active(t));}p.update(62000,0);assertFalse(p.active(62000));}
  @Test public void slowMovingTrafficDoesNotCountAsParking(){DriveAutoPolicy p=started();for(int t=2000;t<=100000;t+=2000){p.update(t,8);assertTrue(p.active(t));}}
  @Test public void movementResetsStopTimer(){DriveAutoPolicy p=started();for(int t=2000;t<=40000;t+=2000)p.update(t,0);p.update(42000,7);for(int t=44000;t<=80000;t+=2000)p.update(t,0);assertTrue(p.active(80000));}
  @Test public void lostOrInvalidSpeedSuspendsWithoutInventingAStop(){DriveAutoPolicy p=started();assertFalse(p.active(12000));p.update(13000,0);assertFalse(p.active(13000));p.update(14000,30);p.update(15000,30);assertTrue(p.active(15000));p.update(16000,Double.NaN);assertFalse(p.active(16000));}
  @Test public void stoppedSessionRestartsOnNextDrive(){DriveAutoPolicy p=started();for(int t=2000;t<=62000;t+=2000)p.update(t,0);assertFalse(p.active(62000));p.update(64000,22);assertFalse(p.active(64000));p.update(66000,22);assertTrue(p.active(66000));}
  @Test public void longGpsGapBreaksStationaryContinuity(){DriveAutoPolicy p=started();p.update(2000,0);p.update(65000,0);assertFalse(p.active(65000));p.update(67000,25);assertFalse(p.active(67000));p.update(69000,25);assertTrue(p.active(69000));}
  void create(){controller=Robolectric.buildService(BackgroundAlertService.class).create();service=controller.get();service.onStartCommand(new Intent(app,BackgroundAlertService.class),0,1);assertTrue(BackgroundAlertService.running);}
  void fix(double kmh,int seconds){ShadowSystemClock.advanceBy(Duration.ofSeconds(seconds));Location l=new Location(LocationManager.GPS_PROVIDER);l.setLatitude(46.75);l.setLongitude(5.9);l.setAccuracy(5);l.setSpeed((float)(kmh/3.6));l.setSpeedAccuracyMetersPerSecond(.5f);l.setTime(System.currentTimeMillis());l.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());service.updateDrive(l);}
  void drivingOutsideApp(){create();fix(30,1);fix(30,1);BackgroundAlertService.setRoadguardVisible(false);Shadows.shadowOf(Looper.getMainLooper()).idle();assertTrue(BackgroundAlertService.monitoring);assertTrue(BackgroundAlertService.bubbleVisible);}
  @Test public void armedServiceDoesNotShowBubbleBeforeDriving(){create();BackgroundAlertService.setRoadguardVisible(false);Shadows.shadowOf(Looper.getMainLooper()).idle();assertFalse(BackgroundAlertService.monitoring);assertFalse(BackgroundAlertService.bubbleVisible);fix(15,1);fix(15,1);assertFalse(BackgroundAlertService.bubbleVisible);}
  @Test public void stationaryMinuteHidesBubbleButKeepsRearmWatch(){drivingOutsideApp();for(int i=0;i<33;i++)fix(0,2);assertFalse(BackgroundAlertService.monitoring);assertFalse(BackgroundAlertService.bubbleVisible);assertTrue(BackgroundAlertService.running);fix(30,2);fix(30,2);assertTrue(BackgroundAlertService.bubbleVisible);}
  @Test public void removingTaskClosesOverlayAndCannotResurrectIt(){drivingOutsideApp();service.onTaskRemoved(new Intent(app,MainActivity.class));assertFalse(BackgroundAlertService.running);assertFalse(BackgroundAlertService.bubbleVisible);assertTrue(Shadows.shadowOf(service).isStoppedBySelf());BackgroundAlertService.setRoadguardVisible(false);ShadowSystemClock.advanceBy(Duration.ofSeconds(90));Shadows.shadowOf(Looper.getMainLooper()).idle();assertFalse(BackgroundAlertService.bubbleVisible);}
  @Test public void explicitStopDisarmsAutomaticResume(){drivingOutsideApp();service.onStartCommand(new Intent(app,BackgroundAlertService.class).setAction(BackgroundAlertService.ACTION_STOP),0,2);assertFalse(BackgroundAlertService.running);assertFalse(BackgroundAlertService.bubbleVisible);assertFalse(app.getSharedPreferences("MainActivity",0).getBoolean("monitor_armed",true));}
  @Test public void returnToRoadguardHidesBubbleWithoutStoppingSession(){drivingOutsideApp();BackgroundAlertService.setRoadguardVisible(true);Shadows.shadowOf(Looper.getMainLooper()).idle();assertFalse(BackgroundAlertService.bubbleVisible);assertTrue(BackgroundAlertService.monitoring);BackgroundAlertService.setRoadguardVisible(false);Shadows.shadowOf(Looper.getMainLooper()).idle();assertTrue(BackgroundAlertService.bubbleVisible);}
  @Test public void backFromRootFinishesAndStopsWatch()throws Exception{
    org.robolectric.android.controller.ActivityController<PhoneRuntimeTest.OfflineActivity> c=Robolectric.buildActivity(PhoneRuntimeTest.OfflineActivity.class).setup();try{PhoneRuntimeTest.OfflineActivity a=c.get();a.initialLoad.get(15,java.util.concurrent.TimeUnit.SECONDS);Shadows.shadowOf(Looper.getMainLooper()).idle();create();a.selected="Carte";a.legacySection=false;a.onBackPressed();assertTrue(a.isFinishing());c.pause().stop();assertFalse(BackgroundAlertService.running);assertFalse(BackgroundAlertService.bubbleVisible);}finally{c.destroy();}
  }
  @Test public void taskLifetimeAndSpotifyListenerAreDeclaredSafely()throws Exception{ServiceInfo monitor=app.getPackageManager().getServiceInfo(new ComponentName(app,BackgroundAlertService.class),0);assertTrue("stopWithTask",(monitor.flags&ServiceInfo.FLAG_STOP_WITH_TASK)!=0);ServiceInfo music=app.getPackageManager().getServiceInfo(new ComponentName(app,SpotifySessionListener.class),0);assertEquals("android.permission.BIND_NOTIFICATION_LISTENER_SERVICE",music.permission);assertTrue("listener exported with system binding permission",music.exported);}
}
