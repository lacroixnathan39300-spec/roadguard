package fr.roadguard.app;

import java.util.Arrays;
import java.util.List;

public final class RadarApproachFilterProbe {
  private static void check(boolean value,String reason){if(!value)throw new AssertionError(reason);}
  public static void main(String[] args){
    check(RadarApproachFilter.ahead(500,0,0,10),"point ahead");
    check(!RadarApproachFilter.ahead(500,180,0,10),"point behind");
    check(!RadarApproachFilter.ahead(500,90,0,10),"point on side road");
    check(!RadarApproachFilter.ahead(Double.NaN,0,0,10),"invalid coordinate");
    List<double[]> route=Arrays.asList(new double[]{46.0000,5.0000},new double[]{46.0100,5.0000},new double[]{46.0200,5.0000});
    RadarApproachFilter.Projection car=RadarApproachFilter.onRoute(route,46.0080,5.0000);
    RadarApproachFilter.Projection upcoming=RadarApproachFilter.onRoute(route,46.0150,5.0000);
    RadarApproachFilter.Projection behind=RadarApproachFilter.onRoute(route,46.0030,5.0000);
    RadarApproachFilter.Projection parallel=RadarApproachFilter.onRoute(route,46.0150,5.0020);
    check(RadarApproachFilter.onUpcomingRoute(car,upcoming,55),"upcoming radar on route");
    check(!RadarApproachFilter.onUpcomingRoute(car,behind,55),"old radar behind car");
    check(!RadarApproachFilter.onUpcomingRoute(car,parallel,55),"radar off route");
    for(double latitude:new double[]{0,46,60})for(double slope:new double[]{-1,.25,1}){
      List<double[]> diagonal=Arrays.asList(new double[]{latitude,5},new double[]{latitude+.01,5+.01*slope});
      RadarApproachFilter.Projection middle=RadarApproachFilter.onRoute(diagonal,latitude+.005,5+.005*slope);
      check(middle.offset<.01,"on-segment projection must not drift with latitude or diagonal bearing");
    }
    List<double[]> crossing=Arrays.asList(new double[]{0,179.99},new double[]{0,-179.99});
    check(RadarApproachFilter.onRoute(crossing,0,180).offset<.01,"antimeridian route");
    check(!RadarApproachFilter.onUpcomingRoute(null,upcoming,55),"missing vehicle projection");
    check(!RadarApproachFilter.onUpcomingRoute(car,upcoming,Double.NaN),"invalid corridor");
    System.out.println("Radar approach geometry OK: forward/behind/parallel roads, diagonal routes at three latitudes, antimeridian and invalid projections");
  }
}
