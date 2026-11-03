package fr.roadguard.app;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.*;

/** Receipt written by a catalog consumer, not by a download or import button. */
final class RadarActivationState {
  private RadarActivationState(){}
  static void consumed(Context context,String owner,String revision,List<BundledRadarCatalog.Point> catalog){
    if(!Arrays.asList("phone","background","car").contains(owner))throw new IllegalArgumentException("Moteur inconnu");
    TreeMap<String,Integer> counts=new TreeMap<>();
    for(BundledRadarCatalog.Point point:catalog)if(point.id.startsWith("sa-"))counts.put(point.country,counts.getOrDefault(point.country,0)+1);
    StringBuilder values=new StringBuilder();for(Map.Entry<String,Integer> entry:counts.entrySet())values.append(entry.getKey()).append('=').append(entry.getValue()).append(';');
    context.getSharedPreferences("radar_activation_receipts",Context.MODE_PRIVATE).edit().putString(owner+".revision",revision).putString(owner+".counts",values.toString()).putLong(owner+".time",System.currentTimeMillis()).commit();
  }
  static int loadedCount(Context context,String country){
    if(country==null||!country.matches("[A-Z]{2}"))return 0;
    SharedPreferences p=context.getSharedPreferences("radar_activation_receipts",Context.MODE_PRIVATE);String current=RadarCatalogRevision.current(context);int maximum=0;
    for(String owner:new String[]{"phone","background","car"})if(current.equals(p.getString(owner+".revision",""))){
      for(String part:p.getString(owner+".counts","").split(";"))if(part.startsWith(country+"="))try{maximum=Math.max(maximum,Integer.parseInt(part.substring(3)));}catch(NumberFormatException invalid){}
    }
    return maximum;
  }
  static boolean confirmed(Context context){
    SharedPreferences p=context.getSharedPreferences("radar_activation_receipts",Context.MODE_PRIVATE);String revision=RadarCatalogRevision.current(context);
    for(String owner:new String[]{"phone","background","car"})if(revision.equals(p.getString(owner+".revision","")))return true;
    return false;
  }
}
