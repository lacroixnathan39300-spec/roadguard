package fr.roadguard.app;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import java.io.*;
import java.net.HttpURLConnection;
import java.util.*;
import org.json.*;

/** A complete immutable snapshot is built off the car UI thread before publication. */
final class CarRouteData {
  final Context context;final SharedPreferences prefs;
  List<double[]> route=new ArrayList<>();List<Turn> turns=new ArrayList<>();
  final List<Radar> radars=new ArrayList<>();DangerSpatialIndex radarIndex=new DangerSpatialIndex(new double[0],new double[0]);
  double seconds=0;String label="Destination",snapshotKey="",catalogRevision="";
  List<BundledRadarCatalog.Point> catalog=Collections.emptyList();
  static final class Turn {final int index;final String text;Turn(int i,String t){index=i;text=t;}}
  static final class Radar {final double lat,lon;final String type;final long expires;final int speedLimit;Radar(double a,double b,String t,long e){this(a,b,t,e,RadarAlertPolicy.labelLimit(t));}Radar(double a,double b,String t,long e,int limit){lat=a;lon=b;type=t;expires=e;speedLimit=RadarAlertPolicy.applicableLimit(t,limit);}}
  CarRouteData(Context c){context=c;RadarSettings.restoreFullFrance(c);prefs=c.getSharedPreferences("MainActivity",0);}
  void loadRoute(){
    List<double[]> points=new ArrayList<>();List<Turn> steps=new ArrayList<>();double duration=0;
    try{snapshotKey=prefs.getString("cached_navigation","");JSONObject o=new JSONObject(snapshotKey);String[] dest=prefs.getString("destination","").split(",");
      if(dest.length!=2||Math.abs(o.getDouble("lat")-Double.parseDouble(dest[0]))>.0001||Math.abs(o.getDouble("lon")-Double.parseDouble(dest[1]))>.0001)throw new IOException("Destination différente");
      JSONArray a=o.getJSONArray("points");if(a.length()<2||a.length()>RouteRequestPolicy.MAX_ROUTE_POINTS)throw new IOException("Tracé invalide");
      for(int i=0;i<a.length();i++){JSONArray p=a.getJSONArray(i);double lat=p.getDouble(0),lon=p.getDouble(1);if(!RouteRequestPolicy.coordinate(lat,lon))throw new IOException("Coordonnées invalides");points.add(new double[]{lat,lon});}
      duration=o.optDouble("seconds",0);if(!Double.isFinite(duration)||duration<0||duration>RouteRequestPolicy.MAX_ROUTE_SECONDS)throw new IOException("Durée invalide");
      JSONArray t=o.optJSONArray("steps");int previous=0;if(t!=null){if(t.length()>5000)throw new IOException("Trop d’étapes");for(int i=0;i<t.length();i++){JSONObject item=t.getJSONObject(i);int index=item.getInt("index");String text=item.getString("text");if(index<previous||index>=points.size()||text.length()>400)throw new IOException("Étape invalide");steps.add(new Turn(index,text));previous=index;}}
      route=points;turns=steps;seconds=duration;label=prefs.getString("destination_label","Destination");
    }catch(Exception e){route=new ArrayList<>();turns=new ArrayList<>();seconds=0;}
  }
  boolean loadRadars(){
    final String revision=RadarCatalogRevision.current(context);
    final List<BundledRadarCatalog.Point> base;
    try{base=InstalledRadarCatalog.active(context,prefs.getBoolean(BundledRadarCatalog.HISTORICAL_PREFERENCE,false),prefs.getBoolean(BundledRadarCatalog.SWISS_HISTORICAL_PREFERENCE,true),Collections.emptyList());}
    catch(Exception unavailable){android.util.Log.w("ROADGUARD_RADARS","Catalogue voiture précédent conservé",unavailable);return false;}
    radars.clear();catalog=base;catalogRevision=revision;
    for(BundledRadarCatalog.Point p:base)radars.add(new Radar(p.lat,p.lon,p.type,0,p.speedKmh));
    try{JSONArray a=new JSONArray(prefs.getString("custom_radars","[]"));for(int i=0;i<a.length()&&i<20000;i++){JSONObject r=a.getJSONObject(i);add(r.optDouble("lat",Double.NaN),r.optDouble("lon",Double.NaN),r.optString("type"),r.optLong("expires",0));}}catch(Exception ignored){}
    try{JSONObject cache=new JSONObject(new String(CommunityFiles.read(context.getFilesDir(),"community-reports.json",2_000_000),"UTF-8"));JSONArray a=cache.optJSONArray("reports");if(a!=null)for(int i=0;i<a.length()&&i<10000;i++){JSONObject r=a.getJSONObject(i);if(r.optLong("expires")>System.currentTimeMillis()&&"active".equals(r.optString("status","active")))add(r.optDouble("lat"),r.optDouble("lng"),CommunityPolicy.label(r.optString("kind")),r.optLong("expires"));}}catch(Exception ignored){}
    try{long now=System.currentTimeMillis();JSONArray memory=ControlMemory.snapshot(context,now);for(int i=0;i<memory.length();i++){JSONObject p=memory.getJSONObject(i);add(p.getDouble("lat"),p.getDouble("lon"),ControlMemory.label(p,now),p.optLong("expires"));}}catch(Exception ignored){}
    double[] lat=new double[radars.size()],lon=new double[radars.size()];for(int i=0;i<lat.length;i++){lat[i]=radars.get(i).lat;lon[i]=radars.get(i).lon;}radarIndex=new DangerSpatialIndex(lat,lon);
    return true;
  }
  private void add(double a,double b,String type,long expiry){if(RouteRequestPolicy.coordinate(a,b)&&DangerTypePolicy.isValidLabel(type)&&(expiry==0||expiry>System.currentTimeMillis()))radars.add(new Radar(a,b,type,expiry));}
  Radar nearest(Location fix){return nearest(fix,false);}
  Radar nearestRadar(Location fix){return nearest(fix,true);}
  private Radar nearest(Location fix,boolean onlyRadar){Radar best=null;double min=5001;for(int i:radarIndex.query(fix.getLatitude(),fix.getLongitude(),5000)){Radar r=radars.get(i);if(onlyRadar&&!DangerTypePolicy.isRadar(r.type))continue;if(r.expires>0&&r.expires<=System.currentTimeMillis())continue;Location p=new Location("radar");p.setLatitude(r.lat);p.setLongitude(r.lon);double d=fix.distanceTo(p);if(fix.hasBearing()&&fix.hasSpeed()&&fix.getSpeed()>2.8&&Math.abs(((fix.bearingTo(p)-fix.getBearing()+540)%360)-180)>105)continue;if(d<min){min=d;best=r;}}return best;}
  static String get(String url,int limit)throws IOException{
    HttpURLConnection c=SecureHttp.open(url,8000,10000,"ROADGUARD/11.14.179 AndroidAuto");
    try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(out.size()+n>limit)throw new IOException("Réponse trop grande");out.write(buffer,0,n);}return out.toString("UTF-8");}finally{c.disconnect();}
  }
  static JSONObject calculate(Location from,double lat,double lon)throws Exception{
    JSONObject root=new JSONObject(get(RouteRequestPolicy.osrmUrl(from.getLatitude(),from.getLongitude(),lat,lon),1500000));
    if(!"Ok".equals(root.optString("code")))throw new IOException("Itinéraire indisponible");JSONObject r=root.getJSONArray("routes").getJSONObject(0);JSONArray coords=r.getJSONObject("geometry").getJSONArray("coordinates");
    if(coords.length()<2||coords.length()>RouteRequestPolicy.MAX_ROUTE_POINTS)throw new IOException("Tracé trop grand");JSONArray points=new JSONArray();List<double[]> geometry=new ArrayList<>();
    for(int i=0;i<coords.length();i++){JSONArray c=coords.getJSONArray(i);double a=c.getDouble(1),b=c.getDouble(0);if(!RouteRequestPolicy.coordinate(a,b))throw new IOException("Coordonnées invalides");points.put(new JSONArray().put(a).put(b));geometry.add(new double[]{a,b});}
    double meters=r.getDouble("distance"),seconds=r.getDouble("duration");if(!Double.isFinite(meters)||!Double.isFinite(seconds)||meters<0||meters>RouteRequestPolicy.MAX_ROUTE_METERS||seconds<0||seconds>RouteRequestPolicy.MAX_ROUTE_SECONDS)throw new IOException("Distance invalide");
    JSONArray steps=new JSONArray(),legs=r.getJSONArray("legs");int earliest=0;
    for(int l=0;l<legs.length();l++){JSONArray a=legs.getJSONObject(l).getJSONArray("steps");for(int i=0;i<a.length();i++){JSONObject st=a.getJSONObject(i),m=st.getJSONObject("maneuver");JSONArray pos=m.getJSONArray("location");double x=pos.getDouble(1),y=pos.getDouble(0);double min=Double.MAX_VALUE;int index=earliest;for(int k=earliest;k<geometry.size();k++){double[] p=geometry.get(k);double d=(p[0]-x)*(p[0]-x)+(p[1]-y)*(p[1]-y);if(d<min){min=d;index=k;}}earliest=index;
      String type=m.optString("type"),mod=m.optString("modifier"),road=st.optString("name");String text="arrive".equals(type)?"Arrivée à destination":"depart".equals(type)?"Départ":"roundabout".equals(type)?(m.optInt("exit",0)>0&&m.optInt("exit",0)<=20?"Au rond-point, prendre la sortie "+m.optInt("exit"):"Au rond-point, suivre la sortie indiquée"):mod.contains("left")?"Tourner à gauche":mod.contains("right")?"Tourner à droite":"Continuer";if(!road.isEmpty())text+=" · "+road.substring(0,Math.min(120,road.length()));steps.put(new JSONObject().put("lat",x).put("lon",y).put("text",text).put("index",index));
    }}
    return new JSONObject().put("lat",lat).put("lon",lon).put("meters",meters).put("seconds",seconds).put("points",points).put("steps",steps);
  }
}
