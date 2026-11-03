package fr.roadguard.app;
import java.util.*;
/** A short grace interval bridges activity replacement without retaining stopped activities. */
final class AppVisibilityPolicy {
  interface Cancellation {void cancel();}
  interface Timer {Cancellation later(Runnable action,long delay);}
  interface Listener {void changed(boolean visible);}
  private final Set<Object> started=Collections.newSetFromMap(new IdentityHashMap<>());
  private final Timer timer;private final Listener listener;
  private Cancellation pending;private long generation;private boolean visible;
  AppVisibilityPolicy(Timer timer,Listener listener){this.timer=timer;this.listener=listener;}
  void started(Object owner){
    if(owner==null)return;started.add(owner);generation++;if(pending!=null){pending.cancel();pending=null;}
    if(!visible){visible=true;listener.changed(true);}
  }
  void stopped(Object owner){
    if(!started.remove(owner)||!started.isEmpty())return;
    long token=++generation;pending=timer.later(()->{if(token==generation&&started.isEmpty()){pending=null;if(visible){visible=false;listener.changed(false);}}},300);
  }
  boolean visible(){return visible;}
}
