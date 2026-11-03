package fr.roadguard.app;

import android.content.Context;
import android.media.*;
import android.os.*;
import android.speech.tts.TextToSpeech;

/** Android focus adapter. The system manages ducking; ROADGUARD never rewrites the music volume. */
final class GuidanceAudio {
  private static GuidanceAudio instance;
  static synchronized GuidanceAudio get(Context context){if(instance==null)instance=new GuidanceAudio(context.getApplicationContext());return instance;}
  final GuidanceAudioPolicy policy;
  private AudioManager manager;
  private AudioFocusRequest request;
  private volatile long generation;
  private GuidanceAudio(Context context){
    manager=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
    policy=new GuidanceAudioPolicy(new GuidanceAudioPolicy.Focus(){public boolean request(){return requestFocus();}public void release(){releaseFocus();}},SystemClock::elapsedRealtime);
  }
  private boolean requestFocus(){
    if(manager==null)return false;final long token=++generation;
    request=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
      .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
      .setOnAudioFocusChangeListener(change->{if(token==generation&&change<0)policy.lost();},new Handler(Looper.getMainLooper())).build();
    return manager.requestAudioFocus(request)==AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
  }
  private void releaseFocus(){
    generation++;AudioFocusRequest previous=request;request=null;
    if(previous!=null&&manager!=null)try{manager.abandonAudioFocusRequest(previous);}catch(RuntimeException ignored){}
  }
  AlertSpeechTracker.Audio speech(TextToSpeech engine){return new SpeechAudioGate(policy,engine);}
}
