package fr.roadguard.app;

import java.text.Normalizer;
import java.util.Locale;

/** Conservative identity: different camera types at one coordinate stay distinct. */
final class RadarIdentity {
  private RadarIdentity(){}
  static String type(String label){
    String value=Normalizer.normalize(label==null?"":label,Normalizer.Form.NFD)
        .replaceAll("\\p{M}","").toLowerCase(Locale.ROOT).trim();
    if(RadarAlertPolicy.isRedLight(label))return "redlight";
    if(RadarAlertPolicy.isCrossing(label))return "crossing";
    if(value.contains("chantier")||value.contains("construction"))return "works";
    if(value.contains("vitesse moyenne")||value.contains("average")||value.contains("troncon")||value.equals("etvm"))return "average";
    if(value.contains("mobile"))return "mobile";
    if(value.contains("zone de danger")||value.contains("zone dangereuse")||value.contains("zone de controle possible"))return "danger";
    if(value.contains("fixe")||value.contains("speed camera")||value.equals("etf"))return "fixed";
    return value;
  }
  static String point(double lat,double lon,String label){
    return AlertMilestones.pointKey(lat,lon)+":"+type(label);
  }
  static String catalog(BundledRadarCatalog.Point point){
    return point.country+":"+point(point.lat,point.lon,point.type);
  }
}
