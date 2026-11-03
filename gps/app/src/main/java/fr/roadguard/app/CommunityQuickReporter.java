package fr.roadguard.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.util.Base64;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import org.json.JSONObject;

/** Sends a single deliberate overlay report using the paired device credential. Runs off the UI thread. */
final class CommunityQuickReporter {
  private CommunityQuickReporter(){}

  static String publish(Context context,Location fix,String kind){
    if(!"fixed".equals(kind)&&!"mobile".equals(kind)&&!"control".equals(kind))return "Type de signalement invalide";
    if(fix==null||!fix.hasAccuracy()||!CommunityPolicy.freshFix(fix.getTime(),fix.getAccuracy(),System.currentTimeMillis())||fix.getAccuracy()>60
        ||!CommunityPolicy.coordinate(fix.getLatitude(),fix.getLongitude()))return "GPS récent et précis requis (60 m maximum)";
    if(!fix.hasSpeed()||fix.getSpeed()>2.8f)return "Arrêtez-vous avant de signaler";
    SharedPreferences prefs=context.getSharedPreferences("roadguard_community",Context.MODE_PRIVATE);
    if(!prefs.getBoolean("approved",false))return "Connectez votre compte dans Communauté";
    if(prefs.getString("display_name","").length()<2)return "Choisissez votre nom sur le site depuis Communauté";
    HttpURLConnection connection=null;
    try{
      String[] pair=prefs.getString("credential","").split(":",2);
      if(pair.length!=2) return "Connectez votre compte dans Communauté";
      KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);
      SecretKey key=(SecretKey)store.getKey("ROADGUARD_COMMUNITY",null);
      if(key==null)return "Connectez votre compte dans Communauté";
      Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.DECRYPT_MODE,key,new GCMParameterSpec(128,Base64.decode(pair[0],Base64.NO_WRAP)));
      String token=new String(cipher.doFinal(Base64.decode(pair[1],Base64.NO_WRAP)),StandardCharsets.UTF_8);
      if(!token.matches("rg_[A-Za-z0-9_-]{43}"))return "Connectez votre compte dans Communauté";
      JSONObject body=new JSONObject().put("kind",kind).put("lat",fix.getLatitude()).put("lng",fix.getLongitude())
          .put("accuracy",fix.getAccuracy()).put("note","");
      byte[] bytes=body.toString().getBytes(StandardCharsets.UTF_8);
      connection=(HttpURLConnection)new URL(CommunitySync.ORIGIN+"/api/v1/reports").openConnection();
      connection.setInstanceFollowRedirects(false);connection.setConnectTimeout(4000);connection.setReadTimeout(6000);
      connection.setRequestMethod("POST");connection.setRequestProperty("Authorization","Bearer "+token);
      connection.setRequestProperty("Idempotency-Key",UUID.randomUUID().toString());
      connection.setRequestProperty("Content-Type","application/json");connection.setRequestProperty("Accept","application/json");
      connection.setDoOutput(true);connection.setFixedLengthStreamingMode(bytes.length);
      try(OutputStream out=connection.getOutputStream()){out.write(bytes);}
      int code=connection.getResponseCode();
      if(code>=200&&code<300)return "Signalement partagé avec la communauté";
      InputStream stream=connection.getErrorStream();
      if(stream!=null)try(InputStream in=stream){byte[] response=new byte[512];int n=in.read(response);if(n>0){String detail=new JSONObject(new String(response,0,n,StandardCharsets.UTF_8)).optString("error","");if(!detail.isEmpty())return detail;}}
      return code==401?"Association expirée : reconnectez le téléphone":"Envoi non confirmé ("+code+")";
    }catch(Exception unavailable){return "Envoi non confirmé : vérifiez le réseau puis réessayez";}
    finally{if(connection!=null)connection.disconnect();}
  }
}
