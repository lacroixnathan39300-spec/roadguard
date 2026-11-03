package fr.roadguard.app;

import java.util.List;

/** Conservative geometric approximation. A GPS bearing never proves road or lane membership. */
final class RadarApproachFilter {
  private RadarApproachFilter(){}

  static boolean ahead(double meters,double bearingToPoint,double travelBearing,double accuracy){
    if(!Double.isFinite(meters)||meters<0||!Double.isFinite(bearingToPoint)||!Double.isFinite(travelBearing))return false;
    if(meters<=Math.max(60,accuracy*2))return true; // GPS uncertainty at close range
    double delta=Math.abs((bearingToPoint-travelBearing+540)%360-180);
    if(delta>70)return false;
    double lateral=Math.abs(Math.sin(Math.toRadians(delta))*meters);
    return lateral<=Math.max(100,accuracy*2)+meters*.10;
  }

  static boolean usesCorridor(NavigationProgress model,NavigationProgress.Position vehicle,double accuracy,double width,boolean forward,boolean reliableCourse,double course){
    return model!=null&&vehicle!=null&&Double.isFinite(accuracy)&&accuracy>0&&accuracy<=30&&Double.isFinite(width)&&width>0
      &&vehicle.lateral<=Math.max(65,width+10)&&(!forward||reliableCourse&&model.directionCompatible(vehicle,course));
  }

  static final class Projection {
    final double offset,along;
    Projection(double offset,double along){this.offset=offset;this.along=along;}
  }
  static Projection onRoute(List<double[]> route,double lat,double lon){
    RouteRequestPolicy.RouteMatch match=RouteRequestPolicy.matchPolyline(lat,lon,route,0);
    return match==null?new Projection(Double.MAX_VALUE,0):new Projection(match.lateralMeters,match.alongMeters);
  }
  static Projection projection(NavigationProgress.Position position){
    return position==null?null:new Projection(position.lateral,position.along);
  }
  static double routeDistance(Projection vehicle,Projection radar,double corridor){
    return onUpcomingRoute(vehicle,radar,corridor)?Math.max(0,radar.along-vehicle.along):Double.NaN;
  }
  static boolean onUpcomingRoute(Projection vehicle,Projection radar,double corridor){
    return vehicle!=null&&radar!=null&&Double.isFinite(corridor)&&corridor>0&&vehicle.offset<=Math.max(65,corridor+10)&&radar.offset<=corridor
      &&radar.along>=vehicle.along-35&&radar.along<=vehicle.along+5500;
  }
}
