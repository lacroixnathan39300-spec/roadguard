package fr.roadguard.app;

/** Explicit decimal coordinates; accepts French decimal commas with a semicolon or whitespace separator. */
final class DestinationCoordinates {
  private DestinationCoordinates(){}
  static double[] parse(String text){
    if(text==null||text.length()>160)return null;
    String value=text.trim().replace('\u00a0',' ');String[] parts;
    if(value.contains(";"))return pair(value.split(";",-1));
    double[] spaced=pair(value.split("\\s+",-1));if(spaced!=null)return spaced;
    return pair(value.split(",",-1));
  }
  private static double[] pair(String[] parts){
    if(parts.length!=2)return null;
    try{
      String a=parts[0].trim().replace(',','.'),b=parts[1].trim().replace(',','.');
      if(!a.matches("[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)")||!b.matches("[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)"))return null;
      double lat=Double.parseDouble(a),lon=Double.parseDouble(b);
      return RouteRequestPolicy.coordinate(lat,lon)?new double[]{lat,lon}:null;
    }catch(NumberFormatException invalid){return null;}
  }
}
