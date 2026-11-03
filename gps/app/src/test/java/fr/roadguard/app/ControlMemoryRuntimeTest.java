package fr.roadguard.app;

import android.content.Context;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={26,36})
public class ControlMemoryRuntimeTest {
  Context c;long now;
  @Before public void setup(){c=RuntimeEnvironment.getApplication();now=System.currentTimeMillis();c.getSharedPreferences("roadguard_controls",0).edit().clear().commit();new java.io.File(c.getFilesDir(),ControlMemory.FILE).delete();new java.io.File(c.getFilesDir(),ControlMemory.FILE+".bak").delete();}
  ControlMemory.Observation o(String id,long time){return new ControlMemory.Observation("openweb",id,"police",46.75,5.9,time);}
  void put(ControlMemory.Observation... input)throws Exception{ControlMemory.ingest(c,Arrays.asList(input),now);}
  JSONObject place()throws Exception{return ControlMemory.snapshot(c,now).getJSONObject(0);}
  @Test public void thirdDistinctReportMakesPlacePermanentAcrossRestart()throws Exception{
    put(o("first",now-3600000));assertEquals(1,place().getInt("confirmations"));assertFalse(place().getBoolean("permanent"));
    put(o("second",now-1800000));assertEquals(2,place().getInt("confirmations"));assertFalse(place().getBoolean("permanent"));
    put(o("third",now));assertEquals(3,place().getInt("confirmations"));assertTrue(place().getBoolean("permanent"));assertEquals(0,place().getLong("expires"));
    assertEquals(1,ControlMemory.snapshot(c,now+90*ControlMemory.DEFAULT_TTL).length());
    assertTrue(new java.io.File(c.getFilesDir(),ControlMemory.FILE).isFile());assertEquals(3,ControlMemory.read(c).getJSONArray("places").getJSONObject(0).getJSONArray("events").length());
  }
  @Test public void pollingSameReportNeverAddsConfirmationOrExtendsItsLifetime()throws Exception{
    put(o("first",now-3600000));long expiry=place().getLong("expires");for(int i=0;i<20;i++)put(o("first",now-3600000));assertEquals(1,place().getInt("confirmations"));assertEquals(expiry,place().getLong("expires"));
  }
  @Test public void sameWazeUuidFromTwoProvidersCountsOnce()throws Exception{
    String uuid="12345678-abcd-1234-5678-123456789abc";put(o("alert-187/"+uuid,now-3600000));
    put(new ControlMemory.Observation("wazeapi",uuid,"police",46.751,5.901,now-1800000));assertEquals(1,ControlMemory.snapshot(c,now).length());assertEquals(1,place().getInt("confirmations"));
  }
  @Test public void simultaneousNearbyReplacementIdsDoNotInflateVotes()throws Exception{put(o("first",now-90000),o("replacement",now));assertEquals(1,place().getInt("confirmations"));}
  @Test public void nearbyObservationsMergeButDistantControlsStaySeparate()throws Exception{
    put(o("a",now-3600000),new ControlMemory.Observation("openweb","b","police",46.75005,5.90005,now-1800000),new ControlMemory.Observation("openweb","c","police",46.76,5.9,now));assertEquals(2,ControlMemory.snapshot(c,now).length());assertEquals(2,place().getInt("confirmations"));
  }
  @Test public void unconfirmedLocationExpiresAndOldPayloadCannotResurrectIt()throws Exception{
    put(o("a",now-3600000),o("b",now-1800000));long later=now+ControlMemory.DEFAULT_TTL;assertEquals(0,ControlMemory.snapshot(c,later).length());ControlMemory.ingest(c,Collections.singletonList(o("b",now-1800000)),later);assertEquals(0,ControlMemory.snapshot(c,later).length());assertEquals(0,ControlMemory.read(c).getJSONArray("places").length());
  }
  @Test public void expiredVotesDoNotCarryOverToNewControl()throws Exception{
    put(o("a",now-3600000),o("b",now-1800000));long later=now+ControlMemory.DEFAULT_TTL;ControlMemory.ingest(c,Collections.singletonList(o("new",later)),later);assertEquals(1,ControlMemory.snapshot(c,later).getJSONObject(0).getInt("confirmations"));
  }
  @Test public void permanentPlaceBecomesHistoricalWhenLatestReportExpires()throws Exception{
    put(o("a",now-3600000),o("b",now-1800000),o("c",now));JSONObject p=ControlMemory.snapshot(c,now+ControlMemory.DEFAULT_TTL+1).getJSONObject(0);assertTrue(p.getBoolean("permanent"));assertTrue(ControlMemory.label(p,now+ControlMemory.DEFAULT_TTL+1).contains("possible"));
  }
  @Test public void invalidFutureAndUnrelatedRecordsAreRejected()throws Exception{
    put(new ControlMemory.Observation("openweb","bad","police",Double.NaN,5,now),new ControlMemory.Observation("openweb","bad2","accident",46,5,now),o("future",now+60000),o("",now));assertEquals(0,ControlMemory.snapshot(c,now).length());
  }
  @Test public void ttlSettingIsBoundedAndAffectsTemporaryPointsOnly()throws Exception{
    put(o("a",now-3600000));c.getSharedPreferences("roadguard_controls",0).edit().putInt("ttl_hours",168).commit();assertEquals(168*ControlMemory.HOUR,ControlMemory.ttl(c));assertEquals(1,ControlMemory.snapshot(c,now+48*ControlMemory.HOUR).length());c.getSharedPreferences("roadguard_controls",0).edit().putInt("ttl_hours",-1).commit();assertEquals(ControlMemory.DEFAULT_TTL,ControlMemory.ttl(c));
  }
  @Test public void manualConfirmationNeedsProximityAndCooldown()throws Exception{
    put(new ControlMemory.Observation("local","manual-first","mobile",46.75,5.9,now));String id=place().getString("id");
    try{ControlMemory.confirm(c,id,47,6,now+31*60000);fail();}catch(java.io.IOException expected){}
    ControlMemory.confirm(c,id,46.75,5.9,now+10*60000);assertEquals(1,place().getInt("confirmations"));assertEquals(now+10*60000,place().getLong("last"));
    ControlMemory.confirm(c,id,46.75,5.9,now+31*60000);assertEquals(2,place().getInt("confirmations"));
    ControlMemory.confirm(c,id,46.75,5.9,now+62*60000);assertTrue(place().getBoolean("permanent"));
  }
  @Test public void repeatedValidationsExtendTemporaryPointWithoutInflatingCount()throws Exception{
    put(new ControlMemory.Observation("local","first","mobile",46.75,5.9,now));String id=place().getString("id");for(int minute:new int[]{5,10,15,20})ControlMemory.confirm(c,id,46.75,5.9,now+minute*60000L);assertEquals(1,place().getInt("confirmations"));assertEquals(now+20*60000L+ControlMemory.DEFAULT_TTL,place().getLong("expires"));assertEquals(1,ControlMemory.snapshot(c,now+ControlMemory.DEFAULT_TTL+10*60000L).length());
  }
  @Test public void repeatedLocalButtonPressesCannotInstantlyCreatePermanentLocation()throws Exception{
    put(new ControlMemory.Observation("local","one","mobile",46.75,5.9,now));put(new ControlMemory.Observation("local","two","mobile",46.75,5.9,now));assertEquals(1,place().getInt("confirmations"));
  }
  @Test public void localReportRepeatedDuringCooldownStillExtendsItsLifetime()throws Exception{
    put(new ControlMemory.Observation("local","first","mobile",46.75,5.9,now));long later=now+10*60000L;ControlMemory.ingest(c,Collections.singletonList(new ControlMemory.Observation("local","again","mobile",46.75,5.9,later)),later);assertEquals(1,place().getInt("confirmations"));assertEquals(later+ControlMemory.DEFAULT_TTL,place().getLong("expires"));
  }
  @Test public void removalDoesNotTouchOtherLocations()throws Exception{
    put(o("a",now),new ControlMemory.Observation("openweb","b","police",47,6,now));String id=place().getString("id");ControlMemory.remove(c,id);assertEquals(1,ControlMemory.snapshot(c,now).length());assertEquals(47,place().getDouble("lat"),.00001);
  }
  @Test public void removedLocationCannotReturnFromSameDownloadedReports()throws Exception{
    put(o("a",now-3600000),o("b",now-1800000));ControlMemory.remove(c,place().getString("id"));put(o("a",now-3600000),o("other-mirror",now-1800000));assertEquals(0,ControlMemory.snapshot(c,now).length());
    ControlMemory.ingest(c,Collections.singletonList(o("genuinely-new",now+600000)),now+600000);assertEquals(1,ControlMemory.snapshot(c,now+600000).getJSONObject(0).getInt("confirmations"));
  }
  @Test public void expiredTemporaryLocationIsRemovedFromDiskOnRead()throws Exception{
    put(o("a",now-3600000));assertEquals(0,ControlMemory.snapshot(c,now+ControlMemory.DEFAULT_TTL).length());assertEquals(0,ControlMemory.read(c).getJSONArray("places").length());
  }
  @Test public void settingsBridgeOpensNativeMemoryWithoutExposingApiSecret()throws Exception{
    org.robolectric.android.controller.ActivityController<PhoneRuntimeTest.OfflineActivity> controller=Robolectric.buildActivity(PhoneRuntimeTest.OfflineActivity.class).setup();
    android.app.AlertDialog dialog=null;try{PhoneRuntimeTest.OfflineActivity a=controller.get();a.initialLoad.get(15,TimeUnit.SECONDS);Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();a.selected="Paramètres";a.render();ControlFeed.prefs(a).edit().putString("credential","private-test-value").commit();new ReferenceUi.Bridge(a,a.referenceWebView).action("controlMemory","");Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();dialog=(android.app.AlertDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();assertNotNull(dialog);assertTrue(dialog.isShowing());assertEquals("Sources externes · facultatif",dialog.getButton(android.app.AlertDialog.BUTTON_NEUTRAL).getText().toString());assertFalse(ReferenceUi.data(a).toString().contains("private-test-value"));assertFalse(ControlFeed.prefs(a).getBoolean("enabled",false));}finally{if(dialog!=null)dialog.dismiss();controller.pause().stop().destroy();}
  }
  @Test public void concurrentFetchesDoNotLoseConfirmations()throws Exception{
    ExecutorService pool=Executors.newFixedThreadPool(3);try{List<Future<?>> tasks=new ArrayList<>();for(int i=0;i<3;i++){final int n=i;tasks.add(pool.submit(()->{try{ControlMemory.ingest(c,Collections.singletonList(o("event"+n,now-n*1800000)),now);}catch(Exception e){throw new RuntimeException(e);}}));}for(Future<?> f:tasks)f.get(10,TimeUnit.SECONDS);assertTrue(place().getBoolean("permanent"));}finally{pool.shutdownNow();}
  }
  @Test public void corruptedFileIsPreservedInsteadOfOverwritten()throws Exception{
    byte[] bad="{broken".getBytes(StandardCharsets.UTF_8);CommunityFiles.write(c.getFilesDir(),ControlMemory.FILE,bad);try{put(o("one",now));fail();}catch(Exception expected){}assertArrayEquals(bad,CommunityFiles.read(c.getFilesDir(),ControlMemory.FILE,100));
  }
  JSONObject alert(String type,String id,long time)throws Exception{return new JSONObject().put("type",type).put("alert_id",id).put("latitude",46.75).put("longitude",5.9).put("publish_datetime_utc",Instant.ofEpochMilli(time).toString());}
  @Test public void openwebParserOnlyKeepsPoliceAndIgnoresJams()throws Exception{
    JSONObject response=new JSONObject().put("alerts",new JSONArray().put(alert("POLICE","p",now)).put(alert("ACCIDENT","a",now)).put(alert("HAZARD","h",now)).put(alert("ROAD_CLOSED","r",now))).put("jams",new JSONArray().put(new JSONObject()));List<ControlMemory.Observation> list=ControlFeed.parse("openweb",response,now,ControlMemory.DEFAULT_TTL);assertEquals(1,list.size());assertEquals("police",list.get(0).kind);
  }
  @Test public void wazeApiParserUsesOwnCoordinatesAndOriginalTimestamp()throws Exception{
    JSONObject item=new JSONObject().put("type","POLICE").put("id","p").put("location",new JSONObject().put("lat",46.75).put("lng",5.9)).put("reported_at",Instant.ofEpochMilli(now-3600000).toString());List<ControlMemory.Observation> list=ControlFeed.parse("wazeapi",new JSONObject().put("alerts",new JSONArray().put(item)),now,ControlMemory.DEFAULT_TTL);assertEquals(1,list.size());assertEquals(now-3600000,list.get(0).at);
  }
  @Test public void malformedAndStaleAlertsNeverBecomeFreshOnDownload()throws Exception{
    JSONObject response=new JSONObject().put("data",new JSONObject().put("alerts",new JSONArray().put(alert("POLICE","a",now-2*ControlMemory.DEFAULT_TTL)).put(alert("POLICE","b",now).put("publish_datetime_utc","invalid")).put(alert("POLICE","good",now))));assertEquals(1,ControlFeed.parse("openweb",response,now,ControlMemory.DEFAULT_TTL).size());
  }
  @Test public void requestHostsAreFixedAndUnknownProvidersRejected()throws Exception{
    assertTrue(ControlFeed.url("openweb",46.75,5.9).startsWith("https://api.openwebninja.com/waze/alerts-and-jams?"));assertTrue(ControlFeed.url("wazeapi",46.75,5.9).startsWith("https://api.wazeapi.com/v1/alerts?"));try{ControlFeed.url("https://evil.example",46,5);fail();}catch(IllegalArgumentException expected){}assertFalse(ControlFeed.configured(c));
  }
  @Test public void choosingSameProviderPreservesStoredCredentialAndOptIn(){
    ControlFeed.prefs(c).edit().putString("provider","openweb").putString("credential","encrypted-placeholder").putString("credential_provider","openweb").putBoolean("enabled",true).commit();assertFalse(ControlFeed.selectProvider(c,"openweb"));assertTrue(ControlFeed.configured(c));assertTrue(ControlFeed.prefs(c).getBoolean("enabled",false));
  }
  @Test public void switchingProviderCannotReuseOtherProvidersCredential(){
    ControlFeed.prefs(c).edit().putString("provider","openweb").putString("credential","encrypted-placeholder").putString("credential_provider","openweb").putBoolean("enabled",true).putLong("last_success",123).commit();assertTrue(ControlFeed.selectProvider(c,"wazeapi"));assertFalse(ControlFeed.configured(c));assertFalse(ControlFeed.prefs(c).getBoolean("enabled",true));assertEquals(0,ControlFeed.prefs(c).getLong("last_success",0));
  }
  @Test public void sharedPointsAreVisibleInPhoneAndCarDataWithExpiry()throws Exception{
    put(o("a",now));List<MainActivity.Danger> phone=new ArrayList<>();ControlMemory.append(c,phone);assertEquals(1,phone.size());assertFalse(phone.get(0).controlId.isEmpty());assertEquals(now+ControlMemory.DEFAULT_TTL,phone.get(0).expires);CarRouteData car=new CarRouteData(c);car.loadRadars();assertTrue(car.radars.stream().anyMatch(p->p.type.equals("Contrôle mobile signalé")&&p.expires==now+ControlMemory.DEFAULT_TTL));int count=car.radars.size();car.loadRadars();assertEquals(count,car.radars.size());
  }
}
