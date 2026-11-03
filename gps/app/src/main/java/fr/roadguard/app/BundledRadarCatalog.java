package fr.roadguard.app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/** Immutable, process-cached radar inventory bundled in the APK; no network. */
final class BundledRadarCatalog {
  static final String FR_ASSET = "radars_embedded_fr.csv";
  static final String CH_ASSET = "radars_embedded_ch.csv";
  static final String HISTORICAL_PREFERENCE = "radar_historical_locations";
  static final String SWISS_HISTORICAL_PREFERENCE = "radar_swiss_historical_locations";
  static final int OFFICIAL_FR_COUNT = 3309;
  private static final String HEADER = "id;type;lat;lon;country;category;speed_kmh;direction_deg;status;sources;source_ids;speed_conflicts";
  private static volatile List<Point> france;
  private static volatile List<Point> switzerland;

  interface Source { InputStream open(String name) throws IOException; }

  static final class Point {
    final String id, type, category, sources, country;
    final double lat, lon;
    final int speedKmh;
    final Double direction;
    final String reliability;
    final boolean historical;
    Point(String id, String type, double lat, double lon, String category,
          boolean historical, String sources, String country) {
      this(id,type,lat,lon,category,historical,sources,country,0);
    }
    Point(String id,String type,double lat,double lon,String category,boolean historical,String sources,String country,int limit){this(id,type,lat,lon,category,historical,sources,country,limit,null,"");}
    Point(String id,String type,double lat,double lon,String category,boolean historical,String sources,String country,int limit,Double direction,String reliability){
      this.direction=direction!=null&&Double.isFinite(direction)&&direction>=0&&direction<360?direction:null;
      this.reliability=reliability==null?"":reliability;
      this.speedKmh=limit>=5&&limit<=200?limit:0;
      this.id=id; this.type=type; this.lat=lat; this.lon=lon;
      this.category=category; this.historical=historical; this.sources=sources;
      this.country=country;
    }
  }

  private static Double readDirection(String value){try{double direction=Double.parseDouble(value);return Double.isFinite(direction)&&direction>=0&&direction<360?direction:null;}catch(Exception unknown){return null;}}

  static List<Point> france(Source source) throws IOException {
    return country(source,"FR");
  }

  static List<Point> switzerland(Source source) throws IOException {
    return country(source,"CH");
  }

  private static List<Point> country(Source source,String code) throws IOException {
    boolean swiss="CH".equals(code);
    List<Point> cached=swiss?switzerland:france;
    if(cached!=null)return cached;
    synchronized(BundledRadarCatalog.class){
      cached=swiss?switzerland:france;
      if(cached==null){
        try(InputStream in=source.open(swiss?CH_ASSET:FR_ASSET)){
          cached=read(new BufferedReader(new InputStreamReader(in,"UTF-8")),code);
        }
        if(swiss)switzerland=cached;else france=cached;
      }
      return cached;
    }
  }

  /** Shared foreground/background selection; each country's switches are independent. */
  static List<Point> active(Source source,boolean frenchHistorical,boolean swissHistorical) throws IOException {
    // Parse both complete inventories before exposing any partial result.
    List<Point> fr=france(source),ch=switzerland(source);
    ArrayList<Point> result=new ArrayList<>();
    for(Point point:fr)if(!point.historical||frenchHistorical)result.add(point);
    for(Point point:ch)if(!point.historical||swissHistorical)result.add(point);
    return Collections.unmodifiableList(result);
  }

  // Validate each file's country before deliberately combining FR and CH inventories.
  private static int readLimit(String speed,String conflicts){if(!conflicts.trim().isEmpty())return 0;try{int n=Integer.parseInt(speed.trim());return n>=5&&n<=200?n:0;}catch(NumberFormatException absent){return 0;}}
  static List<Point> read(BufferedReader reader,String country) throws IOException {
    if(!("FR".equals(country)||"CH".equals(country)))throw new IOException("Pays radar invalide");
    if(!HEADER.equals(reader.readLine()))throw new IOException("En-tête de base radar invalide");
    ArrayList<Point> result=new ArrayList<>();
    HashSet<String> ids=new HashSet<>();
    String line;
    while((line=reader.readLine())!=null){
      if(result.size()>=100000)throw new IOException("Base radar trop volumineuse");
      String[] c=line.split(";",-1);
      if(c.length!=12||!country.equals(c[4])||!c[0].matches("rg-[a-f0-9]{20}")
          ||!ids.add(c[0])||!DangerTypePolicy.isValidLabel(c[1])||!DangerTypePolicy.isRadar(c[1])
          ||!("inventory".equals(c[8])||"historical".equals(c[8]))||c[9].isEmpty()||c[10].isEmpty())
        throw new IOException("Ligne radar invalide : "+(result.size()+2));
      try{
        double lat=Double.parseDouble(c[2]),lon=Double.parseDouble(c[3]);
        if(!Double.isFinite(lat)||!Double.isFinite(lon)||Math.abs(lat)>90||Math.abs(lon)>180)
          throw new NumberFormatException("Coordonnées hors limites");
        result.add(new Point(c[0],c[1],lat,lon,c[5],"historical".equals(c[8]),c[9],country,readLimit(c[6],c[11]),readDirection(c[7]),""));
      }catch(NumberFormatException invalid){throw new IOException("Coordonnées radar invalides",invalid);}
    }
    if(result.isEmpty())throw new IOException("Base radar vide");
    return Collections.unmodifiableList(result);
  }
}
