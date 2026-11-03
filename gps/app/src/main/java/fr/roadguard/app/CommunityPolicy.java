package fr.roadguard.app;
import java.util.Locale;
/** Shared, dependency-free limits for the Android community client. */
final class CommunityPolicy {
 static final long REPORT_INTERVAL=120000L,PRESENCE_INTERVAL=600000L,PRESENCE_TTL=1200000L,STARTUP_TIMEOUT=15000L;
 static boolean coordinate(double lat,double lon){return Double.isFinite(lat)&&Double.isFinite(lon)&&Math.abs(lat)<=85&&Math.abs(lon)<=180;}
 static boolean freshFix(long fixTime,float accuracy,long now){return now-fixTime>=-5000&&now-fixTime<30000&&Float.isFinite(accuracy)&&accuracy>0&&accuracy<=100;}
 static boolean visible(long expires,long now){return expires>now&&expires-now<=PRESENCE_TTL+5000;}
 static boolean due(long last,long now,long interval){return last==0||now<last||now-last>=interval;}
 static String bbox(double lat,double lon){if(!coordinate(lat,lon))throw new IllegalArgumentException("Invalid area");lat=Math.round(lat*50)/50.0;lon=Math.round(lon*50)/50.0;return String.format(Locale.US,"%.5f,%.5f,%.5f,%.5f",Math.max(-180,lon-.18),Math.max(-85,lat-.18),Math.min(180,lon+.18),Math.min(85,lat+.18));}
 static String kind(String label){String l=label==null?"":label.toLowerCase(Locale.ROOT);if(l.contains("feu rouge"))return "redlight";if(l.contains("fixe"))return "fixed";if(l.contains("contrôle routier")||l.contains("controle routier")||l.contains("gendarmerie")||l.contains("police"))return "control";if(l.contains("mobile"))return "mobile";if(l.contains("accident"))return "accident";if(l.contains("arrêt")||l.contains("arrete"))return "stopped";if(l.contains("bouchon"))return "traffic";if(l.contains("objet")||l.contains("obstacle"))return "obstacle";if(l.contains("travaux"))return "works";if(l.contains("danger"))return "danger";return "other";}
 static String label(String kind){switch(kind){case "fixed":return "Radar fixe";case "mobile":return "Radar mobile";case "control":return "Contrôle routier";case "redlight":return "Radar feu rouge";case "danger":return "Zone dangereuse";case "accident":return "Accident";case "stopped":return "Véhicule arrêté";case "traffic":return "Bouchon";case "obstacle":return "Objet sur la route";case "works":return "Travaux";default:return "Autre";}}
 static boolean recentPresence(String kind,long confirmed,long now){return !("mobile".equals(kind)||"control".equals(kind))||confirmed>0&&now>=confirmed&&now-confirmed<7200000L;}
 static String reportLabel(String kind,long confirmed,long now){return recentPresence(kind,confirmed,now)?label(kind):("mobile".equals(kind)?"Lieu de radar mobile mémorisé":"Lieu de contrôle mémorisé")+" · présence non confirmée";}
 static String color(String value,String fallback){return value!=null&&value.matches("#[0-9a-fA-F]{6}")?value:fallback;}
 static boolean uuid(String value){return value!=null&&value.matches("[0-9a-fA-F-]{36}");}
 private CommunityPolicy(){}
}
