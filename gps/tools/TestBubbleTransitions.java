package fr.roadguard.app;
import java.util.*;
public final class TestBubbleTransitions {
 static int checks;
 static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
 static final class Timer implements AppVisibilityPolicy.Timer {
  final List<Runnable> tasks=new ArrayList<>();final List<Boolean> cancelled=new ArrayList<>();
  public AppVisibilityPolicy.Cancellation later(Runnable task,long delay){check(delay==300,"activity grace interval");int index=tasks.size();tasks.add(task);cancelled.add(false);return ()->cancelled.set(index,true);}
 }
 public static void main(String[] args){
  for(int mask=0;mask<32;mask++){boolean armed=(mask&1)!=0,app=(mask&2)!=0,screen=(mask&4)!=0,lock=(mask&8)!=0,permission=(mask&16)!=0;check(BubblePlacement.show(armed,app,screen,lock,permission)==(armed&&!app&&screen&&!lock&&permission),"visibility gate "+mask);}
  Timer timer=new Timer();List<Boolean> states=new ArrayList<>();AppVisibilityPolicy policy=new AppVisibilityPolicy(timer,states::add);Object first=new Object(),next=new Object(),download=new Object();
  policy.started(first);check(policy.visible()&&states.equals(Arrays.asList(true)),"first ROADGUARD page did not hide bubble");
  policy.started(download);policy.stopped(first);check(policy.visible()&&timer.tasks.isEmpty(),"own download page exposed bubble");
  policy.stopped(download);check(policy.visible()&&timer.tasks.size()==1,"activity replacement lacks grace");policy.started(next);check(timer.cancelled.get(0),"new activity did not cancel pending hide");timer.tasks.get(0).run();check(policy.visible()&&states.equals(Arrays.asList(true)),"stale hide callback flickered bubble during rotation");
  policy.stopped(next);timer.tasks.get(1).run();check(!policy.visible()&&states.equals(Arrays.asList(true,false)),"leaving the app did not permit bubble");
  policy.stopped(new Object());check(timer.tasks.size()==2,"unknown/stopped page affected count");policy.started(first);policy.started(first);policy.stopped(first);timer.tasks.get(2).run();check(!policy.visible(),"duplicate start retained Activity owner");
  int placements=0;
  for(int width:new int[]{160,393,852})for(int height:new int[]{180,393,852})for(int boxWidth:new int[]{76,248})for(int boxHeight:new int[]{76,160})for(int x:new int[]{-100,20,1000})for(int y:new int[]{-100,40,1000}){
    int left=4,top=24,right=width-4,bottom=height-24;BubblePlacement.Box bubble=BubblePlacement.fit(left,top,right,bottom,boxWidth,boxHeight,x,y);
    check(bubble.x>=left&&bubble.y>=top&&bubble.x+bubble.width<=right&&bubble.y+bubble.height<=bottom,"body outside safe area");BubblePlacement.Box plus=BubblePlacement.action(left,top,right,bottom,bubble,44,4);
    if(plus!=null){placements++;check(!plus.overlaps(bubble,4)&&plus.x>=left&&plus.y>=top&&plus.x+plus.width<=right&&plus.y+plus.height<=bottom,"quick report overlaps bubble or screen edge");}
  }
  BubblePlacement.Box full=BubblePlacement.fit(0,0,76,76,76,76,0,0);check(BubblePlacement.action(0,0,76,76,full,44,4)==null,"unplaceable report button should use long-press fallback");
  check(BubblePlacement.action(0,0,300,300,new BubblePlacement.Box(200,220,76,76),44,4).y<220,"bottom-edge report action did not move above");
  for(int ratio:new int[]{0,2500,5000,7500,10000})for(int size:new int[]{76,248}){
    int pixel=BubblePlacement.coordinate(ratio,24,828,size),stored=BubblePlacement.ratio(pixel,24,828,size);check(Math.abs(stored-ratio)<12,"position ratio lost across pixel roundtrip");int rotated=BubblePlacement.coordinate(stored,4,389,size);check(rotated>=4&&rotated+size<=389,"rotation placed bubble outside bounds");
  }
  check(BubblePlacement.coordinate(10000,0,40,76)==0,"small display ratio overflow");
  System.out.println("PASS bubble transitions: "+checks+" assertions, activity ownership/grace, screen/lock/permission and "+placements+" report placements; no Android window rendering");
 }
}
