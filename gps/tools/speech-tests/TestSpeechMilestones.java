package fr.roadguard.app;
import android.content.*;import android.content.res.*;import android.speech.tts.*;import java.io.*;import java.util.*;
public class TestSpeechMilestones {
 static int checks;
 static void check(boolean value,String reason){checks++;if(!value)throw new AssertionError(reason);}
 static class Prefs implements SharedPreferences {
  final Map<String,Object> values=new HashMap<>();
  public Map<String,?> getAll(){return new HashMap<>(values);}public long getLong(String k,long f){return values.containsKey(k)?(Long)values.get(k):f;}public int getInt(String k,int f){return values.containsKey(k)?(Integer)values.get(k):f;}public boolean getBoolean(String k,boolean f){return values.containsKey(k)?(Boolean)values.get(k):f;}public String getString(String k,String f){return values.containsKey(k)?(String)values.get(k):f;}
  public Editor edit(){return new Editor(){final Map<String,Object> update=new HashMap<>();boolean reset;public Editor putLong(String k,long v){update.put(k,v);return this;}public Editor putInt(String k,int v){update.put(k,v);return this;}public Editor putBoolean(String k,boolean v){update.put(k,v);return this;}public Editor putString(String k,String v){update.put(k,v);return this;}public Editor clear(){reset=true;return this;}public boolean commit(){if(reset)values.clear();values.putAll(update);return true;}public void apply(){commit();}};}
 }
 static class App extends Context {
  final Prefs prefs=new Prefs();public File getFilesDir(){return new File(".");}public AssetManager getAssets(){return new AssetManager(new File("."));}public SharedPreferences getSharedPreferences(String n,int m){return prefs;}
 }
 public static void main(String[] args){App app=new App();double lat=46.7,lon=6;String type="Radar fixe";
  AlertMilestones.Claim first=AlertMilestones.reserve(app,lat,lon,type,1);check(first.kind==AlertMilestones.FIRST,"first warning names radar");check(!AlertMilestones.pending(app,lat,lon,type,1),"shared duplicate blocked");AlertMilestones.release(app,first);check(AlertMilestones.pending(app,lat,lon,type,1),"queue rejection returns first stage");
  TextToSpeech speech=new TextToSpeech();AlertSpeechTracker tracker=new AlertSpeechTracker(app);tracker.install(speech);speech.result=TextToSpeech.ERROR;
  check(!tracker.speak(speech,"test",AlertMilestones.reserve(app,lat,lon,type,1)),"immediate speech error");check(AlertMilestones.pending(app,lat,lon,type,1),"immediate error does not consume type");
  speech.result=TextToSpeech.SUCCESS;check(tracker.speak(speech,"test",AlertMilestones.reserve(app,lat,lon,type,1)),"speech accepted");check(tracker.hasPending(),"queued-before-start is pending");speech.listener.onError(speech.lastId,7);check(AlertMilestones.pending(app,lat,lon,type,1),"async error restores stage");
  tracker.speak(speech,"test",AlertMilestones.reserve(app,lat,lon,type,1));speech.listener.onStop(speech.lastId,true);check(!tracker.hasPending(),"stop clears tracker");check(AlertMilestones.pending(app,lat,lon,type,1),"foreground handoff may retry");
  tracker.speak(speech,"test",AlertMilestones.reserve(app,lat,lon,type,1));speech.listener.onDone(speech.lastId);check(!AlertMilestones.pending(app,lat,lon,type,1),"finished speech retained");check(!tracker.hasPending(),"completed speech clears tracker");
  AlertMilestones.Claim next=AlertMilestones.reserve(app,lat,lon,type,3);check(next.kind==AlertMilestones.FOLLOWUP,"next distance omits repeated type");AlertMilestones.release(app,next);check(!AlertMilestones.pending(app,lat,lon,type,1),"rollback retains successful earlier stage");check(AlertMilestones.pending(app,lat,lon,type,3),"failed followup retry");
  AlertMilestones.Claim earlier=AlertMilestones.reserve(app,lat,lon,type,2),later=AlertMilestones.reserve(app,lat,lon,type,3);AlertMilestones.release(app,earlier);check(!AlertMilestones.pending(app,lat,lon,type,3),"old rollback cannot erase newer warning");
  long now=System.currentTimeMillis()+10000;AlertMilestones.rearmDistant(app,lat,lon,now);check(!AlertMilestones.pending(app,lat,lon,type,3),"nearby radar does not rearm");AlertMilestones.rearmDistant(app,lat+.03,lon,now+11000);check(AlertMilestones.pending(app,lat,lon,type,1),"leave 1.5km rearms return pass");
  tracker.speak(speech,"test",AlertMilestones.reserve(app,lat,lon,type,1));tracker.cancelAll();check(AlertMilestones.pending(app,lat,lon,type,1),"lifecycle cancellation retry");speech.listener.onDone(null);speech.listener.onError(null);
  check(AlertMilestones.reserve(app,Double.NaN,lon,type,1)==null,"invalid position");check(AlertMilestones.reserve(app,lat,lon,type,10)==null,"invalid stage");
  final int[] retries={0};tracker.watchNavigation("nav1",()->retries[0]++);check(tracker.hasNavigation(),"navigation queue tracked");speech.listener.onStop("nav1",true);check(retries[0]==1&&!tracker.hasNavigation(),"interrupted navigation may retry");
  tracker.watchNavigation("nav2",()->retries[0]++);speech.listener.onDone("nav2");tracker.cancelNavigation();check(retries[0]==1,"completed navigation not replayed");
  tracker.watchNavigation("nav3",()->retries[0]++);tracker.cancelAll();check(retries[0]==2,"lifecycle cancels queued instruction");
  final java.util.List<Runnable> deadlines=new ArrayList<>();final int[] cancellations={0};
  AlertSpeechTracker timed=new AlertSpeechTracker(app,(task,delay)->{check(delay==60000,"bounded pending speech lifetime");deadlines.add(task);return ()->cancellations[0]++;});
  timed.install(speech);timed.watchNavigation("stuck-nav",()->retries[0]++);deadlines.get(0).run();check(!timed.hasNavigation()&&retries[0]==3,"missing callbacks cannot block navigation forever");
  AlertMilestones.Claim stalled=AlertMilestones.reserve(app,lat,lon,type,1);timed.speak(speech,"test",stalled);deadlines.get(1).run();check(!timed.hasPending()&&AlertMilestones.pending(app,lat,lon,type,1),"stalled radar returned to ledger");
  timed.watchNavigation("done-nav",()->retries[0]++);speech.listener.onDone("done-nav");check(cancellations[0]>=3,"completion cancels watchdog");
  System.out.println("PASS "+checks+" speech/milestone checks; real ledger and tracker, injected preferences/speech boundaries");
 }
}
