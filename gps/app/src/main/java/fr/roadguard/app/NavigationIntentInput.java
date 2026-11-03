package fr.roadguard.app;
import java.net.URI;
import java.net.URLDecoder;
final class NavigationIntentInput {
  static String query(String input){
    if(input==null||input.length()>2048)return null;
    try{URI u=new URI(input);String scheme=u.getScheme();if(!"geo".equals(scheme)&&!"google.navigation".equals(scheme))return null;
      String raw=u.getRawSchemeSpecificPart(),q=null;int split=raw.indexOf('?');String args=split<0?raw:raw.substring(split+1);
      for(String part:args.split("&"))if(part.startsWith("q=")){q=URLDecoder.decode(part.substring(2),"UTF-8");break;}
      if(q==null&&"geo".equals(scheme))q=URLDecoder.decode(split<0?raw:raw.substring(0,split),"UTF-8");
      if(q==null)return null;q=q.trim();return q.length()>=2&&q.length()<=120?q:null;
    }catch(Exception e){return null;}
  }
  static double[] coordinates(String q){if(q==null)return null;int label=q.indexOf('(');return DestinationCoordinates.parse(label<0?q:q.substring(0,label));}
  private NavigationIntentInput(){}
}
