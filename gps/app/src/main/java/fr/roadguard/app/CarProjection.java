package fr.roadguard.app;
final class CarProjection {
  static double x(double lon,double world){return (lon+180)/360*world;}
  static double y(double lat,double world){double a=Math.toRadians(Math.max(-85.05112878,Math.min(85.05112878,lat)));return (1-Math.log(Math.tan(a)+1/Math.cos(a))/Math.PI)/2*world;}
  static double deltaX(double x,double center,double world){double d=x-center;return d-Math.floor((d+world/2)/world)*world;}
  private CarProjection(){}
}
