package fr.roadguard.app;
import java.util.*;
public final class TestRouteContinuity {
 static int checks;
 static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
 public static void main(String[] args){
  List<double[]> loop=Arrays.asList(new double[]{0,0},new double[]{0,.01},new double[]{.01,.01},new double[]{.01,0},new double[]{.00005,0},new double[]{.00005,.01},new double[]{.02,.01});
  NavigationProgress progress=new NavigationProgress(loop);
  NavigationProgress.Position first=progress.track(0,.002,0,1000,12,5);
  check(first.segment==0,"initial first leg");
  check(progress.measure(.000049,.00205,0).segment==4,"fixture must reproduce the future parallel-leg snap");
  NavigationProgress.Position continuous=progress.track(.000049,.00205,0,2000,12,5);
  check(continuous.segment==0&&continuous.along-first.along<100,"continuous fix skipped kilometres of the loop");
  check(!continuous.arrived(),"loop proximity is not an arrival");
  check(progress.track(.000049,.00205,0,2000,12,5)==continuous,"same GNSS measurement advanced twice");
  check(progress.track(0,.003,0,1999,12,5)==null,"older measurement accepted");
  check(progress.track(0,.003,0,2000,12,5)==null,"same timestamp with different coordinates accepted");
  NavigationProgress.Position outside=progress.track(.01,.009,0,3000,12,5);
  check(outside.lateral>100&&!outside.arrived(),"far off-route jump silently advanced cursor");
  NavigationProgress.Position returned=progress.track(0,.0021,0,4000,12,5);check(returned.segment==0,"off-route result poisoned the progress anchor");
  NavigationProgress.Position reacquired=progress.track(.00005,.008,0,40001,12,5);check(reacquired.segment==4,"long interruption prevented legitimate reacquisition");
  check(progress.track(Double.NaN,0,0,50000,12,5)==null,"invalid GPS invented a route match");
  NavigationProgress restored=new NavigationProgress(loop);check(restored.track(.00005,.005,4,5000,12,5).segment>=4,"restored cursor returned to departure");
  List<double[]> bend=Arrays.asList(new double[]{0,0},new double[]{0,.01},new double[]{.01,.01},new double[]{.01,0},new double[]{.0001,0});
  NavigationProgress model=new NavigationProgress(bend);NavigationProgress.Position vehicle=model.measure(0,0,0),radar=model.upcoming(.0001,0,vehicle,35,5500);
  double road=RadarApproachFilter.routeDistance(RadarApproachFilter.projection(vehicle),RadarApproachFilter.projection(radar),35);
  check(road>4000&&road<5000,"curved-route distance became near straight-line distance");
  check(road>1000,"nearby return-leg radar must not enter the 1 km alert range yet");
  check(Double.isNaN(RadarApproachFilter.routeDistance(new RadarApproachFilter.Projection(0,500),new RadarApproachFilter.Projection(0,400),35)),"behind radar accepted");
  check(Double.isNaN(RadarApproachFilter.routeDistance(new RadarApproachFilter.Projection(0,0),new RadarApproachFilter.Projection(100,500),35)),"parallel road outside corridor accepted");
  RouteRequestPolicy.RouteMatch crossing=RouteRequestPolicy.matchPolyline(0,180,Arrays.asList(new double[]{0,179.9},new double[]{0,-179.9}),0);
  check(crossing.lateralMeters<1,"antimeridian route lost");
  RouteRequestPolicy.RouteMatch greenwich=RouteRequestPolicy.matchPolyline(0,0,Arrays.asList(new double[]{0,179.9},new double[]{0,-179.9}),0);
  check(greenwich.lateralMeters>10000000,"antimeridian segment falsely crossed Greenwich");
  check(RouteRequestPolicy.matchPolyline(0,0,loop,3,2)==null,"inverted segment range accepted");
  check(RouteRequestPolicy.matchPolyline(0,0,Collections.emptyList(),0)==null,"empty route accepted");
  NavigationProgress.Position atEnd=model.measure(.0001,0,3);check(atEnd.arrived(),"actual final arrival lost");
  check(model.toStep(vehicle,1)>1000&&model.remainingSeconds(vehicle,600)==600,"route distance and ETA inconsistent");
  System.out.println("PASS route continuity: "+checks+" checks, real projection/tracker/corridor classes; no GPS hardware");
 }
}
