package fr.roadguard.app;
public final class TestGuidanceAudio {
 static int checks;
 static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
 static final class Clock implements GuidanceAudioPolicy.Clock {long at=1000;public long now(){return at;}}
 static class Focus implements GuidanceAudioPolicy.Focus {int requests,releases;boolean granted=true;public boolean request(){requests++;return granted;}public void release(){releases++;}}
 public static void main(String[] args){
  Clock clock=new Clock();Focus focus=new Focus();GuidanceAudioPolicy policy=new GuidanceAudioPolicy(focus,clock);int[] stops={0,0,0};
  GuidanceAudioPolicy.Lease beep=policy.acquire(GuidanceAudioPolicy.BEEP,()->stops[0]++);check(beep!=null&&beep.active()&&focus.requests==1,"beep lease acquisition");
  GuidanceAudioPolicy.Lease nav=policy.acquire(GuidanceAudioPolicy.NAVIGATION,()->stops[1]++);check(nav!=null&&nav.active()&&!beep.active()&&stops[0]==1,"voice preempts beep");check(focus.requests==1&&focus.releases==0,"preemption must retain one focus request");
  beep.release();check(nav.active()&&focus.releases==0,"revoked tone released a newer voice's focus");
  check(policy.acquire(GuidanceAudioPolicy.PREVIEW,()->{})==null,"preview interrupted navigation");check(policy.acquire(GuidanceAudioPolicy.NAVIGATION,()->{})==null,"equal priority interrupted existing voice");
  GuidanceAudioPolicy.Lease radar=policy.acquire(GuidanceAudioPolicy.RADAR,()->stops[2]++);check(radar!=null&&stops[1]==1&&radar.active(),"radar did not preempt navigation");
  nav.release();check(radar.active()&&focus.releases==0,"late navigation release stole radar focus");
  check(policy.acquire(GuidanceAudioPolicy.BEEP,()->{})==null,"beep covered radar voice");
  radar.release();check(focus.releases==1&&!radar.active(),"completion did not restore focus");radar.release();check(focus.releases==1,"completion released twice");
  focus.granted=false;check(policy.acquire(GuidanceAudioPolicy.NAVIGATION,()->{})==null,"focus denial ignored");check(focus.requests==2&&focus.releases==2,"denied request not cleaned up");
  focus.granted=true;GuidanceAudioPolicy.Lease interrupted=policy.acquire(GuidanceAudioPolicy.RADAR,()->stops[2]++);policy.lost();check(!interrupted.active()&&stops[2]==1,"focus loss did not revoke sound");
  int requests=focus.requests;check(policy.acquire(GuidanceAudioPolicy.BEEP,()->{})==null&&focus.requests==requests,"immediate retry fought audio interruption");clock.at+=1999;check(policy.acquire(GuidanceAudioPolicy.RADAR,()->{})==null,"interruption cooldown too short");clock.at++;GuidanceAudioPolicy.Lease resumed=policy.acquire(GuidanceAudioPolicy.NAVIGATION,()->{});check(resumed!=null,"fresh audio cannot resume after cooldown");interrupted.release();check(resumed.active(),"late interrupted owner released resumed focus");resumed.release();
  GuidanceAudioPolicy.Lease throwing=policy.acquire(GuidanceAudioPolicy.BEEP,()->{throw new IllegalStateException("fixture");});GuidanceAudioPolicy.Lease safe=policy.acquire(GuidanceAudioPolicy.RADAR,()->{});check(safe!=null&&safe.active()&&!throwing.active(),"interruption callback failure broke the next owner");safe.release();
  check(policy.acquire(-1,()->{})==null&&policy.acquire(100,()->{})==null,"invalid priority accepted");
  final GuidanceAudioPolicy[] reentrant={null};Focus immediate=new Focus(){@Override public boolean request(){super.request();reentrant[0].lost();return true;}};reentrant[0]=new GuidanceAudioPolicy(immediate,clock);check(reentrant[0].acquire(GuidanceAudioPolicy.NAVIGATION,()->{})==null,"focus lost during acquisition returned an active token");
  System.out.println("PASS audio ownership: "+checks+" checks; real broker, injected focus/clock, no Android sound");
 }
}
