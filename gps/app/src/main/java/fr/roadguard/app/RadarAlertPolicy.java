package fr.roadguard.app;

import java.util.regex.*;

/** Known camera limits only; never infer a road limit from a police report. */
final class RadarAlertPolicy {
  // ToneGenerator accepts 0..100; both levels use the maximum, while the
  // over-speed warning uses a distinct higher-frequency tone.
  static final int QUIET_VOLUME=100, LOUD_VOLUME=100;
  static int limit(String value){try{int n=Integer.parseInt(value.trim());return n>=5&&n<=200?n:0;}catch(Exception absent){return 0;}}
  static int labelLimit(String label){if(label==null)return 0;Matcher m=Pattern.compile("(?i)(?<![0-9])([0-9]{1,3})\\s*km\\s*/\\s*h\\b").matcher(label);return m.find()?limit(m.group(1)):0;}
  static boolean isRedLight(String label){if(label==null)return false;String s=label.trim().toLowerCase(java.util.Locale.ROOT).replace('-',' ');return s.equals("etfr")||s.contains("feu rouge")||s.contains("feux rouges")||s.contains("red light")||s.contains("redlight");}
  static int applicableLimit(String label,int limit){return isRedLight(label)||isCrossing(label)?0:(limit>=5&&limit<=200?limit:0);}
  static boolean isCrossing(String label){if(label==null)return false;String s=label.trim().toLowerCase(java.util.Locale.ROOT);return s.equals("etpn")||s.contains("passage à niveau")||s.contains("passage a niveau")||s.contains("level crossing");}
  static String displayType(String label){if(label==null)return "Danger";String s=label.toLowerCase(java.util.Locale.ROOT);return (s.contains("chantier")||s.contains("construction"))&&!s.contains("confirmer")?label+" · présence à confirmer":label;}
  /** Keep the first spoken warning short, even for catalog labels with source details. */
  static String voiceType(String label){
    if(label==null||label.trim().isEmpty())return "Radar";
    String shortLabel=label.split("\\s*·\\s*",2)[0].replaceFirst("(?i)\\s*[-–:]\\s*[0-9]{1,3}\\s*km\\s*/\\s*h.*$","").trim();
    return shortLabel.length()>60?shortLabel.substring(0,60).trim():shortLabel;
  }
  static boolean over(double speed,int limit){return limit>=5&&limit<=200&&Double.isFinite(speed)&&speed>limit&&speed<=396;}
  static boolean halo(double meters,boolean alreadyVisible){return Double.isFinite(meters)&&meters>=0&&meters<=(alreadyVisible?1050:1000);}
  static int stage(double meters){return !Double.isFinite(meters)||meters<0||meters>1000?0:1;}
  private RadarAlertPolicy(){}
}
