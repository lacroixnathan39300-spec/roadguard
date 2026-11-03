package fr.roadguard.app;

/** Motion is separate from position: a coarse network fix cannot erase GNSS speed/course. */
final class GpsMotion {
  static final long SPEED_TTL_MS=6000, COURSE_TTL_MS=15000;
  private long speedAt=-1,courseAt=-1,gnssAt=-1,lastAt=-1,zeroCandidateAt=-1;
  private double kmh=Double.NaN,course=Double.NaN;
  boolean accept(long at,boolean gnss,double accuracy,double speedMps,double speedAccuracy,double bearing,double bearingAccuracy){
    if(at<0||at<=lastAt||!Double.isFinite(accuracy)||accuracy<0||accuracy>2000)return false;
    // Prefer the satellite source while fresh; interleaved coarse fixes otherwise make the map jump.
    if(!gnss&&gnssAt>=0&&at-gnssAt<5000)return false;
    lastAt=at;if(gnss)gnssAt=at;
    boolean validSpeed=gnss&&accuracy<=50&&Double.isFinite(speedMps)&&speedMps>=0&&speedMps<=110
        &&(!Double.isFinite(speedAccuracy)||speedAccuracy<=3);
    if(validSpeed){
      double next=speedMps*3.6;
      // One zero during motion can be a receiver dropout. Confirm a stop on a second fix.
      boolean confirmZero=next<1&&Double.isFinite(kmh)&&kmh>=8&&speedAt>=0&&at-speedAt<=SPEED_TTL_MS;
      if(confirmZero){
        if(zeroCandidateAt<0||at-zeroCandidateAt>4000)zeroCandidateAt=at;
        else if(at-zeroCandidateAt>=700){kmh=0;speedAt=at;zeroCandidateAt=-1;}
      }else{kmh=next;speedAt=at;zeroCandidateAt=-1;}
    }
    if(gnss&&validSpeed&&speedMps*3.6>=10&&accuracy<=35&&Double.isFinite(bearing)
        &&(!Double.isFinite(bearingAccuracy)||bearingAccuracy<=35)){
      course=((bearing%360)+360)%360;courseAt=at;
    }
    return true;
  }
  double speed(long now){return speedAt>=0&&now>=speedAt&&now-speedAt<=SPEED_TTL_MS?kmh:Double.NaN;}
  double speedMetersPerSecond(long now){return speed(now)/3.6;}
  double course(){return course;}
  boolean courseReliable(long now){return courseAt>=0&&now>=courseAt&&now-courseAt<=COURSE_TTL_MS;}
  void invalidateMotion(){speedAt=-1;courseAt=-1;gnssAt=-1;zeroCandidateAt=-1;kmh=Double.NaN;course=Double.NaN;}
  void resetCourse(){course=Double.NaN;courseAt=-1;}
  static double displaySpeed(double raw,int offset){
    if(!Double.isFinite(raw)||raw<0)return Double.NaN;
    // A stopped vehicle stays at zero, including with positive compensation.
    return raw<1?0:Math.max(0,raw+Math.max(-15,Math.min(15,offset)));
  }
}
