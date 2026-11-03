package fr.roadguard.app;

import java.util.List;
import java.util.Locale;

/** Route-distance display shared by the HUD and route summary. No camera changes. */
final class NavigationProgress {
  final List<double[]> points;
  final double[] cumulative;
  final double total;
  private Position anchor,lastResult;
  private long anchorAt,lastMeasuredAt;
  private double anchorLat,anchorLon,lastLat,lastLon;
  NavigationProgress(List<double[]> points){this.points=points;cumulative=new double[points.size()];for(int i=1;i<points.size();i++)cumulative[i]=cumulative[i-1]+meters(points.get(i-1),points.get(i));total=cumulative.length==0?0:cumulative[cumulative.length-1];}
  static final class Position {
    final int segment;final double fraction,along,remaining,lateral;
    Position(int s,double f,double a,double r,double l){segment=s;fraction=f;along=a;remaining=r;lateral=l;}
    boolean arrived(){return remaining<=30&&lateral<=50;}
  }
  Position measure(double lat,double lon,int earliest){
    return measure(lat,lon,earliest,points.size()-2);
  }
  private Position measure(double lat,double lon,int earliest,int latest){
    RouteRequestPolicy.RouteMatch m=RouteRequestPolicy.matchPolyline(lat,lon,points,earliest,latest);if(m==null)return null;
    double along=cumulative[m.segmentIndex]+m.fraction*(cumulative[m.segmentIndex+1]-cumulative[m.segmentIndex]);
    return new Position(m.segmentIndex,m.fraction,along,Math.max(0,total-along),m.lateralMeters);
  }
  /** Bound continuous fixes by elapsed time and actual displacement, rather than segment count.
   * A long GNSS interruption permits reacquisition anywhere after the caller's cursor. */
  Position track(double lat,double lon,int earliest,long measuredAt,double speed,double accuracy){
    if(!RouteRequestPolicy.coordinate(lat,lon))return null;
    if(measuredAt<=0)return measure(lat,lon,earliest);
    if(measuredAt<lastMeasuredAt)return null;
    if(measuredAt==lastMeasuredAt)return lat==lastLat&&lon==lastLon?lastResult:null;
    Position result;
    long age=measuredAt-anchorAt;
    if(anchor!=null&&age>0&&age<=30000){
      double uncertainty=Double.isFinite(accuracy)&&accuracy>0?Math.min(100,accuracy):30;
      double movement=meters(new double[]{anchorLat,anchorLon},new double[]{lat,lon});
      double velocity=Double.isFinite(speed)&&speed>=0?Math.min(100,speed):55;
      double advance=Math.max(180,Math.min(movement*2+uncertainty*2,velocity*age/1000d*2+uncertainty*2+120));
      int first=Math.max(Math.max(0,earliest),segmentAt(Math.max(0,anchor.along-60)));
      result=measure(lat,lon,first,Math.max(first,segmentAt(Math.min(total,anchor.along+advance))));
    }else result=measure(lat,lon,earliest);
    lastMeasuredAt=measuredAt;lastLat=lat;lastLon=lon;lastResult=result;
    if(result!=null&&result.lateral<=100){anchor=result;anchorAt=measuredAt;anchorLat=lat;anchorLon=lon;}
    return result;
  }
  Position upcoming(double lat,double lon,Position vehicle,double behind,double ahead){
    if(vehicle==null||!Double.isFinite(behind)||!Double.isFinite(ahead)||behind<0||ahead<=0)return null;
    return measure(lat,lon,segmentAt(Math.max(0,vehicle.along-behind)),segmentAt(Math.min(total,vehicle.along+ahead)));
  }
  /** Compare with the local route tangent, including either side of a nearby junction.
   * This detects opposite travel; it cannot prove membership of a parallel carriageway. */
  boolean directionCompatible(Position vehicle,double course){
    if(vehicle==null||!Double.isFinite(course)||course<0||course>=360||vehicle.segment<0||vehicle.segment>=points.size()-1)return false;
    for(int segment=Math.max(0,vehicle.segment-1);segment<=Math.min(points.size()-2,vehicle.segment+1);segment++){
      if(segment<vehicle.segment&&vehicle.along-cumulative[vehicle.segment]>30)continue;
      if(segment>vehicle.segment&&cumulative[vehicle.segment+1]-vehicle.along>30)continue;
      if(cumulative[segment+1]-cumulative[segment]<1)continue;
      double[] a=points.get(segment),b=points.get(segment+1);
      double la=Math.toRadians(a[0]),lb=Math.toRadians(b[0]),dl=Math.toRadians(b[1]-a[1]);
      double heading=(Math.toDegrees(Math.atan2(Math.sin(dl)*Math.cos(lb),Math.cos(la)*Math.sin(lb)-Math.sin(la)*Math.cos(lb)*Math.cos(dl)))+360)%360;
      if(Math.abs((heading-course+540)%360-180)<=75)return true;
    }
    return false;
  }
  private int segmentAt(double along){
    if(cumulative.length<2)return 0;
    int index=java.util.Arrays.binarySearch(cumulative,along);
    if(index<0)index=-index-2;
    return Math.max(0,Math.min(cumulative.length-2,index));
  }
  double toStep(Position p,int index){return index<0||index>=cumulative.length?Double.NaN:Math.max(0,cumulative[index]-p.along);}
  double remainingSeconds(Position p,double routeSeconds){return total>0?Math.max(0,routeSeconds*Math.min(1,p.remaining/total)):0;}
  static boolean departure(String instruction){return normalized(instruction).startsWith("depart");}
  static String glyph(String instruction){String t=normalized(instruction);if(t.contains("arrive"))return "⚑";if(t.contains("demi-tour")||t.contains("demi tour"))return "↶";if(t.contains("rond-point"))return "⟳";if(t.contains("gauche"))return "↰";if(t.contains("droite"))return "↱";return "↑";}
  private static String normalized(String s){return java.text.Normalizer.normalize(s==null?"":s,java.text.Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.FRANCE);}
  private static double meters(double[] a,double[] b){double la=Math.toRadians(b[0]-a[0]),lo=Math.toRadians(b[1]-a[1]);double h=Math.sin(la/2)*Math.sin(la/2)+Math.cos(Math.toRadians(a[0]))*Math.cos(Math.toRadians(b[0]))*Math.sin(lo/2)*Math.sin(lo/2);return 6371000*2*Math.atan2(Math.sqrt(h),Math.sqrt(Math.max(0,1-h)));}
}
