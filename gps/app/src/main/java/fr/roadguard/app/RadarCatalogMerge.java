package fr.roadguard.app;

import java.util.*;

/** Additive assembly: preserve the original identity and never resolve conflicting evidence by rank. */
final class RadarCatalogMerge {
  private final ArrayList<BundledRadarCatalog.Point> points=new ArrayList<>();
  private final Map<String,Integer> indices=new HashMap<>();
  private final Set<String> positions=new HashSet<>();
  private final Map<Long,List<Integer>> cells=new HashMap<>();
  private static long cell(int y,int x){return ((long)y<<32)|(x&0xffffffffL);}
  private static int y(double lat){return (int)Math.floor((lat+90)*1000);}
  private static int x(double lon){return ((int)Math.floor((lon+180)*1000)%360000+360000)%360000;}
  private void index(BundledRadarCatalog.Point p,int at){cells.computeIfAbsent(cell(y(p.lat),x(p.lon)),k->new ArrayList<>()).add(at);}
  private static boolean compatible(BundledRadarCatalog.Point a,BundledRadarCatalog.Point b){
    return a.historical==b.historical&&!(a.speedKmh>0&&b.speedKmh>0&&a.speedKmh!=b.speedKmh)
      &&!(a.direction!=null&&b.direction!=null&&angle(a.direction,b.direction)>.01);
  }
  /** Only reconcile an unambiguous Lufop observation already represented by a Lufop source.
   * Nearby independent cameras, opposite orientations and conflicting limits stay distinct. */
  private Integer lufopMatch(BundledRadarCatalog.Point incoming){
    if(!incoming.id.startsWith("lufop-")||!incoming.sources.toLowerCase(Locale.ROOT).contains("lufop"))return null;
    int match=-1,span=(int)Math.ceil(35/(111320*Math.max(.01,Math.cos(Math.toRadians(incoming.lat))))*1000)+1;
    for(int yy=y(incoming.lat-35/110574d);yy<=y(incoming.lat+35/110574d);yy++)for(int dx=-span;dx<=span;dx++){
      List<Integer> bucket=cells.get(cell(yy,(x(incoming.lon)+dx+360000)%360000));if(bucket==null)continue;
      for(int at:bucket){BundledRadarCatalog.Point original=points.get(at);
        if(!original.country.equals(incoming.country)||!RadarIdentity.type(original.type).equals(RadarIdentity.type(incoming.type))
          ||!original.sources.toLowerCase(Locale.ROOT).contains("lufop")||!compatible(original,incoming))continue;
        double la=Math.toRadians(original.lat-incoming.lat),lo=Math.toRadians(original.lon-incoming.lon);
        double h=Math.sin(la/2)*Math.sin(la/2)+Math.cos(Math.toRadians(original.lat))*Math.cos(Math.toRadians(incoming.lat))*Math.sin(lo/2)*Math.sin(lo/2);
        if(2*6371000*Math.asin(Math.sqrt(Math.min(1,h)))>35)continue;
        if(match>=0&&match!=at)return null;match=at;
      }
    }
    return match<0?null:match;
  }
  RadarCatalogMerge(List<BundledRadarCatalog.Point> originals){
    // Existing inventories are retained even if they already contain co-located records.
    for(BundledRadarCatalog.Point p:originals){indices.putIfAbsent(RadarIdentity.catalog(p),points.size());index(p,points.size());points.add(p);positions.add(position(p));}
  }
  boolean containsPosition(BundledRadarCatalog.Point p){return positions.contains(position(p));}
  private static String position(BundledRadarCatalog.Point p){return p.country+":"+AlertMilestones.pointKey(p.lat,p.lon);}
  void add(BundledRadarCatalog.Point incoming){
    String key=RadarIdentity.catalog(incoming);Integer index=indices.get(key);
    if(index==null)index=lufopMatch(incoming);
    if(index==null){indices.put(key,points.size());index(incoming,points.size());points.add(incoming);positions.add(position(incoming));return;}
    indices.putIfAbsent(key,index);
    BundledRadarCatalog.Point original=points.get(index);
    // A historical observation cannot qualify a current installation. Opposite orientations or
    // contradictory limits must not be presented as corroborating the original record.
    if(original.historical!=incoming.historical
        ||original.speedKmh>0&&incoming.speedKmh>0&&original.speedKmh!=incoming.speedKmh
        ||original.direction!=null&&incoming.direction!=null&&angle(original.direction,incoming.direction)>.01)return;
    points.set(index,new BundledRadarCatalog.Point(original.id,original.type,original.lat,original.lon,
        original.category,original.historical,join(original.sources,incoming.sources),original.country,
        original.speedKmh>0?original.speedKmh:incoming.speedKmh,
        original.direction!=null?original.direction:incoming.direction,join(original.reliability,incoming.reliability)));
  }
  private static double angle(double a,double b){return Math.abs(((a-b+540)%360)-180);}
  private static String join(String a,String b){
    LinkedHashSet<String> values=new LinkedHashSet<>();
    for(String text:new String[]{a,b})if(text!=null)for(String value:text.split(" \u00b7 "))if(!value.trim().isEmpty())values.add(value.trim());
    return String.join(" · ",values);
  }
  List<BundledRadarCatalog.Point> snapshot(){return Collections.unmodifiableList(new ArrayList<>(points));}
}
