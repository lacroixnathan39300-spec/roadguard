package fr.roadguard.app;
import android.app.Application;import android.content.Intent;import android.net.Uri;import android.os.Bundle;import android.view.*;import android.widget.*;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.*;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk={27,28},application=Application.class)
public class RadarToolsRuntimeTest {
 public static class Host extends MainActivity {
  boolean connected=true,moving=false;int saved,files,countries,manage,zips,reloads,renders;
  @Override public void onCreate(Bundle b){setTheme(android.R.style.Theme_Material);frame=new FrameLayout(this);setContentView(frame);}
  @Override boolean accountConnected(){return connected;}
  @Override void refreshLastPosition(){}
  @Override void addCustomRadar(){saved++;}
  @Override void openCsv(){files++;}
  @Override void chooseLufopSwissCsvZip(){zips++;}
  @Override void openSimpleRadarCountryPicker(){countries++;}
  @Override void manageCustomRadars(){manage++;}
  @Override void render(){renders++;}
  @Override void requestRadarCatalogReload(boolean force){reloads++;}
  @Override void loadOfficialRadarDatabase(){}
  
 }
 private Host host;
 @Before public void setup(){host=Robolectric.buildActivity(Host.class).create().get();host.getPreferences(0).edit().clear().commit();}
 private View label(View view,String text){if(view instanceof TextView&&text.contentEquals(((TextView)view).getText()))return view;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){View found=label(((ViewGroup)view).getChildAt(i),text);if(found!=null)return found;}return null;}
 private void click(String text){View v=label(host.frame,text);assertNotNull(text,v);while(!v.isClickable()&&v.getParent() instanceof View)v=(View)v.getParent();assertTrue(v.performClick());}
 @Test public void hubDispatchesExistingNativeActions(){RadarToolsUi.show(host);click("Enregistrer un radar ici");click("Télécharger des radars par pays");click("Mes radars enregistrés");click("Importer un ZIP Lufop · Suisse");assertEquals(1,host.saved);assertEquals(1,host.countries);assertEquals(1,host.manage);assertEquals(1,host.zips);}
 @Test public void accountGateIsKept(){host.connected=false;RadarToolsUi.show(host);assertEquals("Compte",host.selected);assertEquals(1,host.renders);assertEquals(0,host.frame.getChildCount());}
 @Test public void csvImportPreservesPersonalPointsAndCountryMetadata()throws Exception{host.getPreferences(0).edit().putString("custom_radars","[{\"lat\":46,\"lon\":6,\"type\":\"Radar fixe\"}]").putString("radar_country_installed","[{\"id\":\"ch\"}]").commit();importCsv("46.75;6.12;Radar fixe\n");assertTrue(host.getPreferences(0).getString("danger_csv","").contains("46.75"));assertTrue(host.getPreferences(0).getString("custom_radars","").contains("46"));assertTrue(host.getPreferences(0).getString("radar_country_installed","").contains("ch"));assertTrue(host.reloads>0);}
 @Test public void emptyImportKeepsPreviousCsv()throws Exception{host.getPreferences(0).edit().putString("danger_csv","46;6;Radar fixe").commit();importCsv("latitude;longitude;type\nnot,a,point");assertEquals("46;6;Radar fixe",host.getPreferences(0).getString("danger_csv",""));assertEquals(0,host.reloads);}
 private void importCsv(String text)throws Exception{java.io.File file=java.io.File.createTempFile("radars-", ".csv",host.getCacheDir());try(java.io.FileOutputStream out=new java.io.FileOutputStream(file)){out.write(text.getBytes("UTF-8"));}host.onActivityResult(44,android.app.Activity.RESULT_OK,new Intent().setData(Uri.fromFile(file)));}
}
