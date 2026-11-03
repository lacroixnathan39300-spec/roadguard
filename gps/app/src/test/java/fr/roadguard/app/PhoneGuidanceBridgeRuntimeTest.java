package fr.roadguard.app;
import android.app.Application;
import android.content.*;
import android.os.*;
import fr.roadguard.ipc.*;
import org.json.JSONObject;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.annotation.Config;
import java.time.Duration;
import java.util.ArrayList;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={26,28,35,36},application=Application.class)
public class PhoneGuidanceBridgeRuntimeTest {
 public static class Host extends PhoneGuidanceBridgeService { boolean approved=true; @Override boolean trustedUid(int uid){return approved;} }
 ServiceController<Host> controller;Host service;SharedPreferences prefs;
 ArrayList<String> seen;
 IGuidanceListener listener;
 String route="{\"lat\":46.8,\"lon\":5.7,\"points\":[[46.7,5.6],[46.8,5.7]]}";
 @Before public void setup() throws Exception {
  controller=Robolectric.buildService(Host.class);service=controller.get();
  prefs=service.getSharedPreferences("MainActivity",0);
  prefs.edit().clear().putBoolean("phone_guidance_share",true).putString("cached_navigation",route)
   .putString("destination","46.8,5.7").putString("destination_label","Maison").putString("installed_map","France").commit();
  controller.create(); seen=new ArrayList<>();
  listener=new IGuidanceListener.Stub(){@Override public void onSnapshot(String raw){seen.add(raw);}};
 }
 @After public void close(){controller.destroy();}
 void idle(){shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200));}
 void publish(String text){new PhoneGuidanceSharing().publish(service,text,true);idle();}
 JSONObject latest()throws Exception{return new JSONObject(seen.get(seen.size()-1));}
 @Test public void subscriptionGetsFullSnapshotAndUpdatesBeforeNotificationThrottle() throws Exception {
  publish("Premier");service.bridge.subscribe(listener);idle();
  assertTrue(latest().getBoolean("active"));assertEquals("Maison",latest().getJSONObject("route").getString("destinationLabel"));
  long first=latest().getLong("sequence");publish("Tournez à droite");
  assertEquals("Tournez à droite",latest().getString("guidance"));assertTrue(latest().getLong("sequence")>first);
 }
 @Test public void revocationClearsRouteImmediatelyAndRejectsStop() throws Exception {
  service.bridge.subscribe(listener);idle();publish("Actif");String id=latest().getJSONObject("route").getString("routeId");
  prefs.edit().putBoolean("phone_guidance_share",false).commit();idle();
  assertFalse(latest().getBoolean("active"));assertTrue(latest().isNull("route"));assertFalse(service.bridge.requestStop(id));
 }
 @Test public void queuedStopRechecksRouteAndKeepsLocalMaps() throws Exception {
  service.bridge.subscribe(listener);idle();publish("Guidage");String id=latest().getJSONObject("route").getString("routeId");
  assertFalse(service.bridge.requestStop("old-route"));assertTrue(service.bridge.requestStop(id));
  prefs.edit().putString("cached_navigation",route+" ").commit();idle();
  assertEquals(route+" ",prefs.getString("cached_navigation",""));
  String current=PhoneRouteGeometry.identity(route+" ","46.8,5.7","Maison");
  assertTrue(service.bridge.requestStop(current));idle();assertFalse(prefs.contains("cached_navigation"));
  assertEquals("France",prefs.getString("installed_map",""));assertFalse(latest().getBoolean("active"));
 }
 @Test public void unapprovedCallerCannotReadOrCommand() throws Exception {
  service.approved=false;
  try {service.bridge.subscribe(listener);fail("unapproved subscriber");} catch(SecurityException expected){}
  try {service.bridge.requestStop("anything");fail("unapproved command");} catch(SecurityException expected){}
  try {service.bridge.navigationVersion();fail("unapproved version");} catch(SecurityException expected){}
  try {service.bridge.navigation("{}",null);fail("unapproved navigation");} catch(SecurityException expected){}
  try {service.bridge.cancelNavigation("scope");fail("unapproved cancellation");} catch(SecurityException expected){}
  try {service.bridge.mapInfo();fail("unapproved map info");} catch(SecurityException expected){}
  try {service.bridge.mapTile("catalog","raster",0,0,0);fail("unapproved tile");} catch(SecurityException expected){}
 }
 @Test public void revokedCertificateStopsExistingSubscription() throws Exception {
  service.bridge.subscribe(listener);idle();publish("Guidage avant révocation");int count=seen.size();
  service.approved=false;publish("Ne doit pas sortir");assertEquals(count,seen.size());
 }
 @Test public void realVerifierRejectsMissingOrUnapprovedPackage() {
  ServiceController<PhoneGuidanceBridgeService> real=Robolectric.buildService(PhoneGuidanceBridgeService.class).create();
  assertFalse(real.get().trustedUid(12345));
  android.content.pm.PackageInfo fake=new android.content.pm.PackageInfo();fake.packageName=PhoneGuidanceBridgeService.COMPANION;
  fake.applicationInfo=new android.content.pm.ApplicationInfo();fake.applicationInfo.packageName=fake.packageName;fake.applicationInfo.uid=12345;
  fake.signatures=new android.content.pm.Signature[]{new android.content.pm.Signature("0011")};
  shadowOf(real.get().getPackageManager()).installPackage(fake);
  assertFalse(real.get().trustedUid(12345));real.destroy();
 }
}
