package fr.roadguard.app;

import java.net.URI;
import java.util.List;

/** Small, testable guardrails for ROADGUARD's demo online route request. */
public final class RouteRequestPolicy {
  public static final int MAX_ROUTE_POINTS=15000;
  public static final double MAX_ROUTE_METERS=5_000_000.0;
  public static final double MAX_ROUTE_SECONDS=7*24*3600.0;
  private RouteRequestPolicy() {}
  public static final class RouteMatch {
    public final int segmentIndex;
    public final double fraction,lateralMeters,alongMeters;
    RouteMatch(int segmentIndex,double fraction,double lateralMeters,double alongMeters){this.segmentIndex=segmentIndex;this.fraction=fraction;this.lateralMeters=lateralMeters;this.alongMeters=alongMeters;}
  }
  public static String osrmUrl(double fromLat,double fromLon,double toLat,double toLon){
    if(!coordinate(fromLat,fromLon)||!coordinate(toLat,toLon))throw new IllegalArgumentException("Invalid route coordinates");
    return String.format(java.util.Locale.US,
      "https://router.project-osrm.org/route/v1/driving/%.6f,%.6f;%.6f,%.6f?overview=full&geometries=geojson&steps=true&alternatives=3",
      fromLon,fromLat,toLon,toLat);
  }
  public static boolean isSafeResponse(int status,String contentType,long contentLength){
    if(status!=200||contentType==null||contentLength>1_500_000L)return false;
    String type=contentType.toLowerCase(java.util.Locale.ROOT);
    return type.startsWith("application/json")||type.startsWith("application/vnd.api+json");
  }
  public static boolean responseWithinLimit(int declaredLength,long observedBytes){
    return declaredLength>=-1&&declaredLength<=1_500_000&&observedBytes>=0&&observedBytes<=1_500_000;
  }
  public static boolean isSafeEndpoint(String value){
    try{
      URI uri=new URI(value);
      return "https".equalsIgnoreCase(uri.getScheme())&&
        "router.project-osrm.org".equalsIgnoreCase(uri.getHost())&&
        uri.getUserInfo()==null&&uri.getFragment()==null&&uri.getQuery()==null;
    }catch(Exception ignored){return false;}
  }
  public static boolean isSafeRouteRequest(String value){
    try{
      URI uri=new URI(value);
      return "https".equalsIgnoreCase(uri.getScheme())&&"router.project-osrm.org".equalsIgnoreCase(uri.getHost())
        &&(uri.getPort()==-1||uri.getPort()==443)&&uri.getUserInfo()==null&&uri.getFragment()==null
        &&uri.getRawPath()!=null&&uri.getRawPath().startsWith("/route/v1/driving/")&&uri.getRawPath().indexOf('%')<0;
    }catch(Exception invalid){return false;}
  }
  public static boolean isValidRouteSummary(double meters,double seconds,int coordinateCount){
    return Double.isFinite(meters)&&Double.isFinite(seconds)&&meters>0&&meters<=MAX_ROUTE_METERS&&
      seconds>0&&seconds<=MAX_ROUTE_SECONDS&&coordinateCount>=2&&coordinateCount<=MAX_ROUTE_POINTS;
  }
  public static boolean coordinate(double lat,double lon){
    return Double.isFinite(lat)&&Double.isFinite(lon)&&Math.abs(lat)<=90&&Math.abs(lon)<=180;
  }
  /** Perpendicular distance from a GPS fix to the closest segment of a [lat, lon] route polyline. */
  public static double distanceToPolylineMeters(double lat,double lon,List<double[]> points){
    RouteMatch match=matchPolyline(lat,lon,points,0);
    return match==null?Double.POSITIVE_INFINITY:match.lateralMeters;
  }
  /** Match a fix to the closest route segment at or after a bounded progress cursor. */
  public static RouteMatch matchPolyline(double lat,double lon,List<double[]> points,int earliestSegment){
    return matchPolyline(lat,lon,points,earliestSegment,points==null?-1:points.size()-2);
  }
  /** Inclusive segment window, used to avoid snapping across a future loop of the route. */
  public static RouteMatch matchPolyline(double lat,double lon,List<double[]> points,int earliestSegment,int latestSegment){
    if(!coordinate(lat,lon)||points==null||points.size()<2||latestSegment<0)return null;
    double northScale=110540.0,eastScale=111320.0*Math.max(0.01,Math.cos(Math.toRadians(lat)));
    double nearest=Double.POSITIVE_INFINITY,along=0,cumulative=0;int matchIndex=-1;double matchFraction=0;
    int first=Math.max(0,Math.min(points.size()-2,earliestSegment));
    int last=Math.min(points.size()-2,latestSegment);if(last<first)return null;
    for(int i=1;i<=last+1;i++){
      double[] a=points.get(i-1),b=points.get(i);
      if(a==null||b==null||a.length<2||b.length<2||!coordinate(a[0],a[1])||!coordinate(b[0],b[1]))continue;
      double ax=wrappedLongitudeDelta(a[1],lon)*eastScale,ay=(a[0]-lat)*northScale;
      double bx=ax+wrappedLongitudeDelta(b[1],a[1])*eastScale,by=(b[0]-lat)*northScale;
      double dx=bx-ax,dy=by-ay,denominator=dx*dx+dy*dy;
      double t=denominator==0?0:Math.max(0,Math.min(1,-(ax*dx+ay*dy)/denominator));
      double segmentLength=Math.sqrt(denominator),distance=Math.hypot(ax+t*dx,ay+t*dy);
      if(i-1>=first&&distance<nearest){nearest=distance;matchIndex=i-1;matchFraction=t;along=cumulative+t*segmentLength;}
      cumulative+=segmentLength;
    }
    return matchIndex<0?null:new RouteMatch(matchIndex,matchFraction,nearest,along);
  }
  private static double wrappedLongitudeDelta(double longitude,double reference){
    double delta=(longitude-reference)%360.0;
    if(delta>180)delta-=360;if(delta<-180)delta+=360;
    return delta;
  }
}
