package fr.roadguard.app;

import android.content.Context;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/** Immutable packaged inventories plus validated additive overlays. No update can remove an original. */
final class UpdatedRadarCatalog {
  private UpdatedRadarCatalog() {}
  static File directory(Context context) { return new File(context.getFilesDir(), "radar_updates"); }
  static List<BundledRadarCatalog.Point> active(Context context, boolean frHistorical, boolean chHistorical) throws IOException {
    List<BundledRadarCatalog.Point> fr = load(context, "FR");
    List<BundledRadarCatalog.Point> ch = load(context, "CH");
    java.util.ArrayList<BundledRadarCatalog.Point> selected = new java.util.ArrayList<>();
    for (BundledRadarCatalog.Point p : fr) if (!p.historical || frHistorical) selected.add(p);
    for (BundledRadarCatalog.Point p : ch) if (!p.historical || chHistorical) selected.add(p);
    try{selected.addAll(NeighborRadarCatalog.all(context.getAssets()::open));}
    catch(IOException absent){android.util.Log.w("ROADGUARD_RADARS","Neighbor supplement unavailable; FR/CH originals remain active",absent);}
    RadarCatalogMerge merged=new RadarCatalogMerge(selected);
    // Lufop overlay is additive. Keep all original and authorized points intact.
    try {
      // The packaged European overlay is available offline for all its countries.
      // Historical/mobile positions keep their existing per-country safety switches.
      for (BundledRadarCatalog.Point p : LufopEuropeCatalog.all(context)) {
        if (!context.getSharedPreferences("radar_country_visibility", Context.MODE_PRIVATE).getBoolean(p.country, true)) continue;
        if (p.historical && (("FR".equals(p.country) && !frHistorical)
            || ("CH".equals(p.country) && !chHistorical)
            || (!"FR".equals(p.country) && !"CH".equals(p.country)))) continue;
        merged.add(p);
      }
    } catch (IOException error) {
      android.util.Log.w("ROADGUARD_RADARS", "Lufop supplement unavailable; original radars remain active", error);
    }
    try{
      for(BundledRadarCatalog.Point p:CanonicalRadarCatalog.all(context)){
        if(!context.getSharedPreferences("radar_country_visibility",Context.MODE_PRIVATE).getBoolean(p.country,true))continue;
        if(p.historical&&(("FR".equals(p.country)&&!frHistorical)||("CH".equals(p.country)&&!chHistorical)||(!"FR".equals(p.country)&&!"CH".equals(p.country))))continue;
        // An unspecified type must not create a second alert beside a more precise original.
        if("UNKNOWN".equals(p.category)&&merged.containsPosition(p))continue;
        merged.add(p);
      }
    }catch(IOException error){android.util.Log.w("ROADGUARD_RADARS","Surcouches canoniques indisponibles ; originaux conservés",error);}
    return merged.snapshot();
  }
  private static List<BundledRadarCatalog.Point> load(Context context, String country) throws IOException {
    List<BundledRadarCatalog.Point> originals="FR".equals(country)?BundledRadarCatalog.france(context.getAssets()::open):BundledRadarCatalog.switzerland(context.getAssets()::open);
    File file = new File(directory(context), "authorized_radars_" + country.toLowerCase(java.util.Locale.ROOT) + ".csv");
    if (file.isFile()) {
      try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(new FileInputStream(file), "UTF-8"))) {
        List<BundledRadarCatalog.Point> update=BundledRadarCatalog.read(reader,country);
        RadarCatalogMerge merged=new RadarCatalogMerge(originals);
        for(BundledRadarCatalog.Point point:update)merged.add(point);
        return merged.snapshot();
      } catch (IOException | RuntimeException invalid) {
        // Keep the packaged catalog operational when an update is damaged.
        android.util.Log.w("ROADGUARD_RADARS", "Invalid updated catalog for " + country + "; using packaged data");
      }
    }
    return originals;
  }
}
