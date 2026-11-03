package fr.roadguard.app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/** Five dated, attributed country extracts bundled independently of the FR/CH catalog. */
final class NeighborRadarCatalog {
  private static final String HEADER="id;type;lat;lon;country";
  private static final String[] CODES={"BE","DE","ES","IT","LU"};
  private static volatile List<BundledRadarCatalog.Point> cache;

  static List<BundledRadarCatalog.Point> all(BundledRadarCatalog.Source source) throws IOException {
    List<BundledRadarCatalog.Point> current=cache;
    if(current!=null)return current;
    synchronized(NeighborRadarCatalog.class){
      if(cache!=null)return cache;
      ArrayList<BundledRadarCatalog.Point> points=new ArrayList<>();
      for(String code:CODES){
        try(InputStream stream=source.open("radars_embedded_"+code.toLowerCase(java.util.Locale.ROOT)+".csv")){
          points.addAll(read(new BufferedReader(new InputStreamReader(stream,"UTF-8")),code));
        }
      }
      cache=Collections.unmodifiableList(points);
      return cache;
    }
  }

  static List<BundledRadarCatalog.Point> read(BufferedReader reader,String country) throws IOException {
    boolean known=false;for(String code:CODES)if(code.equals(country))known=true;
    if(!known||!HEADER.equals(reader.readLine()))throw new IOException("Base radar pays invalide");
    ArrayList<BundledRadarCatalog.Point> points=new ArrayList<>();
    HashSet<String> ids=new HashSet<>();String line;
    while((line=reader.readLine())!=null){
      if(points.size()>=30000)throw new IOException("Base pays trop volumineuse");
      String[] values=line.split(";",-1);
      if(values.length!=5||!country.equals(values[4])||!values[0].matches("mapatlas-[a-z]{2}-[0-9]+")
          ||!ids.add(values[0])||!DangerTypePolicy.isRadar(values[1]))throw new IOException("Point radar invalide");
      try{
        double lat=Double.parseDouble(values[2]),lon=Double.parseDouble(values[3]);
        if(!Double.isFinite(lat)||!Double.isFinite(lon)||Math.abs(lat)>90||Math.abs(lon)>180)throw new NumberFormatException();
        points.add(new BundledRadarCatalog.Point(values[0],values[1],lat,lon,"MapAtlas 2024",false,"MapAtlas CC BY 4.0",country));
      }catch(NumberFormatException invalid){throw new IOException("Coordonnées radar invalides",invalid);}
    }
    if(points.isEmpty())throw new IOException("Pays radar vide");
    return points;
  }
}
