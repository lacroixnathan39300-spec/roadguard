package fr.roadguard.app;

/** Advance radar alerts by the measured installation lag without moving map markers. */
final class RadarLeadDistance {
  static final double METERS=20.0;
  private RadarLeadDistance(){}
  static double effective(double raw,double bearingToRadar,double course,boolean reliableCourse){
    if(!Double.isFinite(raw)||raw<0)return Double.NaN;
    if(!reliableCourse||!Double.isFinite(bearingToRadar)||!Double.isFinite(course))return Math.max(0,raw-METERS);
    double delta=Math.toRadians(bearingToRadar-course);
    double forward=raw*Math.cos(delta)-METERS;
    if(forward<0)return -1;
    return Math.hypot(forward,raw*Math.sin(delta));
  }
  static double onRoute(double raw){
    if(!Double.isFinite(raw)||raw<0)return Double.NaN;
    return raw<=METERS?-1:raw-METERS;
  }
}
