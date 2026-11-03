package fr.roadguard.app;
import android.content.Context;
import android.media.*;
import android.os.*;
/** Short hazard pulses share sound ownership with speech and radar proximity tones. */
final class GuidanceTone {
  private final Handler handler=new Handler(Looper.getMainLooper());
  private GuidanceAudioPolicy.Lease lease;
  private ToneGenerator tone;
  private long generation;
  void play(Context context,int count){
    stop();
    GuidanceAudioPolicy.Lease acquired=GuidanceAudio.get(context).policy.acquire(GuidanceAudioPolicy.BEEP,this::stop);
    if(acquired==null)return;
    lease=acquired;final long token=++generation;
    try{
      final ToneGenerator current=new ToneGenerator(AudioManager.STREAM_MUSIC,65);tone=current;
      int pulses=Math.max(1,Math.min(3,count));
      for(int i=0;i<pulses;i++){final int duration=i==0?145:115;handler.postDelayed(()->{if(token==generation&&tone==current&&acquired.active())try{current.startTone(ToneGenerator.TONE_PROP_BEEP,duration);}catch(RuntimeException unavailable){stop();}},i*290L);}
      handler.postDelayed(()->{if(token==generation)stop();},(pulses-1)*290L+215L);
    }catch(RuntimeException unavailable){stop();}
  }
  void stop(){generation++;handler.removeCallbacksAndMessages(null);ToneGenerator previous=tone;tone=null;if(previous!=null)try{previous.release();}catch(RuntimeException ignored){}GuidanceAudioPolicy.Lease owner=lease;lease=null;if(owner!=null)owner.release();}
}
