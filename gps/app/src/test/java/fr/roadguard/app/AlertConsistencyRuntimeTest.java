package fr.roadguard.app;

import android.app.Application;
import android.location.Location;
import android.os.*;
import java.io.*;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={26,36})
@LooperMode(LooperMode.Mode.PAUSED)
public class AlertConsistencyRuntimeTest {
  Application app;
  @Before public void setup(){app=RuntimeEnvironment.getApplication();app.getSharedPreferences("roadguard_alert_milestones",0).edit().clear().commit();}
  Location fix(){Location fix=new Location("gps");fix.setLatitude(46.75);fix.setLongitude(5.9);fix.setAccuracy(5);fix.setTime(100000);fix.setElapsedRealtimeNanos(100000000000L);return fix;}
  @Test public void staleFutureAndImpreciseFixesCannotProduceWarnings(){
    Location f=fix();assertTrue(AlertPresentation.fresh(f,105000,105000000000L));
    assertFalse(AlertPresentation.fresh(f,116000,116000000000L));
    assertFalse(AlertPresentation.fresh(f,94000,105000000000L));
    assertFalse(AlertPresentation.fresh(f,105000,116000000000L));
    assertFalse(AlertPresentation.fresh(f,105000,99000000000L));
    f.setAccuracy(Float.NaN);assertFalse(AlertPresentation.fresh(f,105000,105000000000L));
    f.setAccuracy(61);assertFalse(AlertPresentation.fresh(f,105000,105000000000L));
  }
  @Test public void displayedAndSpokenDistancesUseTheSameValue(){
    for(double meters:new double[]{0,12,73,99,249,499,999,1249,3999,4999}){
      int value=AlertPresentation.roundedMeters(meters);
      assertEquals(value+" mètres",AlertPresentation.voice(meters));
      assertEquals(value>=1000?String.format(Locale.FRANCE,"%.2f km",value/1000d):value+" m",AlertPresentation.text(meters));
    }
    assertEquals("—",AlertPresentation.text(Double.NaN));assertEquals(-1,AlertPresentation.roundedMeters(-1));
  }
  @Test public void differentTypesAtSamePointHaveIndependentSharedMilestones(){
    assertTrue(AlertMilestones.claim(app,46.75,5.9,"Radar fixe",6));
    assertFalse(AlertMilestones.claim(app,46.75,5.9,"ETF",6));
    assertTrue(AlertMilestones.claim(app,46.75,5.9,"Radar feu rouge",6));
    assertFalse(AlertMilestones.pending(app,46.75,5.9,"red light",6));
    assertEquals(0,AlertMilestones.stage(true,5001));assertEquals(0,AlertMilestones.stage(false,701));
  }
  @Test public void redLightsCrossingsAndUnknownLimitsNeverProveOverspeed(){
    for(String type:new String[]{"Radar feu rouge · source","red light camera","ETFR"," ETFR ","red-light camera","Radar passage à niveau","level crossing","ETPN"})
      assertEquals(type,0,RadarAlertPolicy.applicableLimit(type,90));
    assertEquals(70,RadarAlertPolicy.applicableLimit("Radar fixe",70));
    assertEquals(0,RadarAlertPolicy.applicableLimit("Radar fixe",300));
    assertTrue(RadarAlertPolicy.displayType("Radar chantier").contains("présence à confirmer"));
  }
  @Test public void warningHysteresisHidesStaleUiAndAllowsCloseApproaches()throws Exception{
    assertFalse(AlertPresentation.nearby(1001,true,false));assertTrue(AlertPresentation.nearby(1030,true,true));
    assertFalse(AlertPresentation.nearby(1051,true,true));
    org.robolectric.android.controller.ActivityController<PhoneRuntimeTest.OfflineActivity> c=Robolectric.buildActivity(PhoneRuntimeTest.OfflineActivity.class).setup();
    try{PhoneRuntimeTest.OfflineActivity a=c.get();a.initialLoad.get(20,java.util.concurrent.TimeUnit.SECONDS);Shadows.shadowOf(Looper.getMainLooper()).idle();
      a.dangers.clear();a.dangers.add(new MainActivity.Danger(46.7535,5.9,"Radar fixe",70));a.rebuildDangerSpatialIndex();
      a.previousAcceptedGps=fix();a.previousAcceptedGps.setTime(System.currentTimeMillis());a.previousAcceptedGps.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
      a.lastLat=46.75;a.lastLon=5.9;a.lastAccuracy=5;a.reliableBearing=false;a.getPreferences(0).edit().putBoolean("forward_dangers",false).commit();a.dangerBanner=new android.widget.TextView(a);
      a.checkDangers();assertEquals(android.view.View.VISIBLE,a.dangerBanner.getVisibility());assertNotNull(a.activeRadarUi);
      a.previousAcceptedGps.setTime(System.currentTimeMillis()-16000);a.checkDangers();
      assertEquals(android.view.View.GONE,a.dangerBanner.getVisibility());assertNull(a.activeRadarUi);assertFalse(a.radarHaloVisible);
      c.pause().stop();
    }finally{c.destroy();}
  }
  @Test public void partialAuthorizedUpdateCannotRemoveAnOriginalOrAnotherType()throws Exception{
    List<BundledRadarCatalog.Point> originals=BundledRadarCatalog.france(app.getAssets()::open);
    File dir=UpdatedRadarCatalog.directory(app);dir.mkdirs();File update=new File(dir,"authorized_radars_fr.csv");
    String header="id;type;lat;lon;country;category;speed_kmh;direction_deg;status;sources;source_ids;speed_conflicts\n";
    java.util.Set<String> redPositions=new java.util.HashSet<>();for(BundledRadarCatalog.Point point:originals)if(RadarAlertPolicy.isRedLight(point.type))redPositions.add(AlertMilestones.pointKey(point.lat,point.lon));
    BundledRadarCatalog.Point first=originals.stream().filter(p->!RadarAlertPolicy.isRedLight(p.type)&&!redPositions.contains(AlertMilestones.pointKey(p.lat,p.lon))).findFirst().get();
    try{
      try(Writer out=new OutputStreamWriter(new FileOutputStream(update),"UTF-8")){out.write(header+"rg-01234567890123456789;Radar feu rouge;"+first.lat+";"+first.lon+";FR;redlight;0;;inventory;test;test:1;\n");}
      List<BundledRadarCatalog.Point> active=UpdatedRadarCatalog.active(app,true,true);
      Set<String> ids=new HashSet<>();for(BundledRadarCatalog.Point p:active)ids.add(p.id);
      for(BundledRadarCatalog.Point p:originals)assertTrue("original retained "+p.id,ids.contains(p.id));
      assertTrue(ids.contains("rg-01234567890123456789"));
      try(Writer out=new FileWriter(update)){out.write("corrupt");}
      active=UpdatedRadarCatalog.active(app,true,true);assertTrue(active.size()>=originals.size());
    }finally{update.delete();}
  }
  @Test public void phoneAndCarReadTheSamePackagedCountryCatalog()throws Exception{
    CarRouteData car=new CarRouteData(app);car.loadRadars();
    List<BundledRadarCatalog.Point> active=UpdatedRadarCatalog.active(app,car.prefs.getBoolean(BundledRadarCatalog.HISTORICAL_PREFERENCE,false),car.prefs.getBoolean(BundledRadarCatalog.SWISS_HISTORICAL_PREFERENCE,true));
    assertEquals(active.size(),car.radars.size());
    for(String cc:new String[]{"FR","CH","DE","BE","ES","IT","LU"})assertTrue(cc,active.stream().anyMatch(p->cc.equals(p.country)));
  }
  @Test public void onlyAuditedCountryArchivesAreOfferedAndWrongBytesAreRejected()throws Exception{
    for(String cc:new String[]{"FR","CH","DE","BE","ES","IT","LU"}){RadarDownloadSource s=RadarDownloadSource.find(cc);assertNotNull(s);assertTrue(RadarDownloadSource.allowed(new java.net.URL(s.url)));}
    assertNull(RadarDownloadSource.find("AL"));assertFalse(RadarDownloadSource.allowed(new java.net.URL("https://example.com/file")));
    File bad=new File(app.getCacheDir(),"bad.7z");try{try(FileOutputStream out=new FileOutputStream(bad)){out.write(new byte[32]);}assertFalse(RadarDownloadSource.find("FR").verify(bad));}finally{bad.delete();}
  }
}
