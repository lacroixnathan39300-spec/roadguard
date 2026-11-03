package fr.roadguard.app;
import android.Manifest;
import android.app.Application;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.SystemClock;
import java.time.Duration;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.*;
import org.robolectric.shadows.ShadowSystemClock;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={26,28,34,36})
@LooperMode(LooperMode.Mode.PAUSED)
public class PhoneRuntimeTest {
  /** Avoid real startup network calls; execute the actual native initializer and screens. */
  public static class OfflineActivity extends MainActivity {
    @Override public android.content.SharedPreferences getPreferences(int mode){return getSharedPreferences("MainActivity",mode);}
    int fullRadarLoads=0;
    @Override void loadDangers(){super.loadDangers();fullRadarLoads++;}
    @Override void showStartupScreen(){community.destroy();community=null;initializeRoadguard();}
  }
  ActivityController<OfflineActivity> controller;OfflineActivity activity;
  @Before public void setup()throws Exception{Shadows.shadowOf((Application)RuntimeEnvironment.getApplication()).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION);controller=Robolectric.buildActivity(OfflineActivity.class).setup();activity=controller.get();activity.initialLoad.get(15,java.util.concurrent.TimeUnit.SECONDS);Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();}
  @After public void close(){controller.pause().stop().destroy();}
  Location fix(String provider,double speed,boolean measured){
    ShadowSystemClock.advanceBy(Duration.ofSeconds(1));Location l=new Location(provider);l.setLatitude(46.75);l.setLongitude(5.9);l.setAccuracy(5);l.setTime(System.currentTimeMillis());l.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
    if(measured){l.setSpeed((float)speed);l.setSpeedAccuracyMetersPerSecond(1);l.setBearing(359);l.setBearingAccuracyDegrees(3);}return l;
  }
  @Test public void rerenderPreservesGpsSubscriptionAndCounter(){
    activity.startGPS();LocationListener original=activity.listener;assertNotNull(original);original.onLocationChanged(fix(LocationManager.GPS_PROVIDER,20,true));assertEquals("72",activity.gpsSpeedText());
    activity.selected="Paramètres";activity.render();assertSame(original,activity.listener);activity.selected="Carte";activity.render();assertSame(original,activity.listener);assertEquals("72",activity.speed.getText().toString());
  }
  @Test public void interleavedNetworkCannotZeroNativeHudOrCourse(){
    activity.startGPS();activity.listener.onLocationChanged(fix(LocationManager.GPS_PROVIDER,25,true));activity.listener.onLocationChanged(fix(LocationManager.NETWORK_PROVIDER,0,false));assertEquals("90",activity.gpsSpeedText());assertEquals(359,activity.lastBearing,.001);assertEquals(90,activity.lastSpeedKmh,.001);
  }
  @Test public void isolatedZeroThenConfirmedStopAndLostSignal(){
    activity.startGPS();activity.listener.onLocationChanged(fix(LocationManager.GPS_PROVIDER,20,true));activity.listener.onLocationChanged(fix(LocationManager.GPS_PROVIDER,0,true));assertEquals("72",activity.gpsSpeedText());activity.listener.onLocationChanged(fix(LocationManager.GPS_PROVIDER,0,true));assertEquals("0",activity.gpsSpeedText());ShadowSystemClock.advanceBy(Duration.ofSeconds(7));assertEquals("--",activity.gpsSpeedText());
  }
  @Test public void speedCompensationDoesNotChangeRawMeasurement(){
    activity.startGPS();activity.getPreferences(0).edit().putInt("speed_offset_kmh",5).commit();activity.listener.onLocationChanged(fix(LocationManager.GPS_PROVIDER,20,true));assertEquals("77",activity.gpsSpeedText());assertEquals(72,activity.lastSpeedKmh,.001);
  }
  @Test public void allNativeReferenceScreensBuildWithoutReleasingGps(){
    activity.startGPS();LocationListener listener=activity.listener;
    for(String page:new String[]{"Accueil","Paramètres","Apparence","Trajet","Alertes","Signaler","Favoris","Journal","Historique","Hors ligne","Communauté","Bases radar","Android Auto"}){activity.selected=page;activity.render();assertNotNull(page,activity.referenceWebView);assertSame(page,listener,activity.listener);}
  }
  @Test public void pauseReleasesSubscriptionAndResumeReacquires(){activity.startGPS();assertNotNull(activity.listener);controller.pause();assertNull(activity.listener);activity.render();assertNull(activity.listener);controller.resume();assertNotNull(activity.listener);}
  @Test public void masterVoiceSwitchAlsoControlsTurnGuidance(){
    activity.selected="Carte";activity.voiceReady=true;activity.navigationPoints.clear();activity.navigationSteps.clear();activity.navigationPoints.add(new double[]{46.75,5.9});activity.navigationPoints.add(new double[]{46.751,5.9});MainActivity.NavStep turn=new MainActivity.NavStep(46.751,5.9,"Tourner à droite");turn.routeIndex=1;activity.navigationSteps.add(turn);
    activity.getPreferences(0).edit().putBoolean("Alertes vocales",false).putBoolean("Guidage vocal",true).commit();activity.announceNextManeuver(fix(LocationManager.GPS_PROVIDER,10,true));assertEquals(-1,activity.spokenStepIndex);
    activity.getPreferences(0).edit().putBoolean("Alertes vocales",true).commit();activity.announceNextManeuver(fix(LocationManager.GPS_PROVIDER,10,true));assertEquals(0,activity.spokenStepIndex);assertTrue(activity.spokenThreshold<3);
  }

  @Test public void routeSavedOnPhoneLoadsInCar()throws Exception{
    activity.destinationLat=46.76;activity.destinationLon=5.9;activity.getPreferences(0).edit().putString("destination","46.76,5.9").putString("destination_label","Favori partagé").commit();
    activity.navigationPoints.add(new double[]{46.75,5.9});activity.navigationPoints.add(new double[]{46.76,5.9});activity.navigationMeters=1112;activity.navigationSeconds=120;activity.saveCachedNavigation();
    CarRouteData data=new CarRouteData(activity);data.loadRoute();assertEquals(2,data.route.size());assertEquals("Favori partagé",data.label);assertEquals(120,data.seconds,.001);
  }

  @Test public void publicReportsRefreshPreservesLocalInventoryAndGps()throws Exception{
    activity.startGPS();LocationListener listener=activity.listener;listener.onLocationChanged(fix(LocationManager.GPS_PROVIDER,20,true));int loads=activity.fullRadarLoads;MainActivity.Danger local=activity.dangers.get(0);
    activity.community=new CommunitySync(activity);activity.community.reports=new org.json.JSONArray().put(new org.json.JSONObject().put("id","12345678-1234-1234-1234-123456789abc").put("lat",46.75).put("lng",5.9).put("kind","danger").put("expires",System.currentTimeMillis()+60000));
    activity.reloadCommunityReports();assertEquals(loads,activity.fullRadarLoads);assertTrue(activity.dangers.contains(local));assertSame(listener,activity.listener);assertEquals("72",activity.gpsSpeedText());assertEquals(1,activity.dangers.stream().filter(d->!d.communityId.isEmpty()).count());
    activity.community.reports=new org.json.JSONArray();activity.reloadCommunityReports();assertEquals(0,activity.dangers.stream().filter(d->!d.communityId.isEmpty()).count());assertTrue(activity.dangers.contains(local));
  }

}
