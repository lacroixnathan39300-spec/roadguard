package fr.roadguard.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.location.*;
import android.os.*;
import android.speech.tts.TextToSpeech;
import android.media.ToneGenerator;
import android.media.AudioManager;
import java.util.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileInputStream;
import org.json.JSONArray;
import org.json.JSONObject;

/** Opt-in foreground service for CSV and saved personal hazards. */
public class BackgroundAlertService extends Service {
  static volatile boolean running=false;
  static volatile boolean bubbleVisible=false;
  static volatile boolean monitoring=false;
  private static volatile BackgroundAlertService activeService;
  private static volatile boolean roadguardVisible=true,appVisible=true;
  static void setAppVisible(boolean visible){appVisible=visible;BackgroundAlertService service=activeService;if(service!=null)service.main.post(service::syncBubbleVisibility);}
  private boolean screenReceiverRegistered;
  private final BroadcastReceiver screenReceiver=new BroadcastReceiver(){@Override public void onReceive(Context context,Intent intent){syncBubbleVisibility();}};
  static void setRoadguardVisible(boolean visible){
    roadguardVisible=visible;
    BackgroundAlertService service=activeService;
    if(service!=null)service.main.post(()->{if(visible)service.silenceRadarWarnings();service.syncBubbleVisibility();});
  }
  /** Refresh the running monitor after an imported radar overlay or country setting changes.
   * Does not start a new location service or change foreground-service permissions. */
  static void requestRadarReload(){
    BackgroundAlertService service=activeService;
    if(service!=null && running && !service.stopping){
      service.main.post(()->{if(activeService==service&&!service.stopping)service.requestPointsReload();});
    }
  }
  private void silenceRadarWarnings(){
    radarAudio.stop();if(speech!=null){speech.stop();alertSpeechTracker.cancelAll();}if(overlay!=null)overlay.clearRadar();
    dangerTones.stop();
  }
  static final String EXTRA_FLOATING="fr.roadguard.app.FLOATING_RADAR";
  static final String ACTION_STOP="fr.roadguard.app.STOP_RADAR_MONITOR";
  static final String ACTION_SHOW="fr.roadguard.app.SHOW_RADAR_BUBBLE";
  static final String ACTION_HIDE="fr.roadguard.app.HIDE_RADAR_BUBBLE";
  private static final String CHANNEL="roadguard_tracking";
  private final Handler main=new Handler(Looper.getMainLooper());
  private RadarBubbleOverlay overlay;
  final DriveAutoPolicy drivePolicy=new DriveAutoPolicy();
  private final GpsMotion driveMotion=new GpsMotion();
  private boolean stopping=false;
  private Location lastAlertFix;
  static Location navigationFix(){BackgroundAlertService s=activeService;return s==null||s.stopping||s.phoneGuidanceFix==null?null:new Location(s.phoneGuidanceFix);}

