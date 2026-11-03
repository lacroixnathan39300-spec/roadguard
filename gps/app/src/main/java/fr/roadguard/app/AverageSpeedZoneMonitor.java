package fr.roadguard.app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Estimate a section average only when both tagged endpoints and a limit are known. */
public final class AverageSpeedZoneMonitor {
  public static final class Zone {
    public final String id;
    public final double startLat,startLon,endLat,endLon;
    public final int limitKmh;
    public Zone(String id,double startLat,double startLon,double endLat,double endLon,int limitKmh) {
      if(id==null||id.isEmpty()||!RouteRequestPolicy.coordinate(startLat,startLon)
          ||!RouteRequestPolicy.coordinate(endLat,endLon)||limitKmh<20||limitKmh>150
          ||distance(startLat,startLon,endLat,endLon)<300
          ||distance(startLat,startLon,endLat,endLon)>30000)throw new IllegalArgumentException("Invalid average zone");
      this.id=id;this.startLat=startLat;this.startLon=startLon;
      this.endLat=endLat;this.endLon=endLon;this.limitKmh=limitKmh;
    }
  }
  public static final class Status {
    public final String text;
    public final boolean active,finished,overLimit;
    Status(String text,boolean active,boolean finished,boolean overLimit) {
      this.text=text;this.active=active;this.finished=finished;this.overLimit=overLimit;
    }
  }
  private static final class Pair {
    double startLat,startLon,endLat,endLon;boolean start,end;int limit;
  }
  /** Point brut d'une base (début, fin ou radar de tronçon), pour les tronçons non appariés. */
  public static final class Point {
    public final double lat,lon;public final String category;public final int limitKmh,directionDeg;
    public Point(double lat,double lon,String category,int limitKmh,int directionDeg){
      this.lat=lat;this.lon=lon;this.category=category;this.limitKmh=limitKmh;this.directionDeg=directionDeg;
    }
  }
  private final ArrayList<Zone> zones=new ArrayList<>();
  private final ArrayList<Point> points=new ArrayList<>();
  private Zone active;
  // Tronçon reconnu en roulant (bases françaises : début et fin non reliés entre eux).
  private boolean dynamic;private int dynamicLimit;
  private double previousLat,previousLon,meters;
  private long startedAt,lastAt;
  private long finishedAt;
  private Status finishedStatus;
  public AverageSpeedZoneMonitor(ArrayList<Zone> known){this(known,null);}
  public AverageSpeedZoneMonitor(ArrayList<Zone> known,ArrayList<Point> rawPoints){
    if(known!=null)zones.addAll(known);if(rawPoints!=null)points.addAll(rawPoints);
  }
  public int zoneCount(){int starts=0;for(Point p:points)if(p.category.equals("average_start"))starts++;return zones.size()+starts;}
  public void reset(){active=null;dynamic=false;dynamicLimit=0;meters=0;startedAt=lastAt=finishedAt=0;finishedStatus=null;}
  /** Lit les points de début/fin/radar de tronçon, même sans identifiant de relation OSM. */
  public static ArrayList<Point> parsePoints(Reader input)throws IOException {
    ArrayList<Point> out=new ArrayList<>();
    try(BufferedReader reader=new BufferedReader(input)){
      String row;while((row=reader.readLine())!=null){
        String[] f=row.split(";",-1);if(f.length<9)continue;
        String category=f[5];
        if(!category.equals("average_start")&&!category.equals("average_end")&&!category.equals("average"))continue;
        if(f[8].equals("historical"))continue;
        try{
          double lat=Double.parseDouble(f[2]),lon=Double.parseDouble(f[3]);
          if(!RouteRequestPolicy.coordinate(lat,lon))continue;
          int limit=f[6].isEmpty()?0:Integer.parseInt(f[6]);
          int dir=f[7].isEmpty()?-1:Integer.parseInt(f[7]);
          out.add(new Point(lat,lon,category,limit>=20&&limit<=150?limit:0,dir>=0&&dir<=360?dir:-1));
        }catch(NumberFormatException ignored){}
      }
    }
    return out;
  }
  private Point nearestPoint(double lat,double lon,String category,double radius){
    Point best=null;double bestD=radius;
    for(Point p:points){if(!p.category.equals(category))continue;double d=distance(lat,lon,p.lat,p.lon);if(d<=bestD){bestD=d;best=p;}}
    return best;
  }
  private Status tryDynamicStart(double lat,double lon,double bearingDeg,long elapsedMs){
    Point start=nearestPoint(lat,lon,"average_start",55);if(start==null)return null;
    int limit=0,dir=-1;
    for(Point p:points){
      if(!p.category.equals("average")||distance(start.lat,start.lon,p.lat,p.lon)>400)continue;
      if(limit==0&&p.limitKmh>0)limit=p.limitKmh;if(dir<0&&p.directionDeg>=0)dir=p.directionDeg;
    }
    if(dir>=0&&headingDifference(bearingDeg,dir)>60)return null;
    dynamic=true;dynamicLimit=limit;startedAt=lastAt=elapsedMs;previousLat=lat;previousLon=lon;meters=0;
    return new Status(limit>0?"Zone moyenne "+limit+" km/h · calcul en cours":"Zone moyenne · calcul en cours",true,false,false);
  }
  public static ArrayList<Zone> parse(Reader input)throws IOException {
    ArrayList<Zone> zones=new ArrayList<>();
    Map<String,Pair> pairs=new HashMap<>();
    try(BufferedReader reader=new BufferedReader(input)){
      String row;while((row=reader.readLine())!=null){
        String[] fields=row.split(";",-1);
        if(fields.length<11)continue;
        String category=fields[5],sources=fields[10];
        if(!category.equals("average_start")&&!category.equals("average_end")&&!category.equals("average"))continue;
        for(String source:sources.split("\\|")){
          int from=source.indexOf("relation/");if(from<0)continue;
          int begin=from+9,end=begin;
          while(end<source.length()&&Character.isDigit(source.charAt(end)))end++;
          if(end==begin)continue;
          String id=source.substring(begin,end);
          try{
            double lat=Double.parseDouble(fields[2]),lon=Double.parseDouble(fields[3]);
            Pair pair=pairs.get(id);if(pair==null){pair=new Pair();pairs.put(id,pair);}
            int limit=fields[6].isEmpty()?0:Integer.parseInt(fields[6]);
            if(limit>=20&&limit<=150)pair.limit=limit;
            if(category.equals("average_start")&&source.contains("/from/")){pair.start=true;pair.startLat=lat;pair.startLon=lon;}
            if(category.equals("average_end")&&source.contains("/to/")){pair.end=true;pair.endLat=lat;pair.endLon=lon;}
          }catch(NumberFormatException ignored){}
        }
      }
    }
    for(Map.Entry<String,Pair> entry:pairs.entrySet()){
      Pair p=entry.getValue();
      if(!p.start||!p.end||p.limit<20)continue;
      try{zones.add(new Zone("osm-"+entry.getKey(),p.startLat,p.startLon,p.endLat,p.endLon,p.limit));}
      catch(IllegalArgumentException ignored){}
    }
    return zones;
  }
  public Status update(double lat,double lon,double speedKmh,double accuracyMeters,double bearingDeg,long elapsedMs){
    if(!RouteRequestPolicy.coordinate(lat,lon)||!Double.isFinite(accuracyMeters)||accuracyMeters>35){
      if(active!=null||dynamic)reset();return null;
    }
    if(active==null){
      if(finishedStatus!=null&&elapsedMs-finishedAt>=0&&elapsedMs-finishedAt<9000)return finishedStatus;
      finishedStatus=null;
      if(speedKmh<5||!Double.isFinite(speedKmh)||!Double.isFinite(bearingDeg))return null;
      for(Zone z:zones)if(distance(lat,lon,z.startLat,z.startLon)<=55
          &&distance(lat,lon,z.endLat,z.endLon)>300
          &&headingDifference(bearingDeg,bearing(z.startLat,z.startLon,z.endLat,z.endLon))<=60){
        active=z;startedAt=lastAt=elapsedMs;previousLat=lat;previousLon=lon;meters=0;
        return new Status("Zone moyenne "+z.limitKmh+" km/h · calcul en cours",true,false,false);
      }
      if(!dynamic)return tryDynamicStart(lat,lon,bearingDeg,elapsedMs);
    }
    long delta=elapsedMs-lastAt;
    if(delta<=0||delta>20000){reset();return null;}
    double segment=distance(previousLat,previousLon,lat,lon);
    if(segment>Math.max(70,delta/1000.0*65)){reset();return null;}
    meters+=segment;previousLat=lat;previousLon=lon;lastAt=elapsedMs;
    double seconds=(elapsedMs-startedAt)/1000.0;
    int average=(int)Math.round(meters*3.6/Math.max(seconds,1));
    if(dynamic){
      if(meters>30000){reset();return null;}
      boolean over=dynamicLimit>0&&average>dynamicLimit;
      String verdict=dynamicLimit==0?"":(over?" · au-dessus":" · OK");
      String shown=dynamicLimit>0?average+"/"+dynamicLimit:String.valueOf(average);
      if(meters>=300&&seconds>=10&&nearestPoint(lat,lon,"average_end",70)!=null){
        String result="Zone terminée · moyenne "+shown+" km/h"+verdict;
        dynamic=false;meters=0;startedAt=lastAt=0;finishedAt=elapsedMs;
        finishedStatus=new Status(result,false,true,over);return finishedStatus;
      }
      return new Status("Moyenne "+shown+" km/h"+verdict,true,false,over);
    }
    boolean over=average>active.limitKmh;
    if(distance(lat,lon,active.endLat,active.endLon)<=70&&seconds>=10){
      String result=String.format(Locale.FRANCE,"Zone terminée · moyenne %d km/h · %s",average,over?"au-dessus":"OK");
      active=null;meters=0;startedAt=lastAt=0;finishedAt=elapsedMs;
      finishedStatus=new Status(result,false,true,over);return finishedStatus;
    }
    return new Status(String.format(Locale.FRANCE,"Moyenne %d/%d km/h · %s",average,active.limitKmh,over?"au-dessus":"OK"),true,false,over);
  }
  private static double distance(double a,double b,double c,double d){
    double lat=Math.toRadians(c-a),lon=Math.toRadians(d-b);
    double h=Math.pow(Math.sin(lat/2),2)+Math.cos(Math.toRadians(a))*Math.cos(Math.toRadians(c))*Math.pow(Math.sin(lon/2),2);
    return 12742000*Math.asin(Math.min(1,Math.sqrt(h)));
  }
  private static double bearing(double a,double b,double c,double d){
    double lon=Math.toRadians(d-b),lat1=Math.toRadians(a),lat2=Math.toRadians(c);
    double y=Math.sin(lon)*Math.cos(lat2);
    double x=Math.cos(lat1)*Math.sin(lat2)-Math.sin(lat1)*Math.cos(lat2)*Math.cos(lon);
    return (Math.toDegrees(Math.atan2(y,x))+360)%360;
  }
  private static double headingDifference(double a,double b){
    double delta=Math.abs((a-b+360)%360);return Math.min(delta,360-delta);
  }
}
