package fr.roadguard.app;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.File;
import java.util.TreeMap;

/** Cheap revision check; catalog parsing stays on a worker. Assets are never modified. */
final class RadarCatalogRevision {
  private RadarCatalogRevision(){}
  static synchronized void changed(Context context){
    SharedPreferences p=context.getSharedPreferences("radar_catalog_revision",Context.MODE_PRIVATE);
    p.edit().putLong("generation",p.getLong("generation",0L)+1L).apply();
  }
  static String current(Context context){
    StringBuilder stamp=new StringBuilder();
    stamp.append(context.getSharedPreferences("radar_catalog_revision",Context.MODE_PRIVATE).getLong("generation",0L));
    SharedPreferences settings=context.getSharedPreferences("MainActivity",Context.MODE_PRIVATE);
    stamp.append(':').append(settings.getBoolean(BundledRadarCatalog.HISTORICAL_PREFERENCE,false));
    stamp.append(':').append(settings.getBoolean(BundledRadarCatalog.SWISS_HISTORICAL_PREFERENCE,true));
    stamp.append(':').append(new TreeMap<>(context.getSharedPreferences("radar_country_visibility",Context.MODE_PRIVATE).getAll()));
    for(String name:new String[]{"authorized_radars_fr.csv","authorized_radars_ch.csv","lufop_europe_asc_overlay.csv", "lufop_europe_asc_overlay.backup","lufop_europe_asc_overlay.backup.previous"}){
      File file=new File(UpdatedRadarCatalog.directory(context),name);
      stamp.append(';').append(name).append(':').append(file.isFile()?file.length():-1).append(':').append(file.lastModified());
    }
    // Legacy index membership and replacement files are part of the same immutable snapshot.
    stamp.append(";installed:").append(digest(settings.getString("radar_country_installed","[]")));
    File[] installed=new File(context.getFilesDir(),"radar_countries").listFiles();
    if(installed!=null){java.util.Arrays.sort(installed,java.util.Comparator.comparing(File::getName));
      for(File file:installed)if(file.getName().matches("[a-z0-9_-]{2,50}\\.csv"))stamp.append(';').append(file.getName()).append(':').append(file.length()).append(':').append(file.lastModified());
    }
    stamp.append(";canonical:").append(CanonicalRadarCatalog.stamp(UpdatedRadarCatalog.directory(context)));
    return stamp.toString();
  }
  private static String digest(String value){
    try{
      byte[] bytes=java.security.MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      StringBuilder result=new StringBuilder();for(byte b:bytes)result.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return result.toString();
    }catch(java.security.NoSuchAlgorithmException unavailable){throw new IllegalStateException(unavailable);}
  }
}
