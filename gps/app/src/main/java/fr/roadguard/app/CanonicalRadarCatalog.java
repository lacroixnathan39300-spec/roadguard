package fr.roadguard.app;

import android.content.Context;
import java.io.*;
import java.util.*;

/** Additional country files; never substitutes a packaged inventory. */
final class CanonicalRadarCatalog {
  private CanonicalRadarCatalog(){}
  static Set<String> countries(File updates){
    TreeSet<String> codes=new TreeSet<>();File[] files=new File(updates,"canonical").listFiles();
    if(files!=null)for(File file:files){String name=file.getName();if(name.matches("[a-z]{2}\\.csv(\\.backup(\\.previous)?)?"))codes.add(name.substring(0,2).toUpperCase(Locale.ROOT));}
    return codes;
  }
  static String stamp(File updates){
    StringBuilder value=new StringBuilder();
    for(String code:countries(updates)){
      File target=CanonicalRadarStore.file(updates,code);
      for(File file:new File[]{target,new File(target+".backup"),new File(target+".backup.previous")})value.append(file.getName()).append(':').append(file.length()).append(':').append(file.lastModified()).append(';');
    }
    return value.toString();
  }
  static List<BundledRadarCatalog.Point> all(Context context)throws IOException{
    File updates=UpdatedRadarCatalog.directory(context);ArrayList<BundledRadarCatalog.Point> result=new ArrayList<>();
    for(String code:countries(updates)){
      try{
        CanonicalRadarStore.Snapshot snapshot=CanonicalRadarStore.load(updates,code);
        if(!snapshot.warning.isEmpty())android.util.Log.w("ROADGUARD_RADARS",code+" : "+snapshot.warning+(snapshot.backup?" ; sauvegarde utilisée":""));
        if(result.size()+snapshot.records.size()>300000){android.util.Log.w("ROADGUARD_RADARS",code+" : limite globale des surcouches atteinte ; autres pays conservés");continue;}
        for(CanonicalRadar record:snapshot.records)result.add(record.point());
      }catch(IOException invalid){android.util.Log.w("ROADGUARD_RADARS",code+" : surcouche inaccessible ; autres pays conservés",invalid);}
    }
    return Collections.unmodifiableList(result);
  }
}
