package fr.roadguard.app;

import android.content.Context;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.*;

/** Private control locations. Polling never counts as another observation. */
final class ControlMemory {
  static final String FILE="control-memory-v1.json";
  static final long HOUR=3600000L, DEFAULT_TTL=24*HOUR, MANUAL_GAP=30*60000L;
  static final int REQUIRED=3, MAX_PLACES=3000;
  private static final ConcurrentHashMap<String,Object> LOCKS=new ConcurrentHashMap<>();
  private static final Pattern UUID=Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
  private static Object lock(Context c){return LOCKS.computeIfAbsent(c.getFilesDir().getAbsolutePath(),s->new Object());}
  static long ttl(Context c){int hours=c.getSharedPreferences("roadguard_controls",0).getInt("ttl_hours",24);return (hours==24||hours==168||hours==720?hours:24)*HOUR;}
  static final class Observation {
    final String id,kind; final double lat,lon; final long at;
    Observation(String source,String eventId,String type,double latitude,double longitude,long time){
      Matcher m=UUID.matcher(eventId==null?"":eventId);String normalized=m.find()?m.group().toLowerCase(Locale.ROOT):eventId;
      String family=("openweb".equals(source)||"wazeapi".equals(source))?"waze":source;
      id=family+":"+normalized;kind=type;lat=latitude;lon=longitude;at=time;
    }
    boolean valid(long now,long ttl){return id.length()>3&&id.length()<=240&&!id.endsWith(":null")&&!id.endsWith(":")&&CommunityPolicy.coordinate(lat,lon)&&at>0&&at<=now+5000&&now-at<ttl&&("police".equals(kind)||"mobile".equals(kind));}
  }
  static JSONObject read(Context c)throws Exception{
    if(!new File(c.getFilesDir(),FILE).exists()&&!new File(c.getFilesDir(),FILE+".bak").exists())return new JSONObject().put("version",1).put("places",new JSONArray());
    JSONObject state=new JSONObject(new String(CommunityFiles.read(c.getFilesDir(),FILE,4_000_000),StandardCharsets.UTF_8));
    if(state.optInt("version")!=1||state.optJSONArray("places")==null||state.getJSONArray("places").length()>MAX_PLACES)throw new IOException("Mémoire des contrôles illisible : fichier conservé");return state;
  }
  private static void write(Context c,JSONObject state)throws Exception{byte[] bytes=state.toString().getBytes(StandardCharsets.UTF_8);if(bytes.length>4_000_000)throw new IOException("Mémoire des contrôles pleine");CommunityFiles.write(c.getFilesDir(),FILE,bytes);}
  private static boolean permanent(JSONObject p){return p.optJSONArray("events")!=null&&p.optJSONArray("events").length()>=REQUIRED;}
  private static boolean visible(JSONObject p,long now,long ttl){return permanent(p)||p.optLong("last")+ttl>now;}
  static double meters(double a,double b,double x,double y){double dlat=Math.toRadians(x-a),dlon=Math.toRadians(y-b);double h=Math.sin(dlat/2)*Math.sin(dlat/2)+Math.cos(Math.toRadians(a))*Math.cos(Math.toRadians(x))*Math.sin(dlon/2)*Math.sin(dlon/2);return 12742000*Math.asin(Math.sqrt(Math.min(1,h)));}
  static boolean ingest(Context c,List<Observation> input,long now)throws Exception{
    synchronized(lock(c)){
      JSONObject state=read(c);JSONArray old=state.getJSONArray("places"),places=new JSONArray();boolean changed=false;long ttl=ttl(c);
      for(int i=0;i<old.length();i++){JSONObject p=old.optJSONObject(i);if(p!=null&&visible(p,now,ttl))places.put(p);else changed=true;}
      JSONArray dismissed=state.optJSONArray("dismissed");
      for(Observation o:input){if(!o.valid(now,ttl))continue;
        boolean removed=false;if(dismissed!=null)for(int i=0;i<dismissed.length();i++){JSONObject d=dismissed.getJSONObject(i);if(o.at<=d.optLong("through")&&meters(o.lat,o.lon,d.getDouble("lat"),d.getDouble("lon"))<=40){removed=true;break;}}if(removed)continue;
        JSONObject found=null;boolean duplicate=false;
        for(int i=0;i<places.length();i++){JSONObject p=places.getJSONObject(i);JSONArray events=p.getJSONArray("events");for(int j=0;j<events.length();j++)if(o.id.equals(events.getJSONObject(j).optString("id"))){duplicate=true;break;}if(duplicate)break;
          if(meters(o.lat,o.lon,p.getDouble("lat"),p.getDouble("lon"))<=40&&(found==null||meters(o.lat,o.lon,p.getDouble("lat"),p.getDouble("lon"))<meters(o.lat,o.lon,found.getDouble("lat"),found.getDouble("lon"))))found=p;
        }
        if(duplicate)continue;
        if(found==null){if(places.length()>=MAX_PLACES)throw new IOException("Mémoire pleine : supprimez un ancien lieu avant de continuer");found=new JSONObject().put("id",java.util.UUID.randomUUID().toString()).put("lat",o.lat).put("lon",o.lon).put("kind",o.kind).put("first",o.at).put("last",o.at).put("events",new JSONArray());places.put(found);}
        if(o.id.startsWith("local:")&&found.optLong("manualAt",0)>0&&now-found.optLong("manualAt")<MANUAL_GAP){found.put("last",Math.max(found.optLong("last"),o.at));changed=true;continue;}
        JSONArray events=found.getJSONArray("events");
        // Different mirrors/new IDs describing the same nearby observation are not extra votes.
        boolean sameTime=false;for(int j=0;j<events.length();j++)if(Math.abs(events.getJSONObject(j).getLong("at")-o.at)<120000)sameTime=true;
        if(o.id.startsWith("local:")){found.put("manualAt",now);changed=true;}
        if(!permanent(found)&&!sameTime){events.put(new JSONObject().put("id",o.id).put("at",o.at));changed=true;}
        if(o.at>found.getLong("last")){found.put("last",o.at);changed=true;}
      }
      if(changed){state.put("places",places);write(c,state);}return changed;
    }
  }
  static JSONArray snapshot(Context c,long now)throws Exception{synchronized(lock(c)){JSONObject state=read(c);JSONArray result=new JSONArray(),keep=new JSONArray(),all=state.getJSONArray("places");long ttl=ttl(c);for(int i=0;i<all.length();i++){JSONObject p=all.getJSONObject(i);if(!visible(p,now,ttl))continue;keep.put(p);JSONObject item=new JSONObject(p.toString());item.put("permanent",permanent(p)).put("confirmations",Math.min(REQUIRED,p.getJSONArray("events").length())).put("expires",permanent(p)?0:p.getLong("last")+ttl).put("recentUntil",p.getLong("last")+ttl);result.put(item);}if(keep.length()!=all.length()){state.put("places",keep);write(c,state);}return result;}}
  static String label(JSONObject p,long now){return p.optLong("recentUntil")>now?("mobile".equals(p.optString("kind"))?"Radar mobile signalé":"Contrôle mobile signalé"):"Zone de contrôle possible · lieu mémorisé";}
  static boolean confirm(Context c,String id,double lat,double lon,long now)throws Exception{
    synchronized(lock(c)){JSONObject state=read(c);JSONArray places=state.getJSONArray("places");for(int i=0;i<places.length();i++){JSONObject p=places.getJSONObject(i);if(!id.equals(p.optString("id")))continue;if(!visible(p,now,ttl(c)))throw new IOException("Ce point temporaire a expiré");if(!CommunityPolicy.coordinate(lat,lon)||meters(lat,lon,p.getDouble("lat"),p.getDouble("lon"))>100)throw new IOException("Approchez-vous à moins de100m pour confirmer");long manual=p.optLong("manualAt",0);boolean newObservation=manual==0||now-manual>=MANUAL_GAP;JSONArray events=p.getJSONArray("events");if(newObservation&&!permanent(p))events.put(new JSONObject().put("id","manual:"+now).put("at",now));if(newObservation)p.put("manualAt",now);p.put("last",now);write(c,state);return true;}throw new IOException("Lieu introuvable");}
  }
  static void remove(Context c,String id)throws Exception{synchronized(lock(c)){JSONObject state=read(c);JSONArray keep=new JSONArray(),dismissed=new JSONArray(),oldDismissed=state.optJSONArray("dismissed"),all=state.getJSONArray("places");long now=System.currentTimeMillis();if(oldDismissed!=null)for(int i=Math.max(0,oldDismissed.length()-MAX_PLACES+1);i<oldDismissed.length();i++){JSONObject d=oldDismissed.getJSONObject(i);if(now-d.optLong("through")<720*HOUR)dismissed.put(d);}for(int i=0;i<all.length();i++){JSONObject p=all.getJSONObject(i);if(!id.equals(p.optString("id")))keep.put(p);else dismissed.put(new JSONObject().put("lat",p.getDouble("lat")).put("lon",p.getDouble("lon")).put("through",now));}state.put("places",keep).put("dismissed",dismissed);write(c,state);}}
  static void append(Context c,List<MainActivity.Danger> target){try{long now=System.currentTimeMillis();JSONArray all=snapshot(c,now);for(int i=0;i<all.length();i++){JSONObject p=all.getJSONObject(i);MainActivity.Danger d=new MainActivity.Danger(p.getDouble("lat"),p.getDouble("lon"),label(p,now));d.controlId=p.getString("id");d.expires=p.optLong("expires");target.add(d);}}catch(Exception e){android.util.Log.w("ROADGUARD","Lecture de la mémoire des contrôles indisponible");}}
  private ControlMemory(){}
}
