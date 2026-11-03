package fr.roadguard.app;
import android.speech.tts.TextToSpeech;
import java.util.*;
public final class TestSpeechPriority {
 static int checks;
 static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
 static final class Timer implements AlertSpeechTracker.Timer {final List<Runnable> tasks=new ArrayList<>();public AlertSpeechTracker.Cancellation later(Runnable task,long delay){tasks.add(task);return ()->{};}}
 public static void main(String[] args){
  TestSpeechMilestones.App app=new TestSpeechMilestones.App();TestGuidanceAudio.Clock clock=new TestGuidanceAudio.Clock();TestGuidanceAudio.Focus focus=new TestGuidanceAudio.Focus();GuidanceAudioPolicy policy=new GuidanceAudioPolicy(focus,clock);
  TextToSpeech speech=new TextToSpeech();speech.stopCallback=true;Timer timer=new Timer();AlertSpeechTracker tracker=new AlertSpeechTracker(app,timer);tracker.useAudio(new SpeechAudioGate(policy,speech));tracker.install(speech);int[] retries={0};
  check(tracker.speakNavigation(speech,"Tourner à droite","navigation",()->retries[0]++),"navigation refused");String nav=speech.lastId;check(tracker.hasNavigation(),"queued navigation missing");
  check(!tracker.speakOther(speech,"Aperçu",GuidanceAudioPolicy.PREVIEW)&&nav.equals(speech.lastId),"preview interrupted navigation");
  AlertMilestones.Claim claim=AlertMilestones.reserve(app,46.7,6,"Radar fixe",1);check(tracker.speak(speech,"Radar à 1 km",claim),"radar refused over navigation");String radar=speech.lastId;
  check(retries[0]==1&&!tracker.hasNavigation()&&tracker.hasPending()&&speech.stopCalls==1,"navigation interruption/rollback failed");check(focus.requests==1&&focus.releases==0,"same-engine preemption released shared focus");
  timer.tasks.get(0).run();speech.listener.onStop(nav,true);check(retries[0]==1&&tracker.hasPending()&&focus.releases==0,"old callback cancelled current radar");
  tracker.cancelNavigation();check(tracker.hasPending()&&focus.releases==0,"cancelling navigation stopped radar");
  AlertMilestones.Claim second=AlertMilestones.reserve(app,46.8,6,"Radar fixe",1);check(!tracker.speak(speech,"Second radar",second),"same-priority radar overlapped first");check(AlertMilestones.pending(app,46.8,6,"Radar fixe",1)&&tracker.hasPending(),"denied second radar consumed its milestone");
  speech.listener.onDone(radar);check(!tracker.hasPending()&&focus.releases==1,"completed radar did not release focus");check(!AlertMilestones.pending(app,46.7,6,"Radar fixe",1),"completed radar incorrectly rearmed");
  check(tracker.speak(speech,"Second radar",AlertMilestones.reserve(app,46.8,6,"Radar fixe",1)),"next radar refused after completion");String interrupted=speech.lastId;policy.lost();check(!tracker.hasPending()&&AlertMilestones.pending(app,46.8,6,"Radar fixe",1),"focus loss consumed an interrupted milestone");
  check(!tracker.speak(speech,"Too soon",AlertMilestones.reserve(app,46.8,6,"Radar fixe",1)),"speech ignored audio suspension");check(AlertMilestones.pending(app,46.8,6,"Radar fixe",1),"suspended speech consumed milestone");
  clock.at+=2000;check(tracker.speak(speech,"Retry",AlertMilestones.reserve(app,46.8,6,"Radar fixe",1)),"retry after suspension failed");String retried=speech.lastId;check(!retried.equals(interrupted),"retry reused old utterance identity");speech.listener.onError(interrupted);check(tracker.hasPending(),"late failure of old utterance cancelled retry");
  int latest=timer.tasks.size()-1;timer.tasks.get(latest).run();check(!tracker.hasPending()&&AlertMilestones.pending(app,46.8,6,"Radar fixe",1),"watchdog did not stop engine and return milestone");
  speech.result=TextToSpeech.ERROR;check(!tracker.speakNavigation(speech,"Failure","nav-failure",()->retries[0]++),"failed engine accepted");check(!tracker.hasNavigation()&&retries[0]==2,"engine failure did not return navigation threshold");speech.result=TextToSpeech.SUCCESS;
  check(tracker.speakOther(speech,"Test voix",GuidanceAudioPolicy.PREVIEW)&&tracker.hasOther(),"preview not tracked");check(tracker.speakNavigation(speech,"Priorité route","new-nav",()->retries[0]++),"navigation did not preempt preview");check(!tracker.hasOther()&&tracker.hasNavigation(),"preview survived preemption");
  tracker.cancelAll();check(!tracker.hasNavigation()&&!tracker.hasOther()&&!tracker.hasPending(),"lifecycle cancellation left pending voice");
  focus.granted=false;check(!tracker.speak(speech,"Denied",AlertMilestones.reserve(app,46.9,6,"Radar fixe",1)),"audio focus denial ignored");check(AlertMilestones.pending(app,46.9,6,"Radar fixe",1),"focus denial consumed milestone");
  System.out.println("PASS speech priorities: "+checks+" checks; real tracker/gate/ledger/broker, injected speech and Android preferences");
 }
}
