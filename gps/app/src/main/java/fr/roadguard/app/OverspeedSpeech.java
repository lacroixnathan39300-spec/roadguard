package fr.roadguard.app;
import android.content.Context;import android.content.SharedPreferences;import android.speech.tts.TextToSpeech;
/** Optional known-limit warning, shared between foreground/background owners and rate limited. */
final class OverspeedSpeech {
 static boolean eligible(double distance,double speed,int limit){return Double.isFinite(distance)&&distance>=0&&distance<=1000&&RadarAlertPolicy.over(speed,limit);}
 static synchronized boolean warn(Context c,AlertSpeechTracker tracker,TextToSpeech tts,boolean ready,double distance,double speed,int limit,long now){
  SharedPreferences p=c.getSharedPreferences("MainActivity",0);if(tracker.hasPending()||tracker.hasOther()||!ready||tts==null||!p.getBoolean("Alertes vocales",true)||!p.getBoolean("overspeed_voice",true)||!eligible(distance,speed,limit))return false;
  SharedPreferences ledger=c.getSharedPreferences("roadguard_radar_audio",0);long at=ledger.getLong("overspeed_spoken_at",0);if(at>0&&now>=at&&now-at<30000)return false;
  ledger.edit().putLong("overspeed_spoken_at",now).apply();
  return tracker.speakOther(tts,"Dépassement de vitesse. Limitation à "+limit+" kilomètres par heure.",GuidanceAudioPolicy.HAZARD,()->{synchronized(OverspeedSpeech.class){if(ledger.getLong("overspeed_spoken_at",0)==now)ledger.edit().remove("overspeed_spoken_at").apply();}});
 }
 private OverspeedSpeech(){}
}
