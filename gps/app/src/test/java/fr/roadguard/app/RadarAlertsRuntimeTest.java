package fr.roadguard.app;
import android.app.*;
import android.content.*;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import java.io.*;
import java.time.Duration;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.shadows.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={26,36})
@LooperMode(LooperMode.Mode.PAUSED)
public class RadarAlertsRuntimeTest {
 Application app;
 @Before public void setup(){app=RuntimeEnvironment.getApplication();app.getSharedPreferences("roadguard_radar_audio",0).edit().clear().commit();app.getSharedPreferences("MainActivity",0).edit().clear().commit();}
 int alert(double m,double speed,int limit,long time){return RadarAlertAudio.claim(app,46.75,5.9,m,speed,limit,time);}
 @Test public void unknownLimitNeverGeneratesLoudAlarm(){assertEquals(1,alert(990,130,0,100000));assertEquals(0,alert(980,140,0,101000));assertEquals(1,alert(949,150,0,102300));}
 @Test public void exceedingKnownLimitImmediatelyEscalates(){assertEquals(1,alert(990,69,70,100000));assertEquals(2,alert(980,71,70,101000));assertEquals(0,alert(970,90,70,102000));assertEquals(2,alert(960,90,70,103000));}
 @Test public void exactLimitIsQuietAndStaleSpeedNeverProvesOverspeed(){assertEquals(1,alert(990,70,70,100000));assertEquals(1,alert(940,Double.NaN,70,102300));assertEquals(0,alert(930,Double.POSITIVE_INFINITY,70,103000));}
 @Test public void repeatedBeepsStartAtOneKilometreAndStopOutside(){assertEquals(0,alert(1001,50,70,100000));assertEquals(1,alert(1000,50,70,101000));assertEquals(0,alert(990,50,70,102000));assertEquals(1,alert(950,50,70,103200));assertEquals(1,alert(250,50,70,105500));assertEquals(1,alert(50,50,70,107800));assertEquals(0,alert(1001,50,70,110000));}
 @Test public void sharedCooldownAppliesAcrossNearbyRadarsAndOwners(){assertEquals(2,alert(400,90,70,100000));assertEquals(0,RadarAlertAudio.claim(app,46.751,5.9,300,90,70,100300));assertEquals(2,RadarAlertAudio.claim(app,46.751,5.9,290,90,70,101500));}
 @Test public void repeatPassCanAlertAgainAfterThirtyMinutes(){assertEquals(1,alert(400,50,70,100000));assertEquals(1,alert(400,50,70,1900000));}
 @Test public void haloHysteresisIsSteadyWithinApproach(){assertFalse(RadarAlertPolicy.halo(1001,false));assertTrue(RadarAlertPolicy.halo(1000,false));assertTrue(RadarAlertPolicy.halo(1030,true));assertFalse(RadarAlertPolicy.halo(1051,true));assertFalse(RadarAlertPolicy.halo(Double.NaN,true));}
 @Test public void onlyExplicitKilometreLimitLabelsAreAccepted(){assertEquals(70,RadarAlertPolicy.labelLimit("Radar fixe · 70 km/h"));assertEquals(0,RadarAlertPolicy.labelLimit("Radar n°70"));assertEquals(0,RadarAlertPolicy.labelLimit("Contrôle mobile"));assertEquals(0,RadarAlertPolicy.limit("-70"));assertEquals(0,RadarAlertPolicy.limit("500"));assertEquals(100,RadarAlertPolicy.LOUD_VOLUME);assertEquals(100,RadarAlertPolicy.QUIET_VOLUME);}
 @Test public void catalogRetainsKnownSpeedButRejectsConflictingValues()throws Exception{
  String header="id;type;lat;lon;country;category;speed_kmh;direction_deg;status;sources;source_ids;speed_conflicts\n";
  String row="rg-01234567890123456789;Radar fixe;46.75;5.9;FR;fixed;70;;inventory;official;official:1;";
  assertEquals(70,BundledRadarCatalog.read(new BufferedReader(new StringReader(header+row+"\n")),"FR").get(0).speedKmh);
  assertEquals(0,BundledRadarCatalog.read(new BufferedReader(new StringReader(header+row+"70|90\n")),"FR").get(0).speedKmh);
 }
 @Test public void floatingWarningRemainsBlueBeyondOldTimeoutAndClearsExplicitly()throws Exception{
  ShadowSettings.setCanDrawOverlays(true);RadarBubbleOverlay b=new RadarBubbleOverlay(app, kind -> {});try{b.show();b.radar("Radar fixe",400);java.lang.reflect.Field f=RadarBubbleOverlay.class.getDeclaredField("bubble");f.setAccessible(true);View view=(View)f.get(b);assertNotNull(view);assertEquals(1f,view.getAlpha(),0);Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(20));assertEquals(1f,view.getAlpha(),0);java.lang.reflect.Field params=RadarBubbleOverlay.class.getDeclaredField("params");params.setAccessible(true);android.view.WindowManager.LayoutParams size=(android.view.WindowManager.LayoutParams)params.get(b);int expanded=size.width;b.clearRadar();assertTrue(size.width<expanded);assertEquals(1f,view.getAlpha(),0);}finally{b.close();}
 }
 @Test public void nativeBlueFrameIsPersistentNonInteractiveAndReleasedOnPause()throws Exception{
  org.robolectric.android.controller.ActivityController<PhoneRuntimeTest.OfflineActivity> c=Robolectric.buildActivity(PhoneRuntimeTest.OfflineActivity.class).setup();try{PhoneRuntimeTest.OfflineActivity a=c.get();a.initialLoad.get(15,java.util.concurrent.TimeUnit.SECONDS);Shadows.shadowOf(Looper.getMainLooper()).idle();a.dangerBanner=new android.widget.TextView(a);a.previousAcceptedGps=new android.location.Location("gps");a.previousAcceptedGps.setLatitude(46.75);a.previousAcceptedGps.setLongitude(5.9);a.previousAcceptedGps.setAccuracy(5);a.previousAcceptedGps.setTime(System.currentTimeMillis());MainActivity.Danger radar=new MainActivity.Danger(46.75,5.9,"Radar fixe",70);a.styleRadarWarning(radar,400);assertTrue(a.radarHaloVisible);assertNotNull(a.radarBlueHalo);assertFalse(a.radarBlueHalo.isClickable());assertEquals(View.VISIBLE,a.radarBlueHalo.getVisibility());Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(10));assertEquals(View.VISIBLE,a.radarBlueHalo.getVisibility());assertEquals(1f,a.dangerBanner.getAlpha(),0);c.pause();assertFalse(a.radarHaloVisible);assertEquals(View.GONE,a.radarBlueHalo.getVisibility());c.stop();}finally{c.destroy();}
 }
 @Test public void loadedLimitsReachPhoneBackgroundAndCar()throws Exception{
  java.util.List<BundledRadarCatalog.Point> points=BundledRadarCatalog.active(n->app.getAssets().open(n),false,true);BundledRadarCatalog.Point known=points.stream().filter(p->p.speedKmh>0).findFirst().get();
  CarRouteData data=new CarRouteData(app);data.loadRadars();assertTrue(data.radars.stream().anyMatch(p->p.lat==known.lat&&p.lon==known.lon&&p.speedLimit==known.speedKmh));
  Shadows.shadowOf(app).grantPermissions(android.Manifest.permission.ACCESS_FINE_LOCATION,android.Manifest.permission.ACCESS_COARSE_LOCATION);
  org.robolectric.android.controller.ServiceController<BackgroundAlertService> c=Robolectric.buildService(BackgroundAlertService.class).create();try{java.lang.reflect.Field f=BackgroundAlertService.class.getDeclaredField("points");f.setAccessible(true);java.util.List<double[]> background=(java.util.List<double[]>)f.get(c.get());for(int wait=0;wait<200&&background.isEmpty();wait++){Shadows.shadowOf(Looper.getMainLooper()).idle();Thread.sleep(10);}assertTrue(background.stream().anyMatch(p->p.length>3&&p[0]==known.lat&&p[1]==known.lon&&(int)p[3]==known.speedKmh));}finally{c.destroy();}
  MainActivity.Danger d=new MainActivity.Danger(known.lat,known.lon,known.type,known.speedKmh);assertEquals(known.speedKmh,d.speedLimit);
 }
 @Test public void nearbyHazardDoesNotMaskBlueRadarApproach()throws Exception{
  org.robolectric.android.controller.ActivityController<PhoneRuntimeTest.OfflineActivity> c=Robolectric.buildActivity(PhoneRuntimeTest.OfflineActivity.class).setup();try{PhoneRuntimeTest.OfflineActivity a=c.get();a.initialLoad.get(15,java.util.concurrent.TimeUnit.SECONDS);Shadows.shadowOf(Looper.getMainLooper()).idle();a.dangers.clear();a.dangers.add(new MainActivity.Danger(46.7535,5.9,"Radar fixe",70));a.dangers.add(new MainActivity.Danger(46.7504,5.9,"Danger"));a.rebuildDangerSpatialIndex();a.lastLat=46.75;a.lastLon=5.9;a.lastAccuracy=5;a.previousAcceptedGps=new android.location.Location("gps");a.previousAcceptedGps.setLatitude(a.lastLat);a.previousAcceptedGps.setLongitude(a.lastLon);a.previousAcceptedGps.setAccuracy(5);a.previousAcceptedGps.setTime(System.currentTimeMillis());a.reliableBearing=false;a.dangerBanner=new android.widget.TextView(a);a.checkDangers();assertTrue(a.radarHaloVisible);assertNotNull(a.activeRadarUi);assertTrue(a.dangerBanner.getText().toString().contains("70 km/h"));a.lastLat=46.76;a.checkDangers();assertFalse(a.radarHaloVisible);c.pause().stop();}finally{c.destroy();}
 }

}
