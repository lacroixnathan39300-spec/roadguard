package fr.roadguard.app;

import java.io.*;
import java.util.*;

/** Worker-only assembly of legacy country files and the community inventory.
 * Originals are never removed. No Activity fields are read or changed here. */
final class RadarSupplementCatalog {
  static final class Source {
    final String id,country;final File file;
    Source(String id,String country,File file){this.id=id;this.country=country;this.file=file;}
  }
  static List<BundledRadarCatalog.Point> merge(List<BundledRadarCatalog.Point> originals,
      List<Source> sources,List<BundledRadarCatalog.Point> community,boolean chHistorical)throws IOException {
    ArrayList<BundledRadarCatalog.Point> result=new ArrayList<>(originals);
    Nearby nearby=new Nearby();for(BundledRadarCatalog.Point p:originals)nearby.add(p);
    int total=0;
    for(Source source:sources){
      cancelled();
      if(!source.id.matches("[a-z0-9_-]{2,50}")||RadarSourcePolicy.quarantined(source.id)
          ||!source.file.isFile()||source.file.length()>32L*1024*1024)continue;
      try(BufferedReader reader=new BufferedReader(new InputStreamReader(new FileInputStream(source.file),"UTF-8"))){
        String line=reader.readLine();if(!"id;type;lat;lon".equalsIgnoreCase(line==null?"":line.trim()))continue;
        int lines=0;while((line=reader.readLine())!=null&&lines++<100000&&total<150000){
          cancelled();if(line.length()>4096)continue;String[] c=line.split(";",-1);if(c.length<4)continue;
          try{
            double lat=Double.parseDouble(c[2].trim()),lon=Double.parseDouble(c[3].trim());String type=c[1].trim();
            if(!coordinate(lat,lon)||!RadarSourcePolicy.include(source.country,type,chHistorical))continue;
            type=RadarSourcePolicy.displayLabel(source.country,type);
            if(!DangerTypePolicy.isRadar(type)||nearby.contains(lat,lon,type,source.country))continue;
            BundledRadarCatalog.Point point=new BundledRadarCatalog.Point("legacy-"+source.id+"-"+lines,type,lat,lon,"legacy",false,"local",source.country,RadarAlertPolicy.labelLimit(type));
            result.add(point);nearby.add(point);total++;
          }catch(NumberFormatException ignored){}
        }
      }catch(InterruptedIOException cancelled){throw cancelled;}
      catch(IOException ignored){/* An unreadable supplement never removes originals. */}
    }
    for(BundledRadarCatalog.Point point:community){
      cancelled();if(total>=150000)break;
      if(coordinate(point.lat,point.lon)&&DangerTypePolicy.isRadar(point.type)&&!nearby.contains(point.lat,point.lon,point.type,point.country)){
        result.add(point);nearby.add(point);total++;
      }
    }
    return Collections.unmodifiableList(result);
  }
  private static void cancelled()throws InterruptedIOException{if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("Chargement radar annulé");}
  private static boolean coordinate(double lat,double lon){return Double.isFinite(lat)&&Double.isFinite(lon)&&Math.abs(lat)<=90&&Math.abs(lon)<=180;}
  private static final class Nearby {
    final Map<Long,List<BundledRadarCatalog.Point>> cells=new HashMap<>();
    int y(double lat){return (int)Math.floor((lat+90)*100);}
    int x(double lon){int n=(int)Math.floor((lon+180)*100);return ((n%36000)+36000)%36000;}
    long key(int y,int x){return ((long)y<<32)|(x&0xffffffffL);}
    void add(BundledRadarCatalog.Point point){if(!DangerTypePolicy.isRadar(point.type))return;cells.computeIfAbsent(key(y(point.lat),x(point.lon)),k->new ArrayList<>()).add(point);}
    boolean contains(double lat,double lon,String type,String country){
      int span=(int)Math.ceil(40/(111320*Math.max(.01,Math.cos(Math.toRadians(lat))))*100)+1;
      for(int a=y(lat-40/110574.0);a<=y(lat+40/110574.0);a++)for(int dx=-span;dx<=span;dx++){
        List<BundledRadarCatalog.Point> bucket=cells.get(key(a,(x(lon)+dx+36000)%36000));if(bucket==null)continue;
        for(BundledRadarCatalog.Point p:bucket){if(country!=null&&!country.isEmpty()&&p.country!=null&&!p.country.isEmpty()&&!country.equals(p.country)||!RadarIdentity.type(type).equals(RadarIdentity.type(p.type)))continue;double da=Math.toRadians(p.lat-lat),db=Math.toRadians(p.lon-lon);
          double h=Math.sin(da/2)*Math.sin(da/2)+Math.cos(Math.toRadians(lat))*Math.cos(Math.toRadians(p.lat))*Math.sin(db/2)*Math.sin(db/2);
          if(2*6371000*Math.asin(Math.sqrt(Math.min(1,h)))<40)return true;
        }
      }
      return false;
    }
  }
  private RadarSupplementCatalog(){}
}