  private final Runnable autoPulse=new Runnable(){public void run(){if(stopping||!running)return;refreshDriving();publishPhoneGuidance();if(SystemClock.elapsedRealtime()-lastPointsRefresh>30000L)requestPointsReload();if(!AlertPresentation.fresh(lastAlertFix,System.currentTimeMillis(),SystemClock.elapsedRealtimeNanos()))silenceRadarWarnings();main.postDelayed(this,1000);}};
  static void stopMonitor(Context context){BackgroundAlertService service=activeService;if(service!=null)service.terminate();context.stopService(new Intent(context,BackgroundAlertService.class));}
  void terminate(){radarAudio.stop();stopping=true;running=false;monitoring=false;main.removeCallbacksAndMessages(null);if(overlay!=null){overlay.close();overlay=null;}bubbleVisible=false;if(speech!=null){speech.stop();alertSpeechTracker.cancelAll();}dangerTones.stop();stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();}
  void updateDrive(Location fix){if(stopping)return;long now=SystemClock.elapsedRealtime();if(fix==null||!CommunityPolicy.freshFix(fix.getTime(),fix.hasAccuracy()?fix.getAccuracy():1000,System.currentTimeMillis()))return;
    if(driveMotion.accept(fix.getElapsedRealtimeNanos()/1000000L,LocationManager.GPS_PROVIDER.equals(fix.getProvider()),fix.hasAccuracy()?fix.getAccuracy():1000,fix.hasSpeed()?fix.getSpeed():Double.NaN,fix.hasSpeedAccuracy()?fix.getSpeedAccuracyMetersPerSecond():Double.NaN,fix.hasBearing()?fix.getBearing():Double.NaN,fix.hasBearingAccuracy()?fix.getBearingAccuracyDegrees():Double.NaN))drivePolicy.update(now,driveMotion.speed(now));refreshDriving();}
  void refreshDriving(){if(stopping)return;boolean active=drivePolicy.active(SystemClock.elapsedRealtime());if(active!=monitoring){monitoring=active;if(!active){radarAudio.stop();if(speech!=null){speech.stop();alertSpeechTracker.cancelAll();}dangerTones.stop();}NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);nm.notify(101,notification(active));}syncBubbleVisibility();}
  private Notification notification(boolean active){PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,fr.roadguard.app.next.PhoneHomeActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);PendingIntent stop=PendingIntent.getService(this,1,new Intent(this,BackgroundAlertService.class).setAction(ACTION_STOP),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);return new Notification.Builder(this,CHANNEL).setContentTitle(active?"ROADGUARD · surveillance active":"ROADGUARD · mode auto en veille").setContentText(active?"Arrêt automatique après 1 minute immobile":"Localisation active · déclenchement au-dessus de 20 km/h").setSmallIcon(android.R.drawable.ic_menu_mylocation).setContentIntent(open).addAction(android.R.drawable.ic_menu_close_clear_cancel,"Arrêter",stop).setOngoing(true).build();}

  private LocationManager locationManager;
  private LocationListener locationListener;
  private TextToSpeech speech;
  private final GuidanceTone dangerTones=new GuidanceTone();
  final RadarAlertAudio radarAudio=new RadarAlertAudio();
  private boolean speechReady=false;
  private final AlertSpeechTracker alertSpeechTracker=new AlertSpeechTracker(this);
  private volatile java.util.List<double[]> activeAlertRoute=java.util.Collections.emptyList();
  private NavigationProgress activeRouteProgress;
  private java.util.List<MainActivity.NavStep> phoneGuidanceSteps=java.util.Collections.emptyList();
  private Location phoneGuidanceFix;
  private final PhoneGuidanceSharing phoneGuidance=new PhoneGuidanceSharing();
  void publishPhoneGuidance(){
    if(roadguardVisible||stopping)return;
    NavigationProgress model=activeRouteProgress;boolean active=model!=null&&activeAlertRoute.size()>1;
    String text="Signal GPS indisponible · guidage en attente";
    if(active&&AlertPresentation.fresh(phoneGuidanceFix,System.currentTimeMillis(),SystemClock.elapsedRealtimeNanos())){
      Location fix=phoneGuidanceFix;NavigationProgress.Position position=model.track(fix.getLatitude(),fix.getLongitude(),Math.max(0,routeProgressSegment-2),fix.getElapsedRealtimeNanos()/1000000L,driveMotion.speedMetersPerSecond(SystemClock.elapsedRealtime()),fix.hasAccuracy()?fix.getAccuracy():30);
      if(position!=null){
        if(position.lateral<=100)routeProgressSegment=Math.max(routeProgressSegment,position.segment);
        text=phoneGuidanceText(model,position,phoneGuidanceSteps);
      }
    }
    phoneGuidance.publish(this,text,active);
  }
  static String phoneGuidanceText(NavigationProgress model,NavigationProgress.Position position,java.util.List<MainActivity.NavStep> steps){
    if(position.lateral>100)return "Hors trajet · ouvre ROADGUARD sur le téléphone pour recalculer";
    if(position.arrived())return "Vous êtes à destination";
    String text=String.format(Locale.FRANCE,"Reste estimé : %.1f km sur le tracé",position.remaining/1000);
    for(MainActivity.NavStep step:steps){double distance=model.toStep(position,step.routeIndex);if(!NavigationProgress.departure(step.instruction)&&distance>=1)return text+String.format(Locale.FRANCE,"\nProchaine manœuvre (~%.0f m) : %s",distance,step.instruction);}
    return text;
  }
  private int routeProgressSegment;
  private volatile int routeReloadGeneration;
  private android.content.SharedPreferences routePreferences,catalogRevisionPreferences,catalogVisibilityPreferences;
  private final android.content.SharedPreferences.OnSharedPreferenceChangeListener catalogPreferenceListener=(prefs,key)->{
    if(prefs==routePreferences&&(key==null||"Alertes vocales".equals(key))&&!prefs.getBoolean("Alertes vocales",true))alertSpeechTracker.cancelAll();
    if(prefs==routePreferences&&(key==null||"danger_beeps".equals(key))&&!prefs.getBoolean("danger_beeps",true)){radarAudio.stop();dangerTones.stop();}
    if(prefs!=routePreferences||key==null||BundledRadarCatalog.HISTORICAL_PREFERENCE.equals(key)
        ||BundledRadarCatalog.SWISS_HISTORICAL_PREFERENCE.equals(key)||"radar_country_installed".equals(key)
        ||"custom_radars".equals(key)||"danger_csv".equals(key))main.post(()->{if(!stopping)requestPointsReload();});
  };
  private String lastRouteSnapshot="",lastRouteDestination="";
  private final android.content.SharedPreferences.OnSharedPreferenceChangeListener routePreferenceListener=(prefs,key)->{
    if(key==null||"cached_navigation".equals(key)||"destination".equals(key))requestRouteSnapshot();
  };
  private void requestRouteSnapshot(){
    if(stopping||routePreferences==null)return;
    final String saved=routePreferences.getString("cached_navigation",""),destination=routePreferences.getString("destination","");
    if(saved.equals(lastRouteSnapshot)&&destination.equals(lastRouteDestination))return;
    lastRouteSnapshot=saved;lastRouteDestination=destination;final int generation=++routeReloadGeneration;
    // Stop using the old corridor immediately when the destination changes.
    activeAlertRoute=java.util.Collections.emptyList();activeRouteProgress=null;phoneGuidanceSteps=java.util.Collections.emptyList();routeProgressSegment=0;publishPhoneGuidance();
    try{radarLoader.execute(()->{
      java.util.List<double[]> parsed=new java.util.ArrayList<>();
      java.util.List<MainActivity.NavStep> turns=new java.util.ArrayList<>();
      try{if(!saved.isEmpty()&&saved.length()<=6000000){JSONObject snapshot=new JSONObject(saved);String[] target=destination.split(",");
        if(target.length!=2)throw new java.io.IOException("Destination absente");double lat=Double.parseDouble(target[0]),lon=Double.parseDouble(target[1]);
        if(!RouteRequestPolicy.coordinate(lat,lon)||Math.abs(snapshot.getDouble("lat")-lat)>.000001||Math.abs(snapshot.getDouble("lon")-lon)>.000001)throw new java.io.IOException("Autre destination");
        JSONArray coordinates=snapshot.getJSONArray("points");if(!RouteRequestPolicy.isValidRouteSummary(snapshot.getDouble("meters"),snapshot.getDouble("seconds"),coordinates.length()))throw new java.io.IOException("Trajet invalide");
        for(int i=0;i<coordinates.length();i++){JSONArray point=coordinates.getJSONArray(i);double a=point.getDouble(0),b=point.getDouble(1);if(!RouteRequestPolicy.coordinate(a,b))throw new java.io.IOException("Position invalide");parsed.add(new double[]{a,b});}
        JSONArray instructions=snapshot.optJSONArray("steps");if(instructions!=null)for(int i=0;i<instructions.length()&&i<300;i++){
          JSONObject turn=instructions.optJSONObject(i);if(turn==null)continue;int index=turn.optInt("index",-1);String text=turn.optString("text","");
          if(index<0||index>=parsed.size()||text.isEmpty()||text.length()>1000)continue;
          MainActivity.NavStep step=new MainActivity.NavStep(0,0,text);step.routeIndex=index;turns.add(step);
        }
      }}catch(Exception invalid){parsed.clear();turns.clear();}
      final java.util.List<double[]> route=java.util.Collections.unmodifiableList(parsed);
      final NavigationProgress progress=route.size()>1?new NavigationProgress(route):null;
      main.post(()->{if(!stopping&&generation==routeReloadGeneration){activeAlertRoute=route;activeRouteProgress=progress;phoneGuidanceSteps=java.util.Collections.unmodifiableList(turns);routeProgressSegment=0;publishPhoneGuidance();}});
    });}catch(java.util.concurrent.RejectedExecutionException stopped){}
  }
  private String lastKey="";
  private long lastSpoken=0;
  private final ArrayList<double[]> points=new ArrayList<>();
  private final ArrayList<String> names=new ArrayList<>();
  private DangerSpatialIndex spatialIndex=new DangerSpatialIndex(new double[0],new double[0]);
  private int indexedBaseCount=0;
  private final java.util.concurrent.ExecutorService radarLoader=java.util.concurrent.Executors.newSingleThreadExecutor(r->{Thread thread=new Thread(r,"radar-monitor-catalog");thread.setDaemon(true);return thread;});
  private final java.util.concurrent.ExecutorService quickReports=java.util.concurrent.Executors.newSingleThreadExecutor(r->{Thread thread=new Thread(r,"radar-quick-report");thread.setDaemon(true);return thread;});
  private boolean quickReportRunning;
  private void reportFromBubble(String kind){
    if(quickReportRunning){android.widget.Toast.makeText(this,"Envoi déjà en cours",android.widget.Toast.LENGTH_SHORT).show();return;}
    Location fix=lastAlertFix==null?null:new Location(lastAlertFix);
    quickReportRunning=true;
    try{quickReports.execute(()->{
      String result=CommunityQuickReporter.publish(this,fix,kind);
      main.post(()->{quickReportRunning=false;android.widget.Toast.makeText(this,result,android.widget.Toast.LENGTH_LONG).show();});
    });}catch(java.util.concurrent.RejectedExecutionException stopped){quickReportRunning=false;}
  }
  private boolean radarReloadRunning,radarReloadPending;
  private static final class RadarPointSet {
    final ArrayList<double[]> points=new ArrayList<>();
    final ArrayList<String> names=new ArrayList<>();
    DangerSpatialIndex spatialIndex=new DangerSpatialIndex(new double[0],new double[0]);
    int indexedBaseCount;
    java.util.List<BundledRadarCatalog.Point> catalog;
  }
  @Override public void onCreate(){super.onCreate();if(!getSharedPreferences("roadguard_community",MODE_PRIVATE).getBoolean("approved",false)||checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){stopSelf();return;}activeService=this;RadarSettings.restoreFullFrance(this);
    NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
    nm.createNotificationChannel(new NotificationChannel(CHANNEL,"ROADGUARD - suivi GPS",NotificationManager.IMPORTANCE_LOW));
    Notification n=notification(false);
    try{if(Build.VERSION.SDK_INT>=29)startForeground(101,n,android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);else startForeground(101,n);}catch(RuntimeException denied){android.util.Log.w("ROADGUARD","Démarrage du suivi refusé par Android",denied);stopSelf();return;}
    IntentFilter screenFilter=new IntentFilter();screenFilter.addAction(Intent.ACTION_SCREEN_OFF);screenFilter.addAction(Intent.ACTION_SCREEN_ON);screenFilter.addAction(Intent.ACTION_USER_PRESENT);
    if(Build.VERSION.SDK_INT>=33)registerReceiver(screenReceiver,screenFilter,Context.RECEIVER_NOT_EXPORTED);else registerReceiver(screenReceiver,screenFilter);screenReceiverRegistered=true;
    stopping=false;monitoring=false;running=true;main.post(autoPulse);
    syncBubbleVisibility();
    speech=new TextToSpeech(this,status->{if(status==TextToSpeech.SUCCESS){speechReady=speech.setLanguage(Locale.FRANCE)>=TextToSpeech.LANG_AVAILABLE;speech.setSpeechRate(getSharedPreferences("MainActivity",MODE_PRIVATE).getInt("voice_rate",100)/100f);speech.setPitch(getSharedPreferences("MainActivity",MODE_PRIVATE).getInt("voice_pitch",100)/100f);try{String wanted=getSharedPreferences("MainActivity",MODE_PRIVATE).getString("native_voice","");if(!wanted.isEmpty()&&speech.getVoices()!=null)for(android.speech.tts.Voice v:speech.getVoices())if(wanted.equals(v.getName()))speech.setVoice(v);}catch(Exception ignored){}}});speech.setAudioAttributes(new android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE).setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH).build());alertSpeechTracker.useAudio(GuidanceAudio.get(this).speech(speech));alertSpeechTracker.install(speech);
    routePreferences=getSharedPreferences("MainActivity",MODE_PRIVATE);routePreferences.registerOnSharedPreferenceChangeListener(routePreferenceListener);
    routePreferences.registerOnSharedPreferenceChangeListener(catalogPreferenceListener);
    catalogRevisionPreferences=getSharedPreferences("radar_catalog_revision",MODE_PRIVATE);catalogRevisionPreferences.registerOnSharedPreferenceChangeListener(catalogPreferenceListener);
    catalogVisibilityPreferences=getSharedPreferences("radar_country_visibility",MODE_PRIVATE);catalogVisibilityPreferences.registerOnSharedPreferenceChangeListener(catalogPreferenceListener);
    requestRouteSnapshot();
    requestPointsReload();
    locationManager=(LocationManager)getSystemService(LOCATION_SERVICE);
    locationListener=new LocationListener(){@Override public void onLocationChanged(Location fix){check(fix);}@Override public void onProviderEnabled(String provider){}@Override public void onProviderDisabled(String provider){if(!LocationManager.GPS_PROVIDER.equals(provider))return;driveMotion.invalidateMotion();lastAlertFix=null;silenceRadarWarnings();drivePolicy.update(SystemClock.elapsedRealtime(),Double.NaN);refreshDriving();}@Override public void onStatusChanged(String provider,int status,Bundle extras){}};
    if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){stopSelf();return;}
    try{locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,2000,0,locationListener);}
    catch(Exception gpsUnavailable){
      try{locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,2000,0,locationListener);}
      catch(Exception noProvider){android.util.Log.w("ROADGUARD","Localisation temporairement indisponible pour la surveillance",noProvider);}
    }
  }
  private long lastPointsRefresh=0L;
  // Serialize reloads and build a complete catalog away from the Android main thread.
  private void requestPointsReload(){
    if(stopping)return;
    if(radarReloadRunning){radarReloadPending=true;return;}
    radarReloadRunning=true;lastPointsRefresh=SystemClock.elapsedRealtime();
    final String revision=RadarCatalogRevision.current(this);
    final boolean keepPrevious=!points.isEmpty();
    try{radarLoader.execute(()->{
      RadarPointSet built=null;
      try{built=buildPoints(keepPrevious);}
      catch(Exception error){android.util.Log.e("ROADGUARD_RADARS","Rechargement du service échoué ; base précédente conservée",error);}
      final RadarPointSet ready=built;
      main.post(()->{
        if(stopping||activeService!=this)return;
        radarReloadRunning=false;
        // A preference/import event during parsing invalidates the entire pending snapshot.
        if(radarReloadPending||!revision.equals(RadarCatalogRevision.current(this))){radarReloadPending=false;requestPointsReload();return;}
        if(ready!=null){
          points.clear();points.addAll(ready.points);
          names.clear();names.addAll(ready.names);
          spatialIndex=ready.spatialIndex;indexedBaseCount=ready.indexedBaseCount;
          lastPointsRefresh=SystemClock.elapsedRealtime();
          if(ready.catalog!=null)RadarActivationState.consumed(this,"background",revision,ready.catalog);
        }
      });
    });}catch(java.util.concurrent.RejectedExecutionException stopped){radarReloadRunning=false;}
  }
  // Reload saved points periodically so edits made in the open app also reach GPS monitoring.
  private RadarPointSet buildPoints(boolean keepPrevious){
    RadarPointSet data=new RadarPointSet();
    if(!loadBundledRadarPoints(data)){if(keepPrevious)throw new IllegalStateException("Catalogue précédent conservé");try{BufferedReader official=new BufferedReader(new InputStreamReader(getAssets().open("radars_france_20251230.csv"),"UTF-8"));official.readLine();String row;int loaded=0;while((row=official.readLine())!=null&&loaded<4000){String[] c=row.split(";",-1);if(c.length<5)continue;try{double lat=Double.parseDouble(c[2].trim()),lon=Double.parseDouble(c[3].trim());if(!Double.isFinite(lat)||!Double.isFinite(lon)||Math.abs(lat)>90||Math.abs(lon)>180)continue;String type=officialRadarName(c[1].trim());if(type!=null){data.points.add(new double[]{lat,lon,Long.MAX_VALUE,RadarAlertPolicy.limit(c[4])});data.names.add(type);loaded++;}}catch(Exception ignored){}}official.close();}catch(Exception e){android.util.Log.e("ROADGUARD","Base radar officielle indisponible",e);}}
    data.indexedBaseCount=data.points.size();rebuildSpatialIndex(data);
    String csv=getSharedPreferences("MainActivity",MODE_PRIVATE).getString("danger_csv","");
    for(String line:csv.split("\n")){String[] c=line.trim().split("[,;]",3);if(c.length<2)continue;
      try{double lat=Double.parseDouble(c[0].trim()),lon=Double.parseDouble(c[1].trim());
        String type=c.length>2?c[2].trim():"Zone de danger";
        if(Double.isFinite(lat)&&Double.isFinite(lon)&&Math.abs(lat)<=90&&Math.abs(lon)<=180&&DangerTypePolicy.isValidLabel(type))
          addPoint(data,lat,lon,type);
      }catch(Exception ignored){}
    }
    try{
      JSONArray saved=new JSONArray(getSharedPreferences("MainActivity",MODE_PRIVATE).getString("custom_radars","[]"));
      long now=System.currentTimeMillis();
      for(int i=0;i<saved.length();i++){
        JSONObject o=saved.optJSONObject(i);if(o==null)continue;
        long expires=o.optLong("expires",0L);if(expires>0&&expires<=now)continue;
        double lat=o.optDouble("lat",Double.NaN),lon=o.optDouble("lon",Double.NaN);
        String type=o.optString("type","").trim();
        if(Double.isFinite(lat)&&Double.isFinite(lon)&&Math.abs(lat)<=90&&Math.abs(lon)<=180&&DangerTypePolicy.isValidLabel(type))
          addPoint(data,lat,lon,type);
      }
    }catch(Exception ignored){}
    // Only read the last downloaded public alerts. Never upload locations from this service.
    try(java.io.InputStream stream=new java.io.ByteArrayInputStream(CommunityFiles.read(getFilesDir(),"community-reports.json",1500000));java.io.ByteArrayOutputStream buffer=new java.io.ByteArrayOutputStream()){byte[] bytes=new byte[8192];int n;while((n=stream.read(bytes))!=-1){if(buffer.size()+n>1500000)throw new java.io.IOException("Community cache too large");buffer.write(bytes,0,n);}long now=System.currentTimeMillis();JSONArray list=new JSONObject(new String(buffer.toByteArray(),"UTF-8")).optJSONArray("reports");if(list!=null)for(int i=0;i<list.length();i++){JSONObject r=list.optJSONObject(i);if(r==null||r.optLong("expires")<=now)continue;double lat=r.optDouble("lat",Double.NaN),lon=r.optDouble("lng",Double.NaN);if(CommunityPolicy.coordinate(lat,lon)){String kind=r.optString("kind");long confirmed=r.optLong("lastConfirmed",r.optLong("created"));if(!CommunityPolicy.recentPresence(kind,confirmed,now))continue;long alertUntil=("mobile".equals(kind)||"control".equals(kind))?Math.min(r.optLong("expires"),confirmed+7200000L):r.optLong("expires");data.points.add(new double[]{lat,lon,alertUntil});data.names.add(CommunityPolicy.label(kind)+" · communauté");}}}catch(Exception ignored){}
    try{long now=System.currentTimeMillis();JSONArray memory=ControlMemory.snapshot(this,now);for(int i=0;i<memory.length();i++){JSONObject p=memory.getJSONObject(i);data.points.add(new double[]{p.getDouble("lat"),p.getDouble("lon"),p.optLong("expires")==0?Long.MAX_VALUE:p.getLong("expires")});data.names.add(ControlMemory.label(p,now));}}catch(Exception ignored){}
    rebuildSpatialIndex(data);
    return data;
  }
  private boolean loadBundledRadarPoints(RadarPointSet data){
    try{
      boolean historical=getSharedPreferences("MainActivity",MODE_PRIVATE).getBoolean(BundledRadarCatalog.HISTORICAL_PREFERENCE,false);
      boolean swissHistorical=getSharedPreferences("MainActivity",MODE_PRIVATE).getBoolean(BundledRadarCatalog.SWISS_HISTORICAL_PREFERENCE,true);
      java.util.List<BundledRadarCatalog.Point> bundled=InstalledRadarCatalog.active(this,historical,swissHistorical,java.util.Collections.emptyList());
      data.catalog=bundled;
      for(BundledRadarCatalog.Point point:bundled){
        data.points.add(new double[]{point.lat,point.lon,Long.MAX_VALUE,point.speedKmh});data.names.add(point.type);
      }
      return true;
    }catch(Exception error){data.points.clear();data.names.clear();android.util.Log.e("ROADGUARD","Base embarquée indisponible; repli officiel",error);return false;}
  }
  private void rebuildSpatialIndex(RadarPointSet data){double[] latitudes=new double[data.points.size()],longitudes=new double[data.points.size()];for(int i=0;i<data.points.size();i++){latitudes[i]=data.points.get(i)[0];longitudes[i]=data.points.get(i)[1];}data.spatialIndex=new DangerSpatialIndex(latitudes,longitudes);}
  private void addPoint(RadarPointSet data,double lat,double lon,String name){
    // Merge overlays against official data with a small spatial query.
    float[] d=new float[1];
    java.util.HashSet<Integer> candidates=new java.util.HashSet<>(data.spatialIndex.query(lat,lon,40));
    for(int i=data.indexedBaseCount;i<data.points.size();i++)candidates.add(i);
    for(int i:candidates){if(i<0||i>=data.points.size())continue;
      double[] p=data.points.get(i);Location.distanceBetween(lat,lon,p[0],p[1],d);
      if(d[0]<40f && RadarIdentity.type(name).equals(RadarIdentity.type(data.names.get(i))))return;
    }
    data.points.add(new double[]{lat,lon});data.names.add(name);
  }
  private String officialRadarName(String code){switch(code.toUpperCase(Locale.ROOT)){case "ETF":return "Radar fixe";case "ETD":return "Radar discriminant";case "ETT":return "Radar tourelle";case "ETU":return "Radar urbain";case "ETVM":return "Radar vitesse moyenne";case "ETFR":return "Radar feu rouge";case "ETPN":return "Radar passage à niveau";default:return null;}}
  private boolean isRadar(String name){return DangerTypePolicy.isRadar(name);}
  private final HashMap<Integer,Integer> reachedStages=new HashMap<>();
  private final HashMap<Integer,Long> lastSeenStages=new HashMap<>();
  void check(Location fix){if(AlertPresentation.fresh(fix,System.currentTimeMillis(),SystemClock.elapsedRealtimeNanos()))phoneGuidanceFix=new Location(fix);updateDrive(fix);publishPhoneGuidance();if(stopping||!monitoring)return;ControlFeed.tick(this,fix,false);if(SystemClock.elapsedRealtime()-lastPointsRefresh>30000L)requestPointsReload();
    if(!getSharedPreferences("MainActivity",MODE_PRIVATE).getBoolean("Alertes de danger",true)){radarAudio.stop();if(overlay!=null)overlay.clearRadar();return;}
    if(!AlertPresentation.fresh(fix,System.currentTimeMillis(),SystemClock.elapsedRealtimeNanos())){radarAudio.stop();if(overlay!=null)overlay.clearRadar();return;}
    lastAlertFix=new Location(fix);if(fix.hasAccuracy()&&fix.getAccuracy()<=50)AlertMilestones.rearmDistant(this,fix.getLatitude(),fix.getLongitude());
    // The visible map owns alerts: never speak or beep from the background service
    // while ROADGUARD is on screen, even if its GPS heartbeat is delayed.
    // Also keep the heartbeat grace period during a foreground/background transition.
    long foregroundFix=getSharedPreferences("MainActivity",MODE_PRIVATE).getLong("foreground_gps_heartbeat",0L);
    long age=System.currentTimeMillis()-foregroundFix;
    if(roadguardVisible||(foregroundFix>0&&age>=0&&age<7000L)){
      silenceRadarWarnings();
      return;
    }
    int index=-1,visualIndex=-1;float nearest=Float.MAX_VALUE,visualDistance=Float.MAX_VALUE;
    java.util.List<double[]> route=activeAlertRoute;
    int widthMode=getSharedPreferences("MainActivity",MODE_PRIVATE).getInt("route_corridor_width",1);double corridorWidth=widthMode==0?35:(widthMode==2?85:55);
    NavigationProgress model=activeRouteProgress;
    NavigationProgress.Position position=model==null?null:model.track(fix.getLatitude(),fix.getLongitude(),Math.max(0,routeProgressSegment-2),fix.getElapsedRealtimeNanos()/1000000L,driveMotion.speedMetersPerSecond(SystemClock.elapsedRealtime()),fix.hasAccuracy()?fix.getAccuracy():30);
    if(position!=null&&position.lateral<=100)routeProgressSegment=Math.max(routeProgressSegment,position.segment);
    RadarApproachFilter.Projection vehicle=RadarApproachFilter.projection(position);
    boolean forward=getSharedPreferences("MainActivity",MODE_PRIVATE).getBoolean("forward_dangers",true);
    boolean course=driveMotion.courseReliable(SystemClock.elapsedRealtime())&&driveMotion.speed(SystemClock.elapsedRealtime())>=10.08&&fix.hasAccuracy()&&fix.getAccuracy()<=35;
    boolean corridor=getSharedPreferences("MainActivity",MODE_PRIVATE).getBoolean("route_corridor_alerts",true)&&RadarApproachFilter.usesCorridor(model,position,fix.hasAccuracy()?fix.getAccuracy():Double.NaN,corridorWidth,forward,course,driveMotion.course());
    for(int i:spatialIndex.query(fix.getLatitude(),fix.getLongitude(),1020)){if(i<0||i>=points.size())continue;double[] p=points.get(i);if(p.length>2&&p[2]<=System.currentTimeMillis())continue;float[] bearings=new float[2];Location.distanceBetween(fix.getLatitude(),fix.getLongitude(),p[0],p[1],bearings);
      boolean radarCandidate=isRadar(names.get(i));
      if(bearings[0]>(radarCandidate?1020:700))continue;
      if(radarCandidate&&forward&&!course)continue;
      if(corridor){double routeMeters=RadarApproachFilter.routeDistance(vehicle,RadarApproachFilter.projection(model.upcoming(p[0],p[1],position,35,5500)),corridorWidth);if(!Double.isFinite(routeMeters))continue;bearings[0]=(float)routeMeters;if(bearings[0]>(radarCandidate?1020:700))continue;}
      if(forward&&course&&!corridor&&!RadarApproachFilter.ahead(bearings[0],bearings[1],driveMotion.course(),fix.hasAccuracy()?fix.getAccuracy():25))continue;
      if(radarCandidate){double compensated=corridor?RadarLeadDistance.onRoute(bearings[0]):RadarLeadDistance.effective(bearings[0],bearings[1],driveMotion.course(),course);if(!Double.isFinite(compensated)||compensated<0)continue;bearings[0]=(float)compensated;}
      if(radarCandidate&&bearings[0]<visualDistance){visualDistance=bearings[0];visualIndex=i;}
      if(!AlertMilestones.pending(this,p[0],p[1],names.get(i),AlertMilestones.stage(radarCandidate,bearings[0])))continue;
      if(bearings[0]<nearest){nearest=bearings[0];index=i;}}
    if(index>=0&&isRadar(names.get(index))&&index!=visualIndex){index=-1;nearest=Float.MAX_VALUE;}
    if(visualIndex>=0&&AlertMilestones.pending(this,points.get(visualIndex)[0],points.get(visualIndex)[1],names.get(visualIndex),AlertMilestones.stage(true,visualDistance))
      &&(index<0||visualDistance<nearest)){index=visualIndex;nearest=visualDistance;}
    if(visualIndex>=0&&visualDistance<=1000){
      double[] p=points.get(visualIndex);int limit=RadarAlertPolicy.applicableLimit(names.get(visualIndex),p.length>3?(int)p[3]:RadarAlertPolicy.labelLimit(names.get(visualIndex)));boolean voiceAvailable=speechReady&&speech!=null&&getSharedPreferences("MainActivity",MODE_PRIVATE).getBoolean("Alertes vocales",true)&&!alertSpeechTracker.radarSpeechFailed();boolean announced=!AlertMilestones.pending(this,p[0],p[1],names.get(visualIndex),1);boolean handoffReady=RadarAnnouncementHandoff.allowBeeps(voiceAvailable,announced,alertSpeechTracker.hasPending());radarAudio.update(this,p[0],p[1],visualDistance,driveMotion.speed(SystemClock.elapsedRealtime()),limit,handoffReady);if(announced)OverspeedSpeech.warn(this,alertSpeechTracker,speech,speechReady,visualDistance,driveMotion.speed(SystemClock.elapsedRealtime()),limit,SystemClock.elapsedRealtime());
      if(overlay!=null)overlay.radar(RadarAlertPolicy.displayType(names.get(visualIndex)),AlertPresentation.roundedMeters(visualDistance));
    }else{radarAudio.stop();if(overlay!=null)overlay.clearRadar();}
    if(index<0||nearest>1000)return;
    boolean radar=isRadar(names.get(index));
    if(!radar&&nearest>700)return;
    int stage=AlertMilestones.stage(radar,nearest);
    long now=System.currentTimeMillis();
    boolean voiceEnabled=speechReady&&speech!=null&&getSharedPreferences("MainActivity",MODE_PRIVATE).getBoolean("Alertes vocales",true);
    // Do not claim a distance while the previous distance is still spoken.
    if(radar&&voiceEnabled&&(alertSpeechTracker.hasPending()||!alertSpeechTracker.hasOther()&&now-lastSpoken<1800L))return;
    if(radar&&!voiceEnabled)return;
    AlertMilestones.Claim claim=radar?AlertMilestones.reserve(this,points.get(index)[0],points.get(index)[1],names.get(index),stage):null;
    int announcement=radar?(claim==null?AlertMilestones.NONE:claim.kind):AlertMilestones.claimKind(this,points.get(index)[0],points.get(index)[1],names.get(index),stage);
    if(announcement==AlertMilestones.NONE)return;

    String key=index+":"+stage;
    lastKey=key;
    if(!radar&&getSharedPreferences("MainActivity",MODE_PRIVATE).getBoolean("danger_beeps",true)){
      String t=names.get(index).toLowerCase(Locale.ROOT);
      int count=isRadar(names.get(index))?2:(t.contains("travaux")||t.contains("bouchon")||t.contains("ralentissement")?1:3);
      dangerTones.play(this,count);
    }
    if(voiceEnabled){
      String message=radar?(announcement==AlertMilestones.FIRST?RadarAlertPolicy.voiceType(names.get(index))+" à "+AlertPresentation.voice(nearest):AlertPresentation.voice(nearest)):((nearest<=250?"Danger proche, ":"Attention, ")+RadarAlertPolicy.displayType(names.get(index))+" à environ "+AlertPresentation.voice(nearest));
      if(radar){if(alertSpeechTracker.speak(speech,message,claim))lastSpoken=now;}
      else if(alertSpeechTracker.speakOther(speech,message,GuidanceAudioPolicy.HAZARD))lastSpoken=now;
    }
  }
  @Override public int onStartCommand(Intent intent,int flags,int startId){
    if(!running){stopSelf();return START_NOT_STICKY;}
    if(intent!=null&&ACTION_STOP.equals(intent.getAction())){getSharedPreferences("MainActivity",MODE_PRIVATE).edit().putBoolean("floating_auto",false).putBoolean("monitor_armed",false).apply();terminate();return START_NOT_STICKY;}
    syncBubbleVisibility();
    return START_NOT_STICKY;
  }
  private void syncBubbleVisibility(){
    if(activeService!=this||stopping)return;
    PowerManager power=(PowerManager)getSystemService(POWER_SERVICE);android.app.KeyguardManager keyguard=(android.app.KeyguardManager)getSystemService(KEYGUARD_SERVICE);
    boolean show=BubblePlacement.show(monitoring&&getSharedPreferences("MainActivity",MODE_PRIVATE).getBoolean("floating_auto",false),appVisible,power!=null&&power.isInteractive(),keyguard!=null&&keyguard.isKeyguardLocked(),android.provider.Settings.canDrawOverlays(this));
    if(show){
      if(overlay==null)overlay=new RadarBubbleOverlay(this,this::reportFromBubble);
      overlay.show();
      bubbleVisible=overlay.isShowing();
    }else{
      if(overlay!=null){overlay.close();overlay=null;}
      bubbleVisible=false;
    }
  }
  @Override public void onConfigurationChanged(android.content.res.Configuration config){super.onConfigurationChanged(config);if(overlay!=null)overlay.reflow();syncBubbleVisibility();}
  @Override public void onTaskRemoved(Intent rootIntent){terminate();super.onTaskRemoved(rootIntent);}
  @Override public void onDestroy(){if(!roadguardVisible)phoneGuidance.stop(this);if(screenReceiverRegistered){unregisterReceiver(screenReceiver);screenReceiverRegistered=false;}if(routePreferences!=null){routePreferences.unregisterOnSharedPreferenceChangeListener(routePreferenceListener);routePreferences.unregisterOnSharedPreferenceChangeListener(catalogPreferenceListener);}if(catalogRevisionPreferences!=null)catalogRevisionPreferences.unregisterOnSharedPreferenceChangeListener(catalogPreferenceListener);if(catalogVisibilityPreferences!=null)catalogVisibilityPreferences.unregisterOnSharedPreferenceChangeListener(catalogPreferenceListener);routeReloadGeneration++;activeAlertRoute=java.util.Collections.emptyList();radarAudio.stop();stopping=true;radarLoader.shutdownNow();quickReports.shutdownNow();main.removeCallbacksAndMessages(null);if(activeService==this)activeService=null;running=false;monitoring=false;bubbleVisible=false;dangerTones.stop();if(overlay!=null){overlay.close();overlay=null;}if(locationManager!=null&&locationListener!=null)try{locationManager.removeUpdates(locationListener);}catch(Exception ignored){}if(speech!=null){speech.stop();alertSpeechTracker.cancelAll();speech.shutdown();}super.onDestroy();}
  @Override public IBinder onBind(Intent intent){return null;}
}
