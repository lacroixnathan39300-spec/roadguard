package fr.roadguard.app;
import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
/** Returns unspoken radar milestones to the shared foreground/background ledger. */
final class AlertSpeechTracker {
  private static final AtomicLong SEQUENCE=new AtomicLong();
  interface Cancellation {void cancel();}
  interface Timer {Cancellation later(Runnable task,long delay);}
  private static final java.util.concurrent.ScheduledThreadPoolExecutor CLOCK=createClock();
  private static java.util.concurrent.ScheduledThreadPoolExecutor createClock(){java.util.concurrent.ScheduledThreadPoolExecutor clock=new java.util.concurrent.ScheduledThreadPoolExecutor(1,r->{Thread t=new Thread(r,"roadguard-speech-watchdog");t.setDaemon(true);return t;});clock.setRemoveOnCancelPolicy(true);return clock;}
  interface Audio {boolean acquire(String id,int priority,Runnable interrupted);void release(String id);void cancel(String id);}
  private Audio audio=new Audio(){public boolean acquire(String id,int priority,Runnable interrupted){return true;}public void release(String id){}public void cancel(String id){}};
  void useAudio(Audio audio){cancelAll();this.audio=java.util.Objects.requireNonNull(audio);}
  private final ConcurrentHashMap<String,Boolean> other=new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String,Runnable> otherFailures=new ConcurrentHashMap<>();
  private final Timer timer;
  private final ConcurrentHashMap<String,Cancellation> expiry=new ConcurrentHashMap<>();
  private void watch(String id){Cancellation deadline=timer.later(()->failed(id),60000);Cancellation old=expiry.put(id,deadline);if(old!=null)old.cancel();}
  private void clearExpiry(String id){Cancellation task=expiry.remove(id);if(task!=null)task.cancel();}
  private final Context context;
  private final ConcurrentHashMap<String,Runnable> navigationFailures=new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String,AlertMilestones.Claim> pending=new ConcurrentHashMap<>();
  private volatile boolean radarSpeechFailed;
  AlertSpeechTracker(Context context){this(context,(task,delay)->{java.util.concurrent.ScheduledFuture<?> future=CLOCK.schedule(task,delay,java.util.concurrent.TimeUnit.MILLISECONDS);return ()->future.cancel(false);});}
  AlertSpeechTracker(Context context,Timer timer){this.context=context;this.timer=timer;}
  void install(TextToSpeech speech){speech.setOnUtteranceProgressListener(new UtteranceProgressListener(){
    @Override public void onStart(String id){}
    @Override public void onDone(String id){if(id!=null){clearExpiry(id);pending.remove(id);navigationFailures.remove(id);otherFailures.remove(id);other.remove(id);audio.release(id);}}
    @Override public void onError(String id){failed(id);}
    @Override public void onError(String id,int error){failed(id);}
    @Override public void onStop(String id,boolean interrupted){failed(id);}
  });}
  private void failed(String id){if(id!=null){clearExpiry(id);audio.cancel(id);other.remove(id);AlertMilestones.Claim radar=pending.remove(id);if(radar!=null)radarSpeechFailed=true;AlertMilestones.release(context,radar);Runnable callback=navigationFailures.remove(id);if(callback!=null)callback.run();Runnable otherCallback=otherFailures.remove(id);if(otherCallback!=null)otherCallback.run();}}
  boolean speak(TextToSpeech speech,String message,AlertMilestones.Claim claim){
    if(claim==null)return false;
    String id="roadguard-radar-"+SEQUENCE.incrementAndGet();pending.put(id,claim);
    if(!audio.acquire(id,GuidanceAudioPolicy.RADAR,()->failed(id))){failed(id);return false;}watch(id);
    try{if(speech!=null&&speech.speak(message,TextToSpeech.QUEUE_FLUSH,null,id)==TextToSpeech.SUCCESS){radarSpeechFailed=false;return true;}}catch(RuntimeException unavailable){}
    failed(id);return false;
  }
  boolean speakNavigation(TextToSpeech speech,String message,String label,Runnable retry){
    String id=label+"-"+SEQUENCE.incrementAndGet();navigationFailures.put(id,retry);
    if(!audio.acquire(id,GuidanceAudioPolicy.NAVIGATION,()->failed(id))){failed(id);return false;}watch(id);
    try{if(speech!=null&&speech.speak(message,TextToSpeech.QUEUE_FLUSH,null,id)==TextToSpeech.SUCCESS)return true;}catch(RuntimeException unavailable){}
    failed(id);return false;
  }
  boolean speakOther(TextToSpeech speech,String message,int priority){return speakOther(speech,message,priority,null);}
  boolean speakOther(TextToSpeech speech,String message,int priority,Runnable retry){
    String id="roadguard-other-"+SEQUENCE.incrementAndGet();other.put(id,true);if(retry!=null)otherFailures.put(id,retry);
    if(!audio.acquire(id,priority,()->failed(id))){failed(id);return false;}watch(id);
    try{if(speech!=null&&speech.speak(message,TextToSpeech.QUEUE_FLUSH,null,id)==TextToSpeech.SUCCESS)return true;}catch(RuntimeException unavailable){}
    failed(id);return false;
  }
  void watchNavigation(String id,Runnable retry){navigationFailures.put(id,retry);watch(id);}
  boolean hasOther(){return !other.isEmpty();}
  boolean hasNavigation(){return !navigationFailures.isEmpty();}
  void failedNavigation(String id){failed(id);}
  void cancelNavigation(){for(String id:navigationFailures.keySet())failed(id);}
  boolean hasPending(){return !pending.isEmpty();}
  boolean radarSpeechFailed(){return radarSpeechFailed;}
  void cancelAll(){for(String id:pending.keySet())failed(id);cancelNavigation();for(String id:other.keySet())failed(id);}
}
