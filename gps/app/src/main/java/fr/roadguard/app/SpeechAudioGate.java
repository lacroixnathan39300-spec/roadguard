package fr.roadguard.app;
import android.speech.tts.TextToSpeech;
import java.util.concurrent.ConcurrentHashMap;
/** Per-engine lease adapter. Can be exercised without an Android AudioManager. */
final class SpeechAudioGate implements AlertSpeechTracker.Audio {
  private final GuidanceAudioPolicy policy;private final TextToSpeech engine;
  private final ConcurrentHashMap<String,GuidanceAudioPolicy.Lease> leases=new ConcurrentHashMap<>();
  SpeechAudioGate(GuidanceAudioPolicy policy,TextToSpeech engine){this.policy=policy;this.engine=engine;}
  public boolean acquire(String id,int priority,Runnable interrupted){
    GuidanceAudioPolicy.Lease lease=policy.acquire(priority,()->{leases.remove(id);try{engine.stop();}catch(RuntimeException ignored){}interrupted.run();});
    if(lease==null)return false;leases.put(id,lease);
    if(!lease.active()){leases.remove(id,lease);lease.release();return false;}
    return true;
  }
  public void release(String id){GuidanceAudioPolicy.Lease lease=leases.remove(id);if(lease!=null)lease.release();}
  public void cancel(String id){GuidanceAudioPolicy.Lease lease=leases.remove(id);if(lease!=null){if(lease.active())try{engine.stop();}catch(RuntimeException ignored){}lease.release();}}
}
