package fr.roadguard.app;

/** Map connectivity preference, independent of the rest of the application. */
final class MapConnectionPolicy {
  private MapConnectionPolicy(){}
  static String normalize(String mode){return "online".equals(mode)||"offline".equals(mode)?mode:"auto";}
  static boolean allowsNetwork(String mode,boolean connected){return connected&&!"offline".equals(normalize(mode));}
}
