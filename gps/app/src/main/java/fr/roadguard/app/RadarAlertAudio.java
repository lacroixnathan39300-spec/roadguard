package fr.roadguard.app;

import android.content.*;
import android.media.*;
import android.os.*;

/** Repeated proximity tones with one shared rate limit across GPS owners. */
final class RadarAlertAudio {
  static final int NONE=0, QUIET=1, LOUD=2;
  static final long QUIET_INTERVAL_MS=1300L, LOUD_INTERVAL_MS=900L;
  static synchronized int claim(Context c,double lat,double lon,double meters,double speed,int limit,long now){
    if(!AlertMilestones.validCoordinates(lat,lon)||RadarAlertPolicy.stage(meters)==0)return NONE;
    SharedPreferences p=c.getSharedPreferences("roadguard_radar_audio",0);
    boolean over=RadarAlertPolicy.over(speed,limit);
    int level=over?LOUD:QUIET,previous=p.getInt("last_level",NONE);
    long at=p.getLong("last_at",0L),interval=RadarProximityCadence.interval(meters);
    if(at>0&&now>=at&&now-at<interval&&!(over&&previous!=LOUD))return NONE;
    p.edit().putLong("last_at",now).putInt("last_level",level).apply();
    return level;
  }
  private final Handler handler=new Handler(Looper.getMainLooper());
  private ToneGenerator tone;
  private GuidanceAudioPolicy.Lease audioLease;
  private long toneGeneration;private boolean continuousTone;private int playingLevel;
  private Context currentContext;
  private double latitude,longitude,distance,speed;
  private int speedLimit;
  private long lastFixElapsed;
  private boolean heartbeatScheduled;
  private final Runnable heartbeat=new Runnable(){@Override public void run(){
    heartbeatScheduled=false;
    if(currentContext==null)return;
    if(SystemClock.elapsedRealtime()-lastFixElapsed>5000L||!currentContext.getSharedPreferences("MainActivity",0).getBoolean("danger_beeps",true)){stop();return;}
    soundTick();
    scheduleHeartbeat();
  }};
  private void scheduleHeartbeat(){if(!heartbeatScheduled){heartbeatScheduled=true;handler.postDelayed(heartbeat,100L);}}
  private void soundTick(){boolean continuous=RadarProximityCadence.continuous(distance);int wanted=RadarAlertPolicy.over(speed,speedLimit)?LOUD:QUIET;
   if(continuousTone&&(!continuous||wanted!=playingLevel||audioLease==null||!audioLease.active()))releaseTone();if(continuousTone)return;
   if(continuous)playTone(wanted,true);else play(claim(currentContext,latitude,longitude,distance,speed,speedLimit,System.currentTimeMillis()));}
  void play(int level){if(level!=NONE)playTone(level,false);}
  private void playTone(int level,boolean continuous){if(currentContext==null)return;GuidanceAudioPolicy.Lease lease=GuidanceAudio.get(currentContext).policy.acquire(GuidanceAudioPolicy.BEEP,this::releaseTone);if(lease==null)return;audioLease=lease;final long generation=++toneGeneration;
   try{tone=new ToneGenerator(AudioManager.STREAM_MUSIC,level==LOUD?RadarAlertPolicy.LOUD_VOLUME:RadarAlertPolicy.QUIET_VOLUME);playingLevel=level;continuousTone=continuous;int pitch=level==LOUD?ToneGenerator.TONE_DTMF_D:ToneGenerator.TONE_DTMF_1;
    if(continuous)tone.startTone(pitch);else{tone.startTone(pitch,RadarProximityCadence.pulse(distance));handler.postDelayed(()->{if(generation==toneGeneration)releaseTone();},RadarProximityCadence.pulse(distance)+25L);}
   }catch(RuntimeException unavailable){releaseTone();}}
  void update(Context c,double lat,double lon,double meters,double speed,int limit,boolean handoffReady){
    if(!handoffReady||!AlertMilestones.validCoordinates(lat,lon)||!c.getSharedPreferences("MainActivity",0).getBoolean("danger_beeps",true)||RadarAlertPolicy.stage(meters)==0){stop();return;}
    currentContext=c.getApplicationContext();latitude=lat;longitude=lon;distance=meters;this.speed=speed;speedLimit=limit;
    lastFixElapsed=SystemClock.elapsedRealtime();
    soundTick();scheduleHeartbeat();
  }
  private void releaseTone(){continuousTone=false;playingLevel=NONE;toneGeneration++;if(tone!=null){try{tone.stopTone();tone.release();}catch(RuntimeException ignored){}tone=null;}GuidanceAudioPolicy.Lease previous=audioLease;audioLease=null;if(previous!=null)previous.release();}
  void stop(){handler.removeCallbacksAndMessages(null);heartbeatScheduled=false;currentContext=null;releaseTone();}
}
