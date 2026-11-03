package fr.roadguard.app;

import android.content.Context;
import org.robolectric.RuntimeEnvironment;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import static org.junit.Assert.*;

/** Requires the Android/Robolectric suite; not covered by the standalone JVM adapters. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
public class InstalledRadarCatalogRuntimeTest {
 Context context;File supplement;
 @Before public void setup()throws Exception{
  context=RuntimeEnvironment.getApplication();
  supplement=new File(new File(context.getFilesDir(),"radar_countries"),"merge_test_fr.csv");supplement.getParentFile().mkdirs();
 }
 @After public void cleanup(){supplement.delete();context.getSharedPreferences("MainActivity",0).edit().remove("radar_country_installed").commit();}
 void install(String rows)throws Exception{
  Files.write(supplement.toPath(),("id;type;lat;lon\n"+rows).getBytes(StandardCharsets.UTF_8));
  context.getSharedPreferences("MainActivity",0).edit().putString("radar_country_installed",new JSONArray().put(new JSONObject().put("id","merge_test_fr").put("country","FR")).toString()).commit();
 }
 @Test public void sharedSelectionDeduplicatesSameTypeAndPreservesDifferentTypes()throws Exception{
  install("a;Radar fixe;0.12345;-150.45678\nb;ETF;0.12346;-150.45678\nc;Radar feu rouge;0.12345;-150.45678\n");
  List<BundledRadarCatalog.Point> active=InstalledRadarCatalog.active(context,true,true,Collections.emptyList());
  long fixed=active.stream().filter(p->p.id.startsWith("legacy-merge_test_fr")&&RadarIdentity.type(p.type).equals("fixed")).count();
  long red=active.stream().filter(p->p.id.startsWith("legacy-merge_test_fr")&&RadarIdentity.type(p.type).equals("redlight")).count();
  assertEquals(1,fixed);assertEquals(1,red);
  for(BundledRadarCatalog.Point original:UpdatedRadarCatalog.active(context,true,true))assertTrue(original.id,active.stream().anyMatch(p->p.id.equals(original.id)));
 }
 @Test public void carSnapshotUsesSameSelectionAndRecordsItsRevision()throws Exception{
  install("a;Radar fixe;0.12345;-150.45678\n");
  CarRouteData data=new CarRouteData(context);assertTrue(data.loadRadars());
  assertEquals(RadarCatalogRevision.current(context),data.catalogRevision);
  assertTrue(data.catalog.stream().anyMatch(p->p.id.startsWith("legacy-merge_test_fr")));
  assertTrue(data.radars.stream().anyMatch(p->Math.abs(p.lat-.12345)<.000001&&Math.abs(p.lon+150.45678)<.000001));
 }
 @Test public void malformedIndexKeepsPackagedCatalog()throws Exception{
  context.getSharedPreferences("MainActivity",0).edit().putString("radar_country_installed","invalid").commit();
  List<BundledRadarCatalog.Point> expected=UpdatedRadarCatalog.active(context,true,true);
  assertEquals(expected.size(),InstalledRadarCatalog.active(context,true,true,Collections.emptyList()).size());
 }
}
