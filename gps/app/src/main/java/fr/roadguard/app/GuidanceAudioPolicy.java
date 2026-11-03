package fr.roadguard.app;

/** One sound owner in the process. Revoked tokens can never release a newer owner's focus. */
final class GuidanceAudioPolicy {
  static final int BEEP=0,PREVIEW=1,NAVIGATION=2,HAZARD=3,RADAR=4;
  interface Focus {boolean request();void release();}
  interface Clock {long now();}
  final class Lease {
    final long id;final int priority;final Runnable interrupted;
    Lease(long id,int priority,Runnable interrupted){this.id=id;this.priority=priority;this.interrupted=interrupted;}
    void release(){GuidanceAudioPolicy.this.release(this);}
    boolean active(){synchronized(GuidanceAudioPolicy.this){return current==this;}}
  }
  private final Focus focus;private final Clock clock;
  private Lease current;private long sequence,blockedUntil;
  GuidanceAudioPolicy(Focus focus,Clock clock){this.focus=focus;this.clock=clock;}
  Lease acquire(int priority,Runnable interrupted){
    Lease previous,candidate;
    synchronized(this){
      if(priority<BEEP||priority>RADAR||clock.now()<blockedUntil||current!=null&&priority<=current.priority)return null;
      previous=current;candidate=new Lease(++sequence,priority,interrupted);current=candidate;
      if(previous==null){
        boolean granted=false;try{granted=focus.request();}catch(RuntimeException denied){}
        if(!granted||current!=candidate){if(current==candidate)current=null;try{focus.release();}catch(RuntimeException ignored){}return null;}
      }
    }
    notifyInterrupted(previous);
    return candidate.active()?candidate:null;
  }
  private void release(Lease owner){synchronized(this){if(current!=owner)return;current=null;try{focus.release();}catch(RuntimeException ignored){}}}
  void lost(){
    Lease previous;
    synchronized(this){previous=current;current=null;blockedUntil=clock.now()+2000;try{focus.release();}catch(RuntimeException ignored){}}
    notifyInterrupted(previous);
  }
  private static void notifyInterrupted(GuidanceAudioPolicy.Lease lease){if(lease!=null&&lease.interrupted!=null)try{lease.interrupted.run();}catch(RuntimeException ignored){}}
}
