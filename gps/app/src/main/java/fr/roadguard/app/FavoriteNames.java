package fr.roadguard.app;
import java.text.Normalizer;
import java.util.Locale;
/** Names used by the built-in destination shortcuts. */
final class FavoriteNames {
  private FavoriteNames(){}
  static String key(String name){String value=Normalizer.normalize(name==null?"":name.trim(),Normalizer.Form.NFD).replaceAll("\\p{M}+","").toLowerCase(Locale.ROOT).replaceAll("\\s+"," ");if(value.equals("maison")||value.equals("chez moi"))return "domicile";return value;}
  static boolean same(String a,String b){String key=key(a);return !key.isEmpty()&&key.equals(key(b));}
}
