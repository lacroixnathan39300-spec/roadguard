package fr.roadguard.app;
import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;

/** The same stored palette is consumed by ROADGUARD's WebView and native map surfaces. */
public final class MapAppearance {
 public static final String[] IDS={"graphite","readable","original","ocean","emerald","amber","custom"};
 public static final String[] NAMES={"Graphite","Haute lisibilité","Original","Océan","Émeraude","Ambre","Personnalisé"};
 public static final String[] KEYS={"background","water","landcover","building","road","roadOutline","motorway","rail","text","halo"};
 public static final String[] LABELS={"Fond","Eau","Nature","Bâtiments","Routes","Bord des routes","Grands axes","Voies ferrées","Noms et textes","Contour des textes"};
 public static SharedPreferences preferences(Context c){return c.getSharedPreferences("MainActivity",Context.MODE_PRIVATE);}
 public static boolean valid(String id){for(String s:IDS)if(s.equals(id))return true;return false;}
 public static JSONObject current(Context context,boolean night){SharedPreferences p=preferences(context);return palette(p.getString("map_appearance_pack","graphite"),night,p.getString("map_palette_custom_"+(night?"night":"day"),"{}"));}
 public static JSONObject palette(String id,boolean night,String custom){
  JSONObject p=new JSONObject();String[] colors=night?new String[]{"#0a1422","#15579c","#163627","#26344d","#daeaff","#425471","#ffd550","#52627e","#edf4ff","#091322"}:new String[]{"#f5f2e9","#88c7e7","#bcd5aa","#cdd1d2","#ffffff","#aeb7b9","#f1bd56","#a4a9b0","#344153","#f8f6ed"};
  if("readable".equals(id))colors=night?new String[]{"#0c1119","#244f78","#193a2c","#354352","#ffffff","#536278","#ffe081","#7b899a","#ffffff","#080c13"}:new String[]{"#fbfaf4","#9bd7ed","#c7dfb4","#cfd4d7","#ffffff","#8c979d","#efba53","#8b9298","#14212d","#ffffff"};
  if("original".equals(id))colors=night?new String[]{"#14252f","#355878","#24482f","#394853","#c7d8ef","#526578","#e9c26b","#74868b","#eef5fa","#10202c"}:new String[]{"#e9eee7","#afd5e8","#bdd6ab","#d9d2c8","#ffffff","#adb5ae","#f2c16a","#a6a5a3","#34403b","#ffffff"};
  try{for(int i=0;i<KEYS.length;i++)p.put(KEYS[i],colors[i]);
   if("ocean".equals(id)){p.put("background",night?"#071b2c":"#eef8ff");p.put("water",night?"#0a5c96":"#72c8f3");p.put("motorway",night?"#6de7ff":"#d8a343");}
   if("emerald".equals(id)){p.put("background",night?"#0a211c":"#f0f8ef");p.put("water",night?"#17646a":"#8ed8d9");p.put("landcover",night?"#175139":"#abd79a");p.put("motorway",night?"#b3ee75":"#e4ad45");}
   if("amber".equals(id)){p.put("background",night?"#211709":"#fff7e8");p.put("water",night?"#29444a":"#b5d6dc");p.put("road",night?"#ffdab0":"#fffdf6");p.put("motorway",night?"#ffa546":"#d88820");}
   p.put("rasterSaturation",night?("original".equals(id)?-.04:"readable".equals(id)?-.12:-.3):("original".equals(id)?0:"readable".equals(id)?-.22:-.44));p.put("rasterContrast",night?.14:"original".equals(id)?0:.08);p.put("rasterBrightness",night?.72:1);
   p.put("rasterHue","ocean".equals(id)?20:"emerald".equals(id)?325:"amber".equals(id)?180:0);
   if("custom".equals(id)){JSONObject draft=new JSONObject(custom==null?"{}":custom);for(String key:KEYS){String v=draft.optString(key,"");if(v.matches("#[a-fA-F0-9]{6}"))p.put(key,v.toLowerCase(java.util.Locale.ROOT));}double hue=draft.optDouble("rasterHue",0);p.put("rasterHue",Double.isFinite(hue)?Math.max(0,Math.min(360,hue)):0);}
  }catch(Exception ignored){}return p;
 }
}
