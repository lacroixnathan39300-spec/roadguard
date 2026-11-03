package fr.roadguard.app;
public final class TestRerouteConfirmation {
 static int checks;
 static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
 static boolean fix(CarReroutePolicy p,long at,double lateral){return p.due(at,at,lateral,5,15,true,false);}
 public static void main(String[] args){
  CarReroutePolicy p=new CarReroutePolicy();check(!fix(p,1000,250),"first noisy fix reroutes");check(!p.due(1000,1001,250,5,15,true,false)&&p.count==1,"same GNSS fix counted twice");check(!fix(p,5000,250),"confirmation too short");check(fix(p,9000,250),"first route correction blocked by the initial 90 seconds of uptime");
  check(!fix(p,10000,250)&&!fix(p,14000,250)&&!fix(p,18000,250),"retry cooldown ignored");
  p.resetDetection();check(p.lastRequest==9000,"route refresh erased retry cooldown");p.reset();check(p.lastRequest==-1&&p.count==0,"new destination did not reset confirmation");
  check(!fix(p,20000,250),"first deviation");check(!fix(p,24000,150)&&p.count==1,"intermediate deviation should preserve but not increment a streak");check(!fix(p,28000,250),"two points counted as three");check(fix(p,30000,250),"hysteresis prevented a confirmed deviation");
  p.reset();fix(p,1000,250);fix(p,5000,250);check(!fix(p,6000,100)&&p.count==0,"return to route did not reset confirmation");check(!fix(p,9000,250),"old streak survived return to route");
  p.reset();fix(p,1000,250);fix(p,5000,250);check(!p.due(9000,9000,250,0,15,true,false)&&p.count==0,"invalid accuracy confirmed deviation");
  p.reset();fix(p,1000,250);fix(p,5000,250);check(!p.due(9000,9000,250,5,Double.NaN,true,false)&&p.count==0,"unknown speed confirmed deviation");
  check(!p.due(10000,10000,250,5,1,true,false),"stationary vehicle reroutes");
  p.reset();fix(p,1000,250);fix(p,5000,250);check(!fix(p,30000,250)&&p.count==1,"long GPS interruption retained old confirmation");
  p.reset();fix(p,1000,250);fix(p,5000,250);check(!p.due(9000,9000,250,5,15,true,true)&&p.count==0,"pending request allowed another reroute");
  check(!p.due(10000,10000,250,5,15,false,false)&&p.count==0,"disabled/offline gate ignored");
  p.reset();check(!p.due(1000,20000,250,5,15,true,false)&&p.count==0,"stale measurement accepted");check(!p.due(20000,19000,250,5,15,true,false),"future measurement accepted");
  check(!p.due(21000,21000,Double.NaN,5,15,true,false),"unknown route distance accepted");
  p.reset();fix(p,1000,250);fix(p,5000,250);fix(p,9000,250);check(!fix(p,90000,250)&&!fix(p,94000,250),"cooldown reacquisition skipped confirmation");check(fix(p,99000,250),"90-second retry never became eligible");
  GpsMotion speed=new GpsMotion();speed.accept(1000,true,5,1,1,0,1);check(Math.abs(speed.speed(1000)-3.6)<.00001&&speed.speedMetersPerSecond(1000)==1,"speed unit conversion");p.reset();check(!p.due(1000,1000,250,5,speed.speedMetersPerSecond(1000),true,false),"km/h mistaken for m/s triggered reroute");
  System.out.println("PASS reroute confirmation: "+checks+" checks, distinct fixes/time/hysteresis/cooldown/units; no external route request");
 }
}
