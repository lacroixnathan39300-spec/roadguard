package fr.roadguard.app;

import android.content.Context;
import org.json.*;
import java.io.*;
import java.util.*;

/** Identical installed-country selection for phone, floating monitoring and car snapshots. */
final class InstalledRadarCatalog {
  private InstalledRadarCatalog(){}
  static List<BundledRadarCatalog.Point> active(Context context,boolean historical,boolean swissHistorical,
      List<BundledRadarCatalog.Point> extra)throws IOException{
    List<BundledRadarCatalog.Point> base=UpdatedRadarCatalog.active(context,historical,swissHistorical);
    ArrayList<RadarSupplementCatalog.Source> sources=new ArrayList<>();
    try{
      String saved=context.getSharedPreferences("MainActivity",Context.MODE_PRIVATE).getString("radar_country_installed","[]");
      if(saved.length()>200000)throw new IOException("Index des surcouches trop volumineux");
      JSONArray installed=new JSONArray(saved);
      for(int i=0;i<installed.length()&&i<200;i++){
        JSONObject item=installed.optJSONObject(i);if(item==null)continue;String id=item.optString("id");
        if(id.matches("[a-z0-9_-]{2,50}"))sources.add(new RadarSupplementCatalog.Source(id,item.optString("country"),new File(new File(context.getFilesDir(),"radar_countries"),id+".csv")));
      }
    }catch(Exception invalid){android.util.Log.w("ROADGUARD_RADARS","Index de surcouches anciennes indisponible ; bases embarquées conservées",invalid);}
    return RadarSupplementCatalog.merge(base,sources,extra,swissHistorical);
  }
}
