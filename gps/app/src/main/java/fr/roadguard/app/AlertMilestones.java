package fr.roadguard.app;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Locale;

/** Shares radar warning milestones across foreground/background GPS owners. */
final class AlertMilestones {
  static final int NONE=0,FIRST=1,FOLLOWUP=2;
  private static final long RESET_MS=30L*60L*1000L;
  private static final String STORE="roadguard_alert_milestones";
  private AlertMilestones(){}
  /** Latitude/longitude must be finite and within WGS84 geographic bounds. */
  static boolean validCoordinates(double lat,double lon){
    return !Double.isNaN(lat)&&!Double.isInfinite(lat)&&lat>=-90d&&lat<=90d
      &&!Double.isNaN(lon)&&!Double.isInfinite(lon)&&lon>=-180d&&lon<=180d;
  }
  static String pointKey(double lat,double lon){
    return String.format(Locale.US,"%.5f:%.5f",lat,lon);
  }
  /** Non-mutating check: a second nearby radar can still be announced. */
  static synchronized boolean pending(Context context,double lat,double lon,int stage){return pending(context,lat,lon,"",stage);}
  static synchronized boolean pending(Context context,double lat,double lon,String type,int stage){
    if(stage<=0||!validCoordinates(lat,lon))return false;
    SharedPreferences prefs=context.getSharedPreferences(STORE,Context.MODE_PRIVATE);
    String key=type==null||type.isEmpty()?pointKey(lat,lon):RadarIdentity.point(lat,lon,type);
    long now=System.currentTimeMillis();
    long previousTime=prefs.getLong(key+"_time",0L);
    int previousStage=prefs.getInt(key+"_stage",0);
    return !(previousTime>0&&now>=previousTime&&now-previousTime<RESET_MS&&stage<=previousStage);
  }
  /** Return 0 for invalid GPS distances; no milestone should be claimed for them. */
  static int stage(boolean radar,float meters){
    if(Float.isNaN(meters)||Float.isInfinite(meters)||meters<0f||meters>(radar?1000:700))return 0;
    // A radar has one spoken approach announcement. Proximity is then conveyed
    // by the accelerating tones instead of repeated distance speech.
    return radar?1:(meters<=250?2:1);
  }
  static synchronized boolean claim(Context context,double lat,double lon,int stage){return claim(context,lat,lon,"",stage);}
  static synchronized boolean claim(Context context,double lat,double lon,String type,int stage){
    return claimKind(context,lat,lon,type,stage)!=NONE;
  }
  /** Atomically claim a milestone and identify the first warning in this approach. */
  static synchronized int claimKind(Context context,double lat,double lon,String type,int stage){
    Claim claim=reserve(context,lat,lon,type,stage);return claim==null?NONE:claim.kind;
  }
  static final class Claim {
    final String key;final int kind,stage,previousStage;final long time,previousTime;boolean released;
    Claim(String key,int kind,int stage,long time,int previousStage,long previousTime){this.key=key;this.kind=kind;this.stage=stage;this.time=time;this.previousStage=previousStage;this.previousTime=previousTime;}
  }
  /** Reserve before enqueueing speech; release if it fails or is interrupted. */
  static synchronized Claim reserve(Context context,double lat,double lon,String type,int stage){
    if(stage<=0||stage>9||!validCoordinates(lat,lon))return null;
    SharedPreferences prefs=context.getSharedPreferences(STORE,Context.MODE_PRIVATE);
    String key=type==null||type.isEmpty()?pointKey(lat,lon):RadarIdentity.point(lat,lon,type);
    long now=System.currentTimeMillis(),previousTime=prefs.getLong(key+"_time",0L);int previousStage=prefs.getInt(key+"_stage",0);
    boolean recent=previousTime>0&&now>=previousTime&&now-previousTime<RESET_MS;
    if(recent&&stage<=previousStage)return null;
    if(!prefs.edit().putLong(key+"_time",now).putInt(key+"_stage",stage).commit())return null;
    return new Claim(key,recent?FOLLOWUP:FIRST,stage,now,previousStage,previousTime);
  }
  static synchronized void release(Context context,Claim claim){
    if(claim==null||claim.released)return;claim.released=true;
    SharedPreferences prefs=context.getSharedPreferences(STORE,Context.MODE_PRIVATE);
    // Never roll back another owner's newer warning.
    if(prefs.getLong(claim.key+"_time",0L)!=claim.time||prefs.getInt(claim.key+"_stage",0)!=claim.stage)return;
    prefs.edit().putLong(claim.key+"_time",claim.previousTime).putInt(claim.key+"_stage",claim.previousStage).commit();
  }
  private static long lastRearmAt;
  static void rearmDistant(Context context,double lat,double lon){rearmDistant(context,lat,lon,System.currentTimeMillis());}
  static synchronized void rearmDistant(Context context,double lat,double lon,long now){
    if(!validCoordinates(lat,lon)||now<=0)return;
    if(now>=lastRearmAt&&now-lastRearmAt<10000)return;lastRearmAt=now;
    SharedPreferences prefs=context.getSharedPreferences(STORE,Context.MODE_PRIVATE);SharedPreferences.Editor edit=null;
    for(java.util.Map.Entry<String,?> entry:prefs.getAll().entrySet()){
      String key=entry.getKey();if(!key.endsWith("_stage")||!(entry.getValue() instanceof Integer)||((Integer)entry.getValue())<=0)continue;
      String point=key.substring(0,key.length()-6);String[] parts=point.split(":",3);if(parts.length<2)continue;
      try{double a=Double.parseDouble(parts[0]),b=Double.parseDouble(parts[1]);if(!validCoordinates(a,b))continue;
        double x=Math.toRadians(a-lat),y=Math.toRadians(b-lon);double h=Math.sin(x/2)*Math.sin(x/2)+Math.cos(Math.toRadians(lat))*Math.cos(Math.toRadians(a))*Math.sin(y/2)*Math.sin(y/2);
        double meters=6371000*2*Math.atan2(Math.sqrt(Math.max(0,Math.min(1,h))),Math.sqrt(Math.max(0,1-h)));
        long time=prefs.getLong(point+"_time",0L);
        if(meters>1500||(time>0&&now>=time&&now-time>=RESET_MS)){if(edit==null)edit=prefs.edit();edit.putLong(point+"_time",0L).putInt(key,0);}
      }catch(NumberFormatException ignored){}
    }
    if(edit!=null)edit.apply();
  }
}
