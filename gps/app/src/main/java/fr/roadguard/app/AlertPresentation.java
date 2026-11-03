package fr.roadguard.app;

import android.location.Location;
import java.util.Locale;

/** One timestamp/precision and distance policy for phone, service and overlay. */
final class AlertPresentation {
  static final long MAX_FIX_AGE_MS=15000L;
  private AlertPresentation(){}
  static boolean fresh(Location fix,long wallTime,long elapsedNanos){
    if(fix==null||!AlertMilestones.validCoordinates(fix.getLatitude(),fix.getLongitude())
        ||!fix.hasAccuracy()||!Float.isFinite(fix.getAccuracy())||fix.getAccuracy()<=0||fix.getAccuracy()>60)return false;
    long wallAge=wallTime-fix.getTime();
    if(wallAge < -5000 || wallAge > MAX_FIX_AGE_MS)return false;
    // Android supplies a monotonic measurement timestamp; reject stale/future fixes
    // even if the wall clock has changed. Zero is tolerated for legacy imports/tests.
    long measured=fix.getElapsedRealtimeNanos();
    return measured<=0 || (elapsedNanos>=measured && elapsedNanos-measured<=MAX_FIX_AGE_MS*1000000L);
  }
  static int roundedMeters(double meters){
    if(!Double.isFinite(meters)||meters<0)return -1;
    int step=meters<100?10:50;
    return Math.max(0,(int)Math.round(Math.min(meters,1000000)/step)*step);
  }
  static String text(double meters){
    int rounded=roundedMeters(meters);
    return rounded<0?"—":rounded>=1000?String.format(Locale.FRANCE,"%.2f km",rounded/1000d):rounded+" m";
  }
  static String voice(double meters){
    int rounded=roundedMeters(meters);
    // Speak metres throughout the 1 km radar alert range: exactly the displayed value.
    return rounded<0?"distance indisponible":rounded+(rounded==1?" mètre":" mètres");
  }
  static boolean nearby(double meters,boolean radar,boolean visible){
    return Double.isFinite(meters)&&meters>=0&&meters<=(radar?(visible?1050:1000):(visible?280:250));
  }
}
