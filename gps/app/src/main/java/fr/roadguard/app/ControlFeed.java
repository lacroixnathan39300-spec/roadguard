package fr.roadguard.app;

import android.content.*;
import android.location.Location;
import android.security.keystore.*;
import android.util.Base64;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;

/** Optional, explicit opt-in. Only the selected fixed HTTPS host ever receives the key. */
final class ControlFeed {
  static final ExecutorService EXECUTOR=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"control-feed");t.setDaemon(true);return t;});
  static volatile String status="Collecte externe désactivée";static volatile long remaining=-1;
  private static boolean busy=false;private static long lastAttempt=0,nextAttempt=0;
  static SharedPreferences prefs(Context c){return c.getSharedPreferences("roadguard_controls",0);}
  private static synchronized void resetSchedule(){lastAttempt=0;nextAttempt=0;remaining=-1;status="Collecte externe désactivée";}
  static boolean selectProvider(Context c,String provider){if(!"openweb".equals(provider)&&!"wazeapi".equals(provider))throw new IllegalArgumentException("Fournisseur inconnu");SharedPreferences p=prefs(c);if(provider.equals(p.getString("provider","openweb")))return false;p.edit().putString("provider",provider).putBoolean("enabled",false).remove("credential").remove("credential_provider").remove("last_success").apply();resetSchedule();return true;}

  private static SecretKey key()throws Exception{KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);if(ks.containsAlias("ROADGUARD_CONTROL_FEED"))return (SecretKey)ks.getKey("ROADGUARD_CONTROL_FEED",null);KeyGenerator g=KeyGenerator.getInstance("AES","AndroidKeyStore");g.init(new KeyGenParameterSpec.Builder("ROADGUARD_CONTROL_FEED",KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build());return g.generateKey();}
  static void saveKey(Context c,String value)throws Exception{if(value==null||value.trim().length()<8||value.length()>512||!value.matches("[\\x21-\\x7E]+"))throw new IOException("Clé invalide");Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key());String stored=Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP)+":"+Base64.encodeToString(cipher.doFinal(value.trim().getBytes(StandardCharsets.UTF_8)),Base64.NO_WRAP);if(!prefs(c).edit().putString("credential",stored).putString("credential_provider",prefs(c).getString("provider","openweb")).putBoolean("enabled",false).commit())throw new IOException("Clé non enregistrée");resetSchedule();}
  private static String secret(String stored)throws Exception{String[] parts=stored.split(":",2);if(parts.length!=2)throw new IOException("Configurez une clé API");Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(parts[0],Base64.NO_WRAP)));return new String(cipher.doFinal(Base64.decode(parts[1],Base64.NO_WRAP)),StandardCharsets.UTF_8);}
  static boolean configured(Context c){return !prefs(c).getString("credential","").isEmpty()&&prefs(c).getString("provider","openweb").equals(prefs(c).getString("credential_provider",""));}
  static List<ControlMemory.Observation> parse(String provider,JSONObject payload,long now,long ttl)throws Exception{
    JSONObject data=payload.optJSONObject("data");JSONArray alerts=(data==null?payload:data).optJSONArray("alerts");if(alerts==null)throw new IOException("Réponse sans liste de signalements");if(alerts.length()>1000)throw new IOException("Trop de signalements");List<ControlMemory.Observation> out=new ArrayList<>();
    for(int i=0;i<alerts.length();i++){JSONObject a=alerts.optJSONObject(i);if(a==null||!"POLICE".equals(a.optString("type")))continue;try{JSONObject location=a.optJSONObject("location");double lat="openweb".equals(provider)?a.getDouble("latitude"):location.getDouble("lat"),lon="openweb".equals(provider)?a.getDouble("longitude"):location.getDouble("lng");String id=a.optString("openweb".equals(provider)?"alert_id":"id","");String date=a.optString("openweb".equals(provider)?"publish_datetime_utc":"reported_at","");long at=Instant.parse(date).toEpochMilli();ControlMemory.Observation o=new ControlMemory.Observation(provider,id,"police",lat,lon,at);if(o.valid(now,ttl))out.add(o);}catch(Exception invalid){/* An incomplete report cannot become a confirmation. */}}
    return out;
  }
  static String url(String provider,double lat,double lon){if(!CommunityPolicy.coordinate(lat,lon))throw new IllegalArgumentException("Position invalide");String bl=String.format(Locale.US,"%.5f,%.5f",Math.max(-85,lat-.08),Math.max(-180,lon-.12)),tr=String.format(Locale.US,"%.5f,%.5f",Math.min(85,lat+.08),Math.min(180,lon+.12));try{if("openweb".equals(provider))return "https://api.openwebninja.com/waze/alerts-and-jams?bottom_left="+URLEncoder.encode(bl,"UTF-8")+"&top_right="+URLEncoder.encode(tr,"UTF-8")+"&max_alerts=200&max_jams=1";if("wazeapi".equals(provider))return "https://api.wazeapi.com/v1/alerts?bottom-left="+URLEncoder.encode(bl,"UTF-8")+"&top-right="+URLEncoder.encode(tr,"UTF-8")+"&limit=500&filter=%5B%22POLICE%22%5D";}catch(UnsupportedEncodingException impossible){throw new AssertionError(impossible);}throw new IllegalArgumentException("Fournisseur inconnu");}
  static void tick(Context context,Location fix,boolean force){
    Context c=context.getApplicationContext();SharedPreferences p=prefs(c);if(!p.getBoolean("enabled",false)||!configured(c)||fix==null||!CommunityPolicy.freshFix(fix.getTime(),fix.getAccuracy(),System.currentTimeMillis()))return;
    long now=System.currentTimeMillis();int minutes=p.getInt("interval_minutes",2);if(minutes!=1&&minutes!=2&&minutes!=5&&minutes!=10)minutes=2;
    synchronized(ControlFeed.class){if(busy||now<nextAttempt||now-lastAttempt<(force?10000:minutes*60000L))return;busy=true;lastAttempt=now;}
    Location position=new Location(fix);String provider=p.getString("provider","openweb"),credential=p.getString("credential","");status="Lecture des contrôles signalés…";
    EXECUTOR.execute(()->{HttpURLConnection connection=null;try{
      if(!p.getBoolean("enabled",false)||!provider.equals(p.getString("credential_provider",""))||!credential.equals(p.getString("credential","")))return;
      String token=secret(credential);connection=(HttpURLConnection)new URL(url(provider,position.getLatitude(),position.getLongitude())).openConnection();connection.setInstanceFollowRedirects(false);connection.setConnectTimeout(6000);connection.setReadTimeout(8000);connection.setRequestProperty("x-api-key",token);connection.setRequestProperty("Accept","application/json");connection.setRequestProperty("User-Agent","ROADGUARD/11.14.195");if("wazeapi".equals(provider)){connection.setRequestProperty("X-Country","eur");connection.setRequestProperty("X-Failover","false");}
      int code=connection.getResponseCode();if(code!=200){long delay=code==429?15*60000L:code==401||code==403?60*60000L:120000L;synchronized(ControlFeed.class){nextAttempt=System.currentTimeMillis()+delay;}throw new IOException(code==429?"Quota ou fréquence atteints · collecte suspendue15min":code==401||code==403?"Clé ou accès refusé · vérifiez la configuration":"Fournisseur indisponible ("+code+")");}
      ByteArrayOutputStream out=new ByteArrayOutputStream();long deadline=System.nanoTime()+15000000000L;try(InputStream in=connection.getInputStream()){byte[] bytes=new byte[8192];int n;while((n=in.read(bytes))!=-1){if(System.nanoTime()>deadline)throw new IOException("Délai de réception dépassé");if(out.size()+n>2_000_000)throw new IOException("Réponse trop volumineuse");out.write(bytes,0,n);}}
      String quota=connection.getHeaderField("X-Quota-Remaining");remaining=-1;try{if(quota!=null)remaining=Long.parseLong(quota);}catch(NumberFormatException ignored){}
      List<ControlMemory.Observation> observations=parse(provider,new JSONObject(new String(out.toByteArray(),StandardCharsets.UTF_8)),System.currentTimeMillis(),ControlMemory.ttl(c));
      // A result started under an old key/provider must not be installed after opt-out.
      synchronized(ControlFeed.class){if(!p.getBoolean("enabled",false)||!provider.equals(p.getString("provider","openweb"))||!credential.equals(p.getString("credential","")))return;ControlMemory.ingest(c,observations,System.currentTimeMillis());}
      p.edit().putLong("last_success",System.currentTimeMillis()).apply();status=observations.size()+" signalements de contrôle reçus · doublons non recomptés";
    }catch(Exception e){status=e instanceof IOException?e.getMessage():"Collecte indisponible · données locales conservées";}finally{if(connection!=null)connection.disconnect();synchronized(ControlFeed.class){busy=false;}}});
  }
  private ControlFeed(){}
}
