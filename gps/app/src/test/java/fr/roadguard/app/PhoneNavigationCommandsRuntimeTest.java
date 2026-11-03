package fr.roadguard.app;
import android.app.Application;
import android.content.*;
import android.location.Location;
import android.os.*;
import fr.roadguard.ipc.*;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={26,28,35},application=Application.class)
public class PhoneNavigationCommandsRuntimeTest {
 Context app;SharedPreferences prefs;PhoneNavigationCommands commands;ArrayList<JSONObject> results;
 INavigationListener listener;
 @Before public void setup(){app=RuntimeEnvironment.getApplication();prefs=app.getSharedPreferences("MainActivity",0);prefs.edit().clear().putBoolean("phone_guidance_share",true).putString("destination","old").putString("cached_navigation","old-route").commit();app.getSharedPreferences("roadguard_community",0).edit().putBoolean("approved",true).commit();
  commands=new PhoneNavigationCommands(app){@Override Location fix(){Location l=new Location("gps");l.setLatitude(46);l.setLongitude(6);return l;}@Override boolean guidanceAvailable(){return true;}};
  results=new ArrayList<>();listener=new INavigationListener.Stub(){public void onResult(String raw){try{results.add(new JSONObject(raw));}catch(Exception e){throw new AssertionError(e);}}};
 }
 @After public void close(){commands.close();}
 JSONObject request(String kind)throws Exception{return new JSONObject().put("id",UUID.randomUUID().toString()).put("scope","test-scope").put("kind",kind);}
 JSONObject start()throws Exception{return request("start").put("lat",47).put("lng",7).put("label","Maison");}
 JSONObject route()throws Exception{return new JSONObject("{\"lat\":47,\"lon\":7,\"meters\":10000,\"seconds\":100,\"points\":[[46,6],[47,7]],\"steps\":[]}");}
 void waitForResult()throws Exception{for(int i=0;i<200&&results.isEmpty();i++){shadowOf(Looper.getMainLooper()).idle();Thread.sleep(5);}assertFalse(results.isEmpty());}
 String status()throws Exception{return results.get(results.size()-1).getString("status");}
 @Test public void offlineSearchAcceptsCoordinatesButMakesNoAddressRequest()throws Exception{
  prefs.edit().putString("map_connection_mode","offline").commit();commands.places=q->{throw new AssertionError("offline network");};
  commands.submit(request("search").put("query","46.5, 6.5").toString(),listener,()->true);assertEquals("ok",status());
  assertEquals(46.5,results.get(0).getJSONArray("places").getJSONObject(0).getDouble("lat"),0);
  commands.submit(request("search").put("query","Ney").toString(),listener,()->true);assertEquals("offline",status());
 }
 @Test public void favoritesAreBoundedDeduplicatedAndInvalidCoordinatesDropped()throws Exception{
  JSONArray a=new JSONArray();for(int i=0;i<60;i++)a.put(new JSONObject().put("lat",i).put("lon",i).put("name","Lieu"));a.put(new JSONObject().put("lat",999).put("lon",0));
  prefs.edit().putString("destination_favorites",a.toString()).commit();commands.submit(request("favorites").toString(),listener,()->true);assertEquals(30,results.get(0).getJSONArray("places").length());
  JSONArray duplicated=new JSONArray("[{\"lat\":1,\"lon\":2,\"name\":\"Maison\"},{\"lat\":1,\"lng\":2,\"name\":\"Même\"},{\"lat\":100,\"lon\":2}]");assertEquals(1,PhoneNavigationCommands.validPlaces(duplicated,"Favori").length());
 }
 @Test public void malformedAndDuplicateRequestsCannotStartTwoRoutes()throws Exception{
  JSONObject r=request("favorites");commands.submit(r.toString(),listener,()->true);commands.submit(r.toString(),listener,()->true);assertEquals("duplicate",status());
  commands.submit(start().put("lat",100).toString(),listener,()->true);assertEquals("invalid",status());assertEquals("old-route",prefs.getString("cached_navigation",""));
 }
 @Test public void consentAndCallerRevocationRefuseReadAndStart()throws Exception{
  commands.submit(request("favorites").toString(),listener,()->false);assertEquals("unavailable",status());prefs.edit().putBoolean("phone_guidance_share",false).commit();commands.submit(start().toString(),listener,()->true);assertEquals("unavailable",status());
 }
 @Test public void successfulCalculationPublishesExactCommittedRouteIdentity()throws Exception{
  commands.calculate=(l,a,b)->route();commands.submit(start().toString(),listener,()->true);waitForResult();assertEquals("started",status());
  assertEquals("47.0000000,7.0000000",prefs.getString("destination",""));assertEquals(1,prefs.getLong("car_route_revision",0));
  String identity=PhoneRouteGeometry.identity(prefs.getString("cached_navigation",""),prefs.getString("destination",""),"Maison");assertEquals(identity,results.get(0).getString("routeId"));
 }
 @Test public void failedCalculationPreservesPreviousRouteAndDestination()throws Exception{
  commands.calculate=(l,a,b)->{throw new java.io.IOException();};commands.submit(start().toString(),listener,()->true);waitForResult();assertEquals("network_error",status());assertEquals("old-route",prefs.getString("cached_navigation",""));assertEquals("old",prefs.getString("destination",""));
 }
 void race(String change)throws Exception{
  CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);commands.calculate=(l,a,b)->{entered.countDown();release.await(2,TimeUnit.SECONDS);return route();};
  commands.submit(start().toString(),listener,()->true);assertTrue(entered.await(2,TimeUnit.SECONDS));
  if(change.equals("cancel"))commands.cancel("test-scope");else if(change.equals("consent"))prefs.edit().putBoolean("phone_guidance_share",false).commit();else if(change.equals("reenable")){commands.revoke();prefs.edit().putBoolean("phone_guidance_share",false).commit();prefs.edit().putBoolean("phone_guidance_share",true).commit();}else if(change.equals("timeout")){shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(26));}else prefs.edit().putString("cached_navigation","new-local-route").commit();
  release.countDown();commands.io.shutdown();assertTrue(commands.io.awaitTermination(2,TimeUnit.SECONDS));shadowOf(Looper.getMainLooper()).idle();
 }
 @Test public void linkScopeCancelledDuringCalculationCannotCommit()throws Exception{race("cancel");assertTrue(results.isEmpty());assertEquals("old-route",prefs.getString("cached_navigation",""));}
 @Test public void consentRevokedDuringCalculationCannotCommit()throws Exception{race("consent");assertTrue(results.isEmpty());assertEquals("old-route",prefs.getString("cached_navigation",""));}
 @Test public void revocationThenReenableDoesNotResurrectCalculation()throws Exception{race("reenable");assertTrue(results.isEmpty());assertEquals("old-route",prefs.getString("cached_navigation",""));}
 @Test public void expiredCalculationCannotReplaceRoute()throws Exception{race("timeout");assertTrue(results.isEmpty());assertEquals("old-route",prefs.getString("cached_navigation",""));}
 @Test public void newerPhoneRouteWinsAgainstLateRemoteCalculation()throws Exception{race("route");assertEquals("route_changed",status());assertEquals("new-local-route",prefs.getString("cached_navigation",""));}
 @Test public void offlineStartPreservesPreviousRoute()throws Exception{prefs.edit().putString("map_connection_mode","offline").commit();commands.submit(start().toString(),listener,()->true);assertEquals("offline",status());assertEquals("old-route",prefs.getString("cached_navigation",""));}
}
