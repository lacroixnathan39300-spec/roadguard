package fr.roadguard.app;

import android.content.Context;
import java.io.*;
import java.util.*;

/** Supplemental, locally bundled Lufop ASC points. Original inventories are untouched. */
final class LufopEuropeCatalog {
  private static final String ASSET="lufop_europe_asc_overlay.csv";
  private LufopEuropeCatalog(){}
  private static volatile List<BundledRadarCatalog.Point> cached;
  private static volatile String cachedSourceStamp;
  private static volatile Map<String,Integer> cachedCounts;
  private static String sourceStamp(Context context){
    File dir=new File(context.getFilesDir(),"radar_updates");
    StringBuilder result=new StringBuilder(dir.getAbsolutePath());
    for(String name:new String[]{"lufop_europe_asc_overlay.csv","lufop_europe_asc_overlay.backup","lufop_europe_asc_overlay.backup.previous"}){
      File file=new File(dir,name);
      result.append(name).append(':').append(file.exists()?file.length():-1).append(':').append(file.exists()?file.lastModified():-1).append(';');
    }
    return result.toString();
  }
  /** Parse the bundled European overlay once, rather than rereading 80k rows per country. */
  static List<BundledRadarCatalog.Point> all(Context context) throws IOException {
    String stamp=sourceStamp(context);
    List<BundledRadarCatalog.Point> current=cached;
    if(current!=null && stamp.equals(cachedSourceStamp))return current;
    synchronized(LufopEuropeCatalog.class){
      stamp=sourceStamp(context);
      if(cached!=null && stamp.equals(cachedSourceStamp))return cached;
      // An imported subset supplements the immutable APK inventory. It must never
      // replace the packaged European points merely because its CSV is valid.
      ArrayList<BundledRadarCatalog.Point> result;
      try(InputStream stream=context.getAssets().open(ASSET)){
        result=new ArrayList<>(parse(stream));
      }
      HashSet<String> seen=new HashSet<>();
      for(BundledRadarCatalog.Point point:result)seen.add(RadarIdentity.catalog(point));
      File update=new File(context.getFilesDir(),"radar_updates/lufop_europe_asc_overlay.csv");
      File backup=new File(context.getFilesDir(),"radar_updates/lufop_europe_asc_overlay.backup");
      // Never let a damaged local update disable the original bundled Europe data.
      File olderBackup=new File(context.getFilesDir(),"radar_updates/lufop_europe_asc_overlay.backup.previous");
      File[] candidates={update,backup,olderBackup};
      for(File candidate:candidates){
        if(!candidate.isFile())continue;
        try(InputStream stream=new FileInputStream(candidate)){
          List<BundledRadarCatalog.Point> overlay=parse(stream);
          for(BundledRadarCatalog.Point point:overlay)if(seen.add(RadarIdentity.catalog(point)))result.add(point);
          break;
        }catch(IOException error){
          android.util.Log.w("ROADGUARD_RADARS","Surcouche Lufop invalide ; inventaire APK conservé, essai sauvegarde : "+candidate.getName(),error);
        }
      }
      if(result.isEmpty())throw new IOException("Base Lufop Europe vide");
      HashMap<String,Integer> counts=new HashMap<>();
      for(BundledRadarCatalog.Point point:result)counts.put(point.country,counts.containsKey(point.country)?counts.get(point.country)+1:1);
      cachedCounts=Collections.unmodifiableMap(counts);
      cached=Collections.unmodifiableList(result);
      // If a file changes while it is being parsed, the next reader must retry.
      cachedSourceStamp=stamp;
      return cached;
    }
  }
  private static List<BundledRadarCatalog.Point> parse(InputStream stream)throws IOException{
    ArrayList<BundledRadarCatalog.Point> result=new ArrayList<>();
    BufferedReader reader=new BufferedReader(new InputStreamReader(stream,"UTF-8"));
    if(!"country;type;lat;lon;speed_kmh;id;label".equals(reader.readLine()))throw new IOException("En-tête Lufop incorrect");
    String line;int lines=0;long chars=0;
    while((line=reader.readLine())!=null){
      chars+=line.length();if(++lines>200000||chars>35000000L)throw new IOException("Archive Lufop trop volumineuse");
      String[] c=line.split(";",-1);
      if(c.length!=7||!c[0].matches("[A-Z]{2}"))throw new IOException("Ligne Lufop invalide : "+lines);
      try{
        double lat=Double.parseDouble(c[2]),lon=Double.parseDouble(c[3]);int speed=Integer.parseInt(c[4]);
        if(!Double.isFinite(lat)||!Double.isFinite(lon)||Math.abs(lat)>90||Math.abs(lon)>180||speed<0||speed>300)
          throw new IOException("Coordonnées Lufop invalides : "+lines);
        boolean historical="mobile".equals(c[1])||"zone_danger".equals(c[1]);
        String type="feu_rouge".equals(c[1])?"Radar feu rouge":("mobile".equals(c[1])?"Radar mobile":("radar_fixe".equals(c[1])?"Radar fixe":"Zone de danger"));
        result.add(new BundledRadarCatalog.Point("lufop-"+c[5],type,lat,lon,c[1],historical,"Lufop ASC",c[0],speed));
      }catch(NumberFormatException invalid){throw new IOException("Nombre Lufop invalide : "+lines,invalid);}
    }
    if(result.isEmpty())throw new IOException("Base Lufop vide");return result;
  }
  static void invalidate(){synchronized(LufopEuropeCatalog.class){cached=null;cachedCounts=null;cachedSourceStamp=null;}}
  /** Fast inventory lookup; the underlying overlay is parsed once per file revision. */
  static int count(Context context,String country) throws IOException {
    all(context);
    Map<String,Integer> counts=cachedCounts;
    Integer value=counts==null?null:counts.get(country);
    return value==null?0:value;
  }
  static List<BundledRadarCatalog.Point> read(Context context,String country) throws IOException {
    ArrayList<BundledRadarCatalog.Point> result=new ArrayList<>();
    for(BundledRadarCatalog.Point p:all(context))if(country.equals(p.country))result.add(p);
    return Collections.unmodifiableList(result);
  }
}
