package fr.roadguard.app;
import java.util.*;
public final class TestRouteDirection {
 static int checks;
 static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
 public static void main(String[] args){
  NavigationProgress east=new NavigationProgress(Arrays.asList(new double[]{0,0},new double[]{0,.01},new double[]{.01,.01}));
  NavigationProgress.Position middle=east.measure(0,.005,0);
  check(east.directionCompatible(middle,90),"east rejected");check(!east.directionCompatible(middle,270),"opposite carriageway accepted");check(!east.directionCompatible(middle,0),"perpendicular travel accepted far from turn");
  NavigationProgress.Position corner=east.measure(0,.00999,0);
  check(east.directionCompatible(corner,0),"next turn tangent rejected near junction");check(east.directionCompatible(corner,90),"current tangent rejected near junction");
  NavigationProgress.Position after=east.measure(.00001,.01,1);check(east.directionCompatible(after,90)&&east.directionCompatible(after,0),"previous tangent lost just after junction");
  NavigationProgress.Position beyond=east.measure(.001,.01,1);check(!east.directionCompatible(beyond,90),"previous tangent leaked past junction allowance");
  check(RadarApproachFilter.usesCorridor(east,middle,5,35,true,true,90),"valid route corridor rejected");
  check(!RadarApproachFilter.usesCorridor(east,middle,5,35,true,true,270),"opposite route disabled directional fallback");
  check(!RadarApproachFilter.usesCorridor(east,middle,5,35,true,false,90),"unknown course accepted with directional filter");
  check(RadarApproachFilter.usesCorridor(east,middle,5,35,false,false,Double.NaN),"explicit nondirectional option ignored");
  for(double invalid:new double[]{0,-1,31,Double.NaN,Double.POSITIVE_INFINITY})check(!RadarApproachFilter.usesCorridor(east,middle,invalid,35,true,true,90),"invalid GPS precision qualified route");
  check(!RadarApproachFilter.usesCorridor(east,east.measure(.01,.005,0),5,35,true,true,90),"distant parallel road qualified route");
  check(!RadarApproachFilter.usesCorridor(null,middle,5,35,true,true,90),"missing route accepted");check(!east.directionCompatible(null,90),"missing match accepted");
  for(double invalid:new double[]{-1,360,Double.NaN,Double.POSITIVE_INFINITY})check(!east.directionCompatible(middle,invalid),"invalid heading accepted");
  NavigationProgress north=new NavigationProgress(Arrays.asList(new double[]{0,0},new double[]{.01,0}));check(north.directionCompatible(north.measure(.005,0,0),359),"north wraparound failed");
  NavigationProgress dateline=new NavigationProgress(Arrays.asList(new double[]{0,179.9},new double[]{0,-179.9}));check(dateline.directionCompatible(dateline.measure(0,180,0),90),"dateline direction reversed");
  NavigationProgress duplicates=new NavigationProgress(Arrays.asList(new double[]{0,0},new double[]{0,0},new double[]{0,.01}));check(duplicates.directionCompatible(duplicates.measure(0,0,0),90),"duplicate vertex masked real tangent");
  NavigationProgress empty=new NavigationProgress(Arrays.asList(new double[]{0,0},new double[]{0,0}));check(!empty.directionCompatible(empty.measure(0,0,0),90),"zero-length route invented direction");
  System.out.println("PASS route direction: "+checks+" checks; local tangent/corners/wraparound/quality and opposite-travel fallback, no road identity inferred");
 }
}
