package fr.roadguard.app;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.SystemClock;
import androidx.car.app.navigation.NavigationManager;
import androidx.car.app.navigation.model.NavigationTemplate;
import androidx.car.app.navigation.model.RoutingInfo;
import androidx.car.app.model.MessageTemplate;
import androidx.car.app.testing.TestCarContext;
import androidx.car.app.testing.navigation.TestNavigationManager;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.shadows.ShadowSystemClock;
import java.time.Duration;
import java.io.File;
import java.util.concurrent.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={26,28,34,36})
@LooperMode(LooperMode.Mode.PAUSED)
public class AndroidRuntimeTest {
  Context app;SharedPreferences prefs;RoadguardCarScreen screen;
  @Before public void setup(){app=RuntimeEnvironment.getApplication();prefs=app.getSharedPreferences("MainActivity",0);prefs.edit().clear().commit();}
  @After public void teardown(){if(screen!=null)screen.close();}
  TestCarContext car(boolean permission){
    if(permission)Shadows.shadowOf((Application)app).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION);
    else Shadows.shadowOf((Application)app).denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION);
    return TestCarContext.createCarContext(app);
  }
  Location fix(double lat,double lon,double speed){Location l=new Location(LocationManager.GPS_PROVIDER);l.setLatitude(lat);l.setLongitude(lon);l.setAccuracy(5);l.setTime(System.currentTimeMillis());l.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());l.setSpeed((float)speed);l.setSpeedAccuracyMetersPerSecond(1);l.setBearing(0);l.setBearingAccuracyDegrees(3);return l;}
  JSONObject route()throws Exception{
    return new JSONObject().put("lat",46.76).put("lon",5.9).put("meters",1112).put("seconds",120)
      .put("points",new JSONArray("[[46.75,5.9],[46.755,5.9],[46.76,5.9]]"))
      .put("steps",new JSONArray("[{\"lat\":46.75,\"lon\":5.9,\"text\":\"Départ\",\"index\":0},{\"lat\":46.755,\"lon\":5.9,\"text\":\"Continuer\",\"index\":1},{\"lat\":46.76,\"lon\":5.9,\"text\":\"Arrivée à destination\",\"index\":2}]"));
  }
  void saveRoute()throws Exception{prefs.edit().putString("destination","46.76,5.9").putString("destination_label","Destination de test").putString("cached_navigation",route().toString()).commit();}
  void prepareNavigation(TestCarContext context)throws Exception{
    saveRoute();screen=new RoadguardCarScreen(context);screen.main.removeCallbacks(screen.pulse);screen.hostReady=true;
    screen.data=new CarRouteData(app);screen.data.loadRoute();screen.data.radars.clear();
    screen.fix=fix(46.7501,5.9,12);screen.motion.accept(SystemClock.elapsedRealtime(),true,5,12,1,0,3);screen.updateRoute();
  }
  @Test public void permissionScreenBuildsWithActualCarLibrary(){screen=new RoadguardCarScreen(car(false));assertTrue(screen.onGetTemplate() instanceof MessageTemplate);}
  @Test public void navigationTemplateAndClusterTripAreValid()throws Exception{
    TestCarContext context=car(true);prepareNavigation(context);
    NavigationTemplate template=(NavigationTemplate)screen.onGetTemplate();assertTrue(template.getNavigationInfo() instanceof RoutingInfo);
    TestNavigationManager nav=(TestNavigationManager)context.getCarService(NavigationManager.class);
    assertEquals(1,nav.getNavigationStartedCount());assertEquals(1,nav.getTripsSent().size());assertTrue(screen.position.remaining>1000);assertNotNull(template.getDestinationTravelEstimate());
  }
  @Test public void stopCannotRestartFromLateSnapshot()throws Exception{
    TestCarContext context=car(true);prepareNavigation(context);CarRouteData late=new CarRouteData(app);late.loadRoute();
    screen.stopNavigation();screen.data=late;screen.updateRoute();
    TestNavigationManager nav=(TestNavigationManager)context.getCarService(NavigationManager.class);
    assertFalse(screen.navigating);assertNull(screen.currentStep);assertEquals(1,nav.getNavigationStartedCount());assertEquals(1,nav.getNavigationEndedCount());assertFalse(prefs.contains("cached_navigation"));
  }
  @Test public void arrivalEndsNavigationOnce()throws Exception{
    TestCarContext context=car(true);prepareNavigation(context);screen.fix=fix(46.76,5.9,0);screen.updateRoute();screen.updateRoute();
    TestNavigationManager nav=(TestNavigationManager)context.getCarService(NavigationManager.class);
    assertEquals(1,nav.getNavigationEndedCount());assertFalse(screen.navigating);assertNull(screen.currentStep);
  }
  @Test public void staleFixDoesNotPublishNewTrip()throws Exception{
    TestCarContext context=car(true);prepareNavigation(context);screen.fix.setTime(System.currentTimeMillis()-31000);screen.updateRoute();
    assertEquals(2,((TestNavigationManager)context.getCarService(NavigationManager.class)).getTripsSent().size());assertTrue(((TestNavigationManager)context.getCarService(NavigationManager.class)).getTripsSent().get(1).isLoading());assertNull(screen.currentStep);
    assertFalse(((NavigationTemplate)screen.onGetTemplate()).getNavigationInfo() instanceof RoutingInfo);
  }
  @Test public void corruptRouteNeverLeavesPartialPoints()throws Exception{
    saveRoute();CarRouteData data=new CarRouteData(app);data.loadRoute();assertEquals(3,data.route.size());
    JSONObject broken=route();broken.put("points",new JSONArray("[[46.75,5.9],[46.755,5.9],[999,5.9]]"));prefs.edit().putString("cached_navigation",broken.toString()).commit();data.loadRoute();assertEquals(0,data.route.size());
  }
  @Test public void routeForAnotherDestinationIsRejected()throws Exception{saveRoute();prefs.edit().putString("destination","47.0,6.0").commit();CarRouteData data=new CarRouteData(app);data.loadRoute();assertEquals(0,data.route.size());}
  @Test public void fullFranceMigrationRunsOnlyOnce(){RadarSettings.restoreFullFrance(app);assertTrue(prefs.getBoolean(BundledRadarCatalog.HISTORICAL_PREFERENCE,false));prefs.edit().putBoolean(BundledRadarCatalog.HISTORICAL_PREFERENCE,false).commit();RadarSettings.restoreFullFrance(app);assertFalse(prefs.getBoolean(BundledRadarCatalog.HISTORICAL_PREFERENCE,true));}
  @Test public void closeReleasesCarResourcesEvenAfterPermissionRevocation()throws Exception{
    TestCarContext context=car(true);prepareNavigation(context);Shadows.shadowOf((Application)app).denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION);
    screen.close();screen.close();assertTrue(screen.closed);assertTrue(screen.map.closed);assertTrue(screen.io.isShutdown());assertNull(screen.listener);
    assertEquals(1,((TestNavigationManager)context.getCarService(NavigationManager.class)).getNavigationEndedCount());
  }
  @Test public void concurrentCommunityCacheReadsStayComplete()throws Exception{
    File dir=new File(app.getFilesDir(),"atomic-test");dir.mkdirs();CommunityFiles.write(dir,"reports.json","{\"v\":0}".getBytes("UTF-8"));
    ExecutorService pool=Executors.newFixedThreadPool(4);java.util.List<Future<?>> futures=new java.util.ArrayList<>();
    for(int t=0;t<4;t++){final int thread=t;futures.add(pool.submit(()->{try{for(int i=0;i<30;i++){CommunityFiles.write(dir,"reports.json",new JSONObject().put("v",thread*100+i).toString().getBytes("UTF-8"));assertTrue(new JSONObject(new String(CommunityFiles.read(dir,"reports.json",1024),"UTF-8")).has("v"));}}catch(Exception e){throw new RuntimeException(e);}}));}
    try{for(Future<?> f:futures)f.get(10,TimeUnit.SECONDS);}finally{pool.shutdownNow();}
  }
  @Test public void everyFavoriteIsReachableAndTemplatesStayValid()throws Exception{
    JSONArray all=new JSONArray();for(int i=0;i<30;i++)all.put(new JSONObject().put("name","Lieu "+i).put("lat",46+i*.001).put("lon",5.9));prefs.edit().putString("destination_favorites",all.toString()).commit();
    CarFavoritesScreen favorites=new CarFavoritesScreen(car(true),(lat,lon,label)->{});
    for(int page=0;page<5;page++){favorites.page=page;androidx.car.app.model.ListTemplate template=(androidx.car.app.model.ListTemplate)favorites.onGetTemplate();assertEquals(6,template.getSingleList().getItems().size());assertEquals("Lieu "+(page*6+5),((androidx.car.app.model.Row)template.getSingleList().getItems().get(5)).getTitle().toString());assertNotNull(template.getActionStrip());}
  }
  @Test public void guidanceNotReadyIsRetriedInsteadOfMarkedSpoken()throws Exception{
    TestCarContext context=car(true);prepareNavigation(context);screen.voiceReady=false;screen.spoken=-1;screen.fix=fix(46.754,5.9,12);screen.updateRoute();assertEquals(-1,screen.spoken);
  }
  @Test public void offRouteStopsPublishingMisleadingTurnInstructions()throws Exception{
    TestCarContext context=car(true);prepareNavigation(context);screen.fix=fix(46.751,5.91,12);screen.updateRoute();assertNull(screen.currentStep);assertTrue(screen.navigating);assertTrue(((TestNavigationManager)context.getCarService(NavigationManager.class)).getTripsSent().get(1).isLoading());
  }
  @Test public void rerouteNeedsDistinctReliableFixesAndCooldown(){
    CarReroutePolicy p=new CarReroutePolicy();assertFalse(p.due(1000,1000,250,5,15,true,false));assertFalse(p.due(1000,1001,250,5,15,true,false));assertFalse(p.due(5000,5000,250,5,15,true,false));assertTrue(p.due(9000,9000,250,5,15,true,false));
    for(long t=10000;t<99000;t+=1000)assertFalse(p.due(t,t,250,5,15,true,false));assertTrue(p.due(99000,99000,250,5,15,true,false));assertFalse(p.due(200000,200000,300,90,15,true,false));
  }
  @Test public void clusterManeuversContainDirectionAndArrival(){
    assertEquals(androidx.car.app.navigation.model.Maneuver.TYPE_TURN_NORMAL_LEFT,CarManeuvers.from("Tourner à gauche").getType());assertEquals(androidx.car.app.navigation.model.Maneuver.TYPE_TURN_NORMAL_RIGHT,CarManeuvers.from("Tourner à droite").getType());assertEquals(androidx.car.app.navigation.model.Maneuver.TYPE_DESTINATION,CarManeuvers.from("Arrivée à destination").getType());assertNotNull(CarManeuvers.from("Au rond-point"));assertEquals(3,CarManeuvers.from("Au rond-point, prendre la sortie 3").getRoundaboutExitNumber());
  }

  @Test public void revokingGpsPermissionReleasesCarLocationSubscription()throws Exception{
    TestCarContext context=car(true);prepareNavigation(context);
    Shadows.shadowOf((Application)app).denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION);
    screen.startGps();assertNull(screen.listener);assertNull(screen.fix);assertTrue(screen.onGetTemplate() instanceof MessageTemplate);
  }
  @Test public void reconnectingCarSessionRestoresSavedRoute()throws Exception{
    TestCarContext context=car(true);prepareNavigation(context);screen.close();
    assertTrue(prefs.contains("cached_navigation"));prepareNavigation(car(true));assertTrue(screen.navigating);assertNotNull(screen.currentStep);
  }

  @Test public void hostTrustListRemainsBoundToKnownPackageAndCertificates(){
    org.robolectric.android.controller.ServiceController<RoadguardCarAppService> service=Robolectric.buildService(RoadguardCarAppService.class).create();
    try{java.util.Map<String,java.util.List<String>> allowed=service.get().createHostValidator().getAllowedHosts();assertEquals(2,allowed.size());assertEquals(3,allowed.get("com.google.android.projection.gearhead").size());assertEquals(3,allowed.get("com.google.android.apps.automotive.templates.host").size());assertFalse(allowed.containsKey("unknown.host"));assertFalse(service.get().createHostValidator().isValidHost(new androidx.car.app.HostInfo("unknown.host",43210)));}finally{service.destroy();}
  }

}
