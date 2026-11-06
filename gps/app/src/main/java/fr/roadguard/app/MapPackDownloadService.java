package fr.roadguard.app;

import android.app.*;
import android.content.*;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.*;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.util.Locale;

/** User-initiated map transfer that survives closing the map or Activity. */
public final class MapPackDownloadService extends Service {
  static final String ACTION_START="fr.roadguard.app.MAP_DOWNLOAD_START";
  static final String ACTION_CANCEL="fr.roadguard.app.MAP_DOWNLOAD_CANCEL";
  private static final String CHANNEL="roadguard_map_download";
  private static final int NOTIFICATION=238;
  private static final Object TRANSFER_LOCK=new Object();
  static volatile boolean running;
  private static volatile MapPackDownloadService activeDownloader;
  private volatile boolean cancelled;
  private volatile HttpURLConnection activeConnection;
  private Thread worker;
  private NotificationManager notifications;

  static boolean isRunning(){return running;}
  static void begin(Context context,JSONObject pack){
    Intent intent=new Intent(context,MapPackDownloadService.class).setAction(ACTION_START).putExtra("pack",pack.toString());
    context.startForegroundService(intent);
  }
  static void cancel(Context context){context.startService(new Intent(context,MapPackDownloadService.class).setAction(ACTION_CANCEL));}
  static String status(Context context){return context.getSharedPreferences("roadguard_map_transfer",MODE_PRIVATE).getString("status","");}
  static long pruneOldFrenchRegions(Context context){
    File directory=new File(context.getFilesDir(),"pmtiles"),national=new File(directory,"france_openmaptiles.pmtiles");
    try{if(!national.isFile()||!new PmtilesReader(national).isVector()||!MainActivity.pmtilesCoversFranceAndCorsica(national))return 0;}
    catch(IOException invalid){return 0;}
    long freed=0;
    for(String region:new String[]{"france_northeast","france_northwest","france_southeast","france_southwest"}){
      for(String suffix:new String[]{".pmtiles",".pmtiles.part",".pmtiles.part.source",".pmtiles.old"}){
        File obsolete=new File(directory,region+suffix);long size=obsolete.length();if(obsolete.isFile()&&obsolete.delete())freed+=size;
      }
      context.getSharedPreferences("MainActivity",MODE_PRIVATE).edit().remove("pmtiles_sha256_"+region).apply();
    }
    return freed;
  }
  private void state(String status){if(activeDownloader!=null&&activeDownloader!=this)return;getSharedPreferences("roadguard_map_transfer",MODE_PRIVATE).edit().putString("status",status).apply();}
  @Override public android.os.IBinder onBind(Intent intent){return null;}
  @Override public void onCreate(){super.onCreate();notifications=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
    notifications.createNotificationChannel(new NotificationChannel(CHANNEL,"Téléchargement des cartes",NotificationManager.IMPORTANCE_LOW));}
  @Override public int onStartCommand(Intent intent,int flags,int startId){
    if(intent!=null&&ACTION_CANCEL.equals(intent.getAction())){
      cancelled=true;HttpURLConnection c=activeConnection;if(c!=null)c.disconnect();if(worker==null||!worker.isAlive()){if(activeDownloader==this){activeDownloader=null;running=false;}stopSelfResult(startId);}return START_NOT_STICKY;
    }
    if(running)return START_NOT_STICKY;
    if(intent==null||!ACTION_START.equals(intent.getAction())){stopSelfResult(startId);return START_NOT_STICKY;}
    final JSONObject pack;
    try{pack=new JSONObject(intent.getStringExtra("pack"));validate(pack);}
    catch(Exception invalid){state("Carte refusée : "+invalid.getMessage());stopSelfResult(startId);return START_NOT_STICKY;}
    running=true;activeDownloader=this;cancelled=false;
    try{Notification initial=notice("Préparation · "+pack.optString("name"),0,true);
      if(Build.VERSION.SDK_INT>=29)startForeground(NOTIFICATION,initial,android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
      else startForeground(NOTIFICATION,initial);}
    catch(RuntimeException denied){if(activeDownloader==this){running=false;activeDownloader=null;}state("Téléchargement refusé par Android : "+denied.getMessage());stopSelfResult(startId);return START_NOT_STICKY;}
    state("Préparation du téléchargement…");
    worker=new Thread(()->{try{synchronized(TRANSFER_LOCK){checkCancel();transfer(pack);}}catch(Exception error){String detail=error.getMessage()==null?"erreur inconnue":error.getMessage();state((cancelled?"Interrompu · reprise possible : ":"Échec · reprise possible : ")+detail);publishNotice(notice(status(this),0,false));}
      finally{boolean owned=activeDownloader==this;if(owned){running=false;activeDownloader=null;}activeConnection=null;if(owned||activeDownloader==null)stopForeground(false);stopSelfResult(startId);}},"roadguard-map-background");
    worker.start();return START_NOT_STICKY;
  }
  private void publishNotice(Notification notice){if(activeDownloader==this)notifications.notify(NOTIFICATION,notice);}
  private Notification notice(String message,int percent,boolean ongoing){
    PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,fr.roadguard.app.next.PhoneHomeActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    Notification.Builder b=new Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.stat_sys_download)
      .setContentTitle("Pilote · carte hors ligne").setContentText(message).setContentIntent(open).setOnlyAlertOnce(true).setOngoing(ongoing);
    if(ongoing){PendingIntent stop=PendingIntent.getService(this,1,new Intent(this,MapPackDownloadService.class).setAction(ACTION_CANCEL),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
      b.setProgress(100,percent,false).addAction(android.R.drawable.ic_menu_close_clear_cancel,"Suspendre",stop);}
    return b.build();
  }
  private static void validate(JSONObject p)throws Exception{
    String id=p.getString("id"),url=p.getString("url");long size=p.getLong("size");
    if(!id.matches("[a-z0-9_-]{2,50}")||!"pmtiles".equals(p.optString("format"))||size<127||size>MainActivity.MAX_PMTILES_DOWNLOAD_BYTES)throw new IOException("Fiche de carte invalide");
    boolean national=p.optBoolean("public_source",false);
    if(national){if(!"france_openmaptiles".equals(id)||!MainActivity.PUBLIC_FRANCE_PMTILES_URL.equals(url)||size!=MainActivity.PUBLIC_FRANCE_PMTILES_BYTES||!MainActivity.PUBLIC_FRANCE_PMTILES_ETAG.equals(p.optString("source_etag")))throw new IOException("Source France inattendue");}
    else if(!p.getString("sha256").matches("(?i)[0-9a-f]{64}"))throw new IOException("Empreinte manquante");
    URL origin=new URL(url);if(!"https".equals(origin.getProtocol())||origin.getUserInfo()!=null)throw new IOException("HTTPS obligatoire");
    // Also checks the exact approved GitHub release origin for regional packs.
    if(!national&&!("github.com".equalsIgnoreCase(origin.getHost())&&origin.getPath().startsWith("/p-souto/project-nomad-extra-maps/releases/download/v1/")&&origin.getPath().endsWith(".pmtiles")))throw new IOException("Origine de carte non autorisée");
  }
  private boolean wifi(){ConnectivityManager cm=(ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);NetworkInfo net=cm==null?null:cm.getActiveNetworkInfo();return net!=null&&net.isConnected()&&net.getType()==ConnectivityManager.TYPE_WIFI;}
  private void checkCancel()throws IOException{if(cancelled||Thread.currentThread().isInterrupted())throw new IOException("interrompu sur demande");
    if(getSharedPreferences("MainActivity",MODE_PRIVATE).getBoolean("maps_wifi_only",false)&&!wifi())throw new IOException("Wi-Fi perdu");}
  private void transfer(JSONObject pack)throws Exception{
    String id=pack.getString("id"),url=pack.getString("url");long expected=pack.getLong("size");boolean national=pack.optBoolean("public_source",false);
    File dir=new File(getFilesDir(),"pmtiles");if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("Stockage inaccessible");
    if(dir.getUsableSpace()<Math.min(expected,128L*1024*1024)+128L*1024*1024)throw new IOException("Espace libre insuffisant");
    checkCancel();MainActivity.checkRemotePmtilesHeader(new URL(url),expected);checkCancel();
    File part=new File(dir,id+".pmtiles.part"),meta=new File(dir,id+".pmtiles.part.source");
    String identity=url+"\n"+expected+"\n"+pack.optString("sha256","")+"\n"+pack.optString("source_etag","");
    String saved="";if(meta.isFile())try(BufferedReader in=new BufferedReader(new FileReader(meta))){String line;StringBuilder b=new StringBuilder();while((line=in.readLine())!=null){if(b.length()>0)b.append('\n');b.append(line);}saved=b.toString();}
    if(!identity.equals(saved)||part.length()>expected){part.delete();meta.delete();}
    try(FileOutputStream out=new FileOutputStream(meta)){out.write(identity.getBytes("UTF-8"));out.getFD().sync();}
    long received=part.isFile()?part.length():0;
    if(dir.getUsableSpace()<expected-received+128L*1024*1024)throw new IOException("Prévoir encore "+((expected-received)/1048576)+" Mio et 128 Mio de réserve");
    if(received<expected){
      HttpURLConnection c=MainActivity.openVerifiedMapAsset(new URL(url),received);activeConnection=c;
      try{
        int response=c.getResponseCode();
        if(received>0&&response==200){received=0;if(!part.delete())throw new IOException("Reprise impossible");}
        else if(response==206){if(!MainActivity.validPmtilesRange(c.getHeaderField("Content-Range"),received,expected))throw new IOException("Plage HTTP incohérente");}
        else if(response!=200)throw new IOException("Serveur HTTP "+response);
        if(response==200&&c.getContentLengthLong()!=expected)throw new IOException("Taille HTTP incorrecte");
        checkCancel();int last=-1;
        try(InputStream in=c.getInputStream();FileOutputStream out=new FileOutputStream(part,received>0)){
          byte[] buffer=new byte[65536];int n;
          while((n=in.read(buffer))!=-1){checkCancel();received+=n;if(received>expected)throw new IOException("Archive plus grande que prévu");out.write(buffer,0,n);
            int pct=(int)(received*100/expected);if(pct!=last){last=pct;String progress="Téléchargement "+pct+" % · "+pack.optString("name");state(progress);publishNotice(notice(progress,pct,true));}}
          out.getFD().sync();
        }
      }finally{activeConnection=null;c.disconnect();}
    }
    if(received!=expected)throw new IOException("Téléchargement incomplet");
    checkCancel();state("Vérification de la carte…");publishNotice(notice("Vérification de la carte…",100,true));
    MessageDigest sha=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(part)){byte[] bytes=new byte[65536];int n;while((n=in.read(bytes))!=-1){checkCancel();sha.update(bytes,0,n);}}
    StringBuilder digest=new StringBuilder();for(byte v:sha.digest())digest.append(String.format(Locale.US,"%02x",v&255));
    if(!national&&!digest.toString().equalsIgnoreCase(pack.getString("sha256"))){part.delete();meta.delete();throw new IOException("Empreinte SHA-256 incorrecte");}
    PmtilesReader reader=new PmtilesReader(part);if(!reader.isVector())throw new IOException("Carte vectorielle attendue");
    if(national||"france".equalsIgnoreCase(pack.optString("country"))&&"country".equalsIgnoreCase(pack.optString("scope"))){
      if(!MainActivity.pmtilesCoversFranceAndCorsica(part))throw new IOException("France et Corse non couvertes");
      if(national&&(reader.vectorTile(10,518,352)==null||reader.vectorTile(10,537,379)==null))throw new IOException("Tuiles de contrôle absentes");
    }
    checkCancel();File target=new File(dir,id+".pmtiles"),old=new File(dir,id+".pmtiles.old");
    if(old.exists()&&!old.delete())throw new IOException("Ancienne sauvegarde bloquée");
    if(target.exists()&&!target.renameTo(old))throw new IOException("Sauvegarde impossible");
    if(!part.renameTo(target)){if(old.exists())old.renameTo(target);throw new IOException("Installation impossible");}
    old.delete();meta.delete();getSharedPreferences("MainActivity",MODE_PRIVATE).edit().putString("pmtiles_sha256_"+id,digest.toString()).apply();
    if(national)pruneOldFrenchRegions(this);
    state("Installée · "+pack.optString("name"));publishNotice(notice("Installée · "+pack.optString("name"),100,false));
  }
  @Override public void onDestroy(){cancelled=true;if(worker!=null)worker.interrupt();HttpURLConnection c=activeConnection;if(c!=null)c.disconnect();if(activeDownloader==this){activeDownloader=null;running=false;}super.onDestroy();}
  @Override public void onTimeout(int startId,int fgsType){cancelled=true;state("Limite Android atteinte · reprise possible");HttpURLConnection c=activeConnection;if(c!=null)c.disconnect();stopSelf();}
}
