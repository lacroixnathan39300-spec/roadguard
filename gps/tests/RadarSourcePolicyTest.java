package fr.roadguard.app;

public final class RadarSourcePolicyTest {
  static void check(boolean value,String name){if(!value)throw new AssertionError(name);}
  public static void main(String[] args){
    check(RadarSourcePolicy.isSwiss("ch"),"CH country");
    check(RadarSourcePolicy.isSwiss("CH-VD"),"Swiss region");
    check(!RadarSourcePolicy.isSwiss("FR"),"French country");
    check(RadarSourcePolicy.quarantined("poi_warner_ch"),"Legacy ambiguous FR-labelled archive");
    for(String id:new String[]{"mapatlas_ch","osm_ch","lufop_ch","lufop_csv_ch"})
      check(!RadarSourcePolicy.quarantined(id),"Verified Swiss pack wrongly disabled: "+id);
    check(RadarSourcePolicy.include("CH","Radar fixe",false),"Swiss fixed control");
    check(RadarSourcePolicy.include("CH","Radar feu rouge",false),"Swiss traffic signal");
    check(!RadarSourcePolicy.include("CH","Radar mobile (Lufop CSV)",false),"Swiss mobile switch off");
    check(RadarSourcePolicy.include("CH","Radar mobile (Lufop CSV)",true),"Swiss mobile switch on");
    check(RadarSourcePolicy.include("FR","Radar mobile",false),"Swiss switch must not change French overlays");
    String label=RadarSourcePolicy.displayLabel("CH","Radar mobile (Lufop CSV)");
    check(label.contains("présence non confirmée"),"Imported mobile warning");
    check(RadarSourcePolicy.displayLabel("CH",label).equals(label),"Label warning repeated");
    check(RadarSourcePolicy.displayLabel("CH","Radar mobile "+new String(new char[67]).replace('\0','x')).length()<=80,"Imported mobile label length");
    System.out.println("Radar source policy OK: Swiss verified overlays active; ambiguous legacy import excluded; mobile switches and unconfirmed labels consistent");
  }
}
