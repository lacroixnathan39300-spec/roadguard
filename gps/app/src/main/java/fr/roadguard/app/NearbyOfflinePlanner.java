package fr.roadguard.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Pure-Java 50 km region queue; no tile fetching and no Android dependency. */
public final class NearbyOfflinePlanner {
  private NearbyOfflinePlanner() {}
  public static final class Cell {
    public final String id;
    public final double lat, lon, distanceKm;
    Cell(String id,double lat,double lon,double distanceKm){this.id=id;this.lat=lat;this.lon=lon;this.distanceKm=distanceKm;}
  }
  public static double distanceKm(double a,double b,double c,double d){
    double p=Math.PI/180.0, da=(c-a)*p, db=(d-b)*p;
    double h=Math.pow(Math.sin(da/2),2)+Math.cos(a*p)*Math.cos(c*p)*Math.pow(Math.sin(db/2),2);
    return 12742.0*Math.asin(Math.min(1,Math.sqrt(h)));
  }
  public static String cellId(double lat,double lon,int km){
    double stepLat=km/111.32;
    double stepLon=km/111.32; // projected reference grid: longitude index scaled at equator
    return km+"km:"+(long)Math.floor(lat/stepLat)+":"+(long)Math.floor(lon/stepLon);
  }
  public static List<Cell> plan(double lat,double lon,int radiusKm,int cellKm){
    if(!Double.isFinite(lat)||!Double.isFinite(lon)||Math.abs(lat)>85||Math.abs(lon)>180||radiusKm<=0||cellKm<=0)throw new IllegalArgumentException("Invalid GPS/region");
    double latStep=cellKm/111.32;
    double lonStep=cellKm/111.32; // stable global longitude grid across GPS movements
    long centerY=(long)Math.floor(lat/latStep), centerX=(long)Math.floor(lon/lonStep);
    int spanY=(int)Math.ceil((double)radiusKm/cellKm)+2;
    int spanX=(int)Math.ceil((double)radiusKm/(cellKm*Math.max(0.05,Math.cos(Math.toRadians(lat)))))+2;
    ArrayList<Cell> out=new ArrayList<>();
    for(int y=-spanY;y<=spanY;y++)for(int x=-spanX;x<=spanX;x++){
      double cy=(centerY+y+0.5)*latStep, cx=(centerX+x+0.5)*lonStep;
      double dist=distanceKm(lat,lon,cy,cx);
      if(dist<=radiusKm+cellKm*0.71){
        String id=String.format(Locale.ROOT,"%dkm:%d:%d",cellKm,centerY+y,centerX+x);
        out.add(new Cell(id,cy,cx,dist));
      }
    }
    Collections.sort(out,Comparator.comparingDouble(c->c.distanceKm));
    return out;
  }
}
