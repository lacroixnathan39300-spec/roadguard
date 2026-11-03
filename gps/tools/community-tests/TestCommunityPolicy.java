package fr.roadguard.app;
public final class TestCommunityPolicy {
 static void test(boolean value,String label){if(!value)throw new AssertionError(label);}
 public static void main(String[] args){long now=1700000000000L;
  test(CommunityPolicy.REPORT_INTERVAL==120000,"report interval");test(CommunityPolicy.PRESENCE_INTERVAL==600000,"presence interval");
  test(CommunityPolicy.freshFix(now-1000,20,now),"fresh precise GPS");test(!CommunityPolicy.freshFix(now-30001,20,now),"stale GPS blocked");test(!CommunityPolicy.freshFix(now+6000,20,now),"future fix blocked");test(!CommunityPolicy.freshFix(now,101,now),"imprecise fix blocked");test(!CommunityPolicy.freshFix(now,Float.NaN,now),"NaN precision blocked");
  test(!CommunityPolicy.coordinate(Double.NaN,5),"unknown coordinates");test(!CommunityPolicy.coordinate(91,0),"latitude bound");test(!CommunityPolicy.coordinate(0,181),"longitude bound");
  test(!CommunityPolicy.visible(now,now),"expired position removed");test(CommunityPolicy.visible(now+1200000,now),"position TTL");test(!CommunityPolicy.visible(now+86400000,now),"unbounded remote position rejected");
  test(!CommunityPolicy.due(now-599999,now,600000),"no early presence update");test(CommunityPolicy.due(now-600000,now,600000),"ten-minute renewal");test(CommunityPolicy.due(now+1,now,600000),"clock rollback recovers");
  String box=CommunityPolicy.bbox(46.746123,5.908234);test(!box.contains("746123")&&!box.contains("908234"),"query center coarsened");String[] b=box.split(",");test(Double.parseDouble(b[0])<5.91&&Double.parseDouble(b[2])>5.91,"bbox covers GPS");
  test("redlight".equals(CommunityPolicy.kind("Radar feu rouge")),"traffic light mapping");test("obstacle".equals(CommunityPolicy.kind("Objet sur la route")),"obstacle mapping");test("stopped".equals(CommunityPolicy.kind("Véhicule arrêté")),"stopped vehicle mapping");
  test("#ff304f".equals(CommunityPolicy.color("<img>","#ff304f")),"CSS injection rejected");test("#329dff".equals(CommunityPolicy.color("#329dff","#ff304f")),"valid accent");
  System.out.println("PASS Android community policy: GPS freshness, consent timing, expiry, geographic bounds, coarse queries, kind mapping and safe colors.");
 }
}
