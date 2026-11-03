package fr.roadguard.app;
public class TestAlertPolicies {
  static void check(boolean value){if(!value)throw new AssertionError();}
  public static void main(String[] args){
    check(AlertPresentation.text(1249).equals("1,25 km"));check(AlertPresentation.voice(1249).equals("1250 mètres"));
    check(AlertPresentation.text(73).equals("70 m"));check(AlertPresentation.voice(73).equals("70 mètres"));
    for(double invalid:new double[]{-1,Double.NaN,Double.POSITIVE_INFINITY}){check(AlertPresentation.roundedMeters(invalid)==-1);check(!AlertPresentation.nearby(invalid,true,true));}
    for(String label:new String[]{" ETFR ","red-light camera","Radar feu rouge · source","Feux rouges","ETPN","Radar passage a niveau","level crossing"})check(RadarAlertPolicy.applicableLimit(label,90)==0);
    check(RadarAlertPolicy.applicableLimit("Radar fixe",70)==70);check(RadarAlertPolicy.applicableLimit("Radar fixe",300)==0);
    check(!RadarAlertPolicy.over(Double.NaN,70));check(!RadarAlertPolicy.over(200,0));check(RadarAlertPolicy.over(71,70));
    check(!AlertPresentation.nearby(1001,true,false));check(AlertPresentation.nearby(1030,true,true));check(!AlertPresentation.nearby(1051,true,true));
    check(AlertMilestones.stage(true,1000)==1);check(AlertMilestones.stage(true,1001)==0);check(AlertMilestones.stage(false,701)==0);
    int[] distances={1000,750,500,300,250,200,150,100,50};
    for(int i=0;i<distances.length;i++)check(AlertMilestones.stage(true,distances[i])==i+1);
    check(!RadarIdentity.point(46.75,5.9,"Radar fixe").equals(RadarIdentity.point(46.75,5.9,"Radar feu rouge")));
    check(RadarIdentity.point(46.75,5.9,"Radar fixe").equals(RadarIdentity.point(46.75,5.9,"ETF")));
    check(RadarAlertPolicy.displayType("Radar chantier").contains("présence à confirmer"));
    check(RadarAlertPolicy.voiceType("Radar mobile · lieu signalé, présence non confirmée").equals("Radar mobile"));
    check(RadarAlertPolicy.voiceType("Radar fixe · 80 km/h").equals("Radar fixe"));
    check(RadarAlertPolicy.LOUD_VOLUME==100&&RadarAlertPolicy.QUIET_VOLUME==100);
    System.out.println("PASS alert policies: matching voice/text distances, invalid values, red lights, crossings, limits, hysteresis and distinct point types");
  }
}
