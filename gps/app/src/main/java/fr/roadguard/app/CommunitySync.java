package fr.roadguard.app;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.AtomicFile;
import android.util.Base64;
import android.widget.Toast;
import android.widget.EditText;
import android.widget.LinearLayout;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Community traffic uses a fixed HTTPS origin. Private positions are never posted without opt-in. */
final class CommunitySync {
 static final String ORIGIN="https://roadguard-community.lacroixnathan39300.chatgpt.site";
 final MainActivity a;final SharedPreferences p;final Handler handler=new Handler(Looper.getMainLooper());
 final ExecutorService executor=Executors.newSingleThreadExecutor();
 volatile boolean foreground=false,ready=false,destroyed=false,fetching=false;
 volatile boolean inventoryUpdating=false;volatile String inventoryStatus="";
 volatile boolean paired=false;volatile String token="",deviceId="",status="Données communautaires en attente";
 volatile JSONArray reports=new JSONArray(),members=new JSONArray(),clusters=new JSONArray(),ownReports=new JSONArray(),inventory=new JSONArray();
 JSONObject serverPreferences=new JSONObject();
 long lastReports=0,lastPeople=0,lastPresence=0,lastPairPoll=0,lastPreferences=0,lastExpiryCheck=0,lastSessionCheck=0;
 double viewLat=Double.NaN,viewLon=Double.NaN,fetchedLat=Double.NaN,fetchedLon=Double.NaN;int viewZoom=14;
 boolean cacheLoaded=false;String pendingReportId="",pendingReportKind="";long pendingReportAt=0;
 final Runnable pulse=new Runnable(){public void run(){if(destroyed||!foreground)return;tick();handler.postDelayed(this,1000);}};
 CommunitySync(MainActivity activity){a=activity;p=a.getSharedPreferences("roadguard_community",0);try{token=decrypt(p.getString("credential",""));deviceId=p.getString("device_id","");paired=!token.isEmpty()&&p.getBoolean("approved",false);}catch(Exception e){p.edit().remove("credential").remove("approved").apply();token="";}}
 static final class ApiFailure extends IOException{final int code;final long retryAfterMs;ApiFailure(int c,String m){this(c,m,0);}ApiFailure(int c,String m,long retry){super(m);code=c;retryAfterMs=retry;}}
 private SecretKey key()throws Exception{KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);if(ks.containsAlias("ROADGUARD_COMMUNITY"))return (SecretKey)ks.getKey("ROADGUARD_COMMUNITY",null);KeyGenerator g=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");g.init(new KeyGenParameterSpec.Builder("ROADGUARD_COMMUNITY",KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setKeySize(256).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());return g.generateKey();}
 private String encrypt(String text)throws Exception{Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key());return Base64.encodeToString(c.getIV(),Base64.NO_WRAP)+":"+Base64.encodeToString(c.doFinal(text.getBytes(StandardCharsets.UTF_8)),Base64.NO_WRAP);}
 private String decrypt(String stored)throws Exception{if(stored==null||stored.isEmpty())return "";String[] pair=stored.split(":",2);if(pair.length!=2)throw new IOException("Invalid credential");Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(pair[0],Base64.NO_WRAP)));return new String(c.doFinal(Base64.decode(pair[1],Base64.NO_WRAP)),StandardCharsets.UTF_8);}
 byte[] requestBytes(String path,String method,JSONObject body,String idempotency,long deadline,int maxBytes)throws Exception{
  if(destroyed)throw new IOException("Application fermée");if(!path.startsWith("/")||path.startsWith("//")||path.contains("\r")||path.contains("\n"))throw new IOException("Adresse invalide");
  long left=deadline-System.currentTimeMillis();if(left<=0)throw new IOException("Délai de synchronisation dépassé");
  HttpURLConnection c=(HttpURLConnection)new URL(ORIGIN+path).openConnection();c.setInstanceFollowRedirects(false);c.setUseCaches(false);c.setConnectTimeout((int)Math.min(4000,left));c.setReadTimeout((int)Math.min(6000,left));c.setRequestMethod(method);c.setRequestProperty("User-Agent","ROADGUARD-Android/11.14.276");c.setRequestProperty("Accept","application/json");c.setRequestProperty("Cache-Control","no-cache");
  if((path.startsWith("/api/v1/")||path.startsWith("/api/updates/"))&&!token.isEmpty())c.setRequestProperty("Authorization","Bearer "+token);
  if(idempotency!=null)c.setRequestProperty("Idempotency-Key",idempotency);
  try{if(body!=null){byte[] bytes=body.toString().getBytes(StandardCharsets.UTF_8);c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");c.setFixedLengthStreamingMode(bytes.length);try(OutputStream out=c.getOutputStream()){out.write(bytes);}}
   int code=c.getResponseCode();InputStream source=code>=200&&code<300?c.getInputStream():c.getErrorStream();ByteArrayOutputStream out=new ByteArrayOutputStream();if(source!=null)try(InputStream in=source){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>maxBytes)throw new IOException("Réponse trop volumineuse");if(System.currentTimeMillis()>deadline||destroyed)throw new IOException("Délai dépassé");out.write(b,0,n);}}
   byte[] bytes=out.toByteArray();if(code<200||code>=300){String message="Connexion communautaire indisponible ("+code+")";try{message=new JSONObject(new String(bytes,StandardCharsets.UTF_8)).optString("error",message);}catch(Exception ignored){}long wait=0;try{wait=Math.min(3600000L,Math.max(0,Long.parseLong(c.getHeaderField("Retry-After"))*1000L));}catch(Exception ignored){}throw new ApiFailure(code,message,wait);}return bytes;
  }finally{c.disconnect();}
 }
 void downloadRelease(String path,File target,long expected,java.util.concurrent.atomic.AtomicBoolean cancel,java.util.function.LongConsumer progress)throws Exception{
  if(!path.matches("/api/updates/download/(drive|gps)-apk-[a-f0-9]{64}"))throw new IOException("Adresse invalide");HttpURLConnection c=(HttpURLConnection)new URL(ORIGIN+path).openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(10000);c.setReadTimeout(20000);c.setRequestProperty("Authorization","Bearer "+token);c.setRequestProperty("User-Agent","ROADGUARD-Update/1.0");try{if(c.getResponseCode()!=200)throw new IOException("Serveur indisponible ("+c.getResponseCode()+")");long total=0;try(InputStream in=c.getInputStream();OutputStream out=new FileOutputStream(target)){byte[] b=new byte[65536];int n;long last=0;while((n=in.read(b))!=-1){if(cancel.get()||destroyed||Thread.currentThread().isInterrupted())throw new IOException("Annulé");total+=n;if(total>expected)throw new IOException("Taille invalide");out.write(b,0,n);if(total-last>512000){progress.accept(total);last=total;}}}if(total!=expected)throw new IOException("Fichier incomplet");progress.accept(total);}finally{c.disconnect();}
 }
 JSONObject request(String path,String method,JSONObject body,String key,long deadline)throws Exception{return new JSONObject(new String(requestBytes("/api/v1/"+path,method,body,key,deadline,1500000),StandardCharsets.UTF_8));}
 static String sha(byte[] bytes)throws Exception{StringBuilder s=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(bytes))s.append(String.format(Locale.ROOT,"%02x",b&255));return s.toString();}
 void atomic(String name,byte[] bytes)throws Exception{CommunityFiles.write(a.getFilesDir(),name,bytes);}
 byte[] read(String name,int max)throws Exception{return CommunityFiles.read(a.getFilesDir(),name,max);}
 void seedInventory(){try{JSONObject m;try(InputStream in=a.getAssets().open("community/manifest.json")){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)out.write(b,0,n);m=new JSONObject(new String(out.toByteArray(),StandardCharsets.UTF_8));}String hash=m.getString("sha256"),url=m.getString("url");if(!hash.matches("[0-9a-f]{64}")||!url.matches("/data/radars-[a-z0-9-]+\\.json"))return;byte[] bytes;try(InputStream in=a.getAssets().open("community/"+url.substring(6));ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>12000000)throw new IOException("Inventaire trop volumineux");out.write(b,0,n);}bytes=out.toByteArray();}if(!sha(bytes).equals(hash))return;inventory=new JSONArray(new String(bytes,StandardCharsets.UTF_8));atomic("community-inventory-"+hash+".json",bytes);atomic("community-inventory-manifest.json",m.toString().getBytes(StandardCharsets.UTF_8));}catch(Exception ignored){}}
 void loadCache(){if(cacheLoaded)return;cacheLoaded=true;try{JSONObject cache=new JSONObject(new String(read("community-reports.json",1500000),StandardCharsets.UTF_8));reports=active(cache.optJSONArray("reports"));fetchedLat=cache.optDouble("lat",Double.NaN);fetchedLon=cache.optDouble("lng",Double.NaN);lastReports=cache.optLong("downloaded",0);}catch(Exception ignored){}try{JSONObject m=new JSONObject(new String(read("community-inventory-manifest.json",10000),StandardCharsets.UTF_8));String hash=m.optString("sha256");if(hash.matches("[0-9a-f]{64}")){byte[] bytes=read("community-inventory-"+hash+".json",12000000);if(sha(bytes).equals(hash))inventory=new JSONArray(new String(bytes,StandardCharsets.UTF_8));}}catch(Exception ignored){}if(inventory.length()==0)seedInventory();}
 JSONArray active(JSONArray input){JSONArray result=new JSONArray();if(input==null)return result;long now=System.currentTimeMillis();for(int i=0;i<input.length();i++){JSONObject r=input.optJSONObject(i);if(r!=null&&r.optLong("expires")>now&&CommunityPolicy.coordinate(r.optDouble("lat"),r.optDouble("lng"))&&"active".equals(r.optString("status","active")))result.put(r);}return result;}
 /** Begin cache and HTTP synchronization only after the map has opened. */
 void prepareStartup(){
  executor.execute(()->{
   loadCache();ui(()->{a.reloadCommunityDangers();pushMap();ReferenceUi.refreshNow(a);});
   long deadline=System.currentTimeMillis()+CommunityPolicy.STARTUP_TIMEOUT;
   try{status="Synchronisation radar en arrière-plan…";downloadInventory(deadline);
    try{JSONObject config=request("config","GET",null,null,deadline);p.edit().putString("map_config",config.toString()).apply();}catch(Exception ignored){}
    status="Inventaire prêt · signalements dès réception du GPS";
   }catch(Exception e){status="Bases locales conservées · synchronisation réseau en attente";}
   ui(()->{a.reloadCommunityDangers();pushMap();ReferenceUi.refreshNow(a);});
  });
 }

 void downloadInventory(long deadline)throws Exception{
  JSONObject m=new JSONObject(new String(requestBytes("/data/manifest.json","GET",null,null,deadline,15000),StandardCharsets.UTF_8));String hash=m.optString("sha256"),url=m.optString("url");int count=m.optInt("count",0);
  if(!hash.matches("[0-9a-f]{64}")||!url.matches("/data/radars-[a-z0-9-]+\\.json")||count<1||count>100000)throw new IOException("Inventaire invalide");
  boolean same=false;try{JSONObject old=new JSONObject(new String(read("community-inventory-manifest.json",15000),StandardCharsets.UTF_8));same=hash.equals(old.optString("sha256"))&&inventory.length()==count;}catch(Exception ignored){}
  if(same){inventoryChecked(m);return;}byte[] bytes=requestBytes(url,"GET",null,null,deadline,12000000);if(!sha(bytes).equals(hash))throw new IOException("Inventaire incomplet : ancienne version conservée");JSONArray points=new JSONArray(new String(bytes,StandardCharsets.UTF_8));if(points.length()!=count)throw new IOException("Nombre de points invalide");for(int i=0;i<points.length();i++){JSONObject r=points.getJSONObject(i);if(!CommunityPolicy.coordinate(r.optDouble("lat"),r.optDouble("lng")))throw new IOException("Coordonnées invalides");}
  atomic("community-inventory-"+hash+".json",bytes);atomic("community-inventory-manifest.json",m.toString().getBytes(StandardCharsets.UTF_8));inventory=points;inventoryChecked(m);
  File[] files=a.getFilesDir().listFiles();if(files!=null)for(File f:files)if(f.getName().matches("community-inventory-[0-9a-f]{64}\\.json")&&!f.getName().contains(hash))f.delete();
 }
 void inventoryChecked(JSONObject manifest){p.edit().putLong("inventory_checked",System.currentTimeMillis()).putString("inventory_version",manifest.optString("version","")).apply();}
 void updateRadarInventory(){
  if(inventoryUpdating||destroyed)return;inventoryUpdating=true;inventoryStatus="Vérification des radars France et Suisse…";ReferenceUi.refreshNow(a);
  executor.execute(()->{boolean ok=false;try{loadCache();downloadInventory(System.currentTimeMillis()+15000);inventoryStatus="Base radar prête · "+inventory.length()+" points permanents vérifiés";ok=true;}catch(Exception e){inventoryStatus="Mise à jour impossible · les bases déjà installées sont conservées";}final boolean success=ok;
   ui(()->{inventoryUpdating=false;if(success)a.reloadCommunityDangers();ReferenceUi.refreshNow(a);Toast.makeText(a,inventoryStatus,Toast.LENGTH_LONG).show();});
  });
 }
 JSONObject downloadReports(double lat,double lon,long deadline)throws Exception{
  JSONArray all=new JSONArray();String cursor="";for(int page=0;page<20;page++){JSONObject r=request("reports?bbox="+CommunityPolicy.bbox(lat,lon)+(cursor.isEmpty()?"":"&cursor="+Uri.encode(cursor)),"GET",null,null,deadline);JSONArray batch=r.optJSONArray("reports");if(batch==null)throw new IOException("Signalements invalides");for(int i=0;i<batch.length();i++){JSONObject item=batch.getJSONObject(i);if(!CommunityPolicy.uuid(item.optString("id"))||!CommunityPolicy.coordinate(item.optDouble("lat"),item.optDouble("lng")))throw new IOException("Signalement invalide");all.put(item);}
   cursor=r.isNull("nextCursor")?"":r.optString("nextCursor","");if(cursor.isEmpty()){JSONObject cache=new JSONObject();cache.put("reports",active(all)).put("lat",lat).put("lng",lon).put("downloaded",System.currentTimeMillis());atomic("community-reports.json",cache.toString().getBytes(StandardCharsets.UTF_8));return cache;}
  }throw new IOException("Zone trop dense : dernière version complète conservée");
 }
 void installSnapshot(JSONObject cache,boolean refresh)throws Exception{reports=active(cache.optJSONArray("reports"));fetchedLat=cache.getDouble("lat");fetchedLon=cache.getDouble("lng");lastReports=cache.getLong("downloaded");p.edit().putFloat("area_lat",(float)(Math.round(fetchedLat*50)/50.0)).putFloat("area_lng",(float)(Math.round(fetchedLon*50)/50.0)).apply();if(refresh){a.reloadCommunityReports();pushMap();ReferenceUi.refreshNow(a);}}
 void ui(Runnable action){handler.post(()->{if(!destroyed&&!a.isFinishing()&&!a.isDestroyed())action.run();});}
 void resume(){foreground=true;lastPreferences=0;lastSessionCheck=0;handler.removeCallbacks(pulse);handler.post(pulse);}
 void pause(){foreground=false;handler.removeCallbacks(pulse);}
 void destroy(){destroyed=true;pause();handler.removeCallbacksAndMessages(null);executor.shutdownNow();members=new JSONArray();clusters=new JSONArray();}
 void initialized(){ready=true;if(a.activityResumed)resume();}
 long lastAttempt=0,lastCamera=0,retryDelay=10000;boolean job=false;
 void tick(){if(!ready)return;long now=System.currentTimeMillis();
  if(now-lastExpiryCheck>1000){lastExpiryCheck=now;if(a.expireCommunityDangers())ReferenceUi.refreshNow(a);}
  if(a.map!=null&&a.mapReady&&now-lastCamera>5000){lastCamera=now;a.map.evaluateJavascript("(function(){if(typeof map==='undefined'||!map.getCenter)return null;var c=map.getCenter();return {lat:c.lat,lng:c.lng,zoom:map.getZoom()};})()",r->{try{JSONObject c=new JSONObject(r);double lat=c.optDouble("lat",Double.NaN),lon=c.optDouble("lng",Double.NaN);if(CommunityPolicy.coordinate(lat,lon)){viewLat=lat;viewLon=lon;viewZoom=Math.max(8,Math.min(19,c.optInt("zoom",14)));}}catch(Exception ignored){}});}
  if(job||now-lastAttempt<retryDelay)return;
  if(!token.isEmpty()&&!paired&&now-lastPairPoll>=5000){lastPairPoll=now;job=true;executor.execute(()->{try{JSONObject d=request("devices/session","GET",null,null,System.currentTimeMillis()+8000);if("approved".equals(d.optString("status"))){paired=true;p.edit().putBoolean("approved",true).putString("display_name",d.optString("displayName","")).putBoolean("access_unlimited",d.optBoolean("unlimited",false)).apply();status="Compte connecté";ui(()->{Toast.makeText(a,"Compte ROADGUARD connecté",Toast.LENGTH_LONG).show();a.onAccountConnected();loadOwn();refreshNow();});}}catch(ApiFailure e){if(e.code==401){clearCredential();status="Association expirée : reconnectez le téléphone";ui(()->a.onAccountLocked());}}catch(Exception ignored){}finally{ui(()->job=false);}});return;}
  if(paired&&now-lastSessionCheck>=300000){lastSessionCheck=now;job=true;executor.execute(()->{try{JSONObject d=request("devices/session","GET",null,null,System.currentTimeMillis()+8000);if(!"approved".equals(d.optString("status"))){clearCredential();status="Connexion à confirmer de nouveau";ui(()->a.onAccountLocked());}}catch(ApiFailure e){if(e.code==401||e.code==403){clearCredential();status="Session expirée : reconnectez ce téléphone";ui(()->a.onAccountLocked());}}catch(Exception ignored){}finally{ui(()->job=false);}});return;}
  double lat=CommunityPolicy.coordinate(viewLat,viewLon)?viewLat:a.lastLat,lon=CommunityPolicy.coordinate(viewLat,viewLon)?viewLon:a.lastLon;
  if(!CommunityPolicy.coordinate(lat,lon))return;
  boolean moved=!CommunityPolicy.coordinate(fetchedLat,fetchedLon)||Math.abs(lat-fetchedLat)>.09||Math.abs(lon-fetchedLon)>.09;
  boolean fetchInventory=CommunityPolicy.due(p.getLong("inventory_checked",0),now,21600000L+Math.floorMod(deviceId.hashCode(),600001));
  boolean fetchReports=moved||CommunityPolicy.due(lastReports,now,CommunityPolicy.REPORT_INTERVAL+Math.floorMod(deviceId.hashCode(),60001));
  boolean people=p.getBoolean("show_members",true)&&(moved||CommunityPolicy.due(lastPeople,now,CommunityPolicy.PRESENCE_INTERVAL));
  boolean prefs=paired&&CommunityPolicy.due(lastPreferences,now,CommunityPolicy.PRESENCE_INTERVAL);
  boolean share=paired&&p.getBoolean("share_position",false)&&CommunityPolicy.due(lastPresence,now,CommunityPolicy.PRESENCE_INTERVAL)&&a.hasFreshPreciseLocation();
  if(!fetchReports&&!fetchInventory&&!people&&!prefs&&!share)return;
  lastAttempt=now;job=true;final double centerLat=lat,centerLon=lon;final int zoom=viewZoom;final Location fix=a.previousAcceptedGps==null?null:new Location(a.previousAcceptedGps);
  executor.execute(()->{try{
   if(prefs)syncPreferences();
   if(fetchInventory){downloadInventory(System.currentTimeMillis()+20000);ui(()->{a.reloadCommunityDangers();ReferenceUi.refreshNow(a);});}
   if(fetchReports){JSONObject snapshot=downloadReports(centerLat,centerLon,System.currentTimeMillis()+12000);ui(()->{try{installSnapshot(snapshot,true);status="Signalements à jour · "+reports.length()+" dans ce secteur";}catch(Exception ignored){}});}
   if(people){JSONObject d=request("presence?bbox="+CommunityPolicy.bbox(centerLat,centerLon)+"&zoom="+zoom,"GET",null,null,System.currentTimeMillis()+8000);lastPeople=System.currentTimeMillis();ui(()->{members=d.optJSONArray("members")==null?new JSONArray():d.optJSONArray("members");clusters=d.optJSONArray("clusters")==null?new JSONArray():d.optJSONArray("clusters");pushMap();ReferenceUi.refreshNow(a);});}
   if(share&&foreground&&fix!=null&&p.getBoolean("share_position",false))postPresence(fix,false);retryDelay=10000;
  }catch(ApiFailure e){lastAttempt=System.currentTimeMillis();retryDelay=Math.max(e.retryAfterMs,Math.min(600000,Math.max(30000,retryDelay*2)))+java.util.concurrent.ThreadLocalRandom.current().nextLong(15001);status=(e.code==429||e.code==503)?"Serveur occupé · reprise dans "+((retryDelay+999)/1000)+" s · données locales conservées":e.getMessage();if(e.code==401){clearCredential();ui(()->a.onAccountLocked());}if(e.code==409){p.edit().putBoolean("share_position",false).remove("presence_lease").apply();ui(()->Toast.makeText(a,e.getMessage(),Toast.LENGTH_LONG).show());}}
   catch(Exception e){lastAttempt=System.currentTimeMillis();retryDelay=Math.min(600000,Math.max(30000,retryDelay*2))+java.util.concurrent.ThreadLocalRandom.current().nextLong(15001);status="Données locales conservées · synchronisation en attente";}
   finally{ui(()->{job=false;ReferenceUi.refreshNow(a);});}
  });
 }
 void refreshNow(){lastReports=0;lastPeople=0;lastPreferences=0;lastAttempt=0;tick();}
 void appendDangers(ArrayList<MainActivity.Danger> target){
  long now=System.currentTimeMillis();JSONArray snapshot=inventory;for(int i=0;i<snapshot.length();i++){JSONObject r=snapshot.optJSONObject(i);if(r==null)continue;double lat=r.optDouble("lat",Double.NaN),lon=r.optDouble("lng",Double.NaN);if(!CommunityPolicy.coordinate(lat,lon)||a.hasNearbyRadar(lat,lon))continue;String type=CommunityPolicy.label(r.optString("kind"));int speed=r.optInt("speed",0);if(speed>0&&speed<=300)type+=" · "+speed+" km/h";target.add(new MainActivity.Danger(lat,lon,type));if(i%200==0)a.rebuildDangerSpatialIndex();}
  appendReports(target);
 }
 void appendReports(ArrayList<MainActivity.Danger> target){long now=System.currentTimeMillis();JSONArray snapshot=reports;
  for(int i=0;i<snapshot.length();i++){JSONObject r=snapshot.optJSONObject(i);if(r==null||r.optLong("expires")<=now)continue;double lat=r.optDouble("lat",Double.NaN),lon=r.optDouble("lng",Double.NaN);if(!CommunityPolicy.coordinate(lat,lon))continue;String kind=r.optString("kind");MainActivity.Danger d=new MainActivity.Danger(lat,lon,CommunityPolicy.reportLabel(kind,r.optLong("lastConfirmed",r.optLong("created")),now)+" · communauté");d.expires=r.optLong("expires");d.communityId=r.optString("id");target.add(d);}
 }
 JSONObject appearance(){JSONObject j=new JSONObject();try{j.put("menuPhoto",p.getBoolean("menu_photo",true));j.put("accent",CommunityPolicy.color(p.getString("accent","#ff304f"),"#ff304f")).put("routeColor",CommunityPolicy.color(p.getString("route_color","#329dff"),"#329dff")).put("cursor",p.getString("cursor","arrow"));}catch(Exception ignored){}return j;}
 void pushMap(){if(a.map==null||!a.mapReady)return;try{JSONArray live=new JSONArray(),groups=new JSONArray();long now=System.currentTimeMillis();if(p.getBoolean("show_members",true)){for(int i=0;i<members.length();i++){JSONObject item=members.optJSONObject(i);if(item!=null&&CommunityPolicy.visible(item.optLong("expires"),now))live.put(item);}for(int i=0;i<clusters.length();i++){JSONObject item=clusters.optJSONObject(i);if(item!=null&&CommunityPolicy.visible(item.optLong("expires"),now))groups.put(item);}}
  a.map.evaluateJavascript("if(typeof setCommunityMembers==='function')setCommunityMembers("+live+","+groups+");if(typeof setRoadguardAppearance==='function')setRoadguardAppearance("+appearance()+");if(typeof setCommunityTileProvider==='function')setCommunityTileProvider("+p.getString("map_config","{}")+")",null);
 }catch(Exception ignored){}}
 JSONObject data(){JSONObject j=new JSONObject();try{JSONArray nearby=new JSONArray();JSONArray activeReports=active(reports);for(int i=0;i<activeReports.length()&&i<40;i++)nearby.put(activeReports.optJSONObject(i));j.put("inventoryUpdating",inventoryUpdating).put("inventoryStatus",inventoryStatus).put("inventoryChecked",p.getLong("inventory_checked",0)).put("inventoryVersion",p.getString("inventory_version","")).put("paired",paired).put("unlimited",p.getBoolean("access_unlimited",false)).put("pending",!token.isEmpty()&&!paired).put("status",status).put("reports",nearby).put("reportCount",activeReports.length()).put("own",ownReports).put("sharing",p.getBoolean("share_position",false)).put("showMembers",p.getBoolean("show_members",true)).put("expires",p.getLong("presence_expires",0)).put("updated",lastReports).put("appearance",appearance()).put("inventory",inventory.length()).put("voice",a.getPreferences(0).getString("native_voice_label","Voix française de l’appareil"));int count=0;long now=System.currentTimeMillis();for(int i=0;i<members.length();i++)if(members.optJSONObject(i)!=null&&members.optJSONObject(i).optLong("expires")>now)count++;for(int i=0;i<clusters.length();i++)if(clusters.optJSONObject(i)!=null&&clusters.optJSONObject(i).optLong("expires")>now)count+=clusters.optJSONObject(i).optInt("count");j.put("memberCount",count);}catch(Exception ignored){}return j;}
 void link(){
  if(paired)return;
  new AlertDialog.Builder(a).setTitle("Mon compte ROADGUARD")
   .setMessage("Connectez-vous directement dans ROADGUARD avec votre numéro et votre mot de passe. Aucun navigateur ni SMS n’est nécessaire.")
   .setNegativeButton("Annuler",null)
   .setNeutralButton("Créer un compte",(d,w)->accountForm(true))
   .setPositiveButton("Se connecter",(d,w)->accountForm(false)).show();
 }
 void accountForm(boolean register){
  LinearLayout fields=new LinearLayout(a);fields.setOrientation(LinearLayout.VERTICAL);
  int margin=(int)(18*a.getResources().getDisplayMetrics().density);fields.setPadding(margin,0,margin,0);
  EditText phone=new EditText(a);phone.setSingleLine(true);phone.setHint("Numéro : +33612345678");phone.setInputType(android.text.InputType.TYPE_CLASS_PHONE);fields.addView(phone);
  EditText name=null;
  if(register){name=new EditText(a);name.setSingleLine(true);name.setHint("Prénom ou pseudonyme");name.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS);fields.addView(name);}
  EditText password=new EditText(a);password.setSingleLine(true);password.setHint("Mot de passe · 12 caractères minimum");password.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);password.setAutofillHints(android.view.View.AUTOFILL_HINT_PASSWORD);fields.addView(password);
  final EditText display=name;
  AlertDialog dialog=new AlertDialog.Builder(a).setTitle(register?"Créer mon compte":"Me connecter")
   .setView(fields).setMessage(register?"Votre numéro sera déclaré, sans vérification par SMS. Gardez ce mot de passe : sa récupération automatique n’est pas encore disponible.":"Saisissez le numéro et le mot de passe de votre compte ROADGUARD.")
   .setNegativeButton("Annuler",null).setPositiveButton(register?"Créer le compte":"Connexion",null).create();
  dialog.setOnShowListener(shown->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String number=phone.getText().toString().trim(),secret=password.getText().toString(),nickname=display==null?"":display.getText().toString().trim();
   number=number.replaceAll("[ .()\\-]","");if(number.matches("0[0-9]{9}"))number="+33"+number.substring(1);
   if(!number.matches("\\+[1-9][0-9]{7,14}")){phone.setError("Utilisez le format international, par exemple +33612345678");return;}
   if(secret.length()<12||secret.length()>128){password.setError("12 à 128 caractères requis");return;}
   if(register&&(nickname.length()<2||nickname.length()>32)){display.setError("2 à 32 caractères requis");return;}
   dialog.dismiss();connectNative(number,secret,nickname,register);
  }));dialog.show();
 }
 void connectNative(String phone,String password,String name,boolean register){
  clearCredential();status=register?"Création du compte en cours…":"Connexion en cours…";ReferenceUi.refreshNow(a);
  executor.execute(()->{try{
   JSONObject body=new JSONObject().put("phone",phone).put("password",password).put("deviceName","ROADGUARD · "+android.os.Build.MANUFACTURER+" "+android.os.Build.MODEL);
   if(register)body.put("displayName",name);
   String signal=Settings.Secure.getString(a.getContentResolver(),Settings.Secure.ANDROID_ID);
   if(signal!=null&&signal.matches("(?i)([0-9a-f]{16}|[0-9a-f]{32})"))body.put("deviceSignal",signal);
   JSONObject result=request(register?"native-auth/register":"native-auth/login","POST",body,null,System.currentTimeMillis()+18000);
   String secret=result.getString("accessToken"),id=result.getString("id");
   if(!secret.matches("rg_[A-Za-z0-9_-]{43}")||!CommunityPolicy.uuid(id)||!"approved".equals(result.optString("status")))throw new IOException("Réponse de connexion invalide");
   String encrypted=encrypt(secret);
   if(!p.edit().putString("credential",encrypted).putString("device_id",id).putBoolean("approved",true).putString("display_name",result.optString("displayName",name)).putString("phone_last_four",phone.substring(phone.length()-4)).putBoolean("access_unlimited",result.optBoolean("unlimited",false)).commit())throw new IOException("Enregistrement impossible sur ce téléphone");
   token=secret;deviceId=id;paired=true;status="Compte connecté";
   ui(()->{Toast.makeText(a,"Compte ROADGUARD connecté",Toast.LENGTH_SHORT).show();a.onAccountConnected();loadOwn();refreshNow();checkAdmin();});
  }catch(Exception error){status=error.getMessage()==null?"Connexion impossible":error.getMessage();ui(()->{Toast.makeText(a,status,Toast.LENGTH_LONG).show();ReferenceUi.refreshNow(a);});}});
 }
 void openWeb(String path){if(!path.startsWith("/")||path.startsWith("//"))return;try{a.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(ORIGIN+path)));}catch(Exception e){Toast.makeText(a,"Aucun navigateur disponible",Toast.LENGTH_LONG).show();}}
 void clearCredential(){token="";paired=false;deviceId="";p.edit().remove("credential").remove("device_id").remove("approved").remove("admin").remove("phone_last_four").remove("presence_lease").putBoolean("share_position",false).apply();}
 void checkAdmin(){if(!paired)return;executor.execute(()->{try{JSONObject r=request("admin/me","GET",null,null,System.currentTimeMillis()+8000);p.edit().putBoolean("admin",r.optBoolean("admin",false)).apply();ui(()->ReferenceUi.refreshNow(a));}catch(Exception ignored){}});}
 void manageAccess(){if(!paired){link();return;}executor.execute(()->{try{JSONObject result=request("admin/access","GET",null,null,System.currentTimeMillis()+9000);ui(()->accessDialog(result));}catch(Exception e){ui(()->Toast.makeText(a,e.getMessage(),Toast.LENGTH_LONG).show());}});}
 void accessDialog(JSONObject result){JSONArray accounts=result.optJSONArray("accounts"),pending=result.optJSONArray("pending");StringBuilder text=new StringBuilder("Comptes enregistrés\n");if(accounts!=null)for(int i=0;i<accounts.length();i++){JSONObject row=accounts.optJSONObject(i);if(row!=null)text.append(row.optString("display_name","Membre")).append(" · …").append(row.optString("last_four")).append(row.optBoolean("admin")?" · Admin":row.optBoolean("unlimited")?" · Illimité":" · Standard").append('\n');}if(pending!=null&&pending.length()>0){text.append("\nNuméros préautorisés\n");for(int i=0;i<pending.length();i++){JSONObject row=pending.optJSONObject(i);if(row!=null)text.append("…").append(row.optString("last_four")).append(row.optBoolean("unlimited")?" · Illimité":" · Standard").append('\n');}}
  android.widget.ScrollView scroll=new android.widget.ScrollView(a);android.widget.TextView list=new android.widget.TextView(a);int pad=(int)(18*a.getResources().getDisplayMetrics().density);list.setPadding(pad,pad,pad,pad);list.setText(text.toString());scroll.addView(list);
  new AlertDialog.Builder(a).setTitle("Gestion des accès").setView(scroll).setNegativeButton("Fermer",null).setPositiveButton("Ajouter ou modifier un numéro",(d,w)->accessForm()).show();}
 void accessForm(){LinearLayout fields=new LinearLayout(a);fields.setOrientation(LinearLayout.VERTICAL);int pad=(int)(18*a.getResources().getDisplayMetrics().density);fields.setPadding(pad,0,pad,0);EditText phone=new EditText(a);phone.setInputType(android.text.InputType.TYPE_CLASS_PHONE);phone.setSingleLine(true);phone.setHint("07 70 27 95 25 ou +33770279525");fields.addView(phone);android.widget.CheckBox unlimited=new android.widget.CheckBox(a);unlimited.setText("Accès illimité");unlimited.setChecked(true);fields.addView(unlimited);AlertDialog dialog=new AlertDialog.Builder(a).setTitle("Autoriser un numéro").setMessage("Numéro conservé sous forme d’empreinte. Aucun SMS n’est envoyé.").setView(fields).setNegativeButton("Annuler",null).setPositiveButton("Enregistrer",null).create();dialog.setOnShowListener(shown->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String number=phone.getText().toString().replaceAll("[ .()\\-]","");if(number.matches("0[0-9]{9}"))number="+33"+number.substring(1);if(!number.matches("\\+[1-9][0-9]{7,14}")){phone.setError("Numéro invalide");return;}dialog.dismiss();final String target=number;executor.execute(()->{try{JSONObject response=request("admin/access","POST",new JSONObject().put("phone",target).put("unlimited",unlimited.isChecked()),null,System.currentTimeMillis()+9000);ui(()->{Toast.makeText(a,"Numéro …"+response.optString("lastFour")+" enregistré",Toast.LENGTH_LONG).show();manageAccess();});}catch(Exception e){ui(()->Toast.makeText(a,e.getMessage(),Toast.LENGTH_LONG).show());}});}));dialog.show();}
 void disconnect(){if(!paired){if(!token.isEmpty()&&CommunityPolicy.uuid(deviceId))executor.execute(()->{try{request("devices/"+deviceId,"DELETE",null,null,System.currentTimeMillis()+8000);}catch(Exception ignored){}clearCredential();ui(()->a.onAccountLocked());});else{clearCredential();a.onAccountLocked();}return;}new AlertDialog.Builder(a).setTitle("Déconnecter ce téléphone ?").setMessage("Le téléphone ne publiera plus de signalements ni de position. Les informations déjà partagées restent retirables depuis le site.").setNegativeButton("Annuler",null).setPositiveButton("Déconnecter",(d,w)->{p.edit().putBoolean("share_position",false).apply();executor.execute(()->{try{request("presence","DELETE",null,null,System.currentTimeMillis()+8000);if(CommunityPolicy.uuid(deviceId))request("devices/"+deviceId,"DELETE",null,null,System.currentTimeMillis()+8000);clearCredential();ui(()->{status="Téléphone déconnecté";ownReports=new JSONArray();a.onAccountLocked();});}catch(Exception e){ui(()->Toast.makeText(a,"Retrait en ligne non confirmé. Réessayez avec le réseau ou révoquez le téléphone depuis le site.",Toast.LENGTH_LONG).show());}});}).show();}
 void chooseName(){EditText input=new EditText(a);input.setSingleLine(true);input.setHint("Prénom ou pseudonyme");new AlertDialog.Builder(a).setTitle("Nom affiché avec tes signalements").setView(input).setNegativeButton("Annuler",null).setPositiveButton("Enregistrer",(d,w)->{String name=input.getText().toString().trim();if(name.length()<2||name.length()>32){Toast.makeText(a,"Nom de 2 à 32 caractères requis",Toast.LENGTH_LONG).show();return;}executor.execute(()->{try{JSONObject b=preferenceBody().put("displayName",name);request("preferences","PUT",b,null,System.currentTimeMillis()+8000);p.edit().putString("display_name",name).apply();ui(()->{Toast.makeText(a,"Nom enregistré",Toast.LENGTH_SHORT).show();ReferenceUi.refreshNow(a);});}catch(Exception e){ui(()->Toast.makeText(a,e.getMessage(),Toast.LENGTH_LONG).show());}});}).show();}
 void publish(String label){a.refreshLastPosition();if(!paired){Toast.makeText(a,"Connectez d’abord votre compte dans Communauté",Toast.LENGTH_LONG).show();return;}if(p.getString("display_name","").length()<2){chooseName();return;}if(!a.hasFreshPreciseLocation()){Toast.makeText(a,"Une position GPS récente à moins de 60 m est nécessaire",Toast.LENGTH_LONG).show();return;}
  String kind=CommunityPolicy.kind(label);long now=System.currentTimeMillis();if(!kind.equals(pendingReportKind)){pendingReportId=UUID.randomUUID().toString();pendingReportKind=kind;pendingReportAt=now;}final String id=pendingReportId;final Location fix=new Location(a.previousAcceptedGps);status="Envoi du signalement…";ReferenceUi.refreshNow(a);
  executor.execute(()->{try{if(!CommunityPolicy.freshFix(fix.getTime(),fix.getAccuracy(),System.currentTimeMillis()))throw new IOException("La position a expiré. Actualisez le GPS.");JSONObject b=new JSONObject();b.put("kind",kind).put("note","").put("lat",fix.getLatitude()).put("lng",fix.getLongitude()).put("accuracy",fix.getAccuracy());request("reports","POST",b,id,System.currentTimeMillis()+10000);ui(()->{pendingReportId="";pendingReportKind="";pendingReportAt=0;Toast.makeText(a,"Signalement partagé avec la communauté",Toast.LENGTH_LONG).show();a.selected="Communauté";a.render();refreshNow();loadOwn();});}catch(Exception e){ui(()->new AlertDialog.Builder(a).setTitle("Envoi non confirmé").setMessage(e.getMessage()+"\nAucun nouvel envoi automatique ne sera effectué.").setNegativeButton("Fermer",(d,w)->{pendingReportKind="";}).setNeutralButton("Garder en privé",(d,w)->{pendingReportKind="";a.saveReport(label);}).setPositiveButton("Réessayer",(d,w)->publish(label)).show());}});
 }
 void loadOwn(){if(!paired)return;executor.execute(()->{try{JSONObject d=request("me/reports","GET",null,null,System.currentTimeMillis()+8000);ui(()->{ownReports=d.optJSONArray("reports")==null?new JSONArray():d.optJSONArray("reports");ReferenceUi.refreshNow(a);});}catch(Exception ignored){}});}
 void vote(String id,String value){if(!paired){link();return;}if(!CommunityPolicy.uuid(id)||!Arrays.asList("seen","gone","flag").contains(value))return;executor.execute(()->{try{JSONObject b=new JSONObject();b.put("value",value);request("reports/"+id+"/vote","POST",b,null,System.currentTimeMillis()+8000);ui(()->{Toast.makeText(a,"Votre retour a été enregistré",Toast.LENGTH_SHORT).show();refreshNow();});}catch(Exception e){ui(()->Toast.makeText(a,e.getMessage(),Toast.LENGTH_LONG).show());}});}
 void remove(String id){if(!paired||!CommunityPolicy.uuid(id))return;new AlertDialog.Builder(a).setTitle("Retirer ce signalement partagé ?").setNegativeButton("Annuler",null).setPositiveButton("Retirer",(d,w)->executor.execute(()->{try{request("reports/"+id,"DELETE",null,null,System.currentTimeMillis()+8000);ui(()->{Toast.makeText(a,"Signalement retiré",Toast.LENGTH_SHORT).show();refreshNow();loadOwn();});}catch(Exception e){ui(()->Toast.makeText(a,e.getMessage(),Toast.LENGTH_LONG).show());}})).show();}
 void sharing(){
  if(p.getBoolean("share_position",false)||p.getLong("presence_expires",0)>System.currentTimeMillis()){
   p.edit().putBoolean("share_position",false).apply();executor.execute(()->{try{request("presence","DELETE",null,null,System.currentTimeMillis()+8000);p.edit().remove("presence_lease").remove("presence_expires").apply();ui(()->{Toast.makeText(a,"Votre position a été retirée de la carte",Toast.LENGTH_LONG).show();ReferenceUi.refreshNow(a);});}catch(Exception e){ui(()->Toast.makeText(a,"Retrait non confirmé en ligne : réessayez avec le réseau. La dernière position expirera automatiquement.",Toast.LENGTH_LONG).show());}});ReferenceUi.refreshNow(a);return;
  }
  if(!paired){link();return;}
  new AlertDialog.Builder(a).setTitle("Partager ma position ?").setMessage("Votre dernière position sera arrondie à environ 300 m et publiée sous un pseudonyme aléatoire. Mise à jour toutes les 10 minutes lorsque ROADGUARD est ouvert. Disparition après 20 minutes sans mise à jour. Vous pourrez la masquer depuis le site ou l’application. Aucun historique de déplacement n’est envoyé.").setNegativeButton("Garder privé",null).setPositiveButton("J’accepte",(d,w)->{a.refreshLastPosition();if(!a.hasFreshPreciseLocation()){Toast.makeText(a,"Attendez une position GPS précise sur la carte",Toast.LENGTH_LONG).show();return;}final Location l=new Location(a.previousAcceptedGps);executor.execute(()->{try{postPresence(l,true);ui(()->{Toast.makeText(a,"Votre position approximative est visible",Toast.LENGTH_LONG).show();ReferenceUi.refreshNow(a);lastPeople=0;});}catch(Exception e){ui(()->Toast.makeText(a,e.getMessage(),Toast.LENGTH_LONG).show());}});}).show();
 }
 void postPresence(Location l,boolean start)throws Exception{
  if(!foreground||!CommunityPolicy.freshFix(l.getTime(),l.getAccuracy(),System.currentTimeMillis())||!start&&!p.getBoolean("share_position",false))throw new IOException("Partage non effectué : attendez un GPS récent dans ROADGUARD ouvert.");
  JSONObject b=new JSONObject();b.put("lat",l.getLatitude()).put("lng",l.getLongitude()).put("accuracy",l.getAccuracy()).put("consent",true).put("start",start).put("lease",p.getString("presence_lease",""));
  JSONObject r=request("presence","PUT",b,null,System.currentTimeMillis()+8000);lastPresence=System.currentTimeMillis();p.edit().putString("presence_lease",r.getString("lease")).putLong("presence_expires",r.getLong("expires")).putBoolean("share_position",true).apply();
 }
 void menuPhoto(){p.edit().putBoolean("menu_photo",!p.getBoolean("menu_photo",true)).apply();markPreferencesChanged();}
 void showMembers(){p.edit().putBoolean("show_members",!p.getBoolean("show_members",true)).apply();markPreferencesChanged();lastPeople=0;pushMap();ReferenceUi.refreshNow(a);}
 JSONObject preferenceBody()throws Exception{JSONObject b=new JSONObject();b.put("accent",CommunityPolicy.color(p.getString("accent","#ff304f"),"#ff304f")).put("theme",a.getPreferences(0).getBoolean("theme_auto",false)?"auto":a.nightMap?"night":"day").put("cursor",p.getString("cursor","arrow")).put("voice",p.getString("web_voice","")).put("speech",a.getPreferences(0).getBoolean("Alertes vocales",true)).put("members",p.getBoolean("show_members",true)).put("routeColor",CommunityPolicy.color(p.getString("route_color","#329dff"),"#329dff")).put("menuPhoto",p.getBoolean("menu_photo",true));return b;}
 void markPreferencesChanged(){p.edit().putBoolean("preferences_dirty",true).putInt("preferences_revision",p.getInt("preferences_revision",0)+1).apply();lastPreferences=0;lastAttempt=0;pushMap();ReferenceUi.refreshNow(a);}
 void preference(String key,String value){
  if("accent".equals(key)&&value!=null&&value.matches("#[0-9a-fA-F]{6}"))p.edit().putString("accent",value.toLowerCase(Locale.ROOT)).apply();
  else if("cursor".equals(key)&&Arrays.asList("arrow","car","dot").contains(value))p.edit().putString("cursor",value).apply();
  else if("route_color".equals(key)&&value!=null&&value.matches("#[0-9a-fA-F]{6}"))p.edit().putString("route_color",value.toLowerCase(Locale.ROOT)).apply();else return;
  a.blue=android.graphics.Color.parseColor(CommunityPolicy.color(p.getString("accent","#ff304f"),"#ff304f"));markPreferencesChanged();
 }
 void syncPreferences()throws Exception{
  if(!paired)return;
  if(p.getBoolean("preferences_dirty",false)){int rev=p.getInt("preferences_revision",0);JSONObject b=preferenceBody();request("preferences","PUT",b,null,System.currentTimeMillis()+8000);if(p.getInt("preferences_revision",0)==rev)p.edit().putBoolean("preferences_dirty",false).apply();}
  JSONObject r=request("session","GET",null,null,System.currentTimeMillis()+8000);lastPreferences=System.currentTimeMillis();
  if(!r.optBoolean("authenticated",false))throw new ApiFailure(401,"Téléphone non connecté");
  p.edit().putBoolean("access_unlimited",r.optBoolean("unlimited",false)).apply();
  String remoteLease=r.optString("presenceLease","");if("null".equals(remoteLease))remoteLease="";
  if(p.getBoolean("share_position",false)&&!remoteLease.equals(p.getString("presence_lease",""))){p.edit().putBoolean("share_position",false).remove("presence_lease").remove("presence_expires").apply();ui(()->Toast.makeText(a,"La visibilité a été modifiée depuis un autre appareil",Toast.LENGTH_LONG).show());}
  JSONObject presence=r.optJSONObject("presence");if(presence!=null)p.edit().putLong("presence_expires",presence.optLong("expires",0)).apply();else if(!p.getBoolean("share_position",false))p.edit().remove("presence_expires").apply();
  JSONObject prefs=r.optJSONObject("preferences");if(prefs!=null&&!p.getBoolean("preferences_dirty",false)){ui(()->{if(p.getBoolean("preferences_dirty",false))return;serverPreferences=prefs;String publicName=prefs.optString("displayName","");if(publicName.length()>=2&&publicName.length()<=32)p.edit().putString("display_name",publicName).apply();String accent=CommunityPolicy.color(prefs.optString("accent"),"#ff304f"),route=CommunityPolicy.color(prefs.optString("routeColor"),"#329dff"),cursor=prefs.optString("cursor","arrow");if(!Arrays.asList("arrow","car","dot").contains(cursor))cursor="arrow";
   p.edit().putString("accent",accent).putString("route_color",route).putString("cursor",cursor).putString("web_voice",prefs.optString("voice","")).putBoolean("show_members",prefs.optBoolean("members",true)).putBoolean("menu_photo",prefs.optBoolean("menuPhoto",true)).apply();
   String theme=prefs.optString("theme","night");boolean automatic="auto".equals(theme);a.nightMap=automatic?(a.getResources().getConfiguration().uiMode&android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES:"night".equals(theme);a.getPreferences(0).edit().putBoolean("theme_auto",automatic).putBoolean("night_map",a.nightMap).putBoolean("Alertes vocales",prefs.optBoolean("speech",false)).apply();a.blue=android.graphics.Color.parseColor(accent);a.applyMapTheme();pushMap();ReferenceUi.refreshNow(a);
  });}
 }
}
