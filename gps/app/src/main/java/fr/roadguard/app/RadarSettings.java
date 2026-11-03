package fr.roadguard.app;
import android.content.Context;
import android.content.SharedPreferences;
final class RadarSettings {
  static void restoreFullFrance(Context c){
    SharedPreferences p=c.getSharedPreferences("MainActivity",0);
    if(!p.getBoolean("france_full_restored_179",false))p.edit()
      .putBoolean(BundledRadarCatalog.HISTORICAL_PREFERENCE,true)
      .putBoolean("france_full_restored_179",true).apply();
  }
  private RadarSettings(){}
}
