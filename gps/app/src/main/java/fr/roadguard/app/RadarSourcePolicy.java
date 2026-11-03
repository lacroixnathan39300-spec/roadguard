package fr.roadguard.app;

import java.util.Locale;

/** Shared rules for downloaded/imported overlays, separate from bundled data. */
final class RadarSourcePolicy {
  private RadarSourcePolicy() {}

  static boolean isSwiss(String country){
    String value=country==null?"":country.trim().toUpperCase(Locale.ROOT);
    return value.equals("CH")||value.startsWith("CH-");
  }

  static boolean quarantined(String id){
    // This legacy import contains FR-labelled points inside a rectangle around
    // Switzerland, not a verified Swiss inventory. Do not activate it by country.
    return "poi_warner_ch".equals(id);
  }

  static boolean historical(String type){
    String value=type==null?"":type.toLowerCase(Locale.ROOT);
    return value.contains("mobile")||value.contains("chantier")
        ||value.contains("temporaire")||value.contains("contrôle possible");
  }

  static boolean include(String country,String type,boolean swissHistorical){
    return !isSwiss(country)||!historical(type)||swissHistorical;
  }

  static String displayLabel(String country,String type){
    if(!isSwiss(country)||!historical(type)||type.toLowerCase(Locale.ROOT).contains("confirm"))return type;
    String suffix=" · présence non confirmée";
    return type.substring(0,Math.min(type.length(),80-suffix.length())).trim()+suffix;
  }
}
