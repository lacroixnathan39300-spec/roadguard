package fr.roadguard.app;
import android.Manifest;import android.webkit.WebResourceResponse;import android.webkit.WebResourceRequest;import java.util.zip.ZipInputStream;import java.util.zip.ZipEntry;import android.app.*;import android.os.*;import android.content.*;import android.content.pm.PackageManager;import android.graphics.Color;import android.graphics.Typeface;import android.graphics.drawable.GradientDrawable;import android.location.*;import android.view.*;import android.webkit.*;import android.widget.*;
import org.xmlpull.v1.XmlPullParser;import android.util.Xml;import java.util.Locale;import java.text.SimpleDateFormat;import java.util.Date;import java.util.ArrayList;import java.io.*;import android.net.Uri;import android.net.ConnectivityManager;import android.net.NetworkInfo;import android.speech.tts.TextToSpeech;import android.media.ToneGenerator;import android.media.AudioManager;import org.json.JSONObject;import org.json.JSONArray;
public class MainActivity extends Activity {
  static class NavStep {double lat,lon;String instruction;int routeIndex=-1;NavStep(double a,double b,String c){lat=a;lon=b;instruction=c;}}
  static final int MAX_ROUTE_CHOICES=4;
  static class RouteOption {ArrayList<double[]> points=new ArrayList<>();ArrayList<NavStep> steps=new ArrayList<>();double meters,seconds;String label="Itinéraire";}
  static class Danger {double lat,lon;long expires=0;String communityId="";String controlId="";String type;int speedLimit;Double sourceDirection;String sourceName="",sourceReliability="";boolean catalogPoint;Danger(double a,double b,String t){this(a,b,t,RadarAlertPolicy.labelLimit(t));}Danger(double a,double b,String t,int limit){lat=a;lon=b;type=t;speedLimit=RadarAlertPolicy.applicableLimit(t,limit);}Danger(BundledRadarCatalog.Point point){this(point.lat,point.lon,point.type,point.speedKmh);sourceDirection=point.direction;sourceName=point.sources;sourceReliability=point.reliability;catalogPoint=true;}}
  ArrayList<Danger> dangers=new ArrayList<>(); int officialRadarCount=0; int bundledLoadedRadarCount=0; int bundledLoadedFranceCount=0,bundledLoadedSwissCount=0,bundledLoadedNeighborCount=0; JSONObject bundledRadarManifest=null; DangerSpatialIndex dangerSpatialIndex=new DangerSpatialIndex(new double[0],new double[0]); ArrayList<double[]> routePoints=new ArrayList<>(); ArrayList<double[]> navigationPoints=new ArrayList<>(); ArrayList<RouteOption> availableRoutes=new ArrayList<>(); int activeRouteIndex=0; double navigationMeters=0,navigationSeconds=0; ArrayList<NavStep> navigationSteps=new ArrayList<>(); ArrayList<ArrayList<double[]>> offlineRoads=new ArrayList<>(); TextToSpeech tts; long lastAlertAt=0; String lastAlertKey=""; boolean voiceReady=false,pmtilesImportRunning=false,pmtilesImportCancel=false; int spokenStepIndex=-1,spokenThreshold=-1;
  LinearLayout root,top,bottom,driveSheet; FrameLayout speedHudFrame; TextView floatingChip; DragHandleView driveSheetHandle; boolean pendingFloatingActivation=false; EditText destinationSearchInput; LinearLayout driveSearchResults; android.os.Handler driveSearchHandler=new android.os.Handler(android.os.Looper.getMainLooper()); Runnable driveSearchRunnable; volatile int driveSearchRequestToken=0; boolean driveSearchSuppressWatcher=false; boolean driveSheetExpanded=false,driveSheetUserOverride=false; int driveSheetLevel=0; float driveSheetTouchY=0f; FrameLayout frame; WebView map,referenceWebView; volatile AverageSpeedZoneMonitor averageZoneMonitor; volatile boolean averageZoneLoading; TextView averageZoneBadge; boolean averageZoneWasOverLimit; String averageZoneLastAnnouncement=""; TextView speed,gps,routeStatus,modeChip,accuracyChip,headingChip,maneuverTitle,maneuverDistance,tripSummary,searchWidget,gpsWidget,dangerBanner; android.animation.ObjectAnimator radarPulse; String activeRadarKey=""; android.widget.ProgressBar routeProgress; LocationManager lm; LocationListener listener; boolean follow=true; boolean compactControls=true; boolean nightMap=false; boolean highContrastMap=true; boolean radarMarkersVisible=true; int mapMarkerSize=1; boolean immersiveDrive=false; boolean headingUp=true; boolean adaptiveZoom=true; boolean lookAhead=true; boolean autoDeclutter=true; boolean drivingDecluttered=false; float lastBearing=0f,lastAccuracy=0f,lastSpeedKmh=0f; boolean reliableBearing=false; int navy=0xff0b0e13,blue=0xfff02d38; String selected="Carte"; boolean legacySection=false; String legacySectionScreen=""; Danger activeRadarUi=null; ArrayList<Danger> referenceRadarItems=new ArrayList<>(); boolean mapReady=false; boolean offlineView=false; boolean mapLibrePreview=false; boolean forceOnlineMapLibre=false; boolean tripRecording=false; long tripStart=0; double tripMeters=0; Location previousTripLocation=null; Location previousAcceptedGps=null; int tripSamples=0; ArrayList<double[]> tripTrack=new ArrayList<>(); double lastLat=Double.NaN,lastLon=Double.NaN; double destinationLat=Double.NaN,destinationLon=Double.NaN;
  GradientDrawable bg(int color,int r){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(r);return d;}
  TextView label(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextColor(Color.WHITE);t.setTextSize(size);t.setGravity(Gravity.CENTER_VERTICAL);return t;}
  LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
  LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
  void pad(View v,int a){v.setPadding(a,a,a,a);} void add(LinearLayout l,View v){l.addView(v,new LinearLayout.LayoutParams(-1,-2));}
  boolean handleRoadguardBack(){if(!accountConnected())return false;if(legacySection){legacySection=false;render();return true;}if(!selected.equals("Carte")){selected="Carte";render();return true;}return false;}
  boolean accountConnected(){return community!=null&&community.paired;}
  void onAccountConnected(){if(isFinishing()||isDestroyed())return;SiteAppUpdate.check(this,false);selected="Carte";legacySection=false;render();if(activityResumed&&listener==null)startGPS();}
  void onAccountLocked(){if(isFinishing()||isDestroyed())return;cancelRouteCalculation();nextDriveSearchToken();BackgroundAlertService.stopMonitor(this);pendingFloatingActivation=false;stopGPS();selected="Compte";legacySection=false;render();}
  @Override public void onBackPressed(){if(!handleRoadguardBack())finish();}
  void configureModernAndroid(){
    if(Build.VERSION.SDK_INT>=33)getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,()->{if(!handleRoadguardBack())finish();});
    if(Build.VERSION.SDK_INT>=35){
      getWindow().setDecorFitsSystemWindows(false);
      View content=getWindow().getDecorView().findViewById(android.R.id.content);
      content.setOnApplyWindowInsetsListener((view,insets)->{
        android.graphics.Insets safe=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout()|WindowInsets.Type.ime());
        view.setPadding(safe.left,safe.top,safe.right,safe.bottom);
        return WindowInsets.CONSUMED;
      });content.requestApplyInsets();
    }
  }
  @Override protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);state.putInt("pending_notification_action",pendingNotificationAction);state.putBoolean("pending_floating_activation",pendingFloatingActivation);
    state.putString("manual_map_view",lastManualMapView);
    if(destinationSearchInput!=null&&destinationSearchDialog==null)state.putString("drive_search_query",destinationSearchInput.getText().toString());
    state.putInt("drive_sheet_level",driveSheetLevel);
    state.putInt("guidance_segment",guidanceProgressSegment);state.putString("route_signature",Integer.toHexString(getPreferences(0).getString("cached_navigation","").hashCode()));}
  int pendingNotificationAction=0;
  boolean requestTrackingNotifications(int next){
    if(Build.VERSION.SDK_INT<33||checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED||getPreferences(0).getBoolean("notification_prompted",false))return false;
    if(pendingNotificationAction!=0)return true;
    pendingNotificationAction=next;getPreferences(0).edit().putBoolean("notification_prompted",true).apply();
    requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},32);return true;
  }
  void showNotificationSettings(){try{startActivity(new Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,getPackageName()));}catch(Exception ignored){Toast.makeText(this,"Ouvre les notifications de ROADGUARD dans les réglages Android",Toast.LENGTH_LONG).show();}}

  @Override public void onCreate(Bundle b){super.onCreate(b);PhoneNavigationStopReceiver.attach(this);MobileAudioStudio.start(this);RoadguardVisibility.install(getApplication());if(b!=null){pendingNotificationAction=b.getInt("pending_notification_action",0);pendingFloatingActivation=b.getBoolean("pending_floating_activation",false);lastManualMapView=b.getString("manual_map_view","");restoredSearchQuery=b.getString("drive_search_query");restoredDriveSheetLevel=b.getInt("drive_sheet_level",0);restoredGuidanceSegment=b.getInt("guidance_segment",-1);restoredRouteSignature=b.getString("route_signature","");}configureModernAndroid();speechPreferences=getPreferences(0);speechPreferences.registerOnSharedPreferenceChangeListener(speechPreferenceListener);mapConnectionMode=MapConnectionPolicy.normalize(getPreferences(0).getString("map_connection_mode",getPreferences(0).getBoolean("map_offline_only",false)?"offline":"auto"));RadarSettings.restoreFullFrance(this);clearLegacyPrefetchSettings();automaticRerouteEnabled=getPreferences(0).getBoolean("auto_reroute",false);immersiveDrive=getPreferences(0).getBoolean("immersive_drive",false);headingUp=getPreferences(0).getBoolean("heading_up",true);adaptiveZoom=getPreferences(0).getBoolean("adaptive_zoom",true);lookAhead=getPreferences(0).getBoolean("look_ahead",true);nightMap=getPreferences(0).getBoolean("night_map",true);if(getPreferences(0).getBoolean("theme_auto",false))nightMap=(getResources().getConfiguration().uiMode&android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES;highContrastMap=getPreferences(0).getBoolean("high_contrast_map",true);radarMarkersVisible=getPreferences(0).getBoolean("radar_markers_visible",true);mapMarkerSize=Math.max(0,Math.min(2,getPreferences(0).getInt("map_marker_size",1)));autoDeclutter=getPreferences(0).getBoolean("auto_declutter",true);getWindow().setStatusBarColor(navy);getWindow().setNavigationBarColor(navy);community=new CommunitySync(this);blue=Color.parseColor(CommunityPolicy.color(community.p.getString("accent","#ff304f"),"#ff304f"));pendingNavigationQuery=NavigationIntentInput.query(getIntent().getDataString());showStartupScreen();}
  String pendingNavigationQuery=null;int navigationIntentGeneration=0;long lastCarRouteRevision=0;
  @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);navigationIntentGeneration++;pendingNavigationQuery=NavigationIntentInput.query(intent.getDataString());dispatchNavigationIntent();}
  void syncCarRoute(){long revision=getPreferences(0).getLong("car_route_revision",0);if(lm==null||revision==lastCarRouteRevision)return;lastCarRouteRevision=revision;navigationPoints.clear();navigationSteps.clear();availableRoutes.clear();navigationProgress=null;loadDestination();loadCachedNavigation();pushRoute();pushDestination();updateManeuverCard();}
  void dispatchNavigationIntent(){if(pendingNavigationQuery==null||previousAcceptedGps==null||!CommunityPolicy.freshFix(previousAcceptedGps.getTime(),lastAccuracy,System.currentTimeMillis()))return;String query=pendingNavigationQuery;pendingNavigationQuery=null;double[] point=NavigationIntentInput.coordinates(query);if(point!=null){applyNavigationIntent(point[0],point[1],query);return;}final int token=++navigationIntentGeneration;final String prior=getPreferences(0).getString("destination","");new Thread(()->{try{JSONObject root=new JSONObject(CarRouteData.get(CommunitySync.ORIGIN+"/api/v1/places?q="+java.net.URLEncoder.encode(query,"UTF-8"),200000));JSONObject item=root.getJSONArray("places").getJSONObject(0);double a=item.getDouble("lat"),b=item.getDouble("lng");if(!RouteRequestPolicy.coordinate(a,b))return;runOnUiThread(()->{if(!isDestroyed()&&token==navigationIntentGeneration&&prior.equals(getPreferences(0).getString("destination","")))applyNavigationIntent(a,b,item.optString("name",query));});}catch(Exception e){runOnUiThread(()->{if(!isDestroyed()&&token==navigationIntentGeneration)Toast.makeText(this,"Destination introuvable",Toast.LENGTH_LONG).show();});}},"roadguard-navigation-intent").start();}
  void applyNavigationIntent(double lat,double lon,String name){clearNavigation();rememberRecentDestination(lat,lon,name);destinationLat=lat;destinationLon=lon;getPreferences(0).edit().putString("destination",String.format(Locale.US,"%.7f,%.7f",lat,lon)).putString("destination_label",name).apply();selected="Carte";legacySection=false;render();calculateRoadRoute();}
  final GpsMotion gpsMotion=new GpsMotion();
  CommunitySync community;
  long lastReferenceRefreshAt=0;
  VisibleTileCache visibleTileCache;
  android.net.ConnectivityManager.NetworkCallback mapNetworkCallback;
  boolean lastMapNetworkOnline;
  Boolean lastPushedMapNetworkOnline;
  Boolean lastPushedGpsFresh;
  boolean vectorEngineDisabledForSession;
  boolean mapEngineReported;
  String mapEngineDiagnostic="";
  final java.util.concurrent.atomic.AtomicInteger localMapAssetsServed=new java.util.concurrent.atomic.AtomicInteger();
  final java.util.concurrent.atomic.AtomicInteger localMapTileRequests=new java.util.concurrent.atomic.AtomicInteger();
  final java.util.concurrent.atomic.AtomicInteger localMapTilesServed=new java.util.concurrent.atomic.AtomicInteger();
  volatile String lastLocalMapFailure="";
  String localMapDiagnostic(){
    StringBuilder result=new StringBuilder("Origine carte : ").append(map==null?"aucune":map.getUrl());
    java.util.List<InstalledPmtilesMosaic.Region> regions=installedMosaicRegions();
    result.append("\nPacks vectoriels détectés : ").append(regions.size());
    for(InstalledPmtilesMosaic.Region region:regions)result.append("\n• ").append(region.file.getName()).append(" · zoom max ").append(region.maxZoom);
    result.append("\nSession : ").append(localMapAssetsServed.get()).append(" ressources locales, ").append(localMapTilesServed.get()).append(" tuiles locales lues / ").append(localMapTileRequests.get()).append(" requêtes");
    if(!lastLocalMapFailure.isEmpty())result.append("\nDernière erreur locale : ").append(lastLocalMapFailure);
    return result.toString();
  }
  String lastManualMapView="";
  boolean gpsSignalSuspended;
  boolean mapInternetAvailable(){
    ConnectivityManager manager=(ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);if(manager==null)return false;
    try{android.net.Network active=manager.getActiveNetwork();android.net.NetworkCapabilities capabilities=manager.getNetworkCapabilities(active);
      return capabilities!=null&&capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)&&capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }catch(Exception unavailable){return networkAvailable();}
  }
  void handleMapEngineState(WebView owner,String state,String detail){
    if(owner!=map||isFinishing()||isDestroyed()||!LocalPagePolicy.mapPage(owner.getUrl()))return;
    if("ready".equals(state)){mapEngineReported=true;return;}
    if(!"failed".equals(state)||!mapLibrePreview||vectorEngineDisabledForSession)return;
    mapEngineReported=true;mapEngineDiagnostic="Moteur vectoriel : "+String.valueOf(detail);if(mapEngineDiagnostic.length()>1000)mapEngineDiagnostic=mapEngineDiagnostic.substring(0,1000);
    vectorEngineDisabledForSession=true;mapLibrePreview=false;offlineView=false;forceOnlineMapLibre=false;
    owner.post(()->{if(map==owner&&!isFinishing()&&!isDestroyed()&&"Carte".equals(selected)){render();Toast.makeText(this,"Moteur vectoriel indisponible · carte 2D de secours",Toast.LENGTH_LONG).show();}});
  }
  boolean offlineMapSwitchScheduled;
  boolean offlineFallbackChecked;
  void observeMapNetwork(){
    ConnectivityManager manager=(ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);if(manager==null||mapNetworkCallback!=null)return;
    mapNetworkCallback=new ConnectivityManager.NetworkCallback(){
      void refresh(){runOnUiThread(()->{if(!isDestroyed()){cancelUnavailableOnlineRoute();refreshMapNetworkCacheMode();ReferenceUi.refresh(MainActivity.this);}});}
      @Override public void onAvailable(android.net.Network network){refresh();}
      @Override public void onLost(android.net.Network network){refresh();}
      @Override public void onCapabilitiesChanged(android.net.Network network,android.net.NetworkCapabilities capabilities){refresh();}
    };
    try{manager.registerDefaultNetworkCallback(mapNetworkCallback);}catch(Exception unavailable){mapNetworkCallback=null;}
  }

  NavigationProgress navigationProgress;
  TextView maneuverGlyph,tripMinutes,tripArrival;
  Danger pendingMapFocus;
  int guidanceFooterHeightDp=0,lastMapSheetOcclusionDp=-1,lastMapTopInsetDp=-1;
  int restoredGuidanceSegment=-1;String restoredRouteSignature="";
  AlertDialog destinationSearchDialog;
  final java.util.concurrent.ExecutorService initialDataExecutor=java.util.concurrent.Executors.newSingleThreadExecutor();
  final LatestSearchHttp destinationSearchHttp=new LatestSearchHttp();
  int nextDriveSearchToken(){int token=++driveSearchRequestToken;destinationSearchHttp.invalidate(token);return token;}
  final java.util.concurrent.ThreadPoolExecutor destinationSearchExecutor=new java.util.concurrent.ThreadPoolExecutor(1,1,0L,java.util.concurrent.TimeUnit.MILLISECONDS,new java.util.concurrent.ArrayBlockingQueue<Runnable>(1),r->new Thread(r,"roadguard-destination-search"),new java.util.concurrent.ThreadPoolExecutor.DiscardOldestPolicy());
  java.util.concurrent.Future<?> initialLoad;boolean activityResumed=false;TextView startupStatus;
  volatile String loadedRadarCatalogRevision="";
  boolean radarDataReady,radarCatalogReloadRunning,radarReloadPending,radarCatalogLoaded;
  long radarLoadGeneration; long mapRadarRequestGeneration;
  String radarLoadStatus="Radars : chargement en arrière-plan…";
  TextView radarLoadingLabel;
  LinearLayout mapTopOverlay,mapActionRail,guidanceFooter;
  ScrollView mapTopScroll;
  TextView inlineMapSpeed;
  boolean mapOverlayLayoutScheduled;
  String restoredSearchQuery,driveSearchDestinationKey="";
  int restoredDriveSheetLevel=-1,mapTopHeightLimit=-1;
  android.widget.RadioGroup mapConnectionSwitch;
  volatile String mapConnectionMode="auto";
  boolean mapNetworkAvailable(){return MapConnectionPolicy.allowsNetwork(mapConnectionMode,mapInternetAvailable());}
  void selectMapConnection(String mode){
    String next=MapConnectionPolicy.normalize(mode);if(next.equals(mapConnectionMode))return;
    mapConnectionMode=next;getPreferences(0).edit().putString("map_connection_mode",mapConnectionMode).apply();
    cancelUnavailableOnlineRoute();forceOnlineMapLibre=false;
    boolean localVector=!vectorEngineDisabledForSession&&mapLibreAssetsPresent()&&!installedMosaicRegions().isEmpty();
    boolean switchRenderer=localVector&&!mapLibrePreview;
    if(map!=null&&mapReady&&!offlineView&&!switchRenderer){
      if(mapConnectionSwitch!=null)for(int i=0;i<mapConnectionSwitch.getChildCount();i++){
        View option=mapConnectionSwitch.getChildAt(i);boolean chosen=mapConnectionMode.equals(option.getTag());
        option.setBackground(chosen?accentPanel(18,false):bg(android.graphics.Color.TRANSPARENT,18));
        if(chosen&&mapConnectionSwitch.getCheckedRadioButtonId()!=option.getId())mapConnectionSwitch.check(option.getId());
      }
      refreshMapNetworkCacheMode();ReferenceUi.refreshNow(this);
    }else{offlineView=false;mapLibrePreview=!vectorEngineDisabledForSession&&mapLibreAssetsPresent()&&!installedMosaicRegions().isEmpty();selected="Carte";render();}
    if("offline".equals(mapConnectionMode))Toast.makeText(this,"Hors ligne · cartes téléchargées et cache local",Toast.LENGTH_SHORT).show();
  }
  static final class MapAction {final String title;final Runnable run;MapAction(String title,Runnable run){this.title=title;this.run=run;}}
  void showMapActions(String title,MapAction... items){String[] labels=new String[items.length];for(int i=0;i<items.length;i++)labels[i]=items[i].title;new AlertDialog.Builder(this).setTitle(title).setItems(labels,(dialog,index)->items[index].run.run()).setNegativeButton("Fermer",null).show();}
  void showCompactMapOptions(){
    ArrayList<MapAction> actions=new ArrayList<>();actions.add(new MapAction("Conduite et guidage",this::showDrivingMapOptions));actions.add(new MapAction("Apparence de la carte",this::showMapAppearanceOptions));actions.add(new MapAction("Bulle radar et surveillance",this::showFloatingRadarControls));actions.add(new MapAction("État et diagnostic",()->showMapActions("État et diagnostic",new MapAction("État GPS et radars",()->showCompactMapDiagnostics(gps==null?"GPS en attente":gps.getText().toString(),radarLoadStatus.isEmpty()?"Bases radar prêtes":radarLoadStatus)),new MapAction("Diagnostic de l’appareil",this::showDeviceDiagnostics))));
    if(routeRequestPending)actions.add(new MapAction("Annuler le calcul du trajet",()->{cancelRouteCalculation();ReferenceUi.refreshNow(this);Toast.makeText(this,"Calcul annulé",Toast.LENGTH_SHORT).show();}));
    showMapActions("Options de la carte",actions.toArray(new MapAction[0]));
  }
  void showDrivingMapOptions(){
    showMapActions("Conduite et guidage",
      new MapAction("Recentrer la carte",()->{follow=true;if(map!=null&&mapReady)map.evaluateJavascript("resumeCamera()",null);pushPosition();}),
      new MapAction("Zoom avant",()->{if(map!=null&&mapReady)map.evaluateJavascript("if(typeof rgManualZoom==='function')rgManualZoom(1)",null);}),
      new MapAction("Zoom arrière",()->{if(map!=null&&mapReady)map.evaluateJavascript("if(typeof rgManualZoom==='function')rgManualZoom(-1)",null);}),
      new MapAction("Orientation : "+(headingUp?"sens de la route":"nord en haut"),()->{headingUp=!headingUp;getPreferences(0).edit().putBoolean("heading_up",headingUp).apply();pushPosition();}),
      new MapAction("Recalcul automatique : "+(automaticRerouteEnabled?"activé":"désactivé"),()->{automaticRerouteEnabled=!getPreferences(0).getBoolean("auto_reroute",false);getPreferences(0).edit().putBoolean("auto_reroute",automaticRerouteEnabled).apply();reroutePolicy.resetDetection();}),
      new MapAction("Instructions vocales : "+(getPreferences(0).getBoolean("Guidage vocal",true)?"activées":"désactivées"),()->getPreferences(0).edit().putBoolean("Guidage vocal",!getPreferences(0).getBoolean("Guidage vocal",true)).apply()));
  }
  void showMapAppearanceOptions(){showMapActions("Apparence de la carte",new MapAction("Palette et textures",this::showMapPackPicker),new MapAction("Carte : "+(nightMap?"nuit":"jour"),()->{nightMap=!nightMap;getPreferences(0).edit().putBoolean("night_map",nightMap).putBoolean("theme_auto",false).apply();if(community!=null)community.markPreferencesChanged();applyMapTheme();}));}
  void showDeviceDiagnostics(){
    new AlertDialog.Builder(this).setTitle("Diagnostic de l’appareil").setMessage(DeviceDiagnostics.describe(this)+"\n\n"+localMapDiagnostic()+"\n\n"+mapEngineDiagnostic)
      .setPositiveButton("Fermer",null)
      .setNeutralButton("Réglages de l’application",(d,w)->{try{startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())));}catch(Exception unavailable){Toast.makeText(this,"Réglages Android indisponibles",Toast.LENGTH_SHORT).show();}})
      .setNegativeButton("Réglages GPS",(d,w)->{try{startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));}catch(Exception unavailable){Toast.makeText(this,"Réglages GPS indisponibles",Toast.LENGTH_SHORT).show();}}).show();
  }
  void showCompactMapDiagnostics(String gpsState,String radarState){
    final String details=DeviceDiagnostics.describe(this)+"\n\n"+localMapDiagnostic()+"\n\n"+gpsState+"\n\n"+radarState+"\n\n"+mapEngineDiagnostic+"\n\nCarte : "+mapConnectionMode+(mapNetworkAvailable()?" · réseau disponible":" · données locales et cache");
    if(map==null){new AlertDialog.Builder(this).setTitle("État de la carte").setMessage(details).setPositiveButton("Fermer",null).show();return;}
    map.evaluateJavascript("typeof getMapDiagnostics==='function'?getMapDiagnostics():((document.getElementById('status')||document.getElementById('rg-camera')||{}).textContent||'')",value->{
      if(isFinishing()||isDestroyed())return;String status="";try{status=new org.json.JSONArray("["+value+"]").optString(0,"");}catch(Exception ignored){}
      new AlertDialog.Builder(this).setTitle("État GPS, carte et radars").setMessage(details+(status.isEmpty()?"":"\n\n"+status)).setPositiveButton("Fermer",null).setNeutralButton("Copier diagnostic",(a,b)->{android.content.ClipboardManager clipboard=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);if(clipboard!=null){clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Diagnostic ROADGUARD",details+"\n\n"+value));Toast.makeText(this,"Diagnostic copié",Toast.LENGTH_SHORT).show();}}).setNegativeButton("Relancer la carte",(a,b)->retryMapTiles()).show();
    });
  }
  void publishMapGpsState(){
    if(map==null||!mapReady)return;
    boolean fresh=!gpsSignalSuspended&&AlertPresentation.fresh(previousAcceptedGps,System.currentTimeMillis(),SystemClock.elapsedRealtimeNanos());
    if(lastPushedGpsFresh!=null&&lastPushedGpsFresh.booleanValue()==fresh)return;lastPushedGpsFresh=fresh;
    map.evaluateJavascript("if(typeof setRoadguardGpsState==='function')setRoadguardGpsState("+fresh+")",null);
  }
  void syncMapTopInset(){
    if(map==null||!mapReady||mapTopOverlay==null)return;
    int inset=(int)Math.ceil(((mapTopScroll!=null?mapTopScroll.getBottom():mapTopOverlay.getBottom())+pxDp(8))/getResources().getDisplayMetrics().density);
    if(inset==lastMapTopInsetDp)return;lastMapTopInsetDp=inset;
    map.evaluateJavascript("window.rgSetTopInset&&window.rgSetTopInset("+inset+")",null);
  }
  java.util.concurrent.Future<?> radarCatalogReload;
  void requestRadarCatalogReload(){requestRadarCatalogReload(false);}
  /** Only a complete worker snapshot is published. Custom points are taken at publication time. */
  void requestRadarCatalogReload(boolean force){
    if(!radarDataReady||isFinishing()||isDestroyed())return;
    if(force){radarLoadGeneration++;radarReloadPending=true;}
    if(radarCatalogReloadRunning)return;
    final String revision=RadarCatalogRevision.current(this);
    if(!radarReloadPending&&revision.equals(loadedRadarCatalogRevision))return;
    radarReloadPending=false;radarCatalogReloadRunning=true;
    final long generation=radarLoadGeneration;
    final boolean historical=getPreferences(0).getBoolean(BundledRadarCatalog.HISTORICAL_PREFERENCE,false);
    final boolean swissHistorical=getPreferences(0).getBoolean(BundledRadarCatalog.SWISS_HISTORICAL_PREFERENCE,true);
    radarLoadStatus=radarCatalogLoaded?"Radars : mise à jour en arrière-plan…":"Radars : chargement en arrière-plan…";updateRadarLoadingLabel();
    radarCatalogReload=initialDataExecutor.submit(()->{
      try{
        final java.util.List<BundledRadarCatalog.Point> points=loadPhoneRadarCatalog(historical,swissHistorical);
        final ArrayList<Danger> catalog=new ArrayList<>();
        int fr=0,ch=0,neighbors=0;
        for(BundledRadarCatalog.Point point:points){catalog.add(new Danger(point));if("FR".equals(point.country))fr++;else if("CH".equals(point.country))ch++;else neighbors++;}
        final int france=fr,swiss=ch,other=neighbors;
        runOnUiThread(()->{
          radarCatalogReloadRunning=false;
          if(isFinishing()||isDestroyed())return;
          if(generation!=radarLoadGeneration||!revision.equals(RadarCatalogRevision.current(this))){requestRadarCatalogReload(true);return;}
          ArrayList<Danger> next=new ArrayList<>();
          for(Danger point:dangers)if(!point.catalogPoint)next.add(point);
          next.addAll(catalog);clearDangerWarning();dangers=next;rebuildDangerSpatialIndex();
          bundledLoadedRadarCount=catalog.size();bundledLoadedFranceCount=france;bundledLoadedSwissCount=swiss;bundledLoadedNeighborCount=other;
          officialRadarCount=BundledRadarCatalog.OFFICIAL_FR_COUNT;radarCatalogLoaded=true;
          loadedRadarCatalogRevision=revision;RadarActivationState.consumed(this,"phone",revision,points);
          radarLoadStatus="";updateRadarLoadingLabel();pushDangers();ReferenceUi.refreshNow(this);
          if(radarReloadPending)requestRadarCatalogReload();
        });
      }catch(Exception error){
        android.util.Log.e("ROADGUARD_RADARS","Catalogue précédent conservé après échec du rechargement",error);
        runOnUiThread(()->{radarCatalogReloadRunning=false;if(isDestroyed())return;radarLoadStatus=radarCatalogLoaded?"Radars : mise à jour échouée · base précédente conservée":"Radars non chargés · toucher pour réessayer";updateRadarLoadingLabel();if(radarReloadPending)requestRadarCatalogReload();});
      }
    });
    if(initialLoad==null)initialLoad=radarCatalogReload;
  }
  java.util.List<BundledRadarCatalog.Point> loadPhoneRadarCatalog(boolean historical,boolean swissHistorical)throws IOException{
    java.util.ArrayList<BundledRadarCatalog.Point> extra=new java.util.ArrayList<>();
    JSONArray inventory=community==null?new JSONArray():community.inventory;
    for(int i=0;i<inventory.length()&&i<100000;i++){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("Chargement annulé");JSONObject r=inventory.optJSONObject(i);if(r==null)continue;double lat=r.optDouble("lat",Double.NaN),lon=r.optDouble("lng",Double.NaN);if(!CommunityPolicy.coordinate(lat,lon))continue;String type=CommunityPolicy.label(r.optString("kind"));extra.add(new BundledRadarCatalog.Point("community-inventory-"+i,type,lat,lon,"community",false,"ROADGUARD community","",r.optInt("speed",0)));}
    return InstalledRadarCatalog.active(getApplicationContext(),historical,swissHistorical,extra);
  }
  void updateRadarLoadingLabel(){if(radarLoadingLabel!=null){radarLoadingLabel.setText(radarLoadStatus);radarLoadingLabel.setVisibility(radarLoadStatus.isEmpty()?View.GONE:View.VISIBLE);}}
  void initializeRoadguard(){
    if(radarDataReady||isFinishing()||isDestroyed())return;
    try{TileArchiveInstaller.recover(new File(getFilesDir(),"tiles"));}catch(IOException unavailable){android.util.Log.w("ROADGUARD","Récupération du pack de tuiles différée",unavailable);}
    visibleTileCache=new VisibleTileCache(new File(getCacheDir(),"visible-tiles"));lm=(LocationManager)getSystemService(LOCATION_SERVICE);
    // Only small local preferences are restored before the map; no catalog or HTTP wait.
    try{parsePrivateDangers(getPreferences(0).getString("danger_csv",""));loadCustomRadars();loadRoute();loadRoads();loadTrip();loadDestination();loadCachedNavigation();}
    catch(Exception error){android.util.Log.e("ROADGUARD","Restauration des préférences locales",error);}
    // Preserve downloaded maps between launches. The mosaic makes every installed
    // region available together, without forcing the user to select one country.
    mapLibrePreview=mapLibreAssetsPresent()&&!installedMosaicRegions().isEmpty();
    installBundledMapIfPresent();installBundledRoadsIfPresent();
    tts=new TextToSpeech(this,status->{if(status==TextToSpeech.SUCCESS){voiceReady=tts.setLanguage(Locale.FRANCE)>=TextToSpeech.LANG_AVAILABLE;tts.setSpeechRate(getPreferences(0).getInt("voice_rate",100)/100f);tts.setPitch(getPreferences(0).getInt("voice_pitch",100)/100f);applyNativeVoice();}});tts.setAudioAttributes(new android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE).setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH).build());alertSpeechTracker.useAudio(GuidanceAudio.get(this).speech(tts));alertSpeechTracker.install(tts);
    startupStatus=null;radarDataReady=true;observeMapNetwork();render();requestRadarCatalogReload(true);initialDataExecutor.execute(()->TileArchiveInstaller.cleanAbandoned(new File(getFilesDir(),"tiles")));if(community!=null)community.initialized();SiteAppUpdate.check(this,false);
  }

  // Show the supplied scene for the first frame while the app initializes.
  void showStartupScreen(){
    FrameLayout startup=new FrameLayout(this);startup.setBackground(new RoadguardBackdrop(this,0x18060b12));
    LinearLayout heading=column();heading.setGravity(Gravity.CENTER);heading.setPadding(pxDp(20),pxDp(35),pxDp(20),0);
    TextView name=label("ROADGUARD",30);name.setGravity(Gravity.CENTER);name.setTypeface(null,Typeface.BOLD);name.setTextColor(0xffff425b);name.setShadowLayer(pxDp(8),0,pxDp(2),0xff000000);add(heading,name);
    TextView tagline=label("VOTRE ROUTE. EN DIRECT.",11);tagline.setGravity(Gravity.CENTER);tagline.setTextColor(0xffe7ecf4);add(heading,tagline);
    FrameLayout.LayoutParams head=new FrameLayout.LayoutParams(-1,-2,Gravity.TOP);startup.addView(heading,head);
    LinearLayout loading=column();loading.setGravity(Gravity.CENTER);loading.setPadding(pxDp(20),pxDp(12),pxDp(20),pxDp(28));
    ProgressBar spinner=new ProgressBar(this);spinner.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(0xffff425b));loading.addView(spinner,new LinearLayout.LayoutParams(pxDp(28),pxDp(28)));
    TextView status=label("Chargement de ROADGUARD…",12);startupStatus=status;status.setGravity(Gravity.CENTER);status.setPadding(0,pxDp(10),0,0);add(loading,status);
    startup.addView(loading,new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM));setContentView(startup);
    // Two frame callbacks let the illustration paint before existing startup work.
    startup.postOnAnimation(()->startup.postOnAnimation(()->{if(!isFinishing()&&!isDestroyed()){initializeRoadguard();if(community!=null)community.prepareStartup();}}));
  }
  // One-time cleanup: prevents settings from older builds from re-enabling removed map downloads.
  void clearLegacyPrefetchSettings(){
    android.content.SharedPreferences.Editor edit=getPreferences(0).edit()
      .remove("licensed_emergency_prefetch_allowed").remove("licensed_emergency_map_enabled")
      .remove("licensed_emergency_xyz_template").remove("licensed_emergency_cache_limit_mb")
      .remove("emergency_last_failed_batch_at").remove("emergency_consecutive_failed_batches")
      .remove("emergency_last_batch_at").remove("emergency_last_batch_attempts")
      .remove("emergency_last_batch_saved").remove("emergency_last_batch_cache_hits")
      .remove("emergency_plan_cursor").remove("emergency_plan_size").remove("emergency_plan_identity")
      .remove("nearby_offline_50km").remove("nearby_offline_queue").remove("nearby_offline_plan_at")
      .remove("last_pack_sync_id").remove("last_pack_sync_stored").remove("last_pack_sync_count")
      .remove("last_pack_sync_remaining").remove("last_pack_sync_at");
    edit.apply();
  }
  boolean mapLibreAssetsPresent(){try{getAssets().open("vendor/maplibre-gl.js").close();getAssets().open("vendor/maplibre-gl.css").close();return true;}catch(Exception absent){return false;}}
  void activateExistingFrancePmtiles(){
    File dir=new File(getFilesDir(),"pmtiles");File[] archives=dir.listFiles((d,n)->n.endsWith(".pmtiles"));if(archives==null)return;
    String active=getPreferences(0).getString("active_vector_pmtiles","");
    if(active.matches("[A-Za-z0-9_-]{1,64}")){
      File candidate=new File(dir,active+".pmtiles");try{if(candidate.isFile()&&new PmtilesReader(candidate).isVector()){mapLibrePreview=mapLibreAssetsPresent();return;}}catch(Exception ignored){}
    }
    for(File f:archives){
      if(!f.getName().matches("[A-Za-z0-9_-]{1,64}\\.pmtiles"))continue;
      try{if(new PmtilesReader(f).isVector()&&pmtilesCoversFranceAndCorsica(f)){
        String id=f.getName().substring(0,f.getName().length()-8);getPreferences(0).edit().putString("active_vector_pmtiles",id).remove("active_raster_pmtiles").apply();
        mapLibrePreview=mapLibreAssetsPresent();return;
      }}catch(Exception ignored){}
    }
  }
  String getPackageVersion(){try{return getPackageManager().getPackageInfo(getPackageName(),0).versionName;}catch(Exception ex){return "inconnue";}}
  String mapStorageSummary(){
    File pm=new File(getFilesDir(),"pmtiles");
    File[] archives=pm.listFiles((dir,name)->name.endsWith(".pmtiles"));
    long total=0;int valid=0,invalid=0;
    if(archives!=null)for(File f:archives){total+=f.length();try{pmtilesHeaderSummary(f);valid++;}catch(Exception e){invalid++;}}
    return "PMTiles : "+valid+" archive(s) valide(s), "+invalid+" invalide(s) · "+(total/(1024*1024))+" Mo stockés · aperçu raster/vectoriel expérimental";
  }
  // Shared graphite/red surface system. Convert inherited navy-blue card fills
  // to charcoal while preserving the red action states and the blue GPS marker.
  int roadguardSurfaceColor(int color,boolean border){
    int a=android.graphics.Color.alpha(color),r=android.graphics.Color.red(color),g=android.graphics.Color.green(color),b=android.graphics.Color.blue(color);
    // Keep ROADGUARD reds intact; lift dark blue/neutral legacy surfaces into
    // readable graphite instead of flattening every card to near-black.
    if(r>g*1.28f&&r>b*1.24f)return color;
    float luma=r*.26f+g*.40f+b*.34f;
    int gray=Math.round(luma*(border?.82f:1.12f)+(border?25f:9f));
    gray=Math.max(border?48:26,Math.min(border?102:62,gray));
    return android.graphics.Color.argb(a,Math.max(0,gray-3),gray,Math.min(255,gray+7));
  }
  GradientDrawable premiumPanel(int topColor,int bottomColor,int border,int radius){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{roadguardSurfaceColor(topColor,false),roadguardSurfaceColor(bottomColor,false)});d.setCornerRadius(pxDp(radius));d.setStroke(pxDp(1),roadguardSurfaceColor(border,true));return d;}
  int accentShade(float light){return Color.rgb(Math.round(Color.red(blue)*light),Math.round(Color.green(blue)*light),Math.round(Color.blue(blue)*light));}
  int accentBorder(){return Color.luminance(blue)<.06?0xff9eb0c2:blue;}
  int accentText(){return Color.luminance(blue)>.42?0xff111820:0xffffffff;}
  GradientDrawable accentPanel(int radius,boolean strong){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{strong?blue:accentShade(.48f),strong?accentShade(.72f):accentShade(.32f)});d.setCornerRadius(pxDp(radius));d.setStroke(pxDp(2),accentBorder());return d;}
  GradientDrawable accentOutline(int radius){GradientDrawable d=premiumPanel(0xf7111822,0xf9090e16,0xff4b5969,radius);d.setStroke(pxDp(2),accentBorder());return d;}
  GradientDrawable accentWarning(int radius){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{accentShade(.36f),accentShade(.20f)});d.setCornerRadius(pxDp(radius));d.setStroke(pxDp(2),accentBorder());return d;}
  void premiumLabel(TextView view,float spacing){view.setLetterSpacing(spacing);}
  TextView button(String text,Runnable action){TextView t=label(text,14);t.setGravity(Gravity.CENTER);pad(t,15);t.setTextColor(0xfff1f2f4);t.setBackground(outlined(0xff20242b,0xff555d68,22));t.setElevation(7);t.setMinHeight(56);t.setMinWidth(56);t.setClickable(true);t.setFocusable(true);t.setContentDescription(text.replace("\n"," "));android.util.TypedValue rippleValue=new android.util.TypedValue();if(getTheme().resolveAttribute(android.R.attr.selectableItemBackground,rippleValue,true)){t.setForeground(getDrawable(rippleValue.resourceId));}t.setOnClickListener(v->{t.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);action.run();});return t;}
  // ROADGUARD 11.14.02 : navigation premium, libelles courts et signalement prioritaire.
  // 11.14.07 : navigation reproduite d'apres la maquette de reference.
  String tabIcon(String name){if(name.equals("Carte"))return "⌖";if(name.equals("Signaler"))return "●";if(name.equals("Trajet"))return "➦";if(name.equals("Alertes"))return "⚠";if(name.equals("Hors ligne"))return "⇩";if(name.equals("Paramètres"))return "☷";return "⚙";}
  String tabCaption(String name){return name.equals("Trajet")?"Itinéraire":name.equals("Paramètres")?"Options":name.equals("Hors ligne")?"Hors ligne":name;}
  TextView navButton(String name){
    boolean mapMenu=selected.equals("Carte")&&name.equals("Carte");
    boolean report=selected.equals("Carte")&&name.equals("Signaler");
    boolean active=selected.equals(name)&&!mapMenu;
    TextView b=label(mapMenu?"☰\nMenu":tabIcon(name)+"\n"+tabCaption(name),11);
    b.setGravity(Gravity.CENTER);b.setMinHeight(pxDp(48));b.setPadding(2,3,2,2);
    b.setTextColor(active?accentBorder():0xffb8bec8);
    b.setTypeface(null,active?Typeface.BOLD:Typeface.NORMAL);
    b.setBackground(report?accentPanel(28,true):active?accentPanel(15,false):premiumPanel(0xff202a36,0xff111821,0xff455468,15));
    if(report){b.setTextColor(accentText());b.setTypeface(null,Typeface.BOLD);b.setMinHeight(pxDp(64));b.setElevation(pxDp(12));}
    b.setClickable(true);b.setFocusable(true);b.setSelected(active);
    b.setContentDescription(mapMenu?"Ouvrir le menu ROADGUARD":"Ouvrir "+name+(active?", onglet actif":""));
    b.setOnClickListener(v->{if(mapMenu){showRoadguardDrawer();return;}if(active)return;b.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);legacySection=false;selected=name;render();});
    return b;
  }
  GradientDrawable outlined(int fill,int stroke,int radius){GradientDrawable d=bg(roadguardSurfaceColor(fill,false),radius);d.setStroke(2,roadguardSurfaceColor(stroke,true));return d;}
  // 11.14.14: reusable premium section header and compact status pill.
  TextView sectionHeading(String text){TextView t=label(text,21);t.setTypeface(null,Typeface.BOLD);t.setTextColor(0xfff6f7fa);t.setPadding(18,16,18,16);t.setBackground(outlined(0xff171b23,0xffa52a37,22));return t;}
  TextView statusPill(String text){TextView t=label(text,12);t.setTextColor(0xffffc2c8);t.setPadding(14,11,14,11);t.setBackground(outlined(0xff241920,0xff743440,18));return t;}
  void sectionIntro(LinearLayout content,String heading,String description){
    LinearLayout hero=column();hero.setPadding(pxDp(16),pxDp(11),pxDp(16),pxDp(11));
    hero.setBackground(premiumPanel(0xff263343,0xff111821,0xffa13d52,21));
    TextView eyebrow=label(description,11);eyebrow.setTextColor(0xffff7181);eyebrow.setTypeface(null,Typeface.BOLD);premiumLabel(eyebrow,0.055f);add(hero,eyebrow);
    TextView title=label(heading,21);title.setTypeface(null,Typeface.BOLD);title.setTextColor(0xfff7f8fb);title.setPadding(0,pxDp(5),0,0);add(hero,title);
    LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2);hp.setMargins(0,0,0,pxDp(9));content.addView(hero,hp);
  }

  // 11.14.72: graphic status panel for secondary pages. Pure presentation, no GPS/radar changes.
  void premiumVisualBanner(LinearLayout parent,String symbol,String headline,String subtitle,int accent){
    LinearLayout banner=row();banner.setGravity(Gravity.CENTER_VERTICAL);banner.setPadding(12,9,12,9);
    banner.setMinimumHeight(pxDp(62));banner.setBackground(premiumPanel(0xff23262d,0xff11151b,accent,22));
    TextView glyph=label(symbol,30);glyph.setGravity(Gravity.CENTER);glyph.setTextColor(0xfff8f9fc);
    glyph.setMinWidth(pxDp(37));glyph.setMinHeight(pxDp(37));glyph.setBackground(outlined(0xff342131,accent,18));banner.addView(glyph);
    LinearLayout copy=column();TextView h=label(headline,17);h.setTypeface(null,Typeface.BOLD);h.setTextColor(0xfffafbff);add(copy,h);
    TextView sub=label(subtitle,12);sub.setTextColor(0xffb7c5d5);sub.setPadding(0,5,0,0);add(copy,sub);
    LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,-2,1);cp.setMargins(13,0,0,0);banner.addView(copy,cp);
    LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,-2);bp.setMargins(0,4,0,8);parent.addView(banner,bp);
  }

  // 11.14.15: reusable layout system for secondary screens; actions remain connected.
  void premiumAction(LinearLayout parent,String symbol,String heading,String subtitle,Runnable action){
    LinearLayout item=row();item.setGravity(Gravity.CENTER_VERTICAL);item.setPadding(pxDp(12),pxDp(8),pxDp(12),pxDp(8));
    item.setBackground(premiumPanel(0xff253140,0xff171d27,0xff4b5969,19));item.setMinimumHeight(pxDp(70));item.setElevation(pxDp(2));
    TextView glyph=label(symbol,21);glyph.setTextColor(0xffff5965);glyph.setGravity(Gravity.CENTER);
    glyph.setMinWidth(pxDp(44));glyph.setMinHeight(pxDp(44));glyph.setBackground(outlined(0xff33212e,0xff914454,15));glyph.setPadding(3,4,3,4);item.addView(glyph);
    LinearLayout words=column();TextView main=label(heading,16);main.setTypeface(null,Typeface.BOLD);main.setTextColor(0xfff6f7fa);main.setMaxLines(2);add(words,main);
    TextView detail=label(subtitle,13);detail.setTextColor(0xffbdc7d4);detail.setMaxLines(2);add(words,detail);
    LinearLayout.LayoutParams wordParams=new LinearLayout.LayoutParams(0,-2,1);wordParams.setMargins(pxDp(11),0,pxDp(8),0);item.addView(words,wordParams);TextView arrow=label("›",22);arrow.setTextColor(0xffff6576);item.addView(arrow);
    item.setClickable(true);item.setFocusable(true);item.setContentDescription(heading+". "+subtitle);
    item.setOnClickListener(v->{item.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);action.run();});
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,pxDp(7));parent.addView(item,lp);
  }
  // Primary call-to-action, consistent with the approved ROADGUARD concept.
  void premiumPrimaryAction(LinearLayout parent,String text,Runnable action){
    TextView cta=button(text,action);
    cta.setGravity(Gravity.CENTER);cta.setTextSize(17);cta.setTypeface(null,Typeface.BOLD);
    cta.setTextColor(0xffffffff);cta.setMinHeight(pxDp(54));
    cta.setBackground(premiumPanel(0xffff475b,0xffae142b,0xffff7080,20));
    cta.setContentDescription(text);
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
    lp.setMargins(3,12,3,12);parent.addView(cta,lp);
  }
  void premiumMetric(LinearLayout parent,String value,String caption){
    LinearLayout card=column();card.setPadding(14,15,14,15);card.setBackground(premiumPanel(0xff28242a,0xff14161b,0xff8a4352,20));
    TextView v=label(value,23);v.setTypeface(null,Typeface.BOLD);v.setTextColor(0xffff6571);add(card,v);
    TextView c=label(caption,11);c.setTextColor(0xffd3d5dc);add(card,c);
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMargins(3,0,3,12);parent.addView(card,lp);
  }
  // 11.14.17: reusable premium screen composition; existing actions remain in place.
  void premiumOverview(LinearLayout content){
    if(selected.equals("Trajet")){
      LinearLayout stats=row();premiumMetric(stats,hasDestination()?"PRÊT":"—","Destination");premiumMetric(stats,"GPS","Guidage");add(content,stats);
      add(content,statusPill("ITINÉRAIRE · RECHERCHE ET GUIDAGE"));
    }else if(selected.equals("Alertes")){
      LinearLayout stats=row();premiumMetric(stats,"RADAR","Alertes locales");premiumMetric(stats,"GPS","Position réelle");add(content,stats);
      add(content,statusPill("RADARS · GESTION LOCALE"));
      premiumRouteStep(content,"⇩","Mises à jour radar","Ouvrir le gestionnaire de bases vérifiées · FR / CH",false,
        ()->RadarDownloadEntry.open(this));
    }else if(selected.equals("Signaler")){
      add(content,statusPill("SIGNALEMENT LOCAL · POSITION GPS"));
      
    }else if(selected.equals("Paramètres")){
      add(content,statusPill("CONFIGURATION · CONDUITE ET AFFICHAGE"));
    }else if(selected.equals("Apparence")){
      add(content,statusPill("THÈMES · CARTE ET INTERFACE"));
    }else if(selected.equals("Hors ligne")){
      add(content,statusPill("CARTOGRAPHIE · STOCKAGE LOCAL CONSERVÉ"));
    }
  }
  // 11.14.75: structured route timeline, using actual state and existing actions.
  void premiumRouteStep(LinearLayout parent,String number,String title,String detail,boolean active,Runnable action){
    LinearLayout item=row();item.setGravity(Gravity.CENTER_VERTICAL);item.setPadding(13,13,13,13);item.setElevation(pxDp(2));
    item.setBackground(premiumPanel(active?0xff30202b:0xff182433,0xff101925,active?0xffad4158:0xff34465b,18));
    TextView index=label(number,17);index.setTypeface(null,Typeface.BOLD);index.setGravity(Gravity.CENTER);
    index.setTextColor(active?0xffffb6c0:0xffd0d9e6);index.setBackground(outlined(active?0xff873344:0xff26354a,active?0xffcb5267:0xff526477,14));
    LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(pxDp(42),pxDp(42));ip.setMargins(0,0,12,0);item.addView(index,ip);
    LinearLayout copy=column();TextView name=label(title,15);name.setTypeface(null,Typeface.BOLD);name.setTextColor(0xfff2f4f9);add(copy,name);
    TextView sub=label(detail,12);sub.setTextColor(0xffaebdcd);add(copy,sub);item.addView(copy,new LinearLayout.LayoutParams(0,-2,1));
    if(action!=null){TextView arrow=label("›",25);arrow.setTextColor(0xffff7180);item.addView(arrow);item.setOnClickListener(v->action.run());item.setContentDescription(title+". "+detail);item.setClickable(true);}
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,9);parent.addView(item,lp);
  }
  void premiumRouteTimeline(LinearLayout parent){
    premiumGroup(parent,"PARCOURS EN 3 ÉTAPES","De la position actuelle jusqu'au guidage.");
    premiumRouteStep(parent,"01","Point de départ","Ma position GPS · actualisée sur la carte",false,()->{selected="Carte";render();});
    premiumRouteStep(parent,"02","Destination",hasDestination()?String.format(Locale.FRANCE,"%.5f, %.5f",destinationLat,destinationLon):"Choisir une adresse ou un lieu",!hasDestination(),()->askAddress());
    premiumRouteStep(parent,"03","Navigation",navigationPoints.size()>1?String.format(Locale.FRANCE,"%.1f km · %.0f min estimées",navigationMeters/1000,navigationSeconds/60):hasDestination()?"Calculer le trajet pour commencer":"En attente d'une destination",hasDestination(),hasDestination()?()->{if(navigationPoints.size()>1){selected="Carte";render();}else if(!routeRequestPending)calculateRoadRoute();}:null);
  }
  void premiumGroup(LinearLayout parent,String title,String caption){
    LinearLayout group=row();group.setGravity(Gravity.CENTER_VERTICAL);group.setPadding(pxDp(4),pxDp(10),pxDp(4),pxDp(7));
    TextView accent=label("",1);accent.setBackground(premiumPanel(0xffff5268,0xffa72b40,0xffff8492,4));
    LinearLayout.LayoutParams accentLp=new LinearLayout.LayoutParams(pxDp(4),pxDp(caption.isEmpty()?25:34));accentLp.setMargins(0,0,pxDp(10),0);group.addView(accent,accentLp);
    LinearLayout copy=column();TextView heading=label(title,15);heading.setTypeface(null,Typeface.BOLD);heading.setTextColor(0xffff7180);premiumLabel(heading,0.035f);add(copy,heading);
    if(!caption.isEmpty()){TextView sub=label(caption,12);sub.setTextColor(0xffb7c1ce);sub.setPadding(0,pxDp(3),0,0);add(copy,sub);}
    group.addView(copy,new LinearLayout.LayoutParams(0,-2,1));add(parent,group);
  }
  void premiumToggle(LinearLayout parent,String symbol,String heading,String detail,String preference,boolean fallback){
    boolean enabled=getPreferences(0).getBoolean(preference,fallback);
    premiumAction(parent,symbol,heading+(enabled?"  ·  ACTIVÉ":"  ·  DÉSACTIVÉ"),detail,()->{
      getPreferences(0).edit().putBoolean(preference,!enabled).apply();render();
    });
  }
  void showRoadguardDrawer(){
    final Dialog dialog=new Dialog(this);LinearLayout shell=column();shell.setPadding(pxDp(14),pxDp(12),pxDp(12),pxDp(12));shell.setBackground(premiumPanel(0xff131e2c,0xff070d16,0xff41546a,0));
    LinearLayout brand=row();TextView wordmark=label("ROADGUARD",19);wordmark.setTypeface(null,Typeface.BOLD);wordmark.setTextColor(accentBorder());brand.addView(wordmark,new LinearLayout.LayoutParams(0,-2,1));TextView close=button("×",dialog::dismiss);close.setTextSize(23);close.setMinWidth(pxDp(44));close.setMinHeight(pxDp(44));LinearLayout.LayoutParams closeLp=new LinearLayout.LayoutParams(-2,-2);closeLp.gravity=Gravity.END;brand.addView(close,closeLp);add(shell,brand);
    LinearLayout profile=row();profile.setPadding(pxDp(10),pxDp(11),pxDp(10),pxDp(11));profile.setBackground(premiumPanel(0xb3222832,0xc4141920,0xff495361,16));TextView avatar=label("●",20);avatar.setGravity(Gravity.CENTER);avatar.setTextColor(0xffbec9d6);avatar.setBackground(outlined(0xff303943,0xff6a7481,25));avatar.setMinWidth(pxDp(42));avatar.setMinHeight(pxDp(42));profile.addView(avatar);LinearLayout account=column();TextView user=label("ROADGUARD",14);user.setTypeface(null,Typeface.BOLD);add(account,user);TextView state=label("Ta route · tes cartes · tes alertes",11);state.setTextColor(0xffb6c0cd);add(account,state);LinearLayout.LayoutParams accountLp=new LinearLayout.LayoutParams(0,-2,1);accountLp.setMargins(pxDp(10),0,0,0);profile.addView(account,accountLp);LinearLayout.LayoutParams profileLp=new LinearLayout.LayoutParams(-1,-2);profileLp.setMargins(0,pxDp(12),0,pxDp(10));shell.addView(profile,profileLp);
    ScrollView menuScroll=new ScrollView(this);menuScroll.setFillViewport(false);menuScroll.setVerticalScrollBarEnabled(false);
    LinearLayout items=column();menuScroll.addView(items);
    drawerGroup(items,"CONDUITE");
    drawerItem(items,"➤","Carte","GPS et recherche de destination",dialog,()->{selected="Carte";render();});
    drawerItem(items,"♥","Favoris","Domicile, travail et destinations enregistrées",dialog,this::showDestinationFavorites);
    drawerItem(items,"◉","Radars et alertes","Voir les alertes et les bases",dialog,()->{legacySection=false;selected="Alertes";render();});
    drawerItem(items,"⚠","Signaler","Danger ou contrôle sur la route",dialog,()->{legacySection=false;selected="Signaler";render();});
    drawerGroup(items,"MES CARTES");
    drawerItem(items,"⇩","Cartes hors connexion","France entière et autres pays",dialog,()->{legacySection=false;selected="Hors ligne";render();});
    drawerGroup(items,"MON APPLICATION");
    drawerItem(items,"◐","Couleurs et apparence","Jour, nuit, curseur et voix",dialog,()->{legacySection=false;selected="Apparence";render();});
    drawerItem(items,"⚙","Réglages","Alertes, GPS et autres options",dialog,()->{legacySection=false;selected="Paramètres";render();});
    drawerItem(items,"♙","Mon compte","Connexion et signalements partagés",dialog,()->{legacySection=false;selected="Compte";render();});
    drawerItem(items,"⌂","Accueil","Revenir aux choix principaux",dialog,()->{legacySection=false;selected="Accueil";render();});
    shell.addView(menuScroll,new LinearLayout.LayoutParams(-1,0,1));dialog.setContentView(shell);Window window=dialog.getWindow();if(window!=null){window.setBackgroundDrawableResource(android.R.color.transparent);window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);WindowManager.LayoutParams lp=window.getAttributes();lp.width=Math.min(pxDp(330),getResources().getDisplayMetrics().widthPixels-pxDp(42));lp.height=WindowManager.LayoutParams.MATCH_PARENT;lp.gravity=Gravity.START;lp.dimAmount=.62f;window.setAttributes(lp);}dialog.show();if(window!=null){window.setLayout(Math.min(pxDp(330),getResources().getDisplayMetrics().widthPixels-pxDp(42)),WindowManager.LayoutParams.MATCH_PARENT);window.setGravity(Gravity.START);}
  }
  void drawerGroup(LinearLayout parent,String name){TextView title=label(name,11);title.setTypeface(null,Typeface.BOLD);title.setLetterSpacing(.12f);title.setTextColor(0xffff7286);title.setPadding(pxDp(12),pxDp(18),0,pxDp(5));add(parent,title);}
  void drawerItem(LinearLayout parent,String icon,String title,String subtitle,Dialog dialog,Runnable action){LinearLayout item=row();item.setPadding(pxDp(10),pxDp(10),pxDp(10),pxDp(10));TextView glyph=label(icon,19);glyph.setGravity(Gravity.CENTER);glyph.setTextColor(accentBorder());glyph.setMinWidth(pxDp(34));item.addView(glyph);LinearLayout copy=column();TextView h=label(title,14);h.setTypeface(null,Typeface.BOLD);h.setTextColor(0xfff5f6f8);add(copy,h);TextView sub=label(subtitle,10);sub.setTextColor(0xffaeb8c5);add(copy,sub);item.addView(copy,new LinearLayout.LayoutParams(0,-2,1));TextView arrow=label("›",20);arrow.setTextColor(accentBorder());item.addView(arrow);item.setBackground(premiumPanel(0xff263243,0xff121c29,0xff43556b,16));item.setClickable(true);item.setFocusable(true);item.setOnClickListener(v->{dialog.dismiss();action.run();});LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,pxDp(4),0,pxDp(4));parent.addView(item,p);}
  void openInlineDestinationSearch(){
    if(destinationSearchDialog!=null&&destinationSearchDialog.isShowing())return;
    LinearLayout body=column();body.setPadding(pxDp(16),pxDp(8),pxDp(16),pxDp(10));
    EditText input=new EditText(this);input.setSingleLine(true);input.setHint("Adresse, commune ou coordonnées");input.setTextColor(0xfff4f6fb);input.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);add(body,input);
    ScrollView scroll=new ScrollView(this);LinearLayout results=column();scroll.addView(results);body.addView(scroll,new LinearLayout.LayoutParams(-1,pxDp(240)));
    destinationSearchInput=input;driveSearchResults=results;
    final AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Rechercher une destination").setView(body).setNegativeButton("Fermer",null).setPositiveButton("Rechercher",null).create();destinationSearchDialog=dialog;
    input.setOnEditorActionListener((v,action,event)->{if(action==android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH){searchDestinationFromBar();return true;}return false;});
    input.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence v,int start,int count,int after){}public void onTextChanged(CharSequence v,int start,int before,int count){}public void afterTextChanged(android.text.Editable v){final String query=v.toString().trim();final int token=nextDriveSearchToken();if(driveSearchRunnable!=null)driveSearchHandler.removeCallbacks(driveSearchRunnable);results.removeAllViews();if(query.length()<3)return;driveSearchRunnable=()->{if(token==driveSearchRequestToken&&destinationSearchInput==input)runDriveSearch(query,false,token);};driveSearchHandler.postDelayed(driveSearchRunnable,750);}});
    dialog.setOnShowListener(d->{dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->searchDestinationFromBar());input.requestFocus();dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);});
    dialog.setOnDismissListener(d->{if(destinationSearchDialog==dialog){destinationSearchDialog=null;nextDriveSearchToken();if(driveSearchRunnable!=null)driveSearchHandler.removeCallbacks(driveSearchRunnable);if(destinationSearchInput==input){destinationSearchInput=null;driveSearchResults=null;}}});dialog.show();
  }

  void render(){averageZoneBadge=null;if(destinationSearchDialog==null&&destinationSearchInput!=null&&driveSearchDestinationKey.equals(getPreferences(0).getString("destination",""))){restoredSearchQuery=destinationSearchInput.getText().toString();restoredDriveSheetLevel=driveSheetLevel;}if(destinationSearchDialog==null){nextDriveSearchToken();if(driveSearchRunnable!=null)driveSearchHandler.removeCallbacks(driveSearchRunnable);destinationSearchInput=null;driveSearchResults=null;}if(legacySection&&!selected.equals(legacySectionScreen))legacySection=false;driveSheet=null;guidanceFooterHeightDp=0;lastMapTopInsetDp=-1;lastMapSheetOcclusionDp=-1;mapTopHeightLimit=-1;mapOverlayLayoutScheduled=false;driveSheetHandle=null;stopRadarPulse();if(map!=null){map.destroy();map=null;}if(referenceWebView!=null){referenceWebView.destroy();referenceWebView=null;}mapTopOverlay=null;mapTopScroll=null;mapActionRail=null;guidanceFooter=null;inlineMapSpeed=null;speedHudFrame=null;mapConnectionSwitch=null;floatingChip=null;radarLoadingLabel=null;speed=null;gps=null;routeStatus=null;modeChip=null;accuracyChip=null;headingChip=null;searchWidget=null;gpsWidget=null;dangerBanner=null;drivingDecluttered=false;maneuverTitle=null;maneuverDistance=null;maneuverGlyph=null;tripMinutes=null;tripArrival=null;tripSummary=null;routeProgress=null;mapReady=false;if(!accountConnected()){selected="Compte";legacySection=false;root=column();root.setBackgroundColor(navy);setContentView(root);frame=new FrameLayout(this);root.addView(frame,new LinearLayout.LayoutParams(-1,-1));showReferenceSection();return;}root=column();root.setBackgroundColor(navy);setContentView(root);top=row();top.setPadding(8,2,8,2);TextView menu=button("☰",()->showRoadguardDrawer());menu.setMinHeight(pxDp(30));menu.setMinWidth(pxDp(30));menu.setPadding(0,0,0,0);menu.setTextSize(22);menu.setContentDescription("Ouvrir le menu latéral ROADGUARD");menu.setBackground(outlined(0xff171d26,0xff343c48,16));top.addView(menu);TextView title=label("◈ ROADGUARD",14);premiumLabel(title,0.06f);title.setSingleLine(true);title.setEllipsize(android.text.TextUtils.TruncateAt.END);title.setPadding(pxDp(6),0,0,0);title.setTypeface(null,Typeface.BOLD);title.setOnClickListener(v->showProjectDashboard());title.setContentDescription("ROADGUARD, toucher pour voir les barres de progression et la maquette");top.addView(title,new LinearLayout.LayoutParams(0,-2,1));TextView version=label("v"+getPackageVersion(),9);version.setSingleLine(true);version.setTextColor(0xffff5260);top.addView(version);if(getResources().getConfiguration().screenWidthDp<390){version.setVisibility(android.view.View.GONE);title.setText("ROADGUARD");}top.setBackground(premiumPanel(0xff182230,0xff090d15,0xff202b3b,0));if(!selected.equals("Carte")&&legacySection){TextView backToDesign=button("‹  VUE MAQUETTE",()->{legacySection=false;render();});top.addView(backToDesign);add(root,top);}
    frame=new FrameLayout(this);root.addView(frame,new LinearLayout.LayoutParams(-1,0,1));if(selected.equals("Carte")){showMap();MusicMapBubble.add(this);}else showSection();bottom=row();bottom.setBackgroundColor(0xff090d15);bottom.setPadding(3,2,3,2);for(String name:new String[]{"Carte","Alertes","Signaler","Paramètres"}){TextView b=navButton(name);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.setMargins(2,0,2,0);bottom.addView(b,p);}if(selected.equals("Carte")&&!immersiveDrive)add(root,bottom);}
  void showProjectDashboard(){
    final int red=0xffe94b5e, muted=0xffaeb9c9, white=0xfff4f6fb;
    LinearLayout shell=column();shell.setBackgroundColor(0xff101722);
    ScrollView scroll=new ScrollView(this);scroll.setFillViewport(false);
    LinearLayout body=column();body.setPadding(pxDp(18),pxDp(16),pxDp(18),pxDp(22));scroll.addView(body);
    TextView heading=label("SUIVI DU PROJET",19);heading.setTextColor(white);add(body,heading);
    TextView subtitle=label("ROADGUARD  ·  "+getPackageVersion(),12);subtitle.setTextColor(muted);add(body,subtitle);
    TextView intervention=label("🔴 INTERVENTION OBLIGATOIRE\nPLUS TARD : vérifier ce lot sur Xiaomi (carte, alertes et paysage), puis Android Auto sur un appareil compatible. Aucun test en conduite n’est déclaré validé ici.",13);
    intervention.setTextColor(0xffffffff);intervention.setPadding(pxDp(14),pxDp(12),pxDp(14),pxDp(12));
    intervention.setBackground(premiumPanel(0xff8b1423,0xff490c16,0xffff465b,12));
    LinearLayout.LayoutParams interventionParams=new LinearLayout.LayoutParams(-1,-2);interventionParams.setMargins(0,pxDp(12),0,pxDp(8));body.addView(intervention,interventionParams);

    try{
      StringBuilder jsonText=new StringBuilder();
      try(java.io.InputStream stream=getAssets().open("roadguard_progress.json");java.io.BufferedReader reader=new java.io.BufferedReader(new java.io.InputStreamReader(stream,"UTF-8"))){
        String line;while((line=reader.readLine())!=null)jsonText.append(line);
      }
      ProgressLedger ledger=new ProgressLedger(new org.json.JSONObject(jsonText.toString()));
      TextView development=label("DÉVELOPPEMENT   "+ProgressLedger.percent(ledger.development),16);development.setTextColor(white);add(body,development);
      TextView validation=label("VALIDATION   "+ProgressLedger.percent(ledger.validation),16);validation.setTextColor(white);add(body,validation);
      TextView deltas=label("Depuis la référence "+ledger.baseline+" : développement "+ProgressLedger.delta(ledger.development-ledger.previousDevelopment)+" · validation "+ProgressLedger.delta(ledger.validation-ledger.previousValidation),12);deltas.setTextColor(muted);add(body,deltas);
      TextView disclaimer=label("Grille historique de 100 points conservée. Les nouveaux correctifs ne sont pas une validation Android de cette grille. Essais sur Xiaomi regroupés à la fin.",12);disclaimer.setTextColor(muted);add(body,disclaimer);
      TextView focus=label("CHANTIER ACTIF\n"+ledger.active,13);focus.setTextColor(white);focus.setPadding(pxDp(12),pxDp(10),pxDp(12),pxDp(10));focus.setBackground(premiumPanel(0xff341a26,0xff1a111b,red,12));add(body,focus);
      for(ProgressLedger.Module module:ledger.modules){
        TextView name=label(module.name+" · poids "+String.format(Locale.FRANCE,"%.4f",module.weight),14);name.setTextColor(white);name.setPadding(0,pxDp(16),0,pxDp(4));add(body,name);
        TextView values=label("Développement "+ProgressLedger.percent(module.development)+" ("+ProgressLedger.delta(module.development-module.previousDevelopment)+")\nValidation "+ProgressLedger.percent(module.validation)+" ("+ProgressLedger.delta(module.validation-module.previousValidation)+")",12);values.setTextColor(muted);add(body,values);
        android.widget.ProgressBar bar=new android.widget.ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(1000000);bar.setProgress((int)Math.round(module.development*10000));bar.setSecondaryProgress((int)Math.round(module.validation*10000));bar.setProgressTintList(android.content.res.ColorStateList.valueOf(red));bar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff334052));
        LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,pxDp(7));bp.setMargins(0,pxDp(6),0,pxDp(8));body.addView(bar,bp);
      }
    }catch(Exception ex){TextView error=label("Suivi indisponible : "+ex.getMessage(),13);error.setTextColor(red);add(body,error);}
    shell.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    TextView close=button("FERMER",()->{});close.setTextColor(0xff70d5c3);
    LinearLayout footer=column();footer.setPadding(pxDp(12),pxDp(6),pxDp(12),pxDp(8));footer.setBackgroundColor(0xff101722);footer.addView(close,new LinearLayout.LayoutParams(-1,pxDp(48)));shell.addView(footer);
    final AlertDialog dialog=new AlertDialog.Builder(this).setView(shell).create();
    close.setOnClickListener(v->dialog.dismiss());dialog.setOnShowListener(d->{if(dialog.getWindow()!=null){dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);dialog.getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels*0.92f),(int)(getResources().getDisplayMetrics().heightPixels*0.78f));}});dialog.show();
  }
  void showDesignReference(){
    try{
      android.graphics.Bitmap bmp=android.graphics.BitmapFactory.decodeStream(getAssets().open("roadguard_maquette.png"));
      if(bmp==null)throw new IOException("Image illisible");
      android.widget.ImageView view=new android.widget.ImageView(this);view.setImageBitmap(bmp);view.setAdjustViewBounds(true);
      view.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
      ScrollView sc=new ScrollView(this);sc.addView(view);
      new AlertDialog.Builder(this).setTitle("Maquette originale · objectif visuel").setMessage("Image de référence uniquement : ce n'est pas une capture de l'application actuelle.").setView(sc).setPositiveButton("Fermer",null).show();
    }catch(Exception e){Toast.makeText(this,"Maquette indisponible : "+e.getMessage(),Toast.LENGTH_LONG).show();}
  }
  public void showAppUpdate(){SiteAppUpdate.check(this,true);}
  void showMap(){
    final String restoreManualView=lastManualMapView;
    final String restoreCachedViewport=getPreferences(0).getString("map_last_viewport","");
    if(!vectorEngineDisabledForSession&&mapLibreAssetsPresent()&&!installedMosaicRegions().isEmpty()){offlineView=false;mapLibrePreview=true;forceOnlineMapLibre=false;}
    else if("online".equals(mapConnectionMode)){offlineView=false;mapLibrePreview=false;forceOnlineMapLibre=false;}
    if(vectorEngineDisabledForSession){mapLibrePreview=false;offlineView=false;}
    mapEngineReported=false;lastPushedMapNetworkOnline=null;lastPushedGpsFresh=null;offlineFallbackChecked=false;map=new WebView(this);final WebView owner=map;LocalPagePolicy.configure(owner,true);
    owner.setWebChromeClient(new android.webkit.WebChromeClient(){@Override public boolean onConsoleMessage(android.webkit.ConsoleMessage message){
      if(map==owner&&message.messageLevel()==android.webkit.ConsoleMessage.MessageLevel.ERROR){String detail=message.message();mapEngineDiagnostic="WebView ligne "+message.lineNumber()+" : "+detail.substring(0,Math.min(800,detail.length()));}return true;
    }});owner.addJavascriptInterface(new MapRadarBridge(owner),"RoadguardMap");refreshMapNetworkCacheMode();map.setWebViewClient(new WebViewClient(){
      @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest request){String url=request.getUrl().toString();if(LocalPagePolicy.mapPage(url))return false;if(request.isForMainFrame()&&request.hasGesture()&&LocalPagePolicy.browserLink(url))try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}catch(Exception unavailable){Toast.makeText(MainActivity.this,"Aucun navigateur disponible",Toast.LENGTH_SHORT).show();}return true;}
      @Override public boolean shouldOverrideUrlLoading(WebView v,String url){return !LocalPagePolicy.mapPage(url);}
@Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest req){return serveMapResource(req);}@Override public void onPageFinished(WebView v,String url){if(map!=owner||v!=owner||!LocalPagePolicy.mapPage(url))return;mapReady=true;lastPushedMapNetworkOnline=null;lastPushedGpsFresh=null;syncMapTopInset();publishMapGpsState();owner.evaluateJavascript(RadarViewport.INSTALL,null);
      if(mapLibrePreview)owner.postDelayed(()->{if(map==owner&&!mapEngineReported)handleMapEngineState(owner,"failed","Aucun signal de démarrage du moteur après 16 secondes");},16000);
map.evaluateJavascript("typeof updateGPS === 'function'",result->{if(map!=owner)return;if(!"true".equals(result)){if(gps!=null)gps.setText("CARTE INDISPONIBLE · moteur WebView");}});      if(driveSheet!=null){int covered=Math.max(0,Math.round(driveSheet.getHeight()/getResources().getDisplayMetrics().density));publishMapBottomOcclusion(covered);}
      if(driveSheet==null&&guidanceFooterHeightDp>0)publishMapBottomOcclusion(guidanceFooterHeightDp);
      refreshMapNetworkCacheMode();if(!Double.isNaN(lastLat))pushPosition();pushDangers();pushRoute();refreshNearbySector();pushRoads();pushDestination();applyMapTheme();applyPendingMapFocus();if(community!=null)community.pushMap();
      if(mapLibrePreview&&!forceOnlineMapLibre){
        java.util.List<InstalledPmtilesMosaic.Region> regions=installedMosaicRegions();
        if(!regions.isEmpty()){
          int vectorMaxZoom=22;for(InstalledPmtilesMosaic.Region region:regions)vectorMaxZoom=Math.min(vectorMaxZoom,region.maxZoom);
          map.evaluateJavascript("setRoadguardVectorTheme(\""+(nightMap?"night":"day")+"\")",null);
          map.evaluateJavascript("setVectorPmtiles(\"all_downloaded\","+vectorMaxZoom+","+mapNetworkAvailable()+")",null);
        }
      }
      if(!mapNetworkAvailable()&&!restoreCachedViewport.isEmpty())map.evaluateJavascript("if(typeof restoreCachedMapViewport==='function')restoreCachedMapViewport("+restoreCachedViewport+")",null);
      if(!restoreManualView.isEmpty())map.evaluateJavascript("if(typeof restoreRoadguardView==='function')restoreRoadguardView("+restoreManualView+")",null);
    }});map.loadUrl(LocalMapAssets.page(offlineView,mapLibrePreview));frame.addView(map,new FrameLayout.LayoutParams(-1,-1));AverageSpeedZoneUi.attach(this,145);LinearLayout hud=row();hud.setPadding(pxDp(11),pxDp(5),pxDp(11),pxDp(5));hud.setBackground(premiumPanel(0xf9111c2b,0xf908101b,0xff52647a,22));hud.setElevation(10);gps=label("⌖  GPS EN ATTENTE",10);premiumLabel(gps,0.07f);gps.setSingleLine(true);gps.setEllipsize(android.text.TextUtils.TruncateAt.END);gps.setTextColor(0xffe9f9ff);hud.addView(gps,new LinearLayout.LayoutParams(0,-2,1));modeChip=label(offlineView?" · LOCAL":" · EN LIGNE",9);modeChip.setSingleLine(true);modeChip.setTextColor(0xffc6cdd3);hud.addView(modeChip);accuracyChip=label("",10);accuracyChip.setVisibility(View.GONE);headingChip=null;mapTopOverlay=column();mapTopOverlay.setElevation(pxDp(10));
  FrameLayout.LayoutParams topOverlayLp=new FrameLayout.LayoutParams(-1,-2,Gravity.TOP|Gravity.START);
  mapTopScroll=new ScrollView(this){@Override protected void onMeasure(int w,int h){
    int available=frame!=null&&frame.getHeight()>0?frame.getHeight():getResources().getDisplayMetrics().heightPixels;
    boolean wide=frame!=null&&frame.getWidth()>=pxDp(500)&&frame.getWidth()>available;
    int footer=driveSheet!=null?driveSheet.getHeight():(guidanceFooter!=null?guidanceFooter.getHeight()+pxDp(9):0);
    int limit=Math.max(pxDp(44),available-pxDp(20)-(wide?0:footer));
    super.onMeasure(w,View.MeasureSpec.makeMeasureSpec(limit,View.MeasureSpec.AT_MOST));
  }};mapTopScroll.setFillViewport(false);mapTopScroll.setClipToPadding(false);mapTopScroll.addView(mapTopOverlay);mapTopScroll.setElevation(pxDp(10));
  topOverlayLp.setMargins(pxDp(10),pxDp(10),pxDp(10),0);frame.addView(mapTopScroll,topOverlayLp);
  mapTopScroll.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{syncMapTopInset();scheduleMapOverlayLayout();});
  mapTopOverlay.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->scheduleMapOverlayLayout());
  frame.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->scheduleMapOverlayLayout());
  hud.setVisibility(View.GONE);gpsWidget=gps;
  LinearLayout connectionRow=row();connectionRow.setPadding(pxDp(10),0,pxDp(4),0);connectionRow.setBackground(premiumPanel(0xee171f2b,0xee0c131d,0xff475669,22));
  mapConnectionSwitch=new android.widget.RadioGroup(this);mapConnectionSwitch.setOrientation(LinearLayout.HORIZONTAL);
  for(String choice:new String[]{"auto","online","offline"}){
    android.widget.RadioButton option=new android.widget.RadioButton(this);option.setId(View.generateViewId());option.setTag(choice);option.setButtonDrawable((android.graphics.drawable.Drawable)null);option.setText("auto".equals(choice)?"Auto":("online".equals(choice)?"En ligne":"Hors ligne"));option.setTextSize(11);option.setTextColor(0xfff4f6fa);option.setGravity(Gravity.CENTER);option.setMinHeight(pxDp(44));option.setMinWidth(0);option.setPadding(pxDp(3),0,pxDp(3),0);option.setBackground(choice.equals(mapConnectionMode)?accentPanel(18,false):bg(android.graphics.Color.TRANSPARENT,18));
    mapConnectionSwitch.addView(option,new android.widget.RadioGroup.LayoutParams(0,-2,1));if(choice.equals(mapConnectionMode))mapConnectionSwitch.check(option.getId());
  }
  connectionRow.addView(mapConnectionSwitch,new LinearLayout.LayoutParams(0,-2,1));mapConnectionSwitch.setOnCheckedChangeListener((group,id)->{View checked=group.findViewById(id);if(checked!=null)selectMapConnection((String)checked.getTag());});
  TextView mapOptions=button("⋯",()->showCompactMapOptions());mapOptions.setTextSize(24);mapOptions.setPadding(0,0,0,0);mapOptions.setMinWidth(pxDp(44));mapOptions.setMinHeight(pxDp(44));mapOptions.setBackgroundColor(android.graphics.Color.TRANSPARENT);mapOptions.setContentDescription("Options de la carte, bulle radar et état GPS");connectionRow.addView(mapOptions,new LinearLayout.LayoutParams(pxDp(44),pxDp(44)));
  mapTopOverlay.addView(connectionRow,new LinearLayout.LayoutParams(-1,-2));floatingChip=null;radarLoadingLabel=null;
  // Destination search is available in the expandable route sheet and Trajet tab.
  searchWidget=null;
  // Cockpit guidance is kept outside the map WebView for legibility.
  if(navigationPoints.size()>1){
    LinearLayout turnCard=column();turnCard.setPadding(pxDp(10),pxDp(7),pxDp(10),pxDp(7));
    turnCard.setBackground(accentOutline(15));turnCard.setElevation(pxDp(12));
    LinearLayout turnLine=row();turnLine.setGravity(Gravity.CENTER_VERTICAL);
    TextView turnGlyph=label("↑",40);maneuverGlyph=turnGlyph;turnGlyph.setTextColor(accentBorder());turnGlyph.setGravity(Gravity.CENTER);turnGlyph.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP,40);turnGlyph.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    turnLine.addView(turnGlyph,new LinearLayout.LayoutParams(pxDp(54),pxDp(52)));
    LinearLayout next=column();
    maneuverDistance=label("Calcul du guidage…",18);maneuverDistance.setTypeface(null,Typeface.BOLD);maneuverDistance.setTextColor(0xfff4f8ff);add(next,maneuverDistance);
    maneuverTitle=label("",12);maneuverTitle.setTextColor(0xffd7e1ea);maneuverTitle.setMaxLines(2);add(next,maneuverTitle);
    turnLine.addView(next,new LinearLayout.LayoutParams(0,-2,1));add(turnCard,turnLine);
    routeProgress=new android.widget.ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);routeProgress.setMax(1000);routeProgress.setProgress(0);routeProgress.setProgressTintList(android.content.res.ColorStateList.valueOf(accentBorder()));routeProgress.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff244054));
    LinearLayout.LayoutParams progParams=new LinearLayout.LayoutParams(-1,pxDp(2));turnCard.addView(routeProgress,progParams);
LinearLayout.LayoutParams turnRowLp=new LinearLayout.LayoutParams(-1,-2);turnRowLp.topMargin=pxDp(8);
    mapTopOverlay.addView(turnCard,turnRowLp);
    if(hud!=null)hud.setVisibility(View.GONE);
    updateManeuverCard();
  }
  // Contextual warning stays hidden unless an imported local hazard is nearby.
  dangerBanner=label("",14);
  dangerBanner.setTextColor(0xffffffff);
  dangerBanner.setTypeface(null,Typeface.BOLD);
  dangerBanner.setPadding(pxDp(16),pxDp(14),pxDp(16),pxDp(14));
  dangerBanner.setBackground(accentWarning(19));
  dangerBanner.setElevation(16);
  dangerBanner.setVisibility(View.GONE);
  dangerBanner.setMaxLines(2);dangerBanner.setContentDescription("Alerte de proximité routière");dangerBanner.setOnClickListener(v->{if(activeRadarUi!=null){selected="Radar";legacySection=false;render();}});
  LinearLayout.LayoutParams warningParams=new LinearLayout.LayoutParams(-1,-2);warningParams.topMargin=pxDp(6);
  mapTopOverlay.addView(dangerBanner,1,warningParams);
  inlineMapSpeed=label(gpsSpeedText()+" km/h",14);inlineMapSpeed.setGravity(Gravity.CENTER);inlineMapSpeed.setPadding(0,pxDp(3),0,pxDp(3));inlineMapSpeed.setVisibility(View.GONE);mapTopOverlay.addView(inlineMapSpeed,Math.min(2,mapTopOverlay.getChildCount()));
  // 11.14.112: compact navigation app speed bubble, red ring and floating above the route sheet.
  android.widget.FrameLayout speedDial=new android.widget.FrameLayout(this);
  speedHudFrame=speedDial;
  speedDial.setMinimumWidth(pxDp(64));speedDial.setMinimumHeight(pxDp(64));
  speedDial.setBackground(accentOutline(48));
  speedDial.setElevation(pxDp(14));
  LinearLayout speedInner=column();speedInner.setGravity(Gravity.CENTER);
  speedInner.setPadding(pxDp(7),pxDp(7),pxDp(7),pxDp(6));
  speed=label(gpsSpeedText(),26);premiumLabel(speed,-0.05f);speed.setTextColor(0xffffffff);
  speed.setTypeface(null,Typeface.BOLD);speed.setGravity(Gravity.CENTER);add(speedInner,speed);
  headingChip=label("km/h",10);headingChip.setTextColor(0xffc8d4e2);
  headingChip.setGravity(Gravity.CENTER);add(speedInner,headingChip);
  android.widget.FrameLayout.LayoutParams innerParams=new android.widget.FrameLayout.LayoutParams(-1,-1,Gravity.CENTER);
  speedDial.addView(speedInner,innerParams);
  FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(pxDp(66),pxDp(66),Gravity.START|Gravity.BOTTOM);
  cp.setMargins(pxDp(12),0,0,pxDp(66));frame.addView(speedDial,cp);
  speedDial.setContentDescription(gpsSpeedText()+" kilomètres par heure");speedDial.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);speedInner.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
  // ROADGUARD 11.14.112: compact navigation controls; report is primary, radar and recenter remain secondary.
  LinearLayout actionRail=column();mapActionRail=actionRail;actionRail.setPadding(pxDp(3),pxDp(3),pxDp(3),pxDp(3));
  final boolean shortScreen=getResources().getConfiguration().screenHeightDp<690;final boolean narrowScreen=getResources().getConfiguration().screenWidthDp<380;final int railGap=shortScreen?4:7;final int railSize=shortScreen||narrowScreen?44:48;
  actionRail.setBackgroundColor(android.graphics.Color.TRANSPARENT);actionRail.setElevation(pxDp(5));
  TextView zoomIn=button("+",()->{if(map!=null&&mapReady)map.evaluateJavascript("if(typeof rgManualZoom==='function')rgManualZoom(1)",null);});zoomIn.setContentDescription("Zoom avant");zoomIn.setTextSize(21);zoomIn.setMinHeight(pxDp(railSize));zoomIn.setMinWidth(pxDp(railSize));zoomIn.setMaxWidth(pxDp(railSize));zoomIn.setPadding(0,0,0,0);zoomIn.setBackground(premiumPanel(0xff171f2b,0xff0c131d,0xff475669,15));LinearLayout.LayoutParams zoomInParams=new LinearLayout.LayoutParams(pxDp(railSize),pxDp(railSize));zoomInParams.setMargins(0,pxDp(railGap),0,pxDp(railGap));actionRail.addView(zoomIn,zoomInParams);zoomIn.setAlpha(0.9f);zoomIn.setVisibility(compactControls?View.GONE:View.VISIBLE);
  TextView zoomOut=button("−",()->{if(map!=null&&mapReady)map.evaluateJavascript("if(typeof rgManualZoom==='function')rgManualZoom(-1)",null);});zoomOut.setContentDescription("Zoom arrière");zoomOut.setTextSize(21);zoomOut.setMinHeight(pxDp(railSize));zoomOut.setMinWidth(pxDp(railSize));zoomOut.setMaxWidth(pxDp(railSize));zoomOut.setPadding(0,0,0,0);zoomOut.setBackground(premiumPanel(0xff171f2b,0xff0c131d,0xff475669,15));LinearLayout.LayoutParams zoomOutParams=new LinearLayout.LayoutParams(pxDp(railSize),pxDp(railSize));zoomOutParams.setMargins(0,pxDp(railGap),0,pxDp(railGap));actionRail.addView(zoomOut,zoomOutParams);zoomOut.setAlpha(0.9f);zoomOut.setVisibility(compactControls?View.GONE:View.VISIBLE);
  TextView recenter=button("⌖",()->{follow=true;if(map!=null&&mapReady)map.evaluateJavascript("resumeCamera()",null);pushPosition();});recenter.setTextSize(21);recenter.setContentDescription("Recentrer le GPS ; appui long pour basculer jour et nuit");recenter.setMinHeight(pxDp(railSize));recenter.setMinWidth(pxDp(railSize));recenter.setMaxWidth(pxDp(railSize));recenter.setPadding(0,0,0,0);recenter.setBackground(accentPanel(23,true));recenter.setTextColor(accentText());LinearLayout.LayoutParams recenterParams=new LinearLayout.LayoutParams(pxDp(railSize),pxDp(railSize));recenterParams.setMargins(0,pxDp(railGap),0,pxDp(railGap));actionRail.addView(recenter,recenterParams);recenter.setOnLongClickListener(v->{nightMap=!nightMap;getPreferences(0).edit().putBoolean("night_map",nightMap).putBoolean("theme_auto",false).apply();if(community!=null)community.markPreferencesChanged();applyMapTheme();Toast.makeText(this,nightMap?"Carte de nuit":"Carte de jour",Toast.LENGTH_SHORT).show();return true;});
  FrameLayout.LayoutParams railp=new FrameLayout.LayoutParams(-2,-2,Gravity.END|Gravity.CENTER_VERTICAL);railp.setMargins(0,0,pxDp(narrowScreen?8:12),pxDp(155));frame.addView(actionRail,railp);
  if(immersiveDrive){TextView exit=button("≡  MENU",()->{immersiveDrive=false;getPreferences(0).edit().putBoolean("immersive_drive",false).apply();render();});exit.setTextSize(12);FrameLayout.LayoutParams ep=new FrameLayout.LayoutParams(-2,-2,Gravity.BOTTOM|Gravity.END);ep.setMargins(0,0,14,142);frame.addView(exit,ep);TextView projectProgress=button("▰ %",()->showProjectDashboard());projectProgress.setTextSize(12);projectProgress.setContentDescription("Afficher les barres d’avancement ROADGUARD même en plein écran");FrameLayout.LayoutParams pp=new FrameLayout.LayoutParams(-2,-2,Gravity.BOTTOM|Gravity.END);pp.setMargins(0,0,14,196);frame.addView(projectProgress,pp);}
  // Waze-inspired bottom sheet: compact during driving, expandable by tapping or swiping.
  if(navigationPoints.size()>1)installGuidanceFooter();else installDriveSheet();
  updateManeuverCard();scheduleMapOverlayLayout();startGPS();}
  void installGuidanceFooter(){
    LinearLayout summary=row();guidanceFooter=summary;summary.setGravity(Gravity.CENTER_VERTICAL);summary.setPadding(pxDp(13),pxDp(8),pxDp(13),pxDp(8));
    summary.setBackground(premiumPanel(0xf20b1723,0xf2071019,0xff38566a,12));summary.setElevation(pxDp(9));
    TextView walking=label("♙",18);walking.setTextColor(0xffffffff);summary.addView(walking);
    TextView minutes=label(Math.max(1,Math.round(navigationSeconds/60))+" min",13);tripMinutes=minutes;minutes.setTypeface(null,Typeface.BOLD);minutes.setGravity(Gravity.CENTER);summary.addView(minutes,new LinearLayout.LayoutParams(0,-2,1));
    tripSummary=label(String.format(Locale.FRANCE,"%.1f km",navigationMeters/1000),13);tripSummary.setTypeface(null,Typeface.BOLD);tripSummary.setGravity(Gravity.CENTER);summary.addView(tripSummary,new LinearLayout.LayoutParams(0,-2,1));
    TextView arrival=label(new java.text.SimpleDateFormat("HH:mm",Locale.FRANCE).format(new Date(System.currentTimeMillis()+(long)(navigationSeconds*1000))),13);tripArrival=arrival;arrival.setTypeface(null,Typeface.BOLD);arrival.setGravity(Gravity.CENTER);summary.addView(arrival,new LinearLayout.LayoutParams(0,-2,1));
    TextView stop=button(getResources().getConfiguration().fontScale>1.25f?"✕":"✕ Arrêter",()->confirmStopGuidance());stop.setTextSize(12);stop.setTypeface(null,Typeface.BOLD);stop.setTextColor(0xffff788d);stop.setMinHeight(pxDp(44));stop.setContentDescription("Arrêter le trajet en cours");stop.setPadding(pxDp(4),pxDp(4),pxDp(4),pxDp(4));stop.setBackground(premiumPanel(0xff38212d,0xff25131e,0xffe94263,12));summary.addView(stop,new LinearLayout.LayoutParams(pxDp(getResources().getConfiguration().fontScale>1.25f?48:89),-2));
    FrameLayout.LayoutParams fp=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM);fp.setMargins(pxDp(11),0,pxDp(11),pxDp(9));frame.addView(summary,fp);summary.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{guidanceFooterHeightDp=Math.max(0,Math.round((b-t)/getResources().getDisplayMetrics().density))+9;if(map!=null&&mapReady)publishMapBottomOcclusion(guidanceFooterHeightDp);scheduleMapOverlayLayout();});
  }
  int pxDp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
  static final class DragHandleView extends TextView {DragHandleView(Context context){super(context);}@Override public boolean performClick(){return super.performClick();}}
  void installDriveSheet(){
    driveSearchDestinationKey=getPreferences(0).getString("destination","");
    driveSheet=column();driveSheet.setPadding(pxDp(14),pxDp(0),pxDp(14),pxDp(5));
    driveSheet.setBackground(premiumPanel(0xf716202d,0xfd080d15,0xff586779,27));driveSheet.setElevation(22);
    driveSheetHandle=new DragHandleView(this);driveSheetHandle.setText("⌃");driveSheetHandle.setContentDescription("Rechercher un trajet, toucher pour agrandir ou réduire le panneau");driveSheetHandle.setGravity(Gravity.CENTER);
    driveSheetHandle.setTextColor(accentBorder());driveSheetHandle.setTypeface(null,Typeface.BOLD);driveSheetHandle.setTextSize(12);driveSheetHandle.setPadding(pxDp(8),pxDp(4),pxDp(8),pxDp(3));driveSheetHandle.setMinHeight(pxDp(44));driveSheetHandle.setFocusable(true);add(driveSheet,driveSheetHandle);
    driveSheetHandle.setOnClickListener(v->{driveSheetUserOverride=true;setDriveSheetLevel(driveSheetLevel==2?0:2);});
    driveSheetHandle.setOnTouchListener(new View.OnTouchListener(){float startY;@Override public boolean onTouch(View v,android.view.MotionEvent e){
      if(e.getActionMasked()==android.view.MotionEvent.ACTION_DOWN){startY=e.getRawY();return true;}
      if(e.getActionMasked()==android.view.MotionEvent.ACTION_UP){float delta=e.getRawY()-startY;driveSheetUserOverride=true;
        if(Math.abs(delta)>pxDp(28))setDriveSheetLevel(delta<0?2:0);
        else {v.performClick();}return true;}
      return e.getActionMasked()==android.view.MotionEvent.ACTION_MOVE;
    }});
    LinearLayout searchRow=row();searchRow.setGravity(Gravity.CENTER_VERTICAL);searchRow.setPadding(pxDp(12),0,pxDp(6),0);searchRow.setMinimumHeight(pxDp(52));
    searchRow.setBackground(premiumPanel(0xff202a36,0xff111821,0xff798797,28));
    TextView searchGlyph=label("⌕",22);searchGlyph.setGravity(Gravity.CENTER);searchGlyph.setTextColor(0xffe6edf5);searchRow.addView(searchGlyph,new LinearLayout.LayoutParams(pxDp(34),-1));
    destinationSearchInput=new EditText(this);destinationSearchInput.setSingleLine(true);destinationSearchInput.setTextSize(16);destinationSearchInput.setTextColor(0xfff4f6fa);destinationSearchInput.setHintTextColor(0xffb8c0cc);destinationSearchInput.setHint("Où va-t-on ?");destinationSearchInput.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);destinationSearchInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);destinationSearchInput.setBackgroundColor(android.graphics.Color.TRANSPARENT);destinationSearchInput.setPadding(0,0,pxDp(4),0);
    String savedLabel=getPreferences(0).getString("destination_label","");if(!savedLabel.isEmpty())destinationSearchInput.setText(savedLabel);
    searchRow.addView(destinationSearchInput,new LinearLayout.LayoutParams(0,-1,1));
    TextView searchAction=button("➜",()->searchDestinationFromBar());searchAction.setTextSize(19);searchAction.setMinWidth(pxDp(44));searchAction.setMinHeight(pxDp(44));searchAction.setPadding(0,0,0,0);searchAction.setContentDescription("Rechercher cette destination sur la carte");searchAction.setBackground(accentPanel(18,false));searchRow.addView(searchAction,new LinearLayout.LayoutParams(pxDp(44),pxDp(44)));
    add(driveSheet,searchRow);
    destinationSearchInput.setOnEditorActionListener((view,action,event)->{if(action==android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH||action==android.view.inputmethod.EditorInfo.IME_ACTION_DONE||(event!=null&&event.getKeyCode()==android.view.KeyEvent.KEYCODE_ENTER)){searchDestinationFromBar();return true;}return false;});
    destinationSearchInput.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){}public void afterTextChanged(android.text.Editable value){if(driveSearchSuppressWatcher)return;String query=value.toString().trim();final int token=nextDriveSearchToken();if(driveSearchRunnable!=null)driveSearchHandler.removeCallbacks(driveSearchRunnable);if(query.length()<3){if(driveSearchResults!=null){driveSearchResults.removeAllViews();driveSearchResults.setVisibility(View.GONE);}return;}if(driveSearchResults!=null){driveSearchResults.removeAllViews();TextView hint=label("Recherche automatique…",12);hint.setTextColor(0xffd6deea);hint.setPadding(pxDp(8),pxDp(5),pxDp(8),pxDp(5));add(driveSearchResults,hint);driveSearchResults.setVisibility(View.VISIBLE);refreshDriveSheetOcclusion();}driveSearchRunnable=()->{if(token==driveSearchRequestToken&&destinationSearchInput!=null&&query.equals(destinationSearchInput.getText().toString().trim()))runDriveSearch(query,false,token);};driveSearchHandler.postDelayed(driveSearchRunnable,750);}});
    driveSearchResults=column();driveSearchResults.setPadding(pxDp(8),pxDp(5),pxDp(8),pxDp(6));driveSearchResults.setBackground(premiumPanel(0xff192432,0xff111821,0xff495a6d,18));driveSearchResults.setVisibility(View.GONE);
    android.widget.ScrollView suggestions=new android.widget.ScrollView(this){@Override protected void onMeasure(int w,int h){int available=frame!=null&&frame.getHeight()>0?frame.getHeight():getResources().getDisplayMetrics().heightPixels;int limit=Math.min(pxDp(210),Math.max(pxDp(44),available/3));super.onMeasure(w,View.MeasureSpec.makeMeasureSpec(limit,View.MeasureSpec.AT_MOST));}};suggestions.setFillViewport(false);suggestions.addView(driveSearchResults);add(driveSheet,suggestions);
    LinearLayout shortcuts=row();shortcuts.setPadding(0,pxDp(4),0,pxDp(2));
    for(String name:new String[]{"⌂  Domicile","▣  Travail","♥  Favoris"}){
      TextView b=button(name,()->{if(name.contains("Domicile"))openNamedFavorite("Domicile");else if(name.contains("Travail"))openNamedFavorite("Travail");else showDestinationFavorites();});if(!name.contains("Favoris"))b.setOnLongClickListener(v->{openNamedFavorite(name.contains("Domicile")?"Domicile":"Travail",true);return true;});b.setTextSize(13);b.setMinHeight(pxDp(46));b.setBackground(outlined(0xff1c222a,0xff4b5664,16));
      LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMargins(3,0,3,0);shortcuts.addView(b,lp);
    }
    add(driveSheet,shortcuts);
    // 11.14.83: no redundant action grid; side rail and bottom tabs remain available.
    FrameLayout.LayoutParams sheetParams=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM);
    sheetParams.setMargins(0,0,0,0);frame.addView(driveSheet,sheetParams);
    driveSheet.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->scheduleMapOverlayLayout());
    setDriveSheetLevel(restoredDriveSheetLevel>=0?restoredDriveSheetLevel:0);restoredDriveSheetLevel=-1;
    if(restoredSearchQuery!=null){driveSearchSuppressWatcher=true;destinationSearchInput.setText(restoredSearchQuery);destinationSearchInput.setSelection(destinationSearchInput.length());driveSearchSuppressWatcher=false;restoredSearchQuery=null;}
  }
  void refreshDriveSheetOcclusion(){if(driveSheet!=null)setDriveSheetLevel(driveSheetLevel);}
  void showDriveSearchStatus(String message){if(driveSearchResults==null)return;driveSearchResults.removeAllViews();TextView status=label(message,13);status.setTextColor(0xffd6deea);status.setPadding(pxDp(8),pxDp(6),pxDp(8),pxDp(6));add(driveSearchResults,status);driveSearchResults.setVisibility(View.VISIBLE);refreshDriveSheetOcclusion();}
  void searchDestinationFromBar(){if(destinationSearchInput==null)return;String query=destinationSearchInput.getText().toString().trim();if(query.length()<2){showDriveSearchStatus("Saisis une adresse, une ville ou des coordonnées.");return;}Object service=getSystemService(android.content.Context.INPUT_METHOD_SERVICE);if(service instanceof android.view.inputmethod.InputMethodManager)((android.view.inputmethod.InputMethodManager)service).hideSoftInputFromWindow(destinationSearchInput.getWindowToken(),0);int token=nextDriveSearchToken();if(driveSearchRunnable!=null)driveSearchHandler.removeCallbacks(driveSearchRunnable);runDriveSearch(query,true,token);}
  void rememberRecentDestination(double lat,double lon,String label){
    if(!RouteRequestPolicy.coordinate(lat,lon))return;
    try{JSONArray recent=new JSONArray();recent.put(new JSONObject().put("lat",lat).put("lon",lon).put("name",label==null?"Destination":label.substring(0,Math.min(180,label.length()))));
      JSONArray old=new JSONArray(getPreferences(0).getString("recent_destinations","[]"));for(int i=0;i<old.length()&&recent.length()<12;i++){JSONObject item=old.optJSONObject(i);if(item!=null&&RouteRequestPolicy.coordinate(item.optDouble("lat",Double.NaN),item.optDouble("lon",Double.NaN))&&geoMeters(lat,lon,item.optDouble("lat"),item.optDouble("lon"))>15)recent.put(item);}
      getPreferences(0).edit().putString("recent_destinations",recent.toString()).apply();
    }catch(Exception ignored){}
  }
  java.util.List<Address> localDestinationMatches(String query){
    ArrayList<Address> results=new ArrayList<>();String wanted=normalizeDriveSearchText(query);if(wanted.isEmpty())return results;
    JSONArray recent;try{recent=new JSONArray(getPreferences(0).getString("recent_destinations","[]"));}catch(Exception ignored){recent=new JSONArray();}
    for(JSONArray list:new JSONArray[]{destinationFavorites(),recent})for(int i=0;i<list.length()&&results.size()<5;i++){
      JSONObject item=list.optJSONObject(i);if(item==null)continue;double lat=item.optDouble("lat",Double.NaN),lon=item.optDouble("lon",Double.NaN);String name=item.optString("name","");
      if(!RouteRequestPolicy.coordinate(lat,lon)||!normalizeDriveSearchText(name).contains(wanted))continue;
      boolean duplicate=false;for(Address existing:results)if(geoMeters(lat,lon,existing.getLatitude(),existing.getLongitude())<15){duplicate=true;break;}if(duplicate)continue;
      Address address=new Address(Locale.FRANCE);address.setLatitude(lat);address.setLongitude(lon);address.setAddressLine(0,name);results.add(address);
    }return results;
  }
  void runDriveSearch(String query,boolean manual,int token){
    if(token!=driveSearchRequestToken)return;
    double[] coordinates=DestinationCoordinates.parse(query);
    if(coordinates!=null){ArrayList<Address> direct=new ArrayList<>();Address item=new Address(Locale.FRANCE);item.setLatitude(coordinates[0]);item.setLongitude(coordinates[1]);item.setAddressLine(0,String.format(Locale.FRANCE,"Coordonnées · %.5f, %.5f",coordinates[0],coordinates[1]));direct.add(item);showDriveSearchAddresses(direct);return;}
    final java.util.List<Address> local=localDestinationMatches(query);
    if(!local.isEmpty())showDriveSearchAddresses(local,"Favoris et destinations récentes");
    if(!mapInternetAvailable()){if(local.isEmpty())showDriveSearchStatus("Sans Internet : cherche un favori, une destination récente ou saisis des coordonnées.");return;}
    if(local.isEmpty())showDriveSearchStatus("Recherche de « "+query+" »…");
    try{destinationSearchExecutor.execute(()->{
      if(token!=driveSearchRequestToken)return;
      try{
        java.util.List<Address> response=null;boolean fromIgn=false;
        try{response=fetchFrenchAutocomplete(query,token);fromIgn=response!=null&&!response.isEmpty();}catch(Exception unavailable){if(token!=driveSearchRequestToken)return;}
        if(token!=driveSearchRequestToken)return;
        if(response==null||response.isEmpty())try{response=fetchFrenchSearch(query,token);fromIgn=response!=null&&!response.isEmpty();}catch(Exception unavailable){if(token!=driveSearchRequestToken)return;}
        if(token!=driveSearchRequestToken)return;
        if((response==null||response.isEmpty())&&local.isEmpty()&&manual&&Geocoder.isPresent())response=new Geocoder(this,Locale.FRANCE).getFromLocationName(query,5);
        final java.util.List<Address> found=combineDriveSearchAddresses(local,rankDriveSearchAddresses(response,query));
        final String source=local.isEmpty()?(fromIgn?"Source : IGN Géoplateforme":"Source : géocodage Android"):"Favoris, destinations récentes et résultats en ligne";
        runOnUiThread(()->{if(isFinishing()||isDestroyed()||driveSearchResults==null||token!=driveSearchRequestToken||destinationSearchInput==null||!query.equals(destinationSearchInput.getText().toString().trim()))return;
          if(found.isEmpty())showDriveSearchStatus("Aucun résultat pertinent pour « "+query+" ». Essaie avec le code postal ou la commune.");else showDriveSearchAddresses(found,source);
        });
      }catch(Exception failed){runOnUiThread(()->{if(!isFinishing()&&!isDestroyed()&&token==driveSearchRequestToken&&local.isEmpty())showDriveSearchStatus("Recherche indisponible. Vérifie la connexion ou saisis les coordonnées.");});}
    });}catch(java.util.concurrent.RejectedExecutionException stopped){}
  }
  java.util.List<Address> combineDriveSearchAddresses(java.util.List<Address> local,java.util.List<Address> remote){
    ArrayList<Address> result=new ArrayList<>();
    for(java.util.List<Address> source:java.util.Arrays.asList(local,remote))if(source!=null)for(Address candidate:source){
      if(result.size()>=5)break;boolean duplicate=false;
      for(Address existing:result)if(geoMeters(existing.getLatitude(),existing.getLongitude(),candidate.getLatitude(),candidate.getLongitude())<15){duplicate=true;break;}
      if(!duplicate)result.add(candidate);
    }
    return result;
  }
  java.util.List<Address> fetchFrenchAutocomplete(String query,int token)throws Exception{StringBuilder address=new StringBuilder("https://data.geopf.fr/geocodage/completion/?text=").append(java.net.URLEncoder.encode(query,"UTF-8")).append("&terr=METROPOLE&maximumResponses=5");if(Double.isFinite(lastLat)&&Double.isFinite(lastLon)&&Math.abs(lastLat)<=90&&Math.abs(lastLon)<=180)address.append("&lonlat=").append(java.net.URLEncoder.encode(String.format(Locale.US,"%.6f,%.6f",lastLon,lastLat),"UTF-8"));String response=destinationSearchHttp.read(address.toString(),token,262144);JSONObject root=new JSONObject(response);if(!"OK".equalsIgnoreCase(root.optString("status","")))return new ArrayList<>();JSONArray results=root.optJSONArray("results");ArrayList<Address> found=new ArrayList<>();if(results==null)return found;for(int i=0;i<results.length()&&found.size()<5;i++){JSONObject item=results.optJSONObject(i);if(item==null)continue;double lon=item.optDouble("x",Double.NaN),lat=item.optDouble("y",Double.NaN);String label=item.optString("fulltext","").trim();if(label.isEmpty()||!Double.isFinite(lat)||!Double.isFinite(lon)||Math.abs(lat)>90||Math.abs(lon)>180)continue;Address result=new Address(Locale.FRANCE);result.setLatitude(lat);result.setLongitude(lon);result.setAddressLine(0,label);if(!item.optString("city","").isEmpty())result.setLocality(item.optString("city"));if(!item.optString("zipcode","").isEmpty())result.setPostalCode(item.optString("zipcode"));found.add(result);}return found;}
  java.util.List<Address> fetchFrenchSearch(String query,int token)throws Exception{
    String url="https://data.geopf.fr/geocodage/search/?q="+java.net.URLEncoder.encode(query,"UTF-8")+"&limit=5";
    JSONArray features=new JSONObject(destinationSearchHttp.read(url,token,262144)).optJSONArray("features");ArrayList<Address> found=new ArrayList<>();if(features==null)return found;
      for(int i=0;i<features.length()&&found.size()<5;i++){JSONObject feature=features.optJSONObject(i);if(feature==null)continue;JSONObject props=feature.optJSONObject("properties"),geometry=feature.optJSONObject("geometry");JSONArray coordinates=geometry==null?null:geometry.optJSONArray("coordinates");if(props==null||coordinates==null||coordinates.length()<2)continue;double lon=coordinates.optDouble(0,Double.NaN),lat=coordinates.optDouble(1,Double.NaN);if(!RouteRequestPolicy.coordinate(lat,lon))continue;Address item=new Address(Locale.FRANCE);item.setLatitude(lat);item.setLongitude(lon);item.setAddressLine(0,props.optString("label",props.optString("name","")));item.setLocality(props.optString("city",""));found.add(item);}return found;
  }
  java.util.List<Address> rankDriveSearchAddresses(java.util.List<Address> input,String query){ArrayList<Address> ranked=new ArrayList<>();if(input==null)return ranked;String wanted=normalizeDriveSearchText(query);String[] words=wanted.split("\\s+");java.util.HashMap<Address,Integer> scores=new java.util.HashMap<>();for(Address a:input){if(a==null)continue;StringBuilder text=new StringBuilder();for(int i=0;i<=a.getMaxAddressLineIndex();i++){String line=a.getAddressLine(i);if(line!=null)text.append(' ').append(line);}if(a.getFeatureName()!=null)text.append(' ').append(a.getFeatureName());if(a.getLocality()!=null)text.append(' ').append(a.getLocality());if(a.getSubAdminArea()!=null)text.append(' ').append(a.getSubAdminArea());if(a.getAdminArea()!=null)text.append(' ').append(a.getAdminArea());String candidate=normalizeDriveSearchText(text.toString());int score=candidate.contains(wanted)?1000:0;for(String word:words)if(word.length()>2)score+=candidate.contains(word)?100:-250;if(score>0){scores.put(a,score);ranked.add(a);}}java.util.Collections.sort(ranked,(a,b)->{int diff=scores.get(b)-scores.get(a);if(diff!=0)return diff;if(Double.isFinite(lastLat)&&Double.isFinite(lastLon)){float[] da=new float[1],db=new float[1];Location.distanceBetween(lastLat,lastLon,a.getLatitude(),a.getLongitude(),da);Location.distanceBetween(lastLat,lastLon,b.getLatitude(),b.getLongitude(),db);return Float.compare(da[0],db[0]);}return 0;});if(ranked.size()>5)return new ArrayList<>(ranked.subList(0,5));return ranked;}
  String normalizeDriveSearchText(String text){String normalized=java.text.Normalizer.normalize(text==null?"":text.toLowerCase(Locale.ROOT),java.text.Normalizer.Form.NFD).replaceAll("\\p{M}+","");return normalized.replaceAll("[^a-z0-9]+"," ").trim();}
  void showDriveSearchAddresses(java.util.List<Address> found){showDriveSearchAddresses(found,"Coordonnées fournies");}
  void showDriveSearchAddresses(java.util.List<Address> found,boolean fromIgn){showDriveSearchAddresses(found,fromIgn?"Suggestions · Géoplateforme IGN":"Suggestions · service du téléphone");}
  void showDriveSearchAddresses(java.util.List<Address> found,String sourceLabel){if(driveSearchResults==null)return;driveSearchResults.removeAllViews();TextView title=label("CHOISIR UNE DESTINATION",10);title.setTypeface(null,Typeface.BOLD);title.setTextColor(0xffff7180);title.setPadding(pxDp(8),pxDp(4),pxDp(8),pxDp(5));add(driveSearchResults,title);int count=Math.min(found.size(),5);for(int i=0;i<count;i++){final Address a=found.get(i);String text=a.getAddressLine(0);if(text==null||text.trim().isEmpty())text=String.format(Locale.FRANCE,"%.5f, %.5f",a.getLatitude(),a.getLongitude());final String resultLabel=text;TextView result=button("⌖  "+resultLabel,()->selectDriveSearchResult(a,resultLabel));result.setTextSize(13);result.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);result.setMaxLines(2);result.setMinHeight(pxDp(42));result.setBackground(outlined(0xff1d2a39,0xff405267,14));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,pxDp(2),0,pxDp(2));driveSearchResults.addView(result,lp);}TextView source=label(sourceLabel,9);source.setTextColor(0xffaeb9c8);source.setPadding(pxDp(8),pxDp(3),pxDp(8),pxDp(2));add(driveSearchResults,source);driveSearchResults.setVisibility(View.VISIBLE);refreshDriveSheetOcclusion();}
  void selectDriveSearchResult(Address address,String label){if(destinationSearchDialog!=null)destinationSearchDialog.dismiss();double lat=address.getLatitude(),lon=address.getLongitude();if(!Double.isFinite(lat)||!Double.isFinite(lon)||Math.abs(lat)>90||Math.abs(lon)>180){Toast.makeText(this,"Coordonnées invalides",Toast.LENGTH_LONG).show();return;}nextDriveSearchToken();if(driveSearchRunnable!=null)driveSearchHandler.removeCallbacks(driveSearchRunnable);rememberRecentDestination(lat,lon,label);destinationLat=lat;destinationLon=lon;clearNavigation();selected="Carte";getPreferences(0).edit().putString("destination",String.format(Locale.US,"%.7f,%.7f",lat,lon)).putString("destination_label",label).apply();if(destinationSearchInput!=null){driveSearchSuppressWatcher=true;destinationSearchInput.setText(label);driveSearchSuppressWatcher=false;}if(driveSearchResults!=null){driveSearchResults.removeAllViews();driveSearchResults.setVisibility(View.GONE);}pushDestination();render();Toast.makeText(this,"Destination placée sur la carte",Toast.LENGTH_SHORT).show();if(Double.isFinite(lastLat)&&Double.isFinite(lastLon)){calculateRoadRoute();}else{Toast.makeText(this,"Destination enregistrée. Attends le GPS pour calculer le trajet.",Toast.LENGTH_LONG).show();}refreshDriveSheetOcclusion();}
  // Three positions: closed, quick destination, and full shortcuts.
  void setDriveSheetExpanded(boolean expanded){setDriveSheetLevel(expanded?2:0);}
  void setDriveSheetLevel(int level){
    driveSheetLevel=Math.max(0,Math.min(2,level));
    driveSheetExpanded=driveSheetLevel==2;
    if(driveSheet==null)return;
    for(int i=1;i<driveSheet.getChildCount();i++){
      // Search input and active results stay on the map; favorite shortcuts expand on demand.
      if(i==1)driveSheet.getChildAt(i).setVisibility(View.VISIBLE);
      else if(i==2)driveSheet.getChildAt(i).setVisibility(driveSearchResults!=null&&driveSearchResults.getChildCount()>0?View.VISIBLE:View.GONE);
      else driveSheet.getChildAt(i).setVisibility(driveSheetLevel==2?View.VISIBLE:View.GONE);
    }
    if(driveSheetHandle!=null){
      String title=driveSheetLevel==0?"⌃":"⌄";
      driveSheetHandle.setText(title);driveSheetHandle.setMinHeight(pxDp(44));driveSheetHandle.setTextSize(driveSheetLevel==0?10:12);
      driveSheetHandle.setContentDescription(driveSheetLevel==0?"Afficher les raccourcis de destination":"Refermer le panneau GPS");
    }
    scheduleMapOverlayLayout();
  }
  void publishMapBottomOcclusion(int covered){
    if(map==null||!mapReady||lastMapSheetOcclusionDp==covered)return;lastMapSheetOcclusionDp=covered;
    map.evaluateJavascript("if(typeof rgSetSheetOcclusion==='function')rgSetSheetOcclusion("+covered+")",null);
  }
  void scheduleMapOverlayLayout(){
    if(frame==null||map==null||mapOverlayLayoutScheduled)return;
    mapOverlayLayoutScheduled=true;final FrameLayout owner=frame;
    owner.post(()->{mapOverlayLayoutScheduled=false;if(frame==owner&&map!=null)layoutMapOverlays();});
  }
  void layoutMapOverlays(){
    if(frame==null||mapTopScroll==null||speedHudFrame==null)return;
    int width=frame.getWidth(),height=frame.getHeight();if(width<=0||height<=0)return;
    boolean wide=width>=pxDp(500)&&width>height;
    int topWidth=wide?Math.min(pxDp(360),Math.round(width*.46f)):width-pxDp(20);
    FrameLayout.LayoutParams topLp=(FrameLayout.LayoutParams)mapTopScroll.getLayoutParams();
    if(topLp.width!=topWidth){topLp.width=topWidth;mapTopScroll.setLayoutParams(topLp);}
    View panel=driveSheet!=null?driveSheet:guidanceFooter;
    if(panel!=null){
      FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams)panel.getLayoutParams();int margin=panel==driveSheet?0:pxDp(11);
      int target=wide?Math.min(pxDp(380),width/2):width-margin*2;
      int gravity=Gravity.BOTTOM|(wide?Gravity.END:Gravity.START);
      if(lp.width!=target||lp.gravity!=gravity){lp.width=target;lp.gravity=gravity;panel.setLayoutParams(lp);}
    }
    int footer=panel==null?0:panel.getHeight()+(panel==driveSheet?0:pxDp(9));
    int topLimit=Math.max(pxDp(44),height-pxDp(20)-(wide?0:footer));
    if(mapTopHeightLimit!=topLimit){mapTopHeightLimit=topLimit;mapTopScroll.requestLayout();}
    if(mapActionRail!=null){
      int room=height-footer-pxDp(20)-(wide?0:mapTopScroll.getHeight());boolean zoom=!compactControls&&room>=pxDp(176);
      for(int i=0;i<Math.min(2,mapActionRail.getChildCount());i++){View child=mapActionRail.getChildAt(i);int visibility=zoom?View.VISIBLE:View.GONE;if(child.getVisibility()!=visibility)child.setVisibility(visibility);}
    }
    MapOverlayPlacement.Box topBox=new MapOverlayPlacement.Box(mapTopScroll.getLeft(),mapTopScroll.getTop(),topWidth,mapTopScroll.getHeight());
    MapOverlayPlacement.Box panelBox=panel==null?null:new MapOverlayPlacement.Box(wide?width-((FrameLayout.LayoutParams)panel.getLayoutParams()).width-pxDp(panel==driveSheet?0:11):pxDp(panel==driveSheet?0:11),height-footer,((FrameLayout.LayoutParams)panel.getLayoutParams()).width,footer);
    MapOverlayPlacement.Box railBox=null;
    if(mapActionRail!=null){
      int railWidth=Math.max(pxDp(50),mapActionRail.getMeasuredWidth()),railHeight=Math.max(pxDp(54),mapActionRail.getMeasuredHeight());
      railBox=MapOverlayPlacement.floating(width,height,railWidth,railHeight,pxDp(8),topBox,panelBox,null,true);
      int visibility=railBox==null?View.GONE:View.VISIBLE;if(mapActionRail.getVisibility()!=visibility)mapActionRail.setVisibility(visibility);
      if(railBox!=null){FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams)mapActionRail.getLayoutParams();int margin=width-railBox.right();
        if(lp.gravity!=(Gravity.TOP|Gravity.END)||lp.topMargin!=railBox.top||lp.rightMargin!=margin){lp.gravity=Gravity.TOP|Gravity.END;lp.topMargin=railBox.top;lp.rightMargin=margin;lp.bottomMargin=0;mapActionRail.setLayoutParams(lp);}}
    }
    int size=pxDp(66);MapOverlayPlacement.Box speedBox=MapOverlayPlacement.floating(width,height,size,size,pxDp(8),topBox,panelBox,railBox);
    boolean floating=speedBox!=null&&getResources().getConfiguration().fontScale<=1.3f;
    if(speedHudFrame.getVisibility()!=(floating?View.VISIBLE:View.GONE))speedHudFrame.setVisibility(floating?View.VISIBLE:View.GONE);
    if(inlineMapSpeed!=null){inlineMapSpeed.setText(gpsSpeedText()+" km/h");int visibility=floating?View.GONE:View.VISIBLE;if(inlineMapSpeed.getVisibility()!=visibility)inlineMapSpeed.setVisibility(visibility);}
    if(floating){FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams)speedHudFrame.getLayoutParams();int gravity=Gravity.TOP|Gravity.START;
      if(lp.gravity!=gravity||lp.leftMargin!=speedBox.left||lp.topMargin!=speedBox.top){lp.gravity=gravity;lp.leftMargin=speedBox.left;lp.topMargin=speedBox.top;lp.bottomMargin=0;speedHudFrame.setLayoutParams(lp);}}
    int covered=Math.max(0,Math.round(footer/getResources().getDisplayMetrics().density));
    if(mapReady)publishMapBottomOcclusion(covered);
    syncMapTopInset();
  }
  void sectionCard(LinearLayout parent,String title,String subtitle,String key,boolean defaultOpen,Runnable content){
    boolean open=getPreferences(0).getBoolean("ui_card_"+key,defaultOpen);
    TextView header=button((open?"⌄  ":"›  ")+title+(subtitle.isEmpty()?"":"  ·  "+subtitle),()->{
      getPreferences(0).edit().putBoolean("ui_card_"+key,!getPreferences(0).getBoolean("ui_card_"+key,defaultOpen)).apply();render();
    });
    header.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);
    header.setTextSize(16);header.setTypeface(null,Typeface.BOLD);
    header.setPadding(18,17,18,17);header.setBackground(outlined(0xff131e2c,0xff41556d,20));
    LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.setMargins(0,12,0,4);parent.addView(header,cp);
    if(open)content.run();
  }
  boolean franceMapPackPresent(){
    // Count a bundled or downloaded archive only when its PMTiles header is readable
    // and its declared geographic bounds cover mainland France and Corsica.
    File dir=new File(getFilesDir(),"pmtiles");
    File[] files=dir.listFiles((d,n)->n.endsWith(".pmtiles"));
    if(files!=null)for(File f:files){
      try{if(f.isFile()&&new PmtilesReader(f).isVector()&&pmtilesCoversFranceAndCorsica(f))return true;}
      catch(Exception ignored){}
    }
    try{
      String[] assets=getAssets().list("maps");
      if(assets!=null)for(String name:assets)if(name.equals("france.mbtiles")||name.equals("france.pmtiles"))return true;
    }catch(IOException ignored){}
    return false;
  }
  // 11.14.61: true premium home, navigable from the header home icon.
  // All tiles use existing application actions, without changing GPS or cache logic.
  // 11.14.71: visual-first welcome hero; existing navigation actions are preserved.
  void premiumHomeHero(LinearLayout parent){
    LinearLayout hero=column();hero.setPadding(pxDp(20),pxDp(20),pxDp(20),pxDp(19));
    hero.setBackground(premiumPanel(0xff2b3a4d,0xff121a25,0xff884456,25));hero.setElevation(pxDp(2));
    TextView eyebrow=label("ROADGUARD  /  COCKPIT PREMIUM",11);
    eyebrow.setTextColor(0xffff7181);eyebrow.setTypeface(null,Typeface.BOLD);add(hero,eyebrow);
    TextView title=label("VOTRE ROUTE. EN DIRECT.",23);title.setTypeface(null,Typeface.BOLD);
    title.setTextColor(0xffffffff);title.setPadding(0,12,0,5);add(hero,title);
    TextView subtitle=label("Carte, guidage et alertes réunis au même endroit.",13);
    subtitle.setTextColor(0xffc5d1e0);add(hero,subtitle);
    LinearLayout indicators=row();indicators.setPadding(0,15,0,0);
    boolean locationPermission=checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED||checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED;
    boolean recentHomeFix=false;
    try{if(locationPermission&&lm!=null){Location homeFix=lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);if(homeFix==null&&lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER))homeFix=lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);recentHomeFix=homeFix!=null&&System.currentTimeMillis()-homeFix.getTime()<=120000;}}catch(Exception ignored){}
    String gpsState=!locationPermission?"⌖ GPS À AUTORISER":recentHomeFix?"⌖ GPS RÉCENT":"⌖ GPS EN ATTENTE";
    TextView gpsBadge=label(gpsState,10);gpsBadge.setTextColor(!locationPermission?0xffffbdc4:recentHomeFix?0xff9ce4cf:0xffffd18a);
    gpsBadge.setSingleLine(true);gpsBadge.setEllipsize(android.text.TextUtils.TruncateAt.END);gpsBadge.setPadding(12,9,12,9);gpsBadge.setBackground(outlined(0xff14273c,0xff416486,13));
    gpsBadge.setContentDescription((!locationPermission?"Autorisation GPS requise":recentHomeFix?"Position GPS récente disponible":"GPS autorisé, en attente d’une position récente")+". Ouvrir la navigation.");gpsBadge.setClickable(true);gpsBadge.setFocusable(true);gpsBadge.setOnClickListener(v->{selected="Carte";render();});
    indicators.addView(gpsBadge);
    TextView localBadge=label("◎  ALERTES LOCALES",11);localBadge.setTextColor(0xffffb6c0);
    localBadge.setPadding(12,9,12,9);localBadge.setBackground(outlined(0xff34202d,0xff874253,13));
    LinearLayout.LayoutParams badgeLp=new LinearLayout.LayoutParams(-2,-2);badgeLp.setMargins(8,0,0,0);
    indicators.addView(localBadge,badgeLp);add(hero,indicators);
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,4,0,15);parent.addView(hero,lp);
  }
  void showPremiumHome(LinearLayout content){
    premiumHomeHero(content);
    premiumPrimaryAction(content,"⌖  OUVRIR LA CARTE",()->{selected="Carte";render();});
    premiumNavigationPreview(content);
    premiumGroup(content,"VOS RACCOURCIS","Accédez rapidement aux fonctions de conduite.");
    premiumAction(content,"➦","Préparer un itinéraire","Choisir une destination et lancer le guidage",()->{selected="Trajet";render();});
    premiumAction(content,"▰","Avancement ROADGUARD","Voir le suivi du projet et de ses écrans",()->showProjectDashboard());
    premiumAction(content,"◎","Radars et alertes","Voir les dangers locaux et les réglages d’alerte",()->{selected="Alertes";render();});
    premiumAction(content,"⚠","Signaler un danger","Ajouter un événement à partir de la position GPS",()->{selected="Signaler";render();});
    premiumAction(content,"▤","Cache cartographique","Voir le cache normal des tuiles consultées",()->{selected="Hors ligne";render();});
    premiumGroup(content,"PERSONNALISATION","Retrouver les options du cockpit premium.");
    premiumAction(content,"◐","Apparence","Mode jour/nuit, contraste et orientation",()->{selected="Apparence";render();});
    premiumAction(content,"⚙","Paramètres","Réglages avancés et sécurité",()->{selected="Paramètres";render();});
    add(content,statusPill("GPS ET CARTE : VÉRIFIER LES AUTORISATIONS SUR LE TÉLÉPHONE"));
    add(content,label("Les radars et signalements enregistrés ici restent locaux tant qu’aucun service de partage n’est connecté.",12));
  }
  // 11.14.64: accessible theme selectors, with persistent settings and immediate UI refresh.
  void premiumThemeChoice(LinearLayout parent,boolean night){
    boolean active=nightMap==night;
    LinearLayout item=row();item.setGravity(Gravity.CENTER_VERTICAL);item.setPadding(15,14,15,14);
    item.setMinimumHeight(pxDp(76));
    item.setElevation(active?pxDp(2):0);item.setBackground(premiumPanel(active?0xff542236:0xff253140,active?0xff281720:0xff141b25,active?0xffff6678:0xff48596c,20));
    TextView symbol=label(night?"☾":"☀",28);symbol.setTextColor(night?0xffa9c5ff:0xffffb96a);
    symbol.setGravity(Gravity.CENTER);symbol.setMinWidth(pxDp(46));item.addView(symbol);
    LinearLayout labels=column();TextView name=label(night?"Navigation de nuit":"Navigation de jour",16);
    name.setTypeface(null,Typeface.BOLD);name.setTextColor(0xfff7f8fa);add(labels,name);
    TextView detail=label(night?"Carte sombre, conduite nocturne":"Carte claire, conduite diurne",12);
    detail.setTextColor(0xffabb8c8);add(labels,detail);
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMargins(10,0,6,0);item.addView(labels,lp);
    TextView selectedMark=label(active?"✓ ACTIF":"CHOISIR",11);selectedMark.setTypeface(null,Typeface.BOLD);
    selectedMark.setTextColor(active?0xffff6473:0xffa7b4c2);item.addView(selectedMark);
    item.setContentDescription((night?"Mode nuit":"Mode jour")+(active?", sélectionné":", sélectionner"));
    item.setClickable(true);item.setFocusable(true);
    item.setOnClickListener(v->{if(nightMap!=night){nightMap=night;getPreferences(0).edit().putBoolean("night_map",nightMap).putBoolean("theme_auto",false).apply();render();}});
    LinearLayout.LayoutParams outer=new LinearLayout.LayoutParams(-1,-2);outer.setMargins(0,0,0,9);parent.addView(item,outer);
  }
  // 11.14.74: visual-only route and theme previews. No map engine or GPS changes.
  void premiumPalettePreview(LinearLayout parent){
    LinearLayout card=column();card.setPadding(16,15,16,16);
    card.setBackground(premiumPanel(0xff1c2b3d,0xff0c131d,0xff684052,22));
    TextView eyebrow=label("APERÇU DE LA PALETTE",11);eyebrow.setTextColor(0xffff6a79);eyebrow.setTypeface(null,Typeface.BOLD);premiumLabel(eyebrow,0.08f);add(card,eyebrow);
    LinearLayout swatches=row();swatches.setPadding(0,13,0,10);
    int[] colors={0xff080e17,0xff172334,0xff33465c,0xffe33b52,0xff68b7ff};
    String[] names={"NUIT","GRAPHITE","ROUTES","ALERTE","GPS"};
    for(int i=0;i<colors.length;i++){
      LinearLayout sample=column();sample.setGravity(Gravity.CENTER_HORIZONTAL);
      TextView swatch=label("",10);swatch.setMinHeight(pxDp(33));swatch.setBackground(outlined(colors[i],i==3?0xffff8291:0xff536579,12));
      add(sample,swatch);TextView name=label(names[i],9);name.setGravity(Gravity.CENTER);name.setTextColor(0xffb9c6d6);name.setPadding(0,7,0,0);add(sample,name);
      LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(0,-2,1);sp.setMargins(2,0,2,0);swatches.addView(sample,sp);
    }
    add(card,swatches);TextView hint=label("Rouge : actions et dangers   •   Bleu : position GPS",12);hint.setTextColor(0xffaebccc);add(card,hint);
    LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.setMargins(0,0,0,14);parent.addView(card,cp);
  }
  void premiumNavigationPreview(LinearLayout parent){
    LinearLayout panel=column();panel.setPadding(17,15,17,16);
    panel.setBackground(premiumPanel(0xff273647,0xff141b25,0xff704353,22));
    TextView eyebrow=label("REPÈRES DU COCKPIT",11);eyebrow.setTextColor(0xffff7484);eyebrow.setTypeface(null,Typeface.BOLD);add(panel,eyebrow);
    LinearLayout cells=row();cells.setPadding(0,12,0,0);
    String[] glyphs={"⌖","➦","⚠"};String[] captions={"POSITION","GUIDAGE","DANGER"};
    for(int i=0;i<glyphs.length;i++){
      LinearLayout cell=column();cell.setPadding(pxDp(9),pxDp(12),pxDp(9),pxDp(12));cell.setGravity(Gravity.CENTER);
      cell.setElevation(pxDp(1));cell.setBackground(premiumPanel(0xff253140,0xff141b25,i==2?0xff914454:0xff45586d,16));
      TextView icon=label(glyphs[i],25);icon.setGravity(Gravity.CENTER);icon.setTextColor(i==2?0xffff6578:0xffb2d6ff);add(cell,icon);
      TextView name=label(captions[i],10);name.setGravity(Gravity.CENTER);name.setTextColor(0xffd1dce9);name.setPadding(0,7,0,0);add(cell,name);
      LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMargins(3,0,3,0);cells.addView(cell,lp);
    }
    add(panel,cells);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,13);parent.addView(panel,lp);
  }
  String mapMarkerSizeLabel(){return mapMarkerSize==0?"Petite":mapMarkerSize==2?"Grande":"Normale";}
  void premiumSettingsSummary(LinearLayout parent){
    LinearLayout summary=column();summary.setPadding(pxDp(14),pxDp(12),pxDp(14),pxDp(12));
    summary.setBackground(premiumPanel(0xff263444,0xff151c26,0xff48596c,18));summary.setElevation(pxDp(1));
    TextView heading=label("CONFIGURATION ACTUELLE",10);heading.setTypeface(null,Typeface.BOLD);heading.setTextColor(0xffff7180);premiumLabel(heading,0.045f);add(summary,heading);
    LinearLayout first=row();first.setPadding(0,pxDp(8),0,0);
    premiumSettingChip(first,nightMap?"☾  NUIT":"☀  JOUR",true);
    premiumSettingChip(first,highContrastMap?"◉  CONTRASTE +":"◉  CONTRASTE STD",highContrastMap);
    add(summary,first);
    LinearLayout second=row();second.setPadding(0,pxDp(5),0,0);
    premiumSettingChip(second,immersiveDrive?"▣  PLEIN ÉCRAN":"▣  AFFICHAGE NORMAL",immersiveDrive);
    premiumSettingChip(second,headingUp?"↗  CAP SUIVI":"↑  NORD EN HAUT",headingUp);
    add(summary,second);
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,pxDp(3),0,pxDp(11));parent.addView(summary,lp);
  }
  void premiumSettingChip(LinearLayout parent,String text,boolean active){
    TextView chip=label(text,9);chip.setTypeface(null,Typeface.BOLD);chip.setTextColor(active?0xffffcad0:0xffc0cad6);chip.setGravity(Gravity.CENTER);chip.setSingleLine(true);chip.setEllipsize(android.text.TextUtils.TruncateAt.END);chip.setPadding(pxDp(7),pxDp(6),pxDp(7),pxDp(6));
    chip.setBackground(outlined(active?0xff38222e:0xff111a26,active?0xff9e485c:0xff35465b,11));
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMargins(pxDp(2),0,pxDp(4),0);parent.addView(chip,lp);
  }
  void showReferenceSection(){ReferenceUi.show(this);}
  void showSection(){if(selected.equals("Gestion radars")){RadarToolsUi.show(this);return;}if(!legacySection){showReferenceSection();return;}ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setVerticalScrollBarEnabled(false);scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);frame.addView(scroll,new FrameLayout.LayoutParams(-1,-1));LinearLayout content=column();content.setPadding(pxDp(12),pxDp(8),pxDp(12),pxDp(104));content.setBackgroundColor(0xff090f19);scroll.addView(content);String sectionTitle=selected.equals("Accueil")?"ACCUEIL":selected.equals("Hors ligne")?"CACHE CARTOGRAPHIQUE":selected.equals("Trajet")?"CALCUL D’ITINÉRAIRE":selected.equals("Alertes")?"RADARS ET ALERTES":selected.equals("Signaler")?"SIGNALER UN DANGER":selected.equals("Apparence")?"APPARENCE":selected.toUpperCase(Locale.FRANCE);sectionIntro(content,sectionTitle,"ROADGUARD  /  "+(selected.equals("Trajet")?"NAVIGATION":selected.equals("Alertes")?"SÉCURITÉ":selected.equals("Signaler")?"SIGNALEMENTS LOCAUX":selected.equals("Hors ligne")?"CARTOGRAPHIE":"PERSONNALISATION"));if(selected.equals("Accueil")){showPremiumHome(content);}else if(selected.equals("Paramètres")){
      premiumSettingsSummary(content);
      premiumGroup(content,"AFFICHAGE","Personnalisation de la navigation et du cockpit.");
      premiumAction(content,"◐","Apparence et thèmes","Jour, nuit, orientation et commandes",()->{selected="Apparence";render();});
      premiumAction(content,"▧","Pack de carte / textures","Graphite · Lisibilité · OSM, chacun en jour ou nuit",()->showMapPackPicker());
      premiumAction(content,"▣","Commandes de la carte","Afficher les outils avancés du GPS",()->{selected="Carte";compactControls=false;render();});
      premiumAction(content,"◉","Contraste des routes",highContrastMap?"Renforcé · toucher pour revenir au standard":"Standard · toucher pour renforcer",()->{highContrastMap=!highContrastMap;getPreferences(0).edit().putBoolean("high_contrast_map",highContrastMap).apply();render();});
      premiumGroup(content,"NAVIGATION","Accès rapide aux réglages sans perdre la carte.");
      premiumAction(content,"⌖","Mode conduite plein écran",immersiveDrive?"Activé · toucher pour désactiver":"Désactivé · toucher pour activer",()->{immersiveDrive=!immersiveDrive;getPreferences(0).edit().putBoolean("immersive_drive",immersiveDrive).apply();render();});
      premiumAction(content,"↗","Orientation du GPS",headingUp?"Suivre la route":"Nord en haut",()->{headingUp=!headingUp;getPreferences(0).edit().putBoolean("heading_up",headingUp).apply();render();});
      premiumGroup(content,"CARTOGRAPHIE","Cache temporaire des zones proches, sans packs complets.");
      premiumAction(content,"▤","Cache cartographique","Gérer le secours temporaire sans téléchargement complet",()->{selected="Hors ligne";render();});
      premiumGroup(content,"SÉCURITÉ","Accès aux alertes et signalements.");
      premiumAction(content,"◎","Radars et alertes","Consulter les radars et leurs réglages",()->{selected="Alertes";render();});
    }else if(selected.equals("Apparence")){
      premiumSettingsSummary(content);
      premiumPalettePreview(content);
      premiumGroup(content,"THÈME","Choisis directement le rendu de la carte.");
      premiumThemeChoice(content,false);
      premiumThemeChoice(content,true);
      premiumAction(content,"◉","Lisibilité des routes",highContrastMap?"Contraste renforcé":"Contraste standard",()->{highContrastMap=!highContrastMap;getPreferences(0).edit().putBoolean("high_contrast_map",highContrastMap).apply();render();});
      premiumAction(content,radarMarkersVisible?"◎":"⊘","Radars sur la carte",radarMarkersVisible?"Affichés · alertes vocales conservées":"Masqués · alertes vocales conservées",()->{radarMarkersVisible=!radarMarkersVisible;getPreferences(0).edit().putBoolean("radar_markers_visible",radarMarkersVisible).apply();if(map!=null&&mapReady)map.evaluateJavascript("if(typeof setRadarMarkersVisible===\'function\')setRadarMarkersVisible("+radarMarkersVisible+")",null);render();});
      premiumAction(content,"◉","Taille des icônes",mapMarkerSizeLabel()+" · radars et événements",()->{mapMarkerSize=(mapMarkerSize+1)%3;getPreferences(0).edit().putInt("map_marker_size",mapMarkerSize).apply();if(map!=null&&mapReady)map.evaluateJavascript("if(typeof setMapMarkerScale==='function')setMapMarkerScale("+mapMarkerSize+")",null);render();});
      premiumGroup(content,"COCKPIT GPS","Réglages de présentation, sans changer le moteur de navigation.");
      premiumGroup(content,"SUIVI GPS","Active ou désactive les aides de présentation pendant le suivi.");
      addMapPreferenceSwitch(content,"Zoom adaptatif","Ajuster le zoom à la vitesse",adaptiveZoom,()->{adaptiveZoom=!adaptiveZoom;getPreferences(0).edit().putBoolean("adaptive_zoom",adaptiveZoom).apply();pushPosition();render();});
      addMapPreferenceSwitch(content,"Anticipation du trajet","Décaler légèrement la carte devant le véhicule",lookAhead,()->{lookAhead=!lookAhead;getPreferences(0).edit().putBoolean("look_ahead",lookAhead).apply();pushPosition();render();});
      premiumAction(content,"⌖","Position du véhicule",getPreferences(0).getInt("camera_anchor_percent",50)+" % de la hauteur visible",()->{int current=getPreferences(0).getInt("camera_anchor_percent",50);int next=current<60?60:current<66?66:current<72?72:50;getPreferences(0).edit().putInt("camera_anchor_percent",next).apply();pushPosition();render();});
      addMapPreferenceSwitch(content,"Réduction des commandes en roulant","Réduire la recherche et les informations GPS au-dessus de 18 km/h",autoDeclutter,()->{autoDeclutter=!autoDeclutter;getPreferences(0).edit().putBoolean("auto_declutter",autoDeclutter).apply();updateDrivingDeclutter(lastSpeedKmh>0);render();});
      premiumAction(content,"▣","Plein écran",immersiveDrive?"Activé":"Désactivé",()->{immersiveDrive=!immersiveDrive;getPreferences(0).edit().putBoolean("immersive_drive",immersiveDrive).apply();render();});
      premiumAction(content,"↗","Orientation de la carte",headingUp?"Suivre la route":"Nord en haut",()->{headingUp=!headingUp;getPreferences(0).edit().putBoolean("heading_up",headingUp).apply();render();});
      premiumAction(content,"⌗","Commandes avancées","Afficher les outils supplémentaires sur la carte",()->{selected="Carte";compactControls=false;render();});
      premiumAction(content,"←","Retour aux paramètres","Revenir au menu de configuration",()->{selected="Paramètres";render();});
    }else if(selected.equals("Trajet")){showPremiumRoute(content);}else if(selected.equals("Hors ligne")){
      premiumGroup(content,"CACHE DE LA CARTE","Cache normal des tuiles demandées pendant l’affichage.");
      add(content,statusPill("CARTE EN LIGNE · AUCUN PRÉCHARGEMENT"));
      add(content,label("ROADGUARD laisse Android/WebView conserver les tuiles déjà consultées. Lors d’une coupure, seules celles encore présentes dans le cache peuvent réapparaître ; durée et couverture ne sont pas garanties.",14));
      premiumAction(content,"◎","Revenir à la carte","Affichage en ligne avec reprise automatique du réseau",()->{offlineView=false;mapLibrePreview=false;forceOnlineMapLibre=false;selected="Carte";render();});
      premiumGroup(content,"DONNÉES LOCALES","Conserver les favoris, les traces GPS et les signalements.");
      add(content,label(localDataSummary(),13));
      add(content,button("Exporter mes données ROADGUARD (JSON)",()->exportBackup()));
      add(content,button("Restaurer une sauvegarde ROADGUARD",()->importBackup()));
      add(content,label("Les anciens fichiers de cartes éventuellement présents ne sont pas supprimés automatiquement. Aucun nouveau téléchargement de carte complète n'est proposé.",12));
    }else if(selected.equals("Alertes")){
    removeExpiredRadars();
    premiumVisualBanner(content,"◎","VIGILANCE SUR LA ROUTE","Bases incluses · points personnels · alertes locales",0xffba3d50);
    add(content,statusPill("●  BASES EMBARQUÉES  ·  DISPONIBLES SANS TÉLÉCHARGEMENT"));
    renderBundledRadarSummary(content);
    renderRadarCountryDownloads(content);
    premiumGroup(content,"RADARS À PROXIMITÉ","Points français inclus et bases ajoutées. Les catégories mobiles et chantiers décrivent des lieux signalés, sans présence confirmée.");
    add(content,statusPill("●  SOCLE OFFICIEL · "+officialRadarCount+" ENTRÉES · DATA.GOUV.FR · 30/12/2025"));
    String radarFilter=getPreferences(0).getString("radar_ui_filter","all");
    // Short two-row category selector: labels remain readable on narrow phones.
    String[] radarFilterKeys={"all","fixed","mobile","tunnel","redlight","average","variable","danger","crossing","works"};
    String[] radarFilterLabels={"TOUS","FIXES","MOBILES","TUNNELS","FEUX","TRONÇONS","VARIABLES","ZONES","PN","CHANTIERS"};
    LinearLayout radarFilterGrid=column();
    for(int rr=0;rr<radarFilterKeys.length;rr+=3){
      LinearLayout radarTabs=row();
      for(int cc=0;cc<3&&rr+cc<radarFilterKeys.length;cc++){
        int ti=rr+cc;String key=radarFilterKeys[ti];
        TextView tab=button(radarFilterLabels[ti]+" · "+countActiveRadars(key),()->{getPreferences(0).edit().putString("radar_ui_filter",key).apply();render();});
        tab.setTextSize(10);tab.setTypeface(null,Typeface.BOLD);tab.setMinHeight(pxDp(44));tab.setTextColor(-1);tab.setSingleLine(true);tab.setEllipsize(android.text.TextUtils.TruncateAt.END);
        tab.setBackground(outlined(key.equals(radarFilter)?0xffcb3047:0xff152234,key.equals(radarFilter)?0xffff6678:0xff405770,15));
        LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(0,-2,1);tp.setMargins(2,2,2,4);radarTabs.addView(tab,tp);
      }
      radarFilterGrid.addView(radarTabs);
    }
    add(content,radarFilterGrid);
    // The category filters cover every loaded source; the list is limited to nearby points.
    java.util.ArrayList<JSONObject> visibleRadars=new java.util.ArrayList<>();
    try{
      boolean gpsAvailable=Double.isFinite(lastLat)&&Double.isFinite(lastLon);
      java.util.ArrayList<Integer> candidates=gpsAvailable?dangerSpatialIndex.query(lastLat,lastLon,50000):new java.util.ArrayList<>();
      if(!gpsAvailable){
        for(int di=0;di<dangers.size()&&visibleRadars.size()<8;di++){Danger point=dangers.get(di);if(!DangerTypePolicy.isRadar(point.type)||!radarMatchesFilter(point.type,radarFilter))continue;JSONObject item=new JSONObject();item.put("lat",point.lat);item.put("lon",point.lon);item.put("type",point.type);visibleRadars.add(item);}
      }else{
        for(int di:candidates){
          if(di<0||di>=dangers.size())continue;Danger point=dangers.get(di);
          if(!DangerTypePolicy.isRadar(point.type)||!radarMatchesFilter(point.type,radarFilter)||geoMeters(lastLat,lastLon,point.lat,point.lon)>50000)continue;
          JSONObject item=new JSONObject();item.put("lat",point.lat);item.put("lon",point.lon);item.put("type",point.type);visibleRadars.add(item);
        }
        java.util.Collections.sort(visibleRadars,(a,b)->Double.compare(
          geoMeters(lastLat,lastLon,a.optDouble("lat"),a.optDouble("lon")),
          geoMeters(lastLat,lastLon,b.optDouble("lat"),b.optDouble("lon"))));
      }
    }catch(Exception ignored){}
    LinearLayout radarSummary=row();radarSummary.setGravity(Gravity.CENTER_VERTICAL);radarSummary.setPadding(pxDp(14),pxDp(10),pxDp(14),pxDp(10));
    radarSummary.setBackground(premiumPanel(0xff202f41,0xff101721,0xff40566f,17));
    TextView count=label(String.valueOf(countActiveRadars(radarFilter)),24);count.setTypeface(null,Typeface.BOLD);count.setTextColor(0xffff7180);count.setGravity(Gravity.CENTER);count.setMinWidth(pxDp(42));radarSummary.addView(count);
    LinearLayout countCopy=column();TextView countTitle=label(countActiveRadars(radarFilter)==1?"RADAR DISPONIBLE":"RADARS DISPONIBLES",11);countTitle.setTypeface(null,Typeface.BOLD);countTitle.setTextColor(0xfff4f6fa);add(countCopy,countTitle);
    TextView countDetail=label("Points présents dans les bases chargées · liste locale limitée aux 50 km autour du GPS.",11);countDetail.setTextColor(0xffb8c5d4);countDetail.setPadding(0,pxDp(3),0,0);add(countCopy,countDetail);
    radarSummary.addView(countCopy,new LinearLayout.LayoutParams(0,-2,1));LinearLayout.LayoutParams radarSummaryLp=new LinearLayout.LayoutParams(-1,-2);radarSummaryLp.setMargins(0,pxDp(2),0,pxDp(9));content.addView(radarSummary,radarSummaryLp);
    if(visibleRadars.isEmpty()){
      LinearLayout empty=column();empty.setGravity(Gravity.CENTER);empty.setPadding(pxDp(18),pxDp(18),pxDp(18),pxDp(18));
      empty.setBackground(premiumPanel(0xff172638,0xff101721,0xff40566f,19));
      TextView emptyIcon=label("◎",30);emptyIcon.setTextColor(0xffa9c7e5);emptyIcon.setGravity(Gravity.CENTER);add(empty,emptyIcon);
      empty.setElevation(pxDp(1));
      TextView emptyTitle=label("Aucun radar de cette catégorie dans les bases chargées",15);emptyTitle.setTypeface(null,Typeface.BOLD);emptyTitle.setTextColor(0xfff2f5f9);emptyTitle.setGravity(Gravity.CENTER);emptyTitle.setPadding(0,pxDp(5),0,pxDp(4));add(empty,emptyTitle);
      TextView emptyDetail=label("Télécharge une base pour ce pays ou élargis le filtre. Les points téléchargés restent disponibles hors connexion.",12);emptyDetail.setTextColor(0xffaebdcd);emptyDetail.setGravity(Gravity.CENTER);add(empty,emptyDetail);
      LinearLayout.LayoutParams emptyLp=new LinearLayout.LayoutParams(-1,-2);emptyLp.setMargins(0,pxDp(4),0,pxDp(10));content.addView(empty,emptyLp);
    }else{
      premiumGroup(content,"RADARS À PROXIMITÉ",Double.isFinite(lastLat)&&Double.isFinite(lastLon)?"Classés du plus proche au plus éloigné, dans un rayon de 50 km":"Position indisponible · premiers points des bases chargées");
      int shown=Math.min(8,visibleRadars.size());
      for(int ri=0;ri<shown;ri++){
        JSONObject point=visibleRadars.get(ri);
        String type=point.optString("type","Radar");
        String detail="Base chargée · position GPS indisponible";
        if(Double.isFinite(lastLat)&&Double.isFinite(lastLon)){
          double meters=geoMeters(lastLat,lastLon,point.optDouble("lat"),point.optDouble("lon"));
          detail=meters<1000?String.format(Locale.FRANCE,"À %.0f m · base chargée",meters):String.format(Locale.FRANCE,"À %.1f km · base chargée",meters/1000);
        }
        final Danger selectedRadar=new Danger(point.optDouble("lat"),point.optDouble("lon"),type);
        premiumAction(content,radarGlyph(type),type,detail,()->showRadarDetails(selectedRadar));
      }
      if(visibleRadars.size()>shown)add(content,label("+ "+(visibleRadars.size()-shown)+" autre(s) radar(s) dans les 50 km",12));
    }
    premiumAction(content,"◎","Mes radars sauvegardés","Consulter les points locaux",()->manageCustomRadars());
    premiumAction(content,"＋","Enregistrer un radar","Utiliser la position GPS actuelle",()->addCustomRadar());
    sectionCard(content,"RÉGLAGES AVANCÉS","Sons, voix, précision et surveillance","radar_advanced",false,()->{
    sectionCard(content,"Bulle radar · avec Waze","GPS en arrière-plan · glisser pour déplacer","background",false,()->{add(content,label("Arme le mode auto depuis cet écran. Au-dessus de 20 km/h, les alertes démarrent. Après 1 minute immobile, alertes et bulle se coupent; une veille GPS permet de repartir. Fermer la tâche ROADGUARD arrête le suivi et retire la bulle.",13));add(content,button("Activer la bulle flottante",()->startFloatingRadar()));add(content,button("Activer les alertes sans bulle",()->startBackgroundAlerts()));add(content,button("Arrêter la surveillance",()->{getPreferences(0).edit().putBoolean("floating_auto",false).putBoolean("monitor_armed",false).apply();BackgroundAlertService.stopMonitor(this);updateFloatingChip();Toast.makeText(this,"Surveillance arrêtée",Toast.LENGTH_SHORT).show();}));});sectionCard(content,"Sons et voix","Bips, annonces","sounds",true,()->{add(content,label("SIGNAUX SONORES",18));add(content,button("Bips de danger : "+(getPreferences(0).getBoolean("danger_beeps",true)?"ACTIFS":"DÉSACTIVÉS"),()->{boolean v=!getPreferences(0).getBoolean("danger_beeps",true);getPreferences(0).edit().putBoolean("danger_beeps",v).apply();render();}));add(content,label("Radar devant vous : bips progressifs dès 1 km, de plus en plus rapprochés jusqu’au son continu dans les 50 derniers mètres. Dépassement de la limitation connue : tonalité plus aiguë et distincte. Annonces vocales à 1 km, puis 750, 500, 300, 250, 200, 150, 100 et 50 m. Limitation absente : bip normal uniquement. Volume lié au volume multimédia du téléphone. Danger : trois bips; travaux/ralentissement : un bip.",12));});sectionCard(content,"Précision des alertes","Direction et trajet","filters",false,()->{add(content,label("FILTRAGE DES ALERTES",18));add(content,button("Radars devant moi : "+(getPreferences(0).getBoolean("forward_dangers",true)?"ACTIF":"DÉSACTIVÉ"),()->{boolean v=!getPreferences(0).getBoolean("forward_dangers",true);getPreferences(0).edit().putBoolean("forward_dangers",v).apply();render();}));add(content,label("Les radars sont annoncés seulement avec un cap GPS fiable et devant le véhicule. Sans cap fiable, les annonces radar attendent. Un trajet calculé aide à écarter les points hors route, mais une route parallèle ou un carrefour peuvent encore tromper le filtre.",12));add(content,label("PRÉCISION DES ALERTES",18));add(content,button("Filtre itinéraire : "+(getPreferences(0).getBoolean("route_corridor_alerts",true)?"ACTIF":"DÉSACTIVÉ"),()->{boolean v=!getPreferences(0).getBoolean("route_corridor_alerts",true);getPreferences(0).edit().putBoolean("route_corridor_alerts",v).apply();render();}));add(content,button("Précision du trajet : "+corridorName(),()->{int next=(getPreferences(0).getInt("route_corridor_width",1)+1)%3;getPreferences(0).edit().putInt("route_corridor_width",next).apply();render();}));add(content,label("Étroit : 35 m · Normal : 55 m · Large : 85 m. Seuls les points à venir proches du tracé sont retenus lorsque le GPS est précis. Ce filtrage ne distingue pas toujours deux chaussées parallèles.",12));});sectionCard(content,"Halo radar","Rouge fixe dès 1 km","halo",true,()->{add(content,label("HALO RADAR",18));add(content,button("Effet radar : "+radarEffectName(),()->{int next=(getPreferences(0).getInt("radar_effect",1)==0?2:0);getPreferences(0).edit().putInt("radar_effect",next).apply();render();}));add(content,label("Cadre et bandeau rouges fixes dès 1 km, conservés pendant l’approche. La bulle affiche le radar et sa distance. Aucun clignotement. Halo désactivable.",12));for(String n:new String[]{"Alertes vocales","Guidage vocal","Alertes de danger","Mode nuit"}){Switch sw=new Switch(this);sw.setText(n);sw.setTextColor(-1);sw.setChecked(getPreferences(0).getBoolean(n,true));sw.setOnCheckedChangeListener((b,v)->getPreferences(0).edit().putBoolean(n,v).apply());add(content,sw);}add(content,label("Guidage vocal expérimental : à activer manuellement. Annonces à 500 m, 150 m et 40 m de la prochaine manoeuvre ; GPS et carte au premier plan nécessaires.",13));add(content,label("Les alertes nécessitent des points de danger disponibles et une position GPS fiable.",13));});
    });
    }else{
      // 11.14.73: premium reporting layout, preserving confirmation and local storage.
      premiumVisualBanner(content,"⚠","SIGNALEMENT RAPIDE","Choisissez un danger sur la route",0xffed485e);
      LinearLayout gpsCard=column();gpsCard.setPadding(16,13,16,13);
      gpsCard.setBackground(premiumPanel(0xff182a3b,0xff101822,0xff405b75,18));
      TextView gpsHeading=label("●  POSITION DU SIGNALEMENT",11);gpsHeading.setTypeface(null,Typeface.BOLD);
      gpsHeading.setTextColor(0xffff697b);add(gpsCard,gpsHeading);
      refreshLastPosition();
      boolean reportGpsReady=Double.isFinite(lastLat)&&Double.isFinite(lastLon);
      TextView gpsDetail=label(reportGpsReady?String.format(Locale.FRANCE,"GPS disponible  •  %.5f, %.5f",lastLat,lastLon):"GPS en attente  •  position non disponible",13);
      gpsDetail.setTextColor(0xffe8f0f8);gpsDetail.setPadding(0,7,0,0);add(gpsCard,gpsDetail);
      LinearLayout.LayoutParams gpsLp=new LinearLayout.LayoutParams(-1,-2);gpsLp.setMargins(0,0,0,14);content.addView(gpsCard,gpsLp);
      premiumGroup(content,"TYPE DE DANGER","Sélectionnez une catégorie pour confirmer.");
      String[] hazards={"Radar fixe","Radar mobile","Contrôle routier (police / gendarmerie)","Accident","Danger","Travaux","Véhicule arrêté","Bouchon","Route fermée","Objet sur la route","Météo / pluie","Autre"};
      String[] hazardIcons={"◎","◉","♟","✕","⚠","⚒","▣","≋","⛔","◆","☂","•••"};
      for(int hi=0;hi<hazards.length;hi+=3){
        LinearLayout gridRow=row();
        for(int hj=hi;hj<Math.min(hi+3,hazards.length);hj++){
          final String hazard=hazards[hj];
          LinearLayout tile=column();tile.setGravity(Gravity.CENTER);tile.setPadding(7,7,7,7);
          tile.setMinimumHeight(pxDp(84));tile.setElevation(pxDp(2));
          tile.setBackground(premiumPanel(0xff252e3a,0xff171d27,0xff4b5969,19));
          TextView glyph=label(hazardIcons[hj],22);glyph.setTextColor(0xffff6072);glyph.setGravity(Gravity.CENTER);
          glyph.setMinHeight(pxDp(36));glyph.setMinWidth(pxDp(42));glyph.setBackground(outlined(0xff30202d,0xff914154,14));
          LinearLayout.LayoutParams glyphLp=new LinearLayout.LayoutParams(pxDp(46),pxDp(38));glyphLp.gravity=Gravity.CENTER_HORIZONTAL;tile.addView(glyph,glyphLp);
          TextView name=label(hazard,11);name.setTypeface(null,Typeface.BOLD);name.setTextColor(0xfff5f7fb);
          name.setGravity(Gravity.CENTER);name.setMaxLines(2);name.setPadding(0,4,0,0);add(tile,name);
          tile.setClickable(true);tile.setFocusable(true);
          tile.setOnClickListener(v->{tile.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);confirmLocalReport(hazard);});
          tile.setContentDescription("Signaler : "+hazard);
          LinearLayout.LayoutParams cell=new LinearLayout.LayoutParams(0,-2,1);cell.setMargins(3,3,3,3);gridRow.addView(tile,cell);
        }
        add(content,gridRow);
      }
      LinearLayout privacy= row();privacy.setGravity(Gravity.CENTER_VERTICAL);privacy.setPadding(pxDp(12),pxDp(9),pxDp(12),pxDp(9));
      privacy.setBackground(outlined(0xff121d2a,0xff34495f,15));
      TextView privacyIcon=label("◉",17);privacyIcon.setTextColor(0xff9cb3ca);privacyIcon.setGravity(Gravity.CENTER);privacyIcon.setMinWidth(pxDp(28));privacy.addView(privacyIcon);
      TextView privacyText=label("PRIVÉ · Enregistré sur ce téléphone. Aucun partage entre conducteurs n’est actif.",11);
      privacyText.setTextColor(0xffb8c5d4);privacyText.setPadding(pxDp(7),0,0,0);privacy.addView(privacyText,new LinearLayout.LayoutParams(0,-2,1));
      LinearLayout.LayoutParams privacyLp=new LinearLayout.LayoutParams(-1,-2);privacyLp.setMargins(0,pxDp(7),0,pxDp(10));content.addView(privacy,privacyLp);
      String saved=getPreferences(0).getString("report_history","");
      String[] entries=saved.isEmpty()?new String[0]:saved.split("\n");
      premiumGroup(content,"HISTORIQUE LOCAL",""+entries.length+" signalement(s) conservé(s) sur 50");
      if(entries.length==0){
        LinearLayout emptyHistory=column();emptyHistory.setGravity(Gravity.CENTER);emptyHistory.setPadding(pxDp(14),pxDp(12),pxDp(14),pxDp(12));
        emptyHistory.setBackground(premiumPanel(0xff263444,0xff151c26,0xff48596c,18));emptyHistory.setElevation(pxDp(1));
        TextView emptyIcon=label("◇",22);emptyIcon.setGravity(Gravity.CENTER);emptyIcon.setTextColor(0xff93b0cf);add(emptyHistory,emptyIcon);
        TextView emptyCopy=label("Aucun signalement pour le moment",12);emptyCopy.setGravity(Gravity.CENTER);emptyCopy.setTextColor(0xffbdc9d7);emptyCopy.setPadding(0,pxDp(3),0,0);add(emptyHistory,emptyCopy);
        LinearLayout.LayoutParams emptyHistoryLp=new LinearLayout.LayoutParams(-1,-2);emptyHistoryLp.setMargins(0,0,0,pxDp(9));content.addView(emptyHistory,emptyHistoryLp);
      }
      int visible=Math.min(entries.length,8);
      for(int i=0;i<visible;i++){
        final String entry=entries[i];
        premiumAction(content,"◷",entry,"Enregistré uniquement sur cet appareil",()->new AlertDialog.Builder(this).setTitle("Signalement local").setMessage(entry).setPositiveButton("OK",null).show());
      }
      if(entries.length>visible)add(content,label("+ "+(entries.length-visible)+" ancien(s) signalement(s) conservé(s)",12));
      if(entries.length>0){
        premiumAction(content,"⇩","Exporter les signalements","Créer un fichier CSV",()->exportReports());
        premiumAction(content,"×","Effacer l'historique","Suppression des données locales",()->new AlertDialog.Builder(this).setTitle("Effacer les signalements ?").setMessage("Cette action supprimera les signalements locaux de cet appareil.").setNegativeButton("Annuler",null).setPositiveButton("Effacer",(dialog,which)->{getPreferences(0).edit().remove("report_history").remove("last_report").apply();render();}).show());
      }
    }}


  JSONArray mapCatalog=new JSONArray();
  void restoreCachedMapCatalog(){try{String cached=getPreferences(0).getString("map_catalog_cache","");if(!cached.isEmpty())mapCatalog=new JSONArray(cached);}catch(Exception ignored){mapCatalog=new JSONArray();}}
  static final long MAX_RADAR_COUNTRY_BYTES=32L*1024*1024;
  static final long MAX_LUFOP_JSON_BYTES=8L*1024*1024;
  volatile boolean radarCountryDownloadRunning=false;
  volatile boolean radarCountryDownloadCancel=false;
  volatile java.net.HttpURLConnection radarCountryConnection=null;
  volatile int radarCatalogRequestToken=0;
  static final String[] RADAR_COUNTRY_CODES={"FR","CH","BE","DE","IT","ES","LU","NL","AT","PT","GB","IE","PL","CZ","DK","SE","NO","FI","US","CA","MA"};
  static final String[] RADAR_COUNTRY_LABELS={"France","Suisse","Belgique","Allemagne","Italie","Espagne","Luxembourg","Pays-Bas","Autriche","Portugal","Royaume-Uni","Irlande","Pologne","Tchéquie","Danemark","Suède","Norvège","Finlande","États-Unis","Canada","Maroc"};
  JSONArray nationalRadarCatalog(){try{return new JSONArray(getPreferences(0).getString("radar_country_catalog_cache","[]"));}catch(Exception ignored){return new JSONArray();}}
  JSONArray installedRadarCountries(){try{return new JSONArray(getPreferences(0).getString("radar_country_installed","[]"));}catch(Exception ignored){return new JSONArray();}}
  boolean isNationalRadarInstalled(String id){JSONArray saved=installedRadarCountries();for(int i=0;i<saved.length();i++)if(id.equals(saved.optJSONObject(i)==null?"":saved.optJSONObject(i).optString("id","")))return true;return false;}
  boolean loadBundledRadarDatabase(){
    bundledLoadedRadarCount=0;bundledLoadedFranceCount=0;bundledLoadedSwissCount=0;bundledLoadedNeighborCount=0;
    try{
      boolean historical=getPreferences(0).getBoolean(BundledRadarCatalog.HISTORICAL_PREFERENCE,false);
      boolean swissHistorical=getPreferences(0).getBoolean(BundledRadarCatalog.SWISS_HISTORICAL_PREFERENCE,true);
      String revision=RadarCatalogRevision.current(this);
      java.util.List<BundledRadarCatalog.Point> points=loadPhoneRadarCatalog(historical,swissHistorical);
      for(BundledRadarCatalog.Point point:points){
        dangers.add(new Danger(point));bundledLoadedRadarCount++;
        if("FR".equals(point.country))bundledLoadedFranceCount++;
        else if("CH".equals(point.country))bundledLoadedSwissCount++;
        else bundledLoadedNeighborCount++;
      }
      loadedRadarCatalogRevision=revision;RadarActivationState.consumed(this,"phone",revision,points);officialRadarCount=BundledRadarCatalog.OFFICIAL_FR_COUNT;
      return true;
    }catch(Exception error){android.util.Log.e("ROADGUARD","Base embarquée indisponible; repli officiel",error);return false;}
  }
  JSONObject bundledRadarManifest(){
    if(bundledRadarManifest!=null)return bundledRadarManifest;
    try(BufferedReader reader=new BufferedReader(new InputStreamReader(getAssets().open("radar_bundle_manifest.json"),"UTF-8"))){
      StringBuilder text=new StringBuilder();String line;
      while((line=reader.readLine())!=null){if(text.length()+line.length()>262144)throw new IOException("Métadonnées trop volumineuses");text.append(line);}
      bundledRadarManifest=new JSONObject(text.toString());return bundledRadarManifest;
    }catch(Exception error){return new JSONObject();}
  }
  void showBundledRadarSources(){
    StringBuilder info=new StringBuilder();
    try(BufferedReader reader=new BufferedReader(new InputStreamReader(getAssets().open("radar_sources_notice.txt"),"UTF-8"))){
      String line;while((line=reader.readLine())!=null){if(info.length()+line.length()>40000)break;info.append(line).append('\n');}
    }catch(Exception error){info.append("Voir le dossier SOURCES-RADARS-11.14.167.md fourni avec le projet.");}
    new AlertDialog.Builder(this).setTitle("Bases incluses · sources et limites").setMessage(info.toString()).setPositiveButton("Fermer",null).show();
  }
  void showNeighborRadarSources(){
    StringBuilder notice=new StringBuilder();
    try(BufferedReader reader=new BufferedReader(new InputStreamReader(getAssets().open("radar_neighbors_notice.txt"),"UTF-8"))){
      String line;while((line=reader.readLine())!=null){if(notice.length()+line.length()>10000)break;notice.append(line).append('\n');}
    }catch(IOException error){notice.append("Sources et licences dans radar_neighbors_manifest.json.");}
    new AlertDialog.Builder(this).setTitle("Radars pays voisins · sources").setMessage(notice.toString()).setPositiveButton("Fermer",null).show();
  }
  void renderBundledRadarSummary(LinearLayout content){
    JSONObject countries=bundledRadarManifest().optJSONObject("countries");
    JSONObject fr=countries==null?null:countries.optJSONObject("FR"),ch=countries==null?null:countries.optJSONObject("CH");
    premiumGroup(content,"BASES DÉJÀ INCLUSES","France, Suisse et cinq pays voisins utilisables sans téléchargement initial.");
    if(fr!=null)premiumAction(content,"✓","France · "+fr.optInt("count")+" points",fr.optInt("static_count")+" points d’inventaire · "+fr.optInt("historical_count")+" lieux temporaires non confirmés · "+bundledLoadedFranceCount+" chargés",()->showBundledRadarSources());
    if(ch!=null)premiumAction(content,"✓","Suisse · "+ch.optInt("count")+" points inclus",ch.optInt("static_count")+" points d’inventaire · "+ch.optInt("historical_count")+" lieux mobiles non confirmés · "+bundledLoadedSwissCount+" chargés · carte et alertes disponibles",()->showBundledRadarSources());
    premiumAction(content,"✓","Pays voisins · 20 263 points inclus","Belgique 2 701 · Allemagne 4 714 · Espagne 2 104 · Italie 10 707 · Luxembourg 37 · snapshot MapAtlas 2024 · "+bundledLoadedNeighborCount+" chargés",()->showNeighborRadarSources());
    boolean historical=getPreferences(0).getBoolean(BundledRadarCatalog.HISTORICAL_PREFERENCE,false);
    addMapPreferenceSwitch(content,"Lieux temporaires et chantiers signalés","France : ajoute les emplacements connus, sans confirmer la présence actuelle d’un radar.",historical,()->{
      getPreferences(0).edit().putBoolean(BundledRadarCatalog.HISTORICAL_PREFERENCE,!historical).apply();
      requestRadarCatalogReload(true);BackgroundAlertService.requestRadarReload();render();
    });
    boolean swissHistorical=getPreferences(0).getBoolean(BundledRadarCatalog.SWISS_HISTORICAL_PREFERENCE,true);
    addMapPreferenceSwitch(content,"Lieux mobiles signalés · Suisse","Emplacements connus, présence actuelle non confirmée. Réglage commun à la carte et au service GPS.",swissHistorical,()->{
      getPreferences(0).edit().putBoolean(BundledRadarCatalog.SWISS_HISTORICAL_PREFERENCE,!swissHistorical).apply();
      requestRadarCatalogReload(true);BackgroundAlertService.requestRadarReload();render();
    });
    premiumAction(content,"ⓘ","Sources et dates des bases","Assemblage du 16/10/2026 · dates des sources différentes · couverture non exhaustive",()->showBundledRadarSources());
  }
  void renderRadarCountryDownloads(LinearLayout content){
    premiumGroup(content,"BASES RADAR PAR PAYS","Télécharge des bases publiques ou importe un ZIP CSV/ASC Lufop par pays.");
    TextView radarSafety=label("BASES RADARS INTÉGRÉES PROTÉGÉES\nLes fichiers d'origine dans l'APK ne sont jamais supprimés. Les mises à jour sont conservées séparément ; retour automatique aux bases d'origine si une mise à jour est invalide.",12);
    radarSafety.setTextColor(0xfff4f6fb);radarSafety.setPadding(pxDp(12),pxDp(10),pxDp(12),pxDp(10));
    radarSafety.setBackground(premiumPanel(0xff243143,0xff101722,0xff587ca3,12));add(content,radarSafety);
    if(radarCountryDownloadRunning){
      TextView warning=label("🔴 INTERVENTION OBLIGATOIRE : téléchargement en cours. Ne ferme pas l'application avant la fin ou utilise Annuler.",12);
      warning.setTextColor(0xffffffff);warning.setPadding(pxDp(12),pxDp(10),pxDp(12),pxDp(10));warning.setBackground(premiumPanel(0xff8b1423,0xff490c16,0xffff465b,12));add(content,warning);
    }
    int countriesInstalled=installedRadarCountries().length();
    TextView downloadTitle=label("BASES SUPPLÉMENTAIRES INSTALLÉES · "+countriesInstalled+" / 7 pays de référence",12);add(content,downloadTitle);
    android.widget.ProgressBar installedProgress=new android.widget.ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
    installedProgress.setMax(7);installedProgress.setProgress(Math.min(7,countriesInstalled));
    installedProgress.setProgressTintList(android.content.res.ColorStateList.valueOf(0xff72d7b4));
    LinearLayout.LayoutParams radarProgressParams=new LinearLayout.LayoutParams(-1,pxDp(9));radarProgressParams.setMargins(0,pxDp(5),0,pxDp(12));content.addView(installedProgress,radarProgressParams);
    add(content,label("Indicateur d'installation des bases supplémentaires (pas un pourcentage de couverture radar). Les 7 bases d'origine restent incluses.",11));

    JSONArray catalog=nationalRadarCatalog(),installed=installedRadarCountries();
    add(content,label(installed.length()+" base(s) nationale(s) supplémentaire(s) installée(s) sur ce téléphone.",12));
    String apiKey=getPreferences(0).getString("lufop_api_key","");
    add(content,button(apiKey.isEmpty()?"⌕  Configurer la clé Lufop (facultatif)":"✓  Clé Lufop configurée · modifier",()->configureLufopKey()));
    if(!apiKey.isEmpty())add(content,button("Supprimer la clé Lufop",()->new AlertDialog.Builder(this).setTitle("Supprimer la clé ?").setMessage("Les bases déjà installées resteront sur le téléphone. Les prochains téléchargements Lufop demanderont une nouvelle clé.").setNegativeButton("Garder",null).setPositiveButton("Supprimer",(d,w)->{getPreferences(0).edit().remove("lufop_api_key").apply();render();}).show()));
    premiumGroup(content,"PAYS VOISINS · ACCÈS RAPIDE","Cinq bases datées sont incluses. Tu peux récupérer des données OSM plus récentes selon tes trajets.");
    for(int i=0;i<5;i++){
      final String[] quickCodes={"DE","ES","IT","BE","LU"};
      final String[] quickNames={"Allemagne","Espagne","Italie","Belgique","Luxembourg"};
      String code=quickCodes[i],name=quickNames[i];
      JSONObject already=installedRadarCountry(osmPackId(code));
      premiumAction(content,already==null?"↻":"✓",name,already==null?"Base incluse · actualiser via OSM sans clé":already.optInt("count")+" radars OSM installés · mettre à jour",()->confirmOsmCountryDownload(code,name));
    }
    add(content,button("⇩  Choisir un pays  ·  téléchargement direct sans clé",()->openRadarCountryPicker()));
    add(content,label("MapAtlas et OpenStreetMap sont accessibles directement. Pour Lufop, importe le ZIP CSV public téléchargé depuis le site : ROADGUARD extrait les fichiers du pays choisi et conserve les catégories. Les points suisses sont chargés pour la carte et les alertes GPS, selon tes réglages.",12));
    LinearLayout actions=row();
    String catalogUrl=getPreferences(0).getString("radar_country_catalog_url","");
    if(!catalogUrl.isEmpty())add(content,button("Actualiser les bases publiées",()->refreshRadarCountryCatalog()));
    add(content,button("Importer un catalogue JSON",()->chooseRadarCountryCatalogFile()));
    add(content,button("Importer un ZIP CSV/ASC Lufop · Suisse",()->chooseLufopSwissCsvZip()));
    add(content,button("Importer un ZIP POI-Warner · source à vérifier",()->choosePoiWarnerSwissZip()));
    if(installed.length()>0){
      for(int i=0;i<installed.length();i++){JSONObject item=installed.optJSONObject(i);if(item==null)continue;String id=item.optString("id",""),name=item.optString("name",id);long rows=item.optLong("count",0);
        boolean quarantined=RadarSourcePolicy.quarantined(id);premiumAction(content,quarantined?"?":"✓",name,rows+" points · "+(quarantined?"source non vérifiée · hors carte/alertes":"hors connexion · carte et alertes disponibles"),()->new AlertDialog.Builder(this).setTitle(name).setMessage((quarantined?"Archive conservée pour vérification : libellés FR et sélection rectangulaire autour de CH, pays non établi. Ces points restent exclus de la carte et des alertes.":"Base radar chargée sur cet appareil, y compris pour la Suisse. La suppression désactive aussi ses alertes GPS. Les emplacements mobiles restent des lieux non confirmés.")+"\n\nSource : "+item.optString("attribution","non précisée")+"\nLicence : "+item.optString("license","non précisée")).setNegativeButton("Fermer",null).setPositiveButton("Supprimer",(d,w)->removeRadarCountry(id)).show());
      }
    }
    if(catalog.length()==0)add(content,label("Les bases OpenStreetMap se téléchargent directement sans clé API. Lufop et les catalogues ROADGUARD restent disponibles en sources complémentaires.",12));
  }
  void addMapPreferenceSwitch(LinearLayout parent,String title,String description,boolean checked,Runnable onChange){
    LinearLayout item=column();item.setPadding(pxDp(12),pxDp(9),pxDp(12),pxDp(9));item.setBackground(premiumPanel(0xff172230,0xff101721,0xff3d5167,16));
    Switch control=new Switch(this);control.setText(title);control.setTextColor(0xfff4f6fa);control.setTextSize(15);control.setChecked(checked);control.setContentDescription(title+". "+description);control.setOnCheckedChangeListener((button,value)->onChange.run());item.addView(control);
    TextView note=label(description,11);note.setTextColor(0xffb8c5d4);note.setPadding(0,pxDp(3),0,0);item.addView(note);
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,pxDp(3),0,pxDp(3));parent.addView(item,lp);
  }
  String radarGlyph(String type){String t=type==null?"":type.toLowerCase(Locale.ROOT);if(t.contains("tunnel"))return "▤";if(t.contains("feu"))return "●";if(t.contains("tronçon")||t.contains("moyenne"))return "↔";if(t.contains("mobile"))return "◉";if(t.contains("passage à niveau"))return "╳";if(t.contains("tourelle"))return "◉";return "◎";}
  void showRadarDetails(Danger radar){activeRadarUi=radar;legacySection=false;selected="Radar";render();}
  void showLegacyRadarDetails(Danger radar){
    double meters=Double.isFinite(lastLat)&&Double.isFinite(lastLon)?geoMeters(lastLat,lastLon,radar.lat,radar.lon):Double.NaN;
    String distance=Double.isFinite(meters)?(meters<1000?String.format(Locale.FRANCE,"%.0f m",meters):String.format(Locale.FRANCE,"%.1f km",meters/1000.0)):"GPS indisponible";
    Dialog dialog=new Dialog(this);LinearLayout page=column();page.setPadding(pxDp(16),pxDp(14),pxDp(16),pxDp(15));page.setBackground(premiumPanel(0xff171c24,0xff090d14,0xff58616e,24));
    LinearLayout header=row();TextView eyebrow=label("ROADGUARD  /  RADAR",10);eyebrow.setTypeface(null,Typeface.BOLD);eyebrow.setTextColor(0xffff6575);header.addView(eyebrow,new LinearLayout.LayoutParams(0,-2,1));TextView close=button("×",dialog::dismiss);close.setTextSize(22);close.setMinHeight(pxDp(38));close.setMinWidth(pxDp(38));header.addView(close);add(page,header);
    LinearLayout hero=row();hero.setPadding(pxDp(12),pxDp(12),pxDp(12),pxDp(12));hero.setBackground(premiumPanel(0xff642235,0xff301520,0xffef536a,20));TextView glyph=label(radarGlyph(radar.type),30);glyph.setGravity(Gravity.CENTER);glyph.setTextColor(0xffff7887);glyph.setBackground(outlined(0xff331d28,0xffe15369,17));glyph.setMinWidth(pxDp(56));glyph.setMinHeight(pxDp(56));hero.addView(glyph);LinearLayout summary=column();TextView title=label(radar.type.toUpperCase(Locale.FRANCE),14);title.setTypeface(null,Typeface.BOLD);title.setTextColor(0xffffffff);add(summary,title);TextView value=label(distance,25);value.setTypeface(null,Typeface.BOLD);value.setTextColor(0xffffffff);value.setPadding(0,pxDp(3),0,0);add(summary,value);LinearLayout.LayoutParams summaryLp=new LinearLayout.LayoutParams(0,-2,1);summaryLp.setMargins(pxDp(12),0,0,0);hero.addView(summary,summaryLp);LinearLayout.LayoutParams heroLp=new LinearLayout.LayoutParams(-1,-2);heroLp.setMargins(0,pxDp(14),0,pxDp(12));page.addView(hero,heroLp);
    premiumGroup(page,"INFORMATIONS DU POINT","Données reçues de la base radar installée.");
    premiumAction(page,"⌖","Position",String.format(Locale.FRANCE,"%.5f, %.5f",radar.lat,radar.lon),null);
    premiumAction(page,"◷","Distance GPS",distance+" · mesure depuis la dernière position",null);
    premiumAction(page,"▤","Source","Base radar chargée sur cet appareil",null);
    TextView note=label("La limitation, la direction et la fiabilité ne sont pas indiquées lorsqu’elles ne figurent pas dans la source.",11);note.setTextColor(0xffb7c1ce);note.setPadding(pxDp(4),pxDp(4),pxDp(4),pxDp(8));add(page,note);
    premiumPrimaryAction(page,"⌖  VOIR SUR LA CARTE",()->{dialog.dismiss();focusRadarOnMap(radar);});dialog.setContentView(page);Window window=dialog.getWindow();if(window!=null){window.setBackgroundDrawableResource(android.R.color.transparent);window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);WindowManager.LayoutParams lp=window.getAttributes();lp.width=Math.min(getResources().getDisplayMetrics().widthPixels-pxDp(28),pxDp(440));lp.height=-2;lp.gravity=Gravity.CENTER;lp.dimAmount=.68f;window.setAttributes(lp);}dialog.show();if(window!=null)window.setLayout(Math.min(getResources().getDisplayMetrics().widthPixels-pxDp(28),pxDp(440)),-2);
  }
  void focusRadarOnMap(Danger radar){pendingMapFocus=radar;offlineView=false;legacySection=false;selected="Carte";render();}
  void applyPendingMapFocus(){if(pendingMapFocus==null||map==null||!mapReady)return;Danger point=pendingMapFocus;pendingMapFocus=null;map.evaluateJavascript(String.format(Locale.US,"if(typeof focusRoadguardPoint==='function')focusRoadguardPoint(%.7f,%.7f)",point.lat,point.lon),null);}

  void configureLufopKey(){
    final EditText input=new EditText(this);input.setSingleLine(true);input.setHint("Clé API Lufop");input.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);input.setText(getPreferences(0).getString("lufop_api_key",""));
    LinearLayout box=column();pad(box,18);add(box,label("ROADGUARD utilisera cette clé uniquement pour demander la base du pays choisi. Elle reste dans les préférences privées de l’application et n’est jamais inscrite dans les journaux. Obtiens une clé sur api.lufop.net; le forfait détermine les pays et le nombre de résultats.",13));add(box,input);
    AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Clé API Lufop").setView(box).setNegativeButton("Fermer",null).setNeutralButton("Créer une clé",(d,w)->openRadarProviderUrl("https://api.lufop.net/en")).setPositiveButton("Enregistrer",null).create();
    dialog.setOnShowListener(v->{dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view->{String key=input.getText().toString().trim();if(key.length()<8||key.length()>256||key.matches(".*\\s+.*")){input.setError("Vérifie la clé API");return;}getPreferences(0).edit().putString("lufop_api_key",key).apply();dialog.dismiss();render();});});dialog.show();
  }
  void showRadarProviders(){
    String[] providers={"MapAtlas · 106 pays, 4 catégories","OpenStreetMap · téléchargement direct intégré","Base officielle France · Ministère de l’Intérieur","Open GATSO POI · CSV Europe quotidien","Lufop · API officielle avec clé","SCDB.info · base mondiale payante","GPS Data Team · cité par RadarDroid","Kostenlos DB · cité par RadarDroid","Lufop · archive CSV publique à importer"};
    new AlertDialog.Builder(this).setTitle("Fournisseurs de bases radar")
      .setItems(providers,(dialog,which)->{
        String title,info,url;
    if(which==0){title="MapAtlas Camera Index";info="Base communautaire publiée pour 106 pays/régions, environ 106 000 points au snapshot 2024-05-21 : radars fixes, feux rouges, tronçons de vitesse moyenne et radars à vitesse variable. ROADGUARD télécharge le GeoJSON du pays choisi et le conserve hors connexion. Licence CC BY 4.0, attribution MapAtlas incluse. Données signalées par des contributeurs, non officielles et possiblement incomplètes.";url="https://mapatlas.eu/tools/speed-camera-map";}
        else if(which==1){title="OpenStreetMap / Overpass";info="Source mondiale sans clé. ROADGUARD interroge directement les radars fixes cartographiés dans le pays choisi et conserve la base hors connexion. Après une panne, l’application essaie plusieurs instances publiques référencées par OpenStreetMap. Données contributives sous ODbL 1.0; couverture non exhaustive, radars mobiles non garantis.";url="https://wiki.openstreetmap.org/wiki/Overpass_API";}
        else if(which==2){title="Base officielle France";info="La base du Ministère de l’Intérieur est déjà intégrée à ROADGUARD, mise à jour publiée le 30 décembre 2025. Elle couvre environ 3 400 radars automatiques fixes et relève de la Licence Ouverte 2.0. Le téléchargement séparé n’est pas nécessaire.";url="https://www.data.gouv.fr/datasets/liste-des-radars-fixes-en-france";}
        else if(which==3){title="Open GATSO POI";info="Projet communautaire proposant des fichiers CSV Europe mis à jour quotidiennement et un téléchargement groupé. Son dépôt indique des sources mélangées, notamment des données officielles, OSM et Lufop; la licence du jeu de données assemblé n’est pas clairement établie pour une redistribution dans ROADGUARD. Le lien ouvre la page des téléchargements, aucun fichier tiers n’est incorporé.";url="https://github.com/1e1/Open-GATSO-POI/releases/latest";}
        else if(which==4){title="API Lufop";info="Téléchargement intégré à ROADGUARD via l’API officielle et une clé personnelle. Le forfait Free est limité à 200 résultats par appel; Pro à 10 000 et Business à 30 000. La documentation annonce 58+ pays, mais pays couverts et volumes varient selon le forfait. ROADGUARD refuse toute réponse qui atteint le plafond pour ne pas installer une base partielle. Données sous ODbL 1.0 : attribution requise.";url="https://api.lufop.net/en";}
        else if(which==5){title="SCDB.info";info="Service payant, annoncé pour 113 pays. Les fichiers sont destinés aux appareils et logiciels listés par SCDB. Les droits de réutilisation et de redistribution dans ROADGUARD ne sont pas établis : aucun fichier SCDB n’est intégré ni importé automatiquement.";url="https://www.scdb.info/en/register/?mode=guest";}
        else if(which==6){title="GPS Data Team";info="Fournisseur mentionné dans la capture RadarDroid. ROADGUARD n’a pas de téléchargement, de format d’import ni de licence de redistribution vérifiés pour cette source.";url="https://www.gps-data-team.com/";}
        else if(which==7){title="Kostenlos DB";info="Source mentionnée dans la capture RadarDroid. Son état actuel, ses pays disponibles et ses conditions de réutilisation pour ROADGUARD restent à vérifier.";url="https://www.radardroid.com/";}
        else{title="Archive CSV/ASC Lufop";info="Télécharge l’archive standard « Zones de danger EU CSV » depuis le site Lufop, puis importe-la avec le bouton « Importer un ZIP CSV/ASC Lufop · Suisse » dans ROADGUARD. L’application lit uniquement les fichiers CSV/ASC suisses de l’archive, conserve les types (fixes avec limitation, mobiles, feux rouges et tunnels) et retire les doublons de coordonnées. Le lien /radardroid-lufop-XX reste réservé à Radardroid et n’est pas utilisé par ROADGUARD. Licence annoncée : ODbL 1.0; attribution Lufop conservée.";url="https://lufop.net/base-radardroid-mise-a-jour-france-europe/";}
        new AlertDialog.Builder(this).setTitle(title).setMessage(info).setNegativeButton("Fermer",null).setPositiveButton("Ouvrir le site",(d,w)->openRadarProviderUrl(url)).show();
      }).setNegativeButton("Fermer",null).show();
  }
  void openRadarProviderUrl(String url){try{Intent intent=new Intent(Intent.ACTION_VIEW,Uri.parse(url));startActivity(intent);}catch(Exception e){Toast.makeText(this,"Aucun navigateur disponible",Toast.LENGTH_LONG).show();}}
  void refreshRadarCountryCatalog(){
    String url=getPreferences(0).getString("radar_country_catalog_url","");if(url.isEmpty()){Toast.makeText(this,"Aucun catalogue distant configuré. Importe un catalogue JSON vérifié.",Toast.LENGTH_LONG).show();return;}final int requestToken=++radarCatalogRequestToken;
    addNetworkOperation("radar-catalog",()->{
      try{java.net.HttpURLConnection c=secureConnection(url);byte[] bytes;try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1){if(out.size()+n>262144)throw new IOException("Catalogue trop volumineux");out.write(buf,0,n);}bytes=out.toByteArray();}finally{c.disconnect();}JSONArray packs=parseRadarCountryCatalog(bytes);
        runOnUiThread(()->{if(requestToken!=radarCatalogRequestToken||!url.equals(getPreferences(0).getString("radar_country_catalog_url","")))return;getPreferences(0).edit().putString("radar_country_catalog_cache",packs.toString()).apply();Toast.makeText(this,packs.length()+" base(s) radar disponible(s)",Toast.LENGTH_SHORT).show();render();});
      }catch(Exception e){runOnUiThread(()->{if(requestToken==radarCatalogRequestToken)Toast.makeText(this,"Liste radar indisponible : "+e.getMessage(),Toast.LENGTH_LONG).show();});}
    });
  }
  JSONArray parseRadarCountryCatalog(byte[] bytes)throws Exception{JSONObject root=new JSONObject(new String(bytes,"UTF-8"));if(root.optInt("schema")!=1)throw new IOException("Version de catalogue inconnue");JSONArray packs=root.getJSONArray("packs");if(packs.length()>100)throw new IOException("Trop de pays");java.util.HashSet<String> ids=new java.util.HashSet<>(),countries=new java.util.HashSet<>();for(int i=0;i<packs.length();i++){JSONObject p=packs.getJSONObject(i);String id=p.getString("id"),link=p.getString("url"),hash=p.getString("sha256"),format=p.optString("format"),country=p.optString("country").toUpperCase(Locale.ROOT),attribution=p.optString("attribution"),license=p.optString("license");java.net.URL u=new java.net.URL(link);if(!id.matches("[a-z0-9_-]{2,50}")||!ids.add(id)||!country.matches("[A-Z]{2,3}(-[A-Z0-9]{2,8})?")||!countries.add(country)||!"roadguard-radar-csv-v1".equals(format)||!"https".equalsIgnoreCase(u.getProtocol())||u.getUserInfo()!=null||!hash.matches("[a-fA-F0-9]{64}")||p.optLong("size",0)<1||p.optLong("size",0)>MAX_RADAR_COUNTRY_BYTES||p.optString("name","").trim().isEmpty()||p.optString("name","").length()>100||attribution.trim().isEmpty()||attribution.length()>240||license.trim().isEmpty()||license.length()>100)throw new IOException("Fiche de pays invalide : "+id);}return packs;}
  void chooseRadarCountryCatalogFile(){try{Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("application/json");DocumentPicker.open(this,intent,62);}catch(Exception e){Toast.makeText(this,"Sélecteur de fichiers indisponible",Toast.LENGTH_LONG).show();}}
  void importRadarCountryCatalog(android.net.Uri uri){try(InputStream in=getContentResolver().openInputStream(uri);ByteArrayOutputStream out=new ByteArrayOutputStream()){if(in==null)throw new IOException("Fichier inaccessible");byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1){if(out.size()+n>262144)throw new IOException("Catalogue supérieur à 256 Kio");out.write(buf,0,n);}JSONArray packs=parseRadarCountryCatalog(out.toByteArray());radarCatalogRequestToken++;getPreferences(0).edit().putString("radar_country_catalog_url","").putString("radar_country_catalog_cache",packs.toString()).putString("radar_country_catalog_source","fichier").apply();Toast.makeText(this,packs.length()+" base(s) radar vérifiée(s) importée(s)",Toast.LENGTH_LONG).show();render();}catch(Exception e){Toast.makeText(this,"Catalogue refusé : "+e.getMessage(),Toast.LENGTH_LONG).show();}}
  void addNetworkOperation(String name,Runnable operation){new Thread(operation,name).start();}
  void openSimpleRadarCountryPicker(){
    // Entrée réellement appelée depuis l'ancienne fenêtre "autres pays".
    // Ouvrir l'écran graphique avec drapeaux, recherche et états colorés.
    RadarDownloadEntry.open(this);
  }
  void openRadarCountryPicker(){
    // Bouton réellement utilisé par l'écran « Bases radar ».
    // Ouvrir l'interface pays au lieu de l'ancien sélecteur MapAtlas.
    RadarDownloadEntry.open(this);
  }
  void openRadarCountryPickerLegacy(){
    addNetworkOperation("mapatlas-region-catalog",()->{try{java.net.HttpURLConnection c=secureConnection("https://mapatlas.eu/data/camera-index/catalog.json");ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=c.getInputStream()){byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1){if(out.size()+n>524288)throw new IOException("Catalogue MapAtlas trop volumineux");out.write(buf,0,n);}}finally{c.disconnect();}JSONObject root=new JSONObject(new String(out.toByteArray(),"UTF-8"));JSONArray regions=root.optJSONArray("regions");if(regions==null||regions.length()>250||!root.optString("licence","").toUpperCase(Locale.ROOT).contains("CC BY 4.0"))throw new IOException("Catalogue MapAtlas invalide");runOnUiThread(()->showMapAtlasRegionPicker(regions));}catch(Exception error){runOnUiThread(()->openRadarCountryPickerFallback());}});
  }
  void showMapAtlasRegionPicker(JSONArray regions){ArrayList<String> codes=new ArrayList<>(),labels=new ArrayList<>();for(int i=0;i<regions.length();i++){JSONObject region=regions.optJSONObject(i);if(region==null)continue;String code=region.optString("code","").toUpperCase(Locale.ROOT),name=region.optString("name",code);if(!code.matches("[A-Z]{2}"))continue;codes.add(code);JSONObject installed=installedRadarCountry(mapAtlasPackId(code));labels.add(name+" · MapAtlas "+String.format(Locale.getDefault(),"%,d",region.optInt("cameras"))+" · OpenStreetMap"+(installed==null?"":" · MapAtlas installé"));}String[] choices=labels.toArray(new String[0]),countryCodes=codes.toArray(new String[0]);new AlertDialog.Builder(this).setTitle("Choisir un pays · bases radar").setItems(choices,(d,w)->{String code=countryCodes[w],name=choices[w].replaceFirst(" · MapAtlas.*$","");showRadarCountrySources(code,name);}).setPositiveButton("Sources classiques",(d,w)->openRadarCountryPickerFallback()).setNegativeButton("Fermer",null).show();}
  void openRadarCountryPickerFallback(){
    ArrayList<String> countries=new ArrayList<>(),names=new ArrayList<>(),labels=new ArrayList<>();
    for(int i=0;i<RADAR_COUNTRY_CODES.length;i++){String code=RADAR_COUNTRY_CODES[i];countries.add(code);names.add(RADAR_COUNTRY_LABELS[i]);JSONObject installed=installedRadarCountry(mapAtlasPackId(code));labels.add((installed==null?"⇩  ":"✓  ")+RADAR_COUNTRY_LABELS[i]+" · "+(installed==null?"MapAtlas + OpenStreetMap + Lufop API":"MapAtlas installé · OpenStreetMap · "+installed.optInt("count")+" points"));}
    JSONArray catalog=nationalRadarCatalog();java.util.HashSet<String> known=new java.util.HashSet<>(countries);
    for(int i=0;i<catalog.length();i++){JSONObject pack=catalog.optJSONObject(i);if(pack==null)continue;String code=pack.optString("country","").toUpperCase(Locale.ROOT);if(known.contains(code))continue;known.add(code);countries.add(code);String name=pack.optString("name",code);names.add(name);labels.add("⇩  "+name+" · MapAtlas + OpenStreetMap + Lufop API");}
    String[] choices=labels.toArray(new String[0]),codes=countries.toArray(new String[0]),displayNames=names.toArray(new String[0]);new AlertDialog.Builder(this).setTitle("Bases radar · choisir un pays").setItems(choices,(d,w)->showRadarCountrySources(codes[w],displayNames[w])).setNeutralButton("Autre pays",(d,w)->chooseAnyRadarCountry()).setNegativeButton("Fermer",null).show();
  }
  void chooseAnyRadarCountry(){
    EditText code=new EditText(this);code.setSingleLine(true);code.setHint("Code ISO à 2 lettres · ex. JP");
    LinearLayout wrapper=column();wrapper.setPadding(pxDp(22),pxDp(6),pxDp(22),0);add(wrapper,code);
    new AlertDialog.Builder(this).setTitle("Radars d’un autre pays")
      .setMessage("Source OpenStreetMap contributive : radars fixes cartographiés, sans garantie d’exhaustivité. Un pays à la fois; aucune carte routière téléchargée.")
      .setView(wrapper).setNegativeButton("Annuler",null).setPositiveButton("Continuer",(dialog,which)->{
        String iso=code.getText().toString().trim().toUpperCase(Locale.ROOT);
        boolean known=false;for(String country:Locale.getISOCountries())if(country.equals(iso)){known=true;break;}
        if(!known){Toast.makeText(this,"Code pays ISO invalide (deux lettres)",Toast.LENGTH_LONG).show();return;}
        String label=new Locale("",iso).getDisplayCountry(Locale.FRANCE);
        confirmOsmCountryDownload(iso,label);
      }).show();
  }
  void confirmOsmCountryDownload(String country,String name){
    String localRule="DE".equals(country)?" En Allemagne, le conducteur ne doit pas utiliser un avertisseur de contrôles de vitesse pendant la conduite.":"";
    new AlertDialog.Builder(this).setTitle("Installer les radars · "+name)
      .setMessage("Télécharge uniquement les radars fixes indiqués dans OpenStreetMap, si le serveur et les données de ce pays sont disponibles. Source © OpenStreetMap contributors · ODbL 1.0. La base précédente est conservée en cas d’échec."+localRule)
      .setNegativeButton("Annuler",null).setPositiveButton("Télécharger",(d,w)->downloadOsmCountry(country,name)).show();
  }
  JSONObject installedRadarCountry(String id){JSONArray installed=installedRadarCountries();for(int i=0;i<installed.length();i++){JSONObject item=installed.optJSONObject(i);if(item!=null&&id.equals(item.optString("id","")))return item;}return null;}
  String mapAtlasPackId(String country){return "mapatlas_"+country.toLowerCase(Locale.ROOT);}
  String osmPackId(String country){return "osm_"+country.toLowerCase(Locale.ROOT);}
  volatile boolean swissRadarBatchActive=false,swissRadarBatchCancelled=false;int swissRadarBatchStep=0;
  void showRadarCountrySources(String country,String name){
    ArrayList<String> labels=new ArrayList<>();ArrayList<Runnable> actions=new ArrayList<>();
    if("FR".equals(country)||"CH".equals(country)){labels.add("ROADGUARD · déjà inclus · vérifier les mises à jour");actions.add(()->{if(community!=null)community.updateRadarInventory();});}
    JSONObject map=installedRadarCountry(mapAtlasPackId(country));labels.add(map==null?"MapAtlas · installer sans clé API":"MapAtlas · "+map.optInt("count")+" points · mettre à jour");actions.add(()->confirmMapAtlasDownload(country,name));
    JSONObject osm=installedRadarCountry(osmPackId(country));labels.add(osm==null?"OpenStreetMap · téléchargement direct sans clé":"OpenStreetMap · "+osm.optInt("count")+" points · mettre à jour");actions.add(()->downloadOsmCountry(country,name));
    JSONObject pack=findRadarCatalogPack(country);if(pack!=null){labels.add("Catalogue ROADGUARD · base supplémentaire vérifiée");actions.add(()->confirmRadarCountryDownload(pack));}
    if("CH".equals(country)){labels.add("Télécharger les 2 bases publiques en série");actions.add(()->confirmSwissRadarBatch(name));}
    if("CH".equals(country)){labels.add("Importer un ZIP CSV/ASC Lufop");actions.add(()->chooseLufopSwissCsvZip());}
    boolean hasKey=!getPreferences(0).getString("lufop_api_key","").isEmpty();labels.add(hasKey?"Lufop API · clé personnelle configurée":"Option avancée Lufop API · clé personnelle nécessaire");actions.add(()->downloadLufopCountry(country,name));
    new AlertDialog.Builder(this).setTitle(name+" · source radar").setItems(labels.toArray(new String[0]),(d,w)->actions.get(w).run()).setNegativeButton("Fermer",null).show();
  }
  void confirmSwissRadarBatch(String name){new AlertDialog.Builder(this).setTitle(name+" · télécharger les bases publiques").setMessage("ROADGUARD récupère MapAtlas et OpenStreetMap, puis les conserve séparément. Leurs points peuvent se recouper. Pour ajouter les catégories détaillées de Lufop, utilise « Importer un ZIP CSV/ASC Lufop » avec l’archive ZIP obtenue depuis le centre Lufop (connexion au site éventuellement nécessaire). Les points suisses seront utilisés sur la carte et pour les alertes GPS, selon tes réglages.").setNegativeButton("Annuler",null).setPositiveButton("Télécharger les 2",(d,w)->{if(radarCountryDownloadRunning||swissRadarBatchActive){Toast.makeText(this,"Un téléchargement radar est déjà en cours.",Toast.LENGTH_SHORT).show();return;}swissRadarBatchActive=true;swissRadarBatchCancelled=false;swissRadarBatchStep=0;runSwissRadarBatchStep(name);}).show();}
  void runSwissRadarBatchStep(String name){if(!swissRadarBatchActive)return;if(swissRadarBatchCancelled||swissRadarBatchStep>=2){swissRadarBatchActive=false;StringBuilder detail=new StringBuilder();String[] ids={mapAtlasPackId("CH"),osmPackId("CH")};String[] labels={"MapAtlas","OpenStreetMap"};for(int j=0;j<ids.length;j++){JSONObject item=installedRadarCountry(ids[j]);if(item!=null){if(detail.length()>0)detail.append(" · ");detail.append(labels[j]).append(" ").append(item.optInt("count")).append(" pts");}}String message=detail.length()==0?"Aucune base nouvelle installée; vérifie la connexion et réessaie.":"Bases conservées séparément : "+detail+". Les données suisses sont disponibles pour la carte et les alertes GPS.";Toast.makeText(this,(swissRadarBatchCancelled?"Téléchargement groupé annulé. ":"")+message,Toast.LENGTH_LONG).show();render();return;}int step=swissRadarBatchStep++;if(step==0)downloadMapAtlasCountry("CH",name);else downloadOsmCountry("CH",name);}
  void finishSwissRadarBatchSource(){if(!swissRadarBatchActive)return;runOnUiThread(()->{if(swissRadarBatchActive)runSwissRadarBatchStep("Suisse");});}
  int lufopSwissCategoryCount(java.util.Map<String,Integer> counts,String category){int total=0;for(java.util.Map.Entry<String,Integer> item:counts.entrySet()){String type=item.getKey();if(type.equals(category+" (Lufop CSV)")||(type.startsWith(category+" · ")&&!type.contains("tunnel")&&type.endsWith("(Lufop CSV)")))total+=item.getValue();}return total;}
  void chooseLufopSwissCsvZip(){try{Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("application/zip");DocumentPicker.open(this,intent,63);}catch(Exception e){Toast.makeText(this,"Sélecteur de fichiers indisponible",Toast.LENGTH_LONG).show();}}
  String lufopSwissCsvType(String entryName){String n=entryName.substring(entryName.lastIndexOf('/')+1).toUpperCase(Locale.ROOT);String ext=n.endsWith(".ASC")?"ASC":n.endsWith(".CSV")?"CSV":null;if(ext==null)return null;String suffix=" (Lufop "+ext+")";if(n.matches("CHFIXECH[0-9]+\\."+ext)){java.util.regex.Matcher m=java.util.regex.Pattern.compile("CHFIXECH([0-9]+)").matcher(n);return m.find()?"Radar fixe · "+m.group(1)+" km/h"+suffix:"Radar fixe"+suffix;}if(("CHFEUROUGECH."+ext).equals(n))return "Radar feu rouge"+suffix;if(("CHMOBILECH."+ext).equals(n))return "Radar mobile"+suffix;if(("CHTUNNELCH."+ext).equals(n))return "Radar fixe · tunnel"+suffix;return null;}
  void choosePoiWarnerSwissZip(){try{Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("application/zip");DocumentPicker.open(this,intent,64);}catch(Exception e){Toast.makeText(this,"Sélecteur de fichiers indisponible",Toast.LENGTH_LONG).show();}}
  String poiWarnerSwissType(String entryName,String rowLabel){String n=entryName.substring(entryName.lastIndexOf('/')+1).toUpperCase(Locale.ROOT);if(!n.endsWith(".CSV"))return null;if(n.matches("FRFIXEFR(30|50|60|70|80|90|110|130)\\.CSV")){java.util.regex.Matcher m=java.util.regex.Pattern.compile("FRFIXEFR([0-9]+)").matcher(n);return m.find()?"Radar fixe · "+m.group(1)+" km/h (POI-Warner)":"Radar fixe (POI-Warner)";}if("FRFIXEFRPASSAGENIVEAU.CSV".equals(n))return "Radar passage à niveau (POI-Warner)";if("FRFIXEFRCOVOITURAGE.CSV".equals(n))return "Radar voie covoiturage (POI-Warner)";if("FRFEUROUGEFR.CSV".equals(n))return "Radar feu rouge (POI-Warner)";if("FRCHANTIERFR.CSV".equals(n)){java.util.regex.Matcher m=java.util.regex.Pattern.compile("(?i)FR\\s*([0-9]{2,3})\\s*$").matcher(rowLabel==null?"":rowLabel.trim());return m.find()&&!"0".equals(m.group(1))?"Radar chantier · "+m.group(1)+" km/h (POI-Warner)":"Radar chantier (POI-Warner)";}if("FRTRONCONDEBUTFR.CSV".equals(n))return "Radar tronçon début (POI-Warner)";if("FRTRONCONFINFR.CSV".equals(n))return "Radar tronçon fin (POI-Warner)";return null;}
  void importPoiWarnerSwissZip(Uri uri){
    if(radarCountryDownloadRunning){Toast.makeText(this,"Un import ou téléchargement radar est déjà en cours.",Toast.LENGTH_SHORT).show();return;}
    radarCountryDownloadRunning=true;radarCountryDownloadCancel=false;ProgressDialog progress=new ProgressDialog(this);progress.setTitle("Importer POI-Warner · Suisse");progress.setMessage("Lecture des CSV et filtrage géographique…");progress.setIndeterminate(true);progress.setCancelable(false);progress.setButton(ProgressDialog.BUTTON_NEGATIVE,"Annuler",(d,w)->{radarCountryDownloadCancel=true;progress.setMessage("Annulation en cours…");});progress.show();
    addNetworkOperation("radar-poi-warner-swiss-import",()->{String id="poi_warner_ch";File dir=new File(getFilesDir(),"radar_countries"),part=new File(getFilesDir(),"radar_countries/"+id+".csv.part");try{
      if(!dir.exists()&&!dir.mkdirs())throw new IOException("Stockage indisponible");java.util.HashSet<String> seen=new java.util.HashSet<>();java.util.HashMap<String,Integer> typeCounts=new java.util.HashMap<>();int accepted=0,duplicates=0,entryCount=0,recognizedEntries=0;long uncompressed=0;
      InputStream source=getContentResolver().openInputStream(uri);if(source==null)throw new IOException("Fichier inaccessible");try(InputStream raw=source;ZipInputStream zip=new ZipInputStream(new BufferedInputStream(raw));BufferedWriter out=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(part),"UTF-8"))){out.write("id;type;lat;lon\n");ZipEntry entry;byte[] buffer=new byte[8192];while((entry=zip.getNextEntry())!=null){if(radarCountryDownloadCancel)throw new IOException("Import annulé");if(++entryCount>500)throw new IOException("Archive contenant trop de fichiers");if(entry.isDirectory())continue;String baseType=poiWarnerSwissType(entry.getName(),"");ByteArrayOutputStream csv=baseType==null?null:new ByteArrayOutputStream();if(csv!=null)recognizedEntries++;int entryBytes=0,n;while((n=zip.read(buffer))!=-1){uncompressed+=n;entryBytes+=n;if(uncompressed>MAX_RADAR_COUNTRY_BYTES||entryBytes>4*1024*1024)throw new IOException("Archive trop volumineuse");if(csv!=null)csv.write(buffer,0,n);}if(csv==null)continue;String text=new String(csv.toByteArray(),"ISO-8859-1");int rowNumber=0;for(String line:text.split("\\r?\\n")){if(radarCountryDownloadCancel)throw new IOException("Import annulé");if(line.trim().isEmpty()||line.trim().startsWith(";"))continue;if(++rowNumber>100000)throw new IOException("Trop de lignes dans un fichier POI-Warner");java.util.ArrayList<String> fields=parseLufopCsvRow(line);if(fields.size()<2)continue;try{double lon=Double.parseDouble(fields.get(0).trim()),lat=Double.parseDouble(fields.get(1).trim());if(!Double.isFinite(lat)||!Double.isFinite(lon)||lat<45.6||lat>47.9||lon<5.8||lon>10.7)continue;String key=String.format(Locale.US,"%.6f,%.6f",lat,lon);if(!seen.add(key)){duplicates++;continue;}String type=poiWarnerSwissType(entry.getName(),fields.size()>2?fields.get(2):"");out.write("poi-warner-ch-"+accepted+";"+type+";"+lat+";"+lon+"\n");accepted++;typeCounts.put(type,typeCounts.containsKey(type)?typeCounts.get(type)+1:1);}catch(NumberFormatException invalid){/* ligne invalide ignorée */}}}}
      if(recognizedEntries==0)throw new IOException("Aucun CSV POI-Warner reconnu");if(accepted==0)throw new IOException("Aucun point dans la fenêtre géographique autour de la Suisse");if(validateRadarCountryCsv(part)!=accepted)throw new IOException("Validation interne échouée");if(radarCountryDownloadCancel)throw new IOException("Import annulé");File target=new File(dir,id+".csv"),backup=new File(dir,id+".csv.previous");if(backup.exists())backup.delete();if(target.exists()&&!target.renameTo(backup))throw new IOException("Sauvegarde de l’ancienne base impossible");if(!part.renameTo(target)){if(backup.exists())backup.renameTo(target);throw new IOException("Installation impossible");}backup.delete();JSONArray installed=installedRadarCountries();for(int i=installed.length()-1;i>=0;i--)if(id.equals(installed.optJSONObject(i)==null?"":installed.optJSONObject(i).optString("id","")))installed.remove(i);JSONObject saved=new JSONObject();saved.put("id",id);saved.put("name","POI-Warner · zone autour CH (libellés FR)");saved.put("country","CH");saved.put("file",id+".csv");saved.put("count",accepted);saved.put("duplicates_removed",duplicates);saved.put("type_counts",new JSONObject(typeCounts));saved.put("attribution","Archive utilisateur POI-Warner_CSV.zip; libellés d’origine FR; attribution auteur non précisée dans l’archive");saved.put("license","Non précisée dans l’archive · stockage local uniquement");saved.put("updated",System.currentTimeMillis());installed.put(saved);getPreferences(0).edit().putString("radar_country_installed",installed.toString()).apply();final int count=accepted,dupes=duplicates;runOnUiThread(()->{progress.dismiss();requestRadarCatalogReload(true);BackgroundAlertService.requestRadarReload();new AlertDialog.Builder(this).setTitle("POI-Warner importé séparément").setMessage(count+" points uniques dans la zone géographique autour de la Suisse · "+dupes+" doublons retirés. Le filtre rectangulaire peut inclure des zones frontalières voisines.\n\nLa source utilise des libellés FR et ne précise pas sa licence dans l’archive. Elle est conservée séparément sur ce téléphone, non vérifiée et exclue de la carte et des alertes.").setPositiveButton("Fermer",null).show();render();});
    }catch(Exception e){part.delete();String msg=radarCountryDownloadCancel?"Import annulé":e.getMessage();runOnUiThread(()->{progress.dismiss();Toast.makeText(this,"Import POI-Warner impossible : "+msg+". La base précédente est conservée.",Toast.LENGTH_LONG).show();});}finally{radarCountryDownloadRunning=false;}});
  }
  void importLufopSwissCsvZip(Uri uri){
    if(radarCountryDownloadRunning){Toast.makeText(this,"Un import ou téléchargement radar est déjà en cours.",Toast.LENGTH_SHORT).show();return;}
    radarCountryDownloadRunning=true;radarCountryDownloadCancel=false;ProgressDialog progress=new ProgressDialog(this);progress.setTitle("Importer la Suisse · Lufop CSV");progress.setMessage("Lecture de l’archive et vérification des fichiers CH…");progress.setIndeterminate(true);progress.setCancelable(false);progress.setButton(ProgressDialog.BUTTON_NEGATIVE,"Annuler",(d,w)->{radarCountryDownloadCancel=true;progress.setMessage("Annulation en cours…");});progress.show();
    addNetworkOperation("radar-lufop-csv-asc-zip-import",()->{String id="lufop_csv_ch";File dir=new File(getFilesDir(),"radar_countries"),part=new File(getFilesDir(),"radar_countries/"+id+".csv.part");try{
      if(!dir.exists()&&!dir.mkdirs())throw new IOException("Stockage indisponible");java.util.HashSet<String> seen=new java.util.HashSet<>();java.util.HashMap<String,Integer> typeCounts=new java.util.HashMap<>();int accepted=0,duplicates=0,entryCount=0,recognizedEntries=0;long uncompressed=0;
      InputStream source=getContentResolver().openInputStream(uri);if(source==null)throw new IOException("Fichier inaccessible");try(InputStream raw=source;ZipInputStream zip=new ZipInputStream(new BufferedInputStream(raw));BufferedWriter out=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(part),"UTF-8"))){out.write("id;type;lat;lon\n");ZipEntry entry;byte[] buffer=new byte[8192];while((entry=zip.getNextEntry())!=null){if(radarCountryDownloadCancel)throw new IOException("Import annulé");if(++entryCount>1000)throw new IOException("Archive contenant trop de fichiers");if(entry.isDirectory())continue;String type=lufopSwissCsvType(entry.getName());ByteArrayOutputStream csv=type==null?null:new ByteArrayOutputStream();if(csv!=null)recognizedEntries++;int entryBytes=0,n;while((n=zip.read(buffer))!=-1){uncompressed+=n;entryBytes+=n;if(uncompressed>MAX_RADAR_COUNTRY_BYTES||entryBytes>4*1024*1024)throw new IOException("Archive trop volumineuse");if(csv!=null)csv.write(buffer,0,n);}if(csv==null)continue;String text=new String(csv.toByteArray(),"ISO-8859-1");int rowNumber=0;for(String line:text.split("\\r?\\n")){if(radarCountryDownloadCancel)throw new IOException("Import annulé");if(line.trim().isEmpty()||line.trim().startsWith(";"))continue;if(++rowNumber>100000)throw new IOException("Trop de lignes dans un fichier suisse");java.util.ArrayList<String> fields=parseLufopCsvRow(line);if(fields.size()<2)continue;try{double lon=Double.parseDouble(fields.get(0).trim()),lat=Double.parseDouble(fields.get(1).trim());if(!Double.isFinite(lat)||!Double.isFinite(lon)||lat<45.6||lat>47.9||lon<5.8||lon>10.7)continue;String key=String.format(Locale.US,"%.6f,%.6f",lat,lon);if(!seen.add(key)){duplicates++;continue;}String rowId="lufop-ch-csv-"+accepted;out.write(rowId+";"+type+";"+lat+";"+lon+"\n");accepted++;typeCounts.put(type,typeCounts.containsKey(type)?typeCounts.get(type)+1:1);}catch(NumberFormatException invalid){/* ligne invalide ignorée */}}}}
      if(recognizedEntries==0)throw new IOException("Aucun fichier suisse CSV/ASC reconnu (CHFixeCH…, CHFeuRougeCH, CHMobileCH, CHTunnelCH)");if(accepted==0)throw new IOException("Aucun point suisse valide dans l’archive");if(validateRadarCountryCsv(part)!=accepted)throw new IOException("Validation interne échouée");if(radarCountryDownloadCancel)throw new IOException("Import annulé");File target=new File(dir,id+".csv"),backup=new File(dir,id+".csv.previous");if(backup.exists())backup.delete();if(target.exists()&&!target.renameTo(backup))throw new IOException("Sauvegarde de l’ancienne base impossible");if(!part.renameTo(target)){if(backup.exists())backup.renameTo(target);throw new IOException("Installation impossible");}backup.delete();JSONArray installed=installedRadarCountries();for(int i=installed.length()-1;i>=0;i--)if(id.equals(installed.optJSONObject(i)==null?"":installed.optJSONObject(i).optString("id","")))installed.remove(i);JSONObject saved=new JSONObject();saved.put("id",id);saved.put("name","Suisse · Lufop CSV/ASC détaillé");saved.put("country","CH");saved.put("file",id+".csv");saved.put("count",accepted);saved.put("duplicates_removed",duplicates);saved.put("type_counts",new JSONObject(typeCounts));saved.put("attribution","Archives Lufop Zones de danger EU CSV/ASC · données Lufop et contributeurs");saved.put("license","ODbL 1.0");saved.put("updated",System.currentTimeMillis());installed.put(saved);getPreferences(0).edit().putString("radar_country_installed",installed.toString()).apply();final int count=accepted,dupes=duplicates;runOnUiThread(()->{progress.dismiss();requestRadarCatalogReload(true);BackgroundAlertService.requestRadarReload();new AlertDialog.Builder(this).setTitle("Base suisse Lufop importée").setMessage(count+" emplacements distincts enregistrés · "+dupes+" doublons retirés.\n\n"+lufopSwissCategoryCount(typeCounts,"Radar fixe")+" fixes · "+lufopSwissCategoryCount(typeCounts,"Radar feu rouge")+" feux rouges · "+lufopSwissCategoryCount(typeCounts,"Radar mobile")+" mobiles · "+lufopSwissCategoryCount(typeCounts,"Radar fixe · tunnel")+" tunnels.\n\nLes données suisses sont disponibles sur la carte et dans les alertes GPS. Les emplacements mobiles signalés ne confirment pas une présence actuelle.").setPositiveButton("Fermer",null).show();render();});
    }catch(Exception e){part.delete();String msg=radarCountryDownloadCancel?"Import annulé":e.getMessage();runOnUiThread(()->{progress.dismiss();Toast.makeText(this,"Import ZIP Lufop impossible : "+msg+". La base précédente est conservée.",Toast.LENGTH_LONG).show();});}finally{radarCountryDownloadRunning=false;finishSwissRadarBatchSource();}});
  }
  java.util.ArrayList<String> parseLufopCsvRow(String line)throws IOException{
    java.util.ArrayList<String> fields=new java.util.ArrayList<>();StringBuilder value=new StringBuilder();boolean quoted=false;
    for(int i=0;i<line.length();i++){char ch=line.charAt(i);if(ch=='"'){if(quoted&&i+1<line.length()&&line.charAt(i+1)=='"'){value.append('"');i++;}else quoted=!quoted;}else if(ch==','&&!quoted){fields.add(value.toString());value.setLength(0);}else value.append(ch);}
    if(quoted)throw new IOException("fichier Lufop mal formé");fields.add(value.toString());return fields;
  }
  JSONObject findRadarCatalogPack(String country){JSONArray catalog=nationalRadarCatalog();for(int i=0;i<catalog.length();i++){JSONObject pack=catalog.optJSONObject(i);if(pack!=null&&country.equalsIgnoreCase(pack.optString("country","")))return pack;}return null;}
  void confirmMapAtlasDownload(String country,String name){String ch="CH".equals(country)?" La base suisse sera ajoutée à la carte et aux alertes GPS, selon tes réglages.":"";new AlertDialog.Builder(this).setTitle("Télécharger "+name+" · MapAtlas").setMessage("Base communautaire MapAtlas Camera Index : radars fixes, feux rouges, contrôles de vitesse moyenne et radars variables selon les points cartographiés. Licence CC BY 4.0, attribution incluse. Les données proviennent de contributeurs, ne sont pas officielles et peuvent être incomplètes; les radars mobiles ne sont pas garantis."+ch).setNegativeButton("Annuler",null).setPositiveButton("Télécharger",(d,w)->downloadMapAtlasCountry(country,name)).show();}
  void downloadMapAtlasCountry(String country,String name){
    if(radarCountryDownloadRunning){Toast.makeText(this,"Un téléchargement radar est déjà en cours.",Toast.LENGTH_SHORT).show();return;}if(!country.matches("[A-Z]{2}"))return;radarCountryDownloadRunning=true;radarCountryDownloadCancel=false;String id=mapAtlasPackId(country);ProgressDialog progress=new ProgressDialog(this);progress.setTitle("Base MapAtlas · "+name);progress.setMessage("Téléchargement des points cartographiés…");progress.setIndeterminate(true);progress.setCancelable(false);progress.setButton(ProgressDialog.BUTTON_NEGATIVE,"Annuler",(d,w)->{radarCountryDownloadCancel=true;if(swissRadarBatchActive&&"CH".equals(country))swissRadarBatchCancelled=true;java.net.HttpURLConnection c=radarCountryConnection;if(c!=null)c.disconnect();progress.setMessage("Annulation en cours…");});progress.show();
    addNetworkOperation("radar-mapatlas-"+country,()->{File dir=new File(getFilesDir(),"radar_countries"),part=new File(getFilesDir(),"radar_countries/"+id+".csv.part");try{if(!dir.exists()&&!dir.mkdirs())throw new IOException("Stockage indisponible");java.net.HttpURLConnection c=SecureHttp.open("https://mapatlas.eu/data/camera-index/"+country+".geojson",20000,60000,"ROADGUARD Android radar download");radarCountryConnection=c;ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(InputStream in=c.getInputStream()){byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1){if(radarCountryDownloadCancel)throw new IOException("Téléchargement annulé");if(bytes.size()+n>MAX_RADAR_COUNTRY_BYTES)throw new IOException("Base supérieure à 32 Mo; ancienne base conservée");bytes.write(buf,0,n);}}finally{c.disconnect();radarCountryConnection=null;}if(radarCountryDownloadCancel)throw new IOException("Téléchargement annulé");JSONObject root=new JSONObject(new String(bytes.toByteArray(),"UTF-8"));if(!"FeatureCollection".equals(root.optString("type"))||!country.equalsIgnoreCase(root.optString("region"))||!root.optString("licence","").toUpperCase(Locale.ROOT).contains("CC BY 4.0"))throw new IOException("Pays, format ou licence MapAtlas invalide");JSONArray features=root.optJSONArray("features");if(features==null||features.length()>100000)throw new IOException("Liste des points invalide ou trop volumineuse");int converted=0;try(BufferedWriter out=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(part),"UTF-8"))){out.write("id;type;lat;lon\n");for(int i=0;i<features.length();i++){if(radarCountryDownloadCancel)throw new IOException("Téléchargement annulé");JSONObject feature=features.optJSONObject(i),geometry=feature==null?null:feature.optJSONObject("geometry"),properties=feature==null?null:feature.optJSONObject("properties");if(geometry==null||properties==null||!"Point".equals(geometry.optString("type")))continue;JSONArray coords=geometry.optJSONArray("coordinates");if(coords==null||coords.length()<2)continue;double lon=coords.optDouble(0,Double.NaN),lat=coords.optDouble(1,Double.NaN);if(!Double.isFinite(lat)||!Double.isFinite(lon)||Math.abs(lat)>90||Math.abs(lon)>180)continue;String cls=properties.optString("class"),type;if("spot".equals(cls))type="Radar fixe (MapAtlas)";else if("signal".equals(cls))type="Radar feu rouge (MapAtlas)";else if("corridor".equals(cls))type="Radar vitesse moyenne (MapAtlas)";else if("adaptive".equals(cls))type="Radar variable (MapAtlas)";else continue;String place=properties.optString("name",properties.optString("ref","")).toLowerCase(Locale.ROOT);if(place.contains("tunnel")||place.contains("tunel"))type+=" · tunnel";String ref=properties.optString("ref","mapatlas-"+i).replaceAll("[^A-Za-z0-9_-]","_");out.write(ref+";"+type+";"+lat+";"+lon+"\n");if(++converted>100000)throw new IOException("Plus de 100 000 points");}}
      if(converted<1)throw new IOException("Aucun point radar reconnu dans la base MapAtlas");if(validateRadarCountryCsv(part)!=converted)throw new IOException("Validation interne de la base échouée");if(radarCountryDownloadCancel)throw new IOException("Téléchargement annulé");final int count=converted;String snapshot=root.optString("snapshot","");File target=new File(dir,id+".csv"),backup=new File(dir,id+".csv.previous");if(backup.exists())backup.delete();if(target.exists()&&!target.renameTo(backup))throw new IOException("Sauvegarde de l’ancienne base impossible");if(!part.renameTo(target)){if(backup.exists())backup.renameTo(target);throw new IOException("Installation impossible");}backup.delete();JSONArray installed=installedRadarCountries();for(int i=installed.length()-1;i>=0;i--)if(id.equals(installed.optJSONObject(i)==null?"":installed.optJSONObject(i).optString("id","")))installed.remove(i);JSONObject saved=new JSONObject();saved.put("id",id);saved.put("name",name+" · MapAtlas (communautaire)");saved.put("country",country);saved.put("file",id+".csv");saved.put("count",count);saved.put("attribution","MapAtlas Camera Index · MapAtlas, https://mapatlas.eu · https://mapatlas.eu/tools/speed-camera-map · GeoJSON converti en CSV par ROADGUARD");saved.put("license","CC BY 4.0");saved.put("snapshot",snapshot);saved.put("updated",System.currentTimeMillis());installed.put(saved);getPreferences(0).edit().putString("radar_country_installed",installed.toString()).apply();runOnUiThread(()->{progress.dismiss();requestRadarCatalogReload(true);BackgroundAlertService.requestRadarReload();Toast.makeText(this,name+" · MapAtlas : "+count+" points installés hors connexion",Toast.LENGTH_LONG).show();render();});
    }catch(Exception e){part.delete();radarCountryConnection=null;String msg=radarCountryDownloadCancel?"Téléchargement annulé":e.getMessage();runOnUiThread(()->{progress.dismiss();Toast.makeText(this,"Téléchargement MapAtlas échoué : "+msg+". La base précédente est conservée.",Toast.LENGTH_LONG).show();});}finally{radarCountryDownloadRunning=false;finishSwissRadarBatchSource();}});
  }
  String lufopPackId(String country){return "lufop_"+country.toLowerCase(Locale.ROOT);}
  void confirmLufopCountryDownload(String country,String name){String key=getPreferences(0).getString("lufop_api_key","");String chNote="CH".equals(country)?" La base suisse sera ajoutée à la carte et aux alertes GPS, selon tes réglages.":"";if(key.isEmpty()){new AlertDialog.Builder(this).setTitle("Télécharger "+name).setMessage("Téléchargement direct depuis OpenStreetMap, sans compte ni clé API. ROADGUARD récupère les radars fixes cartographiés dans ce pays et les conserve hors connexion. La couverture dépend des contributeurs : les radars mobiles ne sont pas garantis et la liste peut être incomplète."+chNote+" Attribution : © OpenStreetMap contributors · ODbL 1.0.").setNegativeButton("Fermer",null).setNeutralButton("Télécharger sans clé",(d,w)->downloadOsmCountry(country,name)).setPositiveButton("Configurer Lufop",(d,w)->configureLufopKey()).show();return;}new AlertDialog.Builder(this).setTitle("Télécharger "+name+" ?").setMessage("Choisis une source : OpenStreetMap (sans clé, couverture contributive des radars fixes) ou Lufop (nécessite une clé)."+chNote).setNegativeButton("Annuler",null).setNeutralButton("OpenStreetMap",(d,w)->downloadOsmCountry(country,name)).setPositiveButton("Lufop",(d,w)->downloadLufopCountry(country,name)).show();}
  void downloadOsmCountry(String country,String name){
    if(radarCountryDownloadRunning){Toast.makeText(this,"Un téléchargement radar est déjà en cours.",Toast.LENGTH_SHORT).show();return;}if(!country.matches("[A-Z]{2}"))return;
    radarCountryDownloadRunning=true;radarCountryDownloadCancel=false;String id=osmPackId(country);ProgressDialog progress=new ProgressDialog(this);progress.setTitle("Base radar · "+name);progress.setMessage("Recherche des radars fixes OpenStreetMap…");progress.setIndeterminate(true);progress.setCancelable(false);progress.setButton(ProgressDialog.BUTTON_NEGATIVE,"Annuler",(d,w)->{radarCountryDownloadCancel=true;if(swissRadarBatchActive&&"CH".equals(country))swissRadarBatchCancelled=true;java.net.HttpURLConnection c=radarCountryConnection;if(c!=null)c.disconnect();progress.setMessage("Annulation en cours…");});progress.show();
    addNetworkOperation("radar-osm-"+country,()->{File dir=new File(getFilesDir(),"radar_countries"),part=new File(getFilesDir(),"radar_countries/"+id+".csv.part");try{if(!dir.exists()&&!dir.mkdirs())throw new IOException("Stockage indisponible");String q="[out:json][timeout:75];area[\"ISO3166-1\"=\""+country+"\"][admin_level=2]->.country;node[\"highway\"=\"speed_camera\"](area.country);out body;";String[] endpoints=RadarSourcePolicy.isSwiss(country)?new String[]{"https://overpass.osm.ch/api/interpreter","https://overpass-api.de/api/interpreter","https://overpass.kumi.systems/api/interpreter","https://z.overpass-api.de/api/interpreter","https://maps.mail.ru/osm/tools/overpass/api/interpreter"}:new String[]{"https://overpass-api.de/api/interpreter","https://overpass.kumi.systems/api/interpreter","https://z.overpass-api.de/api/interpreter","https://maps.mail.ru/osm/tools/overpass/api/interpreter"};ByteArrayOutputStream bytes=null;IOException lastOverpassError=null;for(int attempt=0;attempt<endpoints.length;attempt++){if(radarCountryDownloadCancel)throw new IOException("Téléchargement annulé");java.net.HttpURLConnection c=null;try{c=(java.net.HttpURLConnection)new java.net.URL(endpoints[attempt]).openConnection();radarCountryConnection=c;c.setConnectTimeout(15000);c.setReadTimeout(90000);c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/x-www-form-urlencoded; charset=UTF-8");c.setRequestProperty("User-Agent","ROADGUARD Android radar download");byte[] body=("data="+java.net.URLEncoder.encode(q,"UTF-8")).getBytes("UTF-8");try(java.io.OutputStream out=c.getOutputStream()){out.write(body);}int code=c.getResponseCode();if(code!=200)throw new IOException("Overpass HTTP "+code);ByteArrayOutputStream response=new ByteArrayOutputStream();try(InputStream in=c.getInputStream()){byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1){if(radarCountryDownloadCancel)throw new IOException("Téléchargement annulé");if(response.size()+n>32*1024*1024)throw new IOException("Réponse trop volumineuse; la base précédente reste conservée");response.write(buf,0,n);}}bytes=response;lastOverpassError=null;break;}catch(IOException endpointError){lastOverpassError=endpointError;if(radarCountryDownloadCancel)throw new IOException("Téléchargement annulé");if(String.valueOf(endpointError.getMessage()).contains("Réponse trop volumineuse"))throw endpointError;if(attempt+1<endpoints.length){try{Thread.sleep(700L*(attempt+1));}catch(InterruptedException interrupted){Thread.currentThread().interrupt();throw new IOException("Téléchargement interrompu",interrupted);}}}finally{if(c!=null)c.disconnect();radarCountryConnection=null;}}if(bytes==null)throw new IOException("Serveurs Overpass temporairement indisponibles après "+endpoints.length+" essais : "+(lastOverpassError==null?"réessaie plus tard":lastOverpassError.getMessage()));if(radarCountryDownloadCancel)throw new IOException("Téléchargement annulé");JSONObject root=new JSONObject(new String(bytes.toByteArray(),"UTF-8"));JSONArray elements=root.optJSONArray("elements");if(elements==null)throw new IOException("Réponse OpenStreetMap invalide");int converted=0;try(BufferedWriter out=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(part),"UTF-8"))){out.write("id;type;lat;lon\n");for(int i=0;i<elements.length();i++){JSONObject element=elements.optJSONObject(i);if(element==null||!"node".equals(element.optString("type")))continue;JSONObject tags=element.optJSONObject("tags");if(tags==null||!"speed_camera".equals(tags.optString("highway")))continue;double lat=element.optDouble("lat",Double.NaN),lon=element.optDouble("lon",Double.NaN);if(!Double.isFinite(lat)||!Double.isFinite(lon)||Math.abs(lat)>90||Math.abs(lon)>180)continue;String radarType="yes".equalsIgnoreCase(tags.optString("tunnel",""))?"Radar fixe · tunnel (OSM)":"Radar fixe (OSM)";out.write("osm-"+element.optLong("id",i)+";"+radarType+";"+lat+";"+lon+"\n");if(++converted>100000)throw new IOException("Plus de 100 000 points");}}if(converted==0)throw new IOException("OpenStreetMap ne renvoie aucun radar fixe cartographié pour ce pays");if(validateRadarCountryCsv(part)!=converted)throw new IOException("Validation interne de la base échouée");if(radarCountryDownloadCancel)throw new IOException("Téléchargement annulé");final int installedCount=converted;File target=new File(dir,id+".csv"),backup=new File(dir,id+".csv.previous");if(backup.exists())backup.delete();if(target.exists()&&!target.renameTo(backup))throw new IOException("Sauvegarde de l’ancienne base impossible");if(!part.renameTo(target)){if(backup.exists())backup.renameTo(target);throw new IOException("Installation impossible");}backup.delete();JSONArray installed=installedRadarCountries();for(int i=installed.length()-1;i>=0;i--)if(id.equals(installed.optJSONObject(i)==null?"":installed.optJSONObject(i).optString("id","")))installed.remove(i);JSONObject saved=new JSONObject();saved.put("id",id);saved.put("name",name+" · OpenStreetMap");saved.put("country",country);saved.put("file",id+".csv");saved.put("count",installedCount);saved.put("attribution","© OpenStreetMap contributors · https://www.openstreetmap.org/copyright");saved.put("license","ODbL 1.0");saved.put("updated",System.currentTimeMillis());installed.put(saved);getPreferences(0).edit().putString("radar_country_installed",installed.toString()).apply();runOnUiThread(()->{progress.dismiss();requestRadarCatalogReload(true);BackgroundAlertService.requestRadarReload();Toast.makeText(this,name+" : "+installedCount+" radars fixes téléchargés",Toast.LENGTH_LONG).show();render();});
      }catch(Exception e){part.delete();radarCountryConnection=null;String msg=radarCountryDownloadCancel?"Téléchargement annulé":safeLufopError(e);runOnUiThread(()->{progress.dismiss();Toast.makeText(this,"Téléchargement OpenStreetMap échoué : "+msg,Toast.LENGTH_LONG).show();});}finally{radarCountryDownloadRunning=false;finishSwissRadarBatchSource();}
    });
  }
  void downloadLufopCountry(String country,String name){
    if(radarCountryDownloadRunning){Toast.makeText(this,"Un téléchargement radar est déjà en cours.",Toast.LENGTH_SHORT).show();return;}String key=getPreferences(0).getString("lufop_api_key","");if(key.isEmpty()){configureLufopKey();return;}if(!country.matches("[A-Z]{2}"))return;
    radarCountryDownloadRunning=true;radarCountryDownloadCancel=false;String id=lufopPackId(country);ProgressDialog progress=new ProgressDialog(this);progress.setTitle("Base radar · "+name);progress.setMessage("Demande sécurisée à Lufop…");progress.setIndeterminate(true);progress.setCancelable(false);progress.setButton(ProgressDialog.BUTTON_NEGATIVE,"Annuler",(d,w)->{radarCountryDownloadCancel=true;java.net.HttpURLConnection c=radarCountryConnection;if(c!=null)c.disconnect();progress.setMessage("Annulation en cours…");});progress.show();
    addNetworkOperation("radar-lufop-"+country,()->{File dir=new File(getFilesDir(),"radar_countries"),part=new File(getFilesDir(),"radar_countries/"+id+".csv.part");try{if(!dir.exists()&&!dir.mkdirs())throw new IOException("Stockage indisponible");String query="https://api.lufop.net/api?key="+java.net.URLEncoder.encode(key,"UTF-8")+"&format=json&nbr=30000&pays="+country.toLowerCase(Locale.ROOT);java.net.HttpURLConnection c=secureConnection(query);radarCountryConnection=c;ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(InputStream in=c.getInputStream()){byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1){if(radarCountryDownloadCancel)throw new IOException("Téléchargement annulé");if(bytes.size()+n>MAX_LUFOP_JSON_BYTES)throw new IOException("Réponse supérieure à 8 Mo; la base précédente reste active");bytes.write(buf,0,n);}}finally{c.disconnect();radarCountryConnection=null;}if(radarCountryDownloadCancel)throw new IOException("Téléchargement annulé");JSONArray records=new JSONArray(new String(bytes.toByteArray(),"UTF-8"));if(records.length()==0)throw new IOException("Lufop ne renvoie aucun radar pour ce pays ou ce forfait");if(records.length()>=30000)throw new IOException("La réponse atteint 30 000 résultats : base probablement incomplète.");if(records.length()==10000)throw new IOException("Lufop renvoie exactement 10 000 résultats, plafond Pro possible : base possiblement incomplète. Essaie un forfait permettant un export complet.");if(records.length()==200)throw new IOException("Lufop renvoie exactement 200 résultats, plafond du forfait Free possible : aucune base partielle installée. Il faut confirmer qu’un export complet est autorisé par ton forfait.");
        int converted=0;try(BufferedWriter out=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(part),"UTF-8"))){out.write("id;type;lat;lon\n");for(int i=0;i<records.length();i++){JSONObject item=records.optJSONObject(i);if(item==null)continue;double lat=item.optDouble("lat",Double.NaN),lon=item.optDouble("lng",Double.NaN);if(!Double.isFinite(lat)||!Double.isFinite(lon)||Math.abs(lat)>90||Math.abs(lon)>180)continue;String type=lufopRadarType(item.optString("name",""),item.optString("type",""));if(type==null)continue;String rid=item.optString("ID",country+"-"+i).replaceAll("[^A-Za-z0-9_-]","_");out.write(rid+";"+type+";"+lat+";"+lon+"\n");converted++;if(converted>100000)throw new IOException("Plus de 100 000 points");}}
        if(converted==0)throw new IOException("Aucun type de radar reconnu dans la réponse Lufop");if(validateRadarCountryCsv(part)!=converted)throw new IOException("Validation interne de la base échouée");if(radarCountryDownloadCancel)throw new IOException("Téléchargement annulé");final int installedCount=converted;File target=new File(dir,id+".csv"),backup=new File(dir,id+".csv.previous");if(backup.exists())backup.delete();if(target.exists()&&!target.renameTo(backup))throw new IOException("Sauvegarde de l’ancienne base impossible");if(!part.renameTo(target)){if(backup.exists())backup.renameTo(target);throw new IOException("Installation impossible");}backup.delete();JSONArray installed=installedRadarCountries();for(int i=installed.length()-1;i>=0;i--)if(id.equals(installed.optJSONObject(i)==null?"":installed.optJSONObject(i).optString("id","")))installed.remove(i);JSONObject saved=new JSONObject();saved.put("id",id);saved.put("name",name+" · Lufop");saved.put("country",country);saved.put("file",id+".csv");saved.put("count",installedCount);saved.put("attribution","Données Lufop et contributeurs · ODbL 1.0");saved.put("license","ODbL 1.0");saved.put("updated",System.currentTimeMillis());installed.put(saved);getPreferences(0).edit().putString("radar_country_installed",installed.toString()).apply();runOnUiThread(()->{progress.dismiss();requestRadarCatalogReload(true);BackgroundAlertService.requestRadarReload();Toast.makeText(this,name+" : "+installedCount+" radars installés hors connexion",Toast.LENGTH_LONG).show();render();});
      }catch(Exception e){part.delete();radarCountryConnection=null;String msg=radarCountryDownloadCancel?"Téléchargement annulé":safeLufopError(e);runOnUiThread(()->{progress.dismiss();Toast.makeText(this,"Téléchargement Lufop échoué : "+msg,Toast.LENGTH_LONG).show();});}finally{radarCountryDownloadRunning=false;}
    });
  }
  String safeLufopError(Exception e){String m=e==null?"erreur inconnue":e.getMessage();if(m==null||m.trim().isEmpty())return "réponse invalide ou clé/forfait refusé";return m.replaceAll("https?://[^\\s]+","[adresse masquée]").replaceAll("(?i)(key=)[^&\\s]+","$1[masquée]");}
  String lufopRadarType(String name,String code){String n=name.toLowerCase(Locale.ROOT);boolean tunnel=n.contains("tunnel")||n.contains("tunel");String type;if(n.contains("mobile")||n.contains("mobil")||n.contains("mobiel")||n.contains("blitzer")||"11".equals(code)||"12".equals(code))type="Radar mobile";else if(n.contains("feu rouge")||n.contains("red light")||n.contains("ampel")||n.contains("semaforo")||"22".equals(code))type="Radar feu rouge";else if(n.contains("vitesse moyenne")||n.contains("average")||n.contains("section control")||"41".equals(code)||"42".equals(code))type="Radar vitesse moyenne";else if(n.contains("chantier")||n.contains("construction")||"154".equals(code))type="Radar de chantier";else if(n.contains("radar")||n.contains("speed camera")||n.contains("caméra")||n.contains("camera")||n.contains("autovelox")||"13".equals(code)||"18".equals(code)||"33".equals(code))type="Radar fixe";else if("85".equals(code)||n.contains("level crossing")||n.contains("passage à niveau"))return "Passage à niveau";else return null;return tunnel?type+" · tunnel":type;}
  boolean isCurrentRadarPack(JSONObject pack){String id=pack==null?"":pack.optString("id","");JSONArray current=nationalRadarCatalog();for(int i=0;i<current.length();i++){JSONObject p=current.optJSONObject(i);if(p!=null&&id.equals(p.optString("id","")))return pack.optString("url","").equals(p.optString("url",""))&&pack.optLong("size",-1)==p.optLong("size",-2)&&pack.optString("sha256","").equalsIgnoreCase(p.optString("sha256",""))&&pack.optString("country","").equalsIgnoreCase(p.optString("country",""));}return false;}
  void confirmRadarCountryDownload(JSONObject pack){if(!isCurrentRadarPack(pack)){Toast.makeText(this,"Cette fiche a changé. Actualise la liste avant de télécharger.",Toast.LENGTH_LONG).show();return;}String name=pack.optString("name","ce pays"),country=pack.optString("country","").toUpperCase(Locale.ROOT);long size=pack.optLong("size",0);String note=country.startsWith("CH")?" La base suisse sera téléchargée, conservée hors connexion et utilisée sur la carte et dans les alertes GPS.":"";new AlertDialog.Builder(this).setTitle("Télécharger "+name+" ?").setMessage("Taille maximale : "+((size+1048575)/1048576)+" Mo. Source : "+pack.optString("attribution")+". Licence : "+pack.optString("license")+". Le fichier est vérifié avant son installation."+note).setNegativeButton("Annuler",null).setPositiveButton("Télécharger",(d,w)->downloadRadarCountry(pack)).show();}
  void downloadRadarCountry(JSONObject pack){
    String id=pack.optString("id"),name=pack.optString("name",id);if(!id.matches("[a-z0-9_-]{2,50}"))return;if(radarCountryDownloadRunning){Toast.makeText(this,"Un téléchargement radar est déjà en cours.",Toast.LENGTH_SHORT).show();return;}if(!isCurrentRadarPack(pack)){Toast.makeText(this,"Cette fiche n’est plus dans le catalogue actuel. Actualise la liste.",Toast.LENGTH_LONG).show();return;}radarCountryDownloadRunning=true;radarCountryDownloadCancel=false;
    final ProgressDialog progress=new ProgressDialog(this);progress.setTitle("Base radar · "+name);progress.setMessage("Connexion sécurisée…");progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);progress.setMax(1000);progress.setProgress(0);progress.setCancelable(false);progress.setButton(ProgressDialog.BUTTON_NEGATIVE,"Annuler",(dialog,which)->{radarCountryDownloadCancel=true;java.net.HttpURLConnection active=radarCountryConnection;if(active!=null)active.disconnect();progress.setMessage("Annulation en cours…");});progress.show();
    addNetworkOperation("radar-country-"+id,()->{File dir=new File(getFilesDir(),"radar_countries");if(!dir.exists()&&!dir.mkdirs()){radarCountryDownloadRunning=false;runOnUiThread(()->{progress.dismiss();Toast.makeText(this,"Stockage indisponible",Toast.LENGTH_LONG).show();});return;}File part=new File(dir,id+".csv.part"),target=new File(dir,id+".csv");
      try{if(!isCurrentRadarPack(pack))throw new IOException("Catalogue modifié avant le téléchargement");java.net.HttpURLConnection c=secureConnection(pack.getString("url"));radarCountryConnection=c;java.security.MessageDigest digest=java.security.MessageDigest.getInstance("SHA-256");long total=0,lastUi=0;final long expected=pack.optLong("size");try(InputStream raw=c.getInputStream();java.security.DigestInputStream in=new java.security.DigestInputStream(raw,digest);FileOutputStream out=new FileOutputStream(part)){byte[] buf=new byte[16384];int n;while((n=in.read(buf))!=-1){if(radarCountryDownloadCancel)throw new IOException("Téléchargement annulé");total+=n;if(total>expected||total>MAX_RADAR_COUNTRY_BYTES)throw new IOException("Fichier supérieur à la taille annoncée");out.write(buf,0,n);long now=System.currentTimeMillis();if(now-lastUi>=200){lastUi=now;final long downloaded=total;runOnUiThread(()->{if(progress.isShowing()){progress.setProgress((int)Math.min(1000,downloaded*1000/expected));progress.setMessage(String.format(Locale.FRANCE,"Téléchargement · %.1f / %.1f Mo",downloaded/1048576.0,expected/1048576.0));}});}}}finally{c.disconnect();radarCountryConnection=null;}
        if(radarCountryDownloadCancel)throw new IOException("Téléchargement annulé");
        StringBuilder hex=new StringBuilder();for(byte b:digest.digest())hex.append(String.format(Locale.ROOT,"%02x",b&255));if(total!=expected||!hex.toString().equalsIgnoreCase(pack.getString("sha256")))throw new IOException("Taille ou empreinte SHA-256 incorrecte");int count=validateRadarCountryCsv(part);if(count<1)throw new IOException("Aucun radar valide dans la base");if(radarCountryDownloadCancel)throw new IOException("Téléchargement annulé");if(!isCurrentRadarPack(pack))throw new IOException("Catalogue modifié pendant le téléchargement");File backup=new File(dir,id+".csv.previous");if(backup.exists())backup.delete();if(target.exists()&&!target.renameTo(backup))throw new IOException("Sauvegarde de l’ancienne base impossible");if(!part.renameTo(target)){if(backup.exists())backup.renameTo(target);throw new IOException("Installation impossible");}backup.delete();
        JSONArray installed=installedRadarCountries();for(int i=installed.length()-1;i>=0;i--)if(id.equals(installed.optJSONObject(i)==null?"":installed.optJSONObject(i).optString("id","")))installed.remove(i);JSONObject saved=new JSONObject();saved.put("id",id);saved.put("name",name);saved.put("country",pack.getString("country"));saved.put("file",id+".csv");saved.put("count",count);saved.put("attribution",pack.getString("attribution"));saved.put("license",pack.getString("license"));saved.put("updated",System.currentTimeMillis());installed.put(saved);getPreferences(0).edit().putString("radar_country_installed",installed.toString()).apply();
        radarCountryDownloadRunning=false;runOnUiThread(()->{progress.dismiss();requestRadarCatalogReload(true);BackgroundAlertService.requestRadarReload();Toast.makeText(this,name+" : "+count+" radars installés hors connexion",Toast.LENGTH_LONG).show();render();});
      }catch(Exception e){part.delete();radarCountryConnection=null;radarCountryDownloadRunning=false;String msg=radarCountryDownloadCancel?"Téléchargement annulé":e.getMessage();runOnUiThread(()->{progress.dismiss();if("Téléchargement annulé".equals(msg))Toast.makeText(this,"Téléchargement annulé. La base précédente est conservée.",Toast.LENGTH_LONG).show();else Toast.makeText(this,"Téléchargement radar échoué : "+msg,Toast.LENGTH_LONG).show();});}
    });
  }
  int validateRadarCountryCsv(File file)throws IOException{int count=0,lines=0;try(BufferedReader r=new BufferedReader(new InputStreamReader(new FileInputStream(file),"UTF-8"))){String header=r.readLine();if(!"id;type;lat;lon".equalsIgnoreCase(header==null?"":header.trim()))throw new IOException("Format CSV attendu : id;type;lat;lon");String line;while((line=r.readLine())!=null){if(++lines>100000)throw new IOException("Plus de 100 000 lignes");String[] c=line.split(";",-1);if(c.length<4)continue;try{double lat=Double.parseDouble(c[2].trim()),lon=Double.parseDouble(c[3].trim());String type=c[1].trim();if(Double.isFinite(lat)&&Double.isFinite(lon)&&Math.abs(lat)<=90&&Math.abs(lon)<=180&&DangerTypePolicy.isRadar(type))count++;}catch(NumberFormatException ignored){}}}return count;}
  void removeRadarCountry(String id){JSONArray installed=installedRadarCountries();for(int i=installed.length()-1;i>=0;i--){JSONObject item=installed.optJSONObject(i);if(item!=null&&id.equals(item.optString("id",""))){new File(new File(getFilesDir(),"radar_countries"),item.optString("file",id+".csv")).delete();installed.remove(i);}}getPreferences(0).edit().putString("radar_country_installed",installed.toString()).apply();requestRadarCatalogReload(true);BackgroundAlertService.requestRadarReload();render();}
  volatile boolean mapDownloadRunning=false;
  volatile boolean mapDownloadCancel=false;
  static final String PUBLIC_FRANCE_PMTILES_URL="https://map.comptoir.net/france-openmaptiles.pmtiles";
  // Source checked on 2026-10-22: pin this publication until a new one is reviewed.
  static final long PUBLIC_FRANCE_PMTILES_BYTES=2898478070L;
  static final String PUBLIC_FRANCE_PMTILES_ETAG="\"67681377-acc343f6\"";
  static final long MAX_PMTILES_DOWNLOAD_BYTES=8L*1024*1024*1024; // 8 Gio; flux sur disque, compteurs long

  // Téléchargement manuel d’une région explicitement présente dans le catalogue.
  // Sans catalogue autorisé, ne simule jamais un téléchargement de France.
  // Only download explicitly authorized, integrity-checked region packs.
  // Never bulk-download tiles from the public OSM tile server: its usage policy
  // does not permit offline prefetching. This UI is intentionally honest when
  // no authorized regional source has been configured.
  void chooseOfflineArea(){
    restoreMapCatalogIfEmpty();
    final java.util.ArrayList<JSONObject> candidates=new java.util.ArrayList<>();
    final java.util.ArrayList<String> labels=new java.util.ArrayList<>();
    for(int i=0;i<mapCatalog.length();i++){
      JSONObject pack=mapCatalog.optJSONObject(i);
      if(pack==null)continue;
      String scope=pack.optString("scope","").toLowerCase(java.util.Locale.ROOT);
      if(!("region".equals(scope)||"department".equals(scope)||"country".equals(scope)))continue;
      try{
        java.net.URL url=new java.net.URL(pack.getString("url"));
        String hash=pack.getString("sha256");
        long size=pack.getLong("size");
        String format=pack.optString("format","");
        if(!"https".equalsIgnoreCase(url.getProtocol())||url.getUserInfo()!=null
          || !hash.matches("[0-9a-fA-F]{64}")||size<=0
          || !("pmtiles".equals(format)||"xyz-zip".equals(format))
          || size>("pmtiles".equals(format)?MAX_PMTILES_DOWNLOAD_BYTES:600L*1024*1024))continue;
        candidates.add(pack);
        labels.add(pack.optString("name",pack.optString("id","Carte"))
          +" · "+scope+" · "+String.format(java.util.Locale.FRANCE,"%.1f",size/1048576.0)+" Mio");
      }catch(Exception invalid){android.util.Log.w("ROADGUARD","Pack régional ignoré",invalid);}
    }
    if(candidates.isEmpty()){
      new AlertDialog.Builder(this).setTitle("Téléchargement de zones")
        .setMessage("La carte en ligne reste accessible depuis l’onglet Carte. Pour enregistrer une région, ROADGUARD a besoin d’un fournisseur autorisant explicitement le téléchargement hors ligne. Aucun pack régional vérifié n’est disponible pour le moment. Les tuiles publiques OpenStreetMap ne peuvent pas être aspirées en masse. Aucune donnée existante n’a été effacée.")
        .setPositiveButton("Afficher la carte en ligne",(d,w)->{offlineView=false;selected="Carte";render();})
        .setNegativeButton("Fermer",null).show();
      return;
    }
    new AlertDialog.Builder(this).setTitle("Choisir une carte à enregistrer")
      .setItems(labels.toArray(new String[0]),(d,which)->confirmMapDownload(candidates.get(which)))
      .setNegativeButton("Annuler",null).show();
  }

  void downloadFranceOneTap(){
    downloadPublicFranceCandidate();
  }

  JSONObject publicFrancePack(long size){
    JSONObject pack=new JSONObject();
    try{pack.put("id","france_openmaptiles");pack.put("name","France complète · métropole et Corse");
      pack.put("country","france");pack.put("country_code","FR");pack.put("scope","country");
      pack.put("format","pmtiles");pack.put("url",PUBLIC_FRANCE_PMTILES_URL);
      pack.put("size",size);pack.put("public_source",true);pack.put("source_etag",PUBLIC_FRANCE_PMTILES_ETAG);
    }catch(org.json.JSONException impossible){throw new IllegalStateException(impossible);}
    return pack;
  }

  // Candidate pack public: no ROADGUARD catalog/server is required. The fixed
  // HTTPS origin, file size, PMTiles structure and France+Corsica bounds are
  // checked before installation; SHA-256 is computed and retained locally.
  void downloadPublicFranceCandidate(){
    File existing=new File(new File(getFilesDir(),"pmtiles"),"france_openmaptiles.pmtiles");
    if(existing.isFile())try{
      if(new PmtilesReader(existing).isVector()&&pmtilesCoversFranceAndCorsica(existing)){
        showDownloadedMapOptions(publicFrancePack(existing.length()));return;
      }
    }catch(IOException invalid){android.util.Log.w("ROADGUARD","France complète locale non valide",invalid);}
    if(getPreferences(0).getBoolean("maps_wifi_only",false)&&!isWifiForMapDownload()){
      new AlertDialog.Builder(this).setTitle("Wi-Fi nécessaire").setMessage("Connecte-toi au Wi-Fi pour vérifier et télécharger le pack France.").setPositiveButton("OK",null).show();return;
    }
    if(MapPackDownloadService.isRunning()){downloadPmtilesPack(publicFrancePack(PUBLIC_FRANCE_PMTILES_BYTES));return;}
    Toast.makeText(this,"Vérification du pack France public…",Toast.LENGTH_LONG).show();
    new Thread(()->{
      try{
        java.net.URL u=new java.net.URL(PUBLIC_FRANCE_PMTILES_URL);
        if(!"https".equalsIgnoreCase(u.getProtocol())||!"map.comptoir.net".equalsIgnoreCase(u.getHost()))throw new IOException("Source publique inattendue");
        long size=probePublicPmtilesSize(u);
        if(size!=PUBLIC_FRANCE_PMTILES_BYTES)throw new IOException("La publication a changé : vérifier la nouvelle archive avant téléchargement");
        checkRemotePmtilesHeader(u,size);
        if(size+128L*1024*1024>getFilesDir().getUsableSpace())throw new IOException("Espace libre insuffisant pour le pack et 128 Mio de réserve");
        JSONObject pack=publicFrancePack(size);
        runOnUiThread(()->new AlertDialog.Builder(this).setTitle("Pack France public")
          .setMessage("France métropolitaine et Corse, y compris le Nord-Est.\nSource : map.comptoir.net · OpenMapTiles · publication du 22/12/2024.\nTaille : "+(size/(1024*1024))+" Mio.\n"+(isWifiForMapDownload()?"\n":"\nRéseau mobile : ce téléchargement utilisera ton forfait de données.\n")+"Le téléchargement reprend après une coupure. L’archive et plusieurs tuiles seront contrôlées avant installation.")
          .setNegativeButton("Annuler",null).setPositiveButton("Télécharger",(d,w)->downloadPmtilesPack(pack)).show());
      }catch(Exception e){String msg=e.getMessage();runOnUiThread(()->new AlertDialog.Builder(this).setTitle("Pack France indisponible").setMessage("La source publique n'a pas pu être vérifiée : "+msg+". Réessaie plus tard ou utilise un catalogue HTTPS configuré.").setPositiveButton("OK",null).show());}
    },"roadguard-france-source-check").start();
  }
  static long probePublicPmtilesSize(java.net.URL u) throws IOException{
    java.net.HttpURLConnection c=(java.net.HttpURLConnection)u.openConnection();c.setRequestMethod("HEAD");c.setConnectTimeout(15000);c.setReadTimeout(30000);c.setInstanceFollowRedirects(false);
    try{int code=c.getResponseCode();long size=c.getContentLengthLong();if(code==200&&size>=127){
      if(!PUBLIC_FRANCE_PMTILES_ETAG.equals(c.getHeaderField("ETag")))throw new IOException("Publication France modifiée : vérification nécessaire");
      return size;
    }}finally{c.disconnect();}
    c=(java.net.HttpURLConnection)u.openConnection();c.setRequestProperty("Range","bytes=0-0");c.setConnectTimeout(15000);c.setReadTimeout(30000);c.setInstanceFollowRedirects(false);
    try{if(c.getResponseCode()!=206)throw new IOException("La source ne fournit ni HEAD valide ni plage HTTP");if(!PUBLIC_FRANCE_PMTILES_ETAG.equals(c.getHeaderField("ETag")))throw new IOException("Publication France modifiée : vérification nécessaire");String range=c.getHeaderField("Content-Range");if(range==null||!range.matches("bytes 0-0/[0-9]+"))throw new IOException("Taille totale absente des en-têtes HTTP");return Long.parseLong(range.substring(range.indexOf('/')+1));}
    catch(NumberFormatException ex){throw new IOException("Taille HTTP invalide",ex);}finally{c.disconnect();}
  }

  // Pays visibles même sans réseau. Le catalogue signé par son hébergeur reste
  // la seule source de packs réellement téléchargeables : aucune URL inventée.
  static final String[] MAP_COUNTRY_NAMES={"🇫🇷 France","🇨🇭 Suisse","🇩🇪 Allemagne","🇧🇪 Belgique","🇮🇹 Italie","🇪🇸 Espagne","🇱🇺 Luxembourg","🇳🇱 Pays-Bas","🇦🇹 Autriche","🇵🇹 Portugal","🇬🇧 Royaume-Uni","🇮🇪 Irlande","🇵🇱 Pologne","🇨🇿 Tchéquie","🇩🇰 Danemark","🇸🇪 Suède","🇳🇴 Norvège","🇫🇮 Finlande","🇬🇷 Grèce","🇭🇷 Croatie","🇸🇮 Slovénie","🇭🇺 Hongrie","🇷🇴 Roumanie","🇧🇬 Bulgarie","🇺🇸 États-Unis","🇨🇦 Canada","🇯🇵 Japon","🇲🇦 Maroc"};
  static final String[] MAP_COUNTRY_IDS={"france","switzerland","germany","belgium","italy","spain","luxembourg","netherlands","austria","portugal","united_kingdom","ireland","poland","czechia","denmark","sweden","norway","finland","greece","croatia","slovenia","hungary","romania","bulgaria","united_states","canada","japan","morocco"};
  void afterCountryDialogDismiss(Runnable action){
    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(()->{
      if(isFinishing()||isDestroyed())return;
      try{action.run();}catch(RuntimeException e){android.util.Log.e("ROADGUARD","Country map action failed",e);Toast.makeText(this,"Impossible d’ouvrir les options de carte : "+e.getClass().getSimpleName(),Toast.LENGTH_LONG).show();}
    },250);
  }
  void chooseCountryMap(){
    new AlertDialog.Builder(this).setTitle("Choisir un pays")
      .setItems(MAP_COUNTRY_NAMES,(dialog,which)->{String id=MAP_COUNTRY_IDS[which],name=MAP_COUNTRY_NAMES[which];dialog.dismiss();afterCountryDialogDismiss(()->downloadCountryOneTap(id,name));})
      .setNeutralButton("Rechercher",(dialog,which)->{dialog.dismiss();afterCountryDialogDismiss(this::searchCountryMap);})
      .setNegativeButton("Annuler",null).show();
  }
  void searchCountryMap(){
    EditText search=new EditText(this);search.setSingleLine(true);search.setHint("Pays, ex. Suisse ou Canada");
    new AlertDialog.Builder(this).setTitle("Rechercher une carte mondiale").setView(search)
      .setNegativeButton("Annuler",null).setPositiveButton("Rechercher",(dialog,which)->{
        String query=search.getText().toString().trim();dialog.dismiss();afterCountryDialogDismiss(()->handleCountrySearch(query));
      }).show();
  }
  void handleCountrySearch(String rawQuery){
    String query=java.text.Normalizer.normalize(rawQuery,java.text.Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT);
    if(query.isEmpty()){chooseCountryMap();return;}
    restoreMapCatalogIfEmpty();
    for(int i=0;i<MAP_COUNTRY_NAMES.length;i++){
      String normalized=java.text.Normalizer.normalize(MAP_COUNTRY_NAMES[i],java.text.Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT);
      if(normalized.contains(query)||MAP_COUNTRY_IDS[i].contains(query)){downloadCountryOneTap(MAP_COUNTRY_IDS[i],MAP_COUNTRY_NAMES[i]);return;}
    }
    for(int i=0;i<mapCatalog.length();i++){
      JSONObject pack=mapCatalog.optJSONObject(i);
      if(pack!=null&&(pack.optString("name").toLowerCase(Locale.ROOT).contains(query)||pack.optString("id").toLowerCase(Locale.ROOT).contains(query))){confirmMapDownload(pack);return;}
    }
    new AlertDialog.Builder(this).setTitle("Pays non trouvé").setMessage("Aucun pays correspondant. Un catalogue mondial hébergé devra fournir les autres pays et régions.").setPositiveButton("OK",null).show();
  }
  void downloadCountryOneTap(String id,String displayName){
    if("france".equals(id)){downloadFranceOneTap();return;}
    Toast.makeText(this,"Téléchargement de cartes permanentes supprimé : cache GPS temporaire uniquement",Toast.LENGTH_LONG).show();
    if(true)return; // Fonction historique désactivée en 11.14.99; aucune donnée existante supprimée.

    restoreMapCatalogIfEmpty();
    for(int i=0;i<mapCatalog.length();i++){
      JSONObject pack=mapCatalog.optJSONObject(i);if(pack==null)continue;
      String packId=pack.optString("id","").toLowerCase(Locale.ROOT);
      String country=pack.optString("country","").toLowerCase(Locale.ROOT);
      // France must be a declared full-country pack; do not select e.g. france_jura.
      // Keep regional selection behavior for other countries in their country menu.
      boolean fullFrance="france".equals(id)&&(packId.equals("france")||packId.equals("france_metropolitaine")||packId.equals("france_metropolitaine_corse")||(country.equals("france")&&"country".equalsIgnoreCase(pack.optString("scope",""))));
      if("france".equals(id)?fullFrance:(packId.equals(id)||packId.startsWith(id+"_")||country.equals(id))){
        if(!pack.optString("sha256","").matches("[a-fA-F0-9]{64}")||pack.optLong("size")<=0||!pack.optString("url","").startsWith("https://")){
          new AlertDialog.Builder(this).setTitle("Carte non vérifiée").setMessage("Le catalogue contient un pack invalide pour "+displayName+".").setPositiveButton("OK",null).show();return;
        }
        confirmMapDownload(pack);return;
      }
    }
    if("france".equals(id)){downloadFranceOneTap();return;}
    new AlertDialog.Builder(this).setTitle(displayName+" · indisponible")
      .setMessage("Aucun pack compatible pour ce pays n'est encore publié dans le catalogue configuré. ROADGUARD n'invente pas de lien et ne télécharge pas les serveurs de tuiles publics.\n\nIl faut héberger un pack autorisé et ajouter sa fiche au catalogue HTTPS.")
      .setPositiveButton("Compris",null).show();
  }

  // Installation directe d'autres fournisseurs : l'utilisateur doit fournir URL, taille et SHA-256.
  void configureDirectMapDownload(){
    Toast.makeText(this,"Téléchargement de cartes permanentes supprimé : cache GPS temporaire uniquement",Toast.LENGTH_LONG).show();
    if(true)return; // Fonction historique désactivée en 11.14.99; aucune donnée existante supprimée.

    LinearLayout fields=column();pad(fields,18);
    EditText name=new EditText(this);name.setSingleLine(true);name.setHint("Nom de la carte (ex. Jura)");add(fields,name);
    EditText url=new EditText(this);url.setSingleLine(true);url.setHint("https://.../carte.pmtiles ou .zip");
    url.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_URI);add(fields,url);
    EditText sha=new EditText(this);sha.setSingleLine(true);sha.setHint("Empreinte SHA-256 (64 caractères)");add(fields,sha);
    EditText mb=new EditText(this);mb.setSingleLine(true);mb.setHint("Taille exacte en octets (fournie par l’hébergeur)");
    mb.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);add(fields,mb);
    new AlertDialog.Builder(this).setTitle("Télécharger une carte autorisée")
      .setMessage("Saisis les informations fournies par ton hébergeur de cartes. ROADGUARD vérifiera l'intégrité avant installation. Les fichiers .osm.pbf bruts ne sont pas compatibles.")
      .setView(fields).setNegativeButton("Annuler",null)
      .setPositiveButton("Vérifier",(d,w)->{
        try{
          String link=url.getText().toString().trim();
          java.net.URL parsed=new java.net.URL(link);
          if(!"https".equalsIgnoreCase(parsed.getProtocol())||parsed.getUserInfo()!=null)throw new IOException("Lien HTTPS obligatoire");
          String hash=sha.getText().toString().trim();
          if(!hash.matches("[a-fA-F0-9]{64}"))throw new IOException("SHA-256 invalide");
          String display=name.getText().toString().trim();
          if(display.isEmpty()||display.length()>80)throw new IOException("Nom requis (80 caractères maximum)");
          long bytes=Long.parseLong(mb.getText().toString().trim());
          if(bytes<1||bytes>600L*1024*1024)throw new IOException("Taille autorisée : 1 octet à 600 Mo");
          String format=parsed.getPath().toLowerCase(java.util.Locale.ROOT).endsWith(".pmtiles")?"pmtiles":parsed.getPath().toLowerCase(java.util.Locale.ROOT).endsWith(".zip")?"xyz-zip":"";
          if(format.isEmpty())throw new IOException("Fichier .pmtiles ou .zip requis");
          JSONObject pack=new JSONObject();
          pack.put("id","direct_"+hash.substring(0,20).toLowerCase(java.util.Locale.ROOT));
          pack.put("name",display);pack.put("url",link);pack.put("sha256",hash);
          pack.put("size",bytes);pack.put("format",format);
          confirmMapDownload(pack);
        }catch(Exception e){new AlertDialog.Builder(this).setTitle("Informations incorrectes").setMessage(e.getMessage()).setPositiveButton("OK",null).show();}
      }).show();
  }

  void configureMapCatalog(){
    Toast.makeText(this,"Téléchargement de cartes permanentes supprimé : cache GPS temporaire uniquement",Toast.LENGTH_LONG).show();
    if(true)return; // Fonction historique désactivée en 11.14.99; aucune donnée existante supprimée.

    EditText field=new EditText(this);field.setSingleLine(true);field.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_URI);
    field.setText(getPreferences(0).getString("map_catalog_url",""));
    new AlertDialog.Builder(this).setTitle("Catalogue HTTPS de cartes")
      .setMessage("Adresse HTTPS du fichier catalogue JSON fourni par un hébergeur de cartes autorisé.")
      .setView(field).setNegativeButton("Annuler",null).setPositiveButton("Enregistrer",(d,w)->{
        String url=field.getText().toString().trim();
        if(!url.isEmpty()&&!url.startsWith("https://")){Toast.makeText(this,"Adresse HTTPS obligatoire",Toast.LENGTH_LONG).show();return;}
        getPreferences(0).edit().putString("map_catalog_url",url).remove("map_catalog_cache").apply();mapCatalog=new JSONArray();refreshMapCatalog();
      }).show();
  }
  void restoreMapCatalogIfEmpty(){
    if(mapCatalog.length()>0)return;
    try(InputStream in=getAssets().open("offline_map_catalog.json")){
      ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;
      while((n=in.read(buffer))!=-1){if(out.size()+n>262144)throw new IOException("Catalogue trop volumineux");out.write(buffer,0,n);}
      JSONObject json=new JSONObject(out.toString("UTF-8"));
      if(json.getInt("schema")!=1)throw new IOException("Catalogue inconnu");
      JSONArray entries=json.getJSONArray("packs");
      if(entries.length()>100)throw new IOException("Trop de cartes");
      java.util.HashSet<String> ids=new java.util.HashSet<>();
      for(int i=0;i<entries.length();i++){
        JSONObject entry=entries.getJSONObject(i);String id=entry.getString("id"),link=entry.getString("url");
        if(!id.matches("[a-z0-9_-]{2,50}")||!ids.add(id)||!entry.getString("sha256").matches("[a-f0-9]{64}")
          ||entry.getLong("size")<127||entry.getLong("size")>MAX_PMTILES_DOWNLOAD_BYTES
          ||!"pmtiles".equals(entry.getString("format"))
          ||!link.equals("https://github.com/p-souto/project-nomad-extra-maps/releases/download/v1/"+id+".pmtiles"))
          throw new IOException("Carte incorrecte : "+id);
      }
      mapCatalog=entries;
    }catch(Exception error){android.util.Log.e("ROADGUARD","Catalogue de cartes inaccessible",error);mapCatalog=new JSONArray();}
  }

  String mapPackFlag(JSONObject pack){
    String code=pack.optString("country_code","").toUpperCase(Locale.ROOT);
    if(!code.matches("[A-Z]{2}")){
      String id=pack.optString("id","");
      // Older cached catalogues have no country_code field.
      switch(id){
        case "andorra":
          code="AD";break;
        case "albania":
        case "tirana":
          code="AL";break;
        case "austria":
        case "vienna":
          code="AT";break;
        case "bosnia":
        case "sarajevo":
          code="BA";break;
        case "belgium":
        case "brussels":
          code="BE";break;
        case "bulgaria":
        case "sofia":
          code="BG";break;
        case "switzerland":
          code="CH";break;
        case "cyprus":
        case "nicosia":
          code="CY";break;
        case "czechia":
        case "prague":
          code="CZ";break;
        case "berlin":
        case "germany_northeast":
        case "germany_northwest":
        case "germany_southeast":
        case "germany_southwest":
          code="DE";break;
        case "copenhagen":
        case "denmark":
          code="DK";break;
        case "estonia":
        case "tallinn":
          code="EE";break;
        case "madrid":
        case "spain":
          code="ES";break;
        case "finland":
        case "helsinki":
          code="FI";break;
        case "france_northeast":
        case "france_northwest":
        case "france_southeast":
        case "france_southwest":
        case "paris":
          code="FR";break;
        case "london":
        case "uk":
          code="GB";break;
        case "athens":
        case "greece":
          code="GR";break;
        case "croatia":
        case "zagreb":
          code="HR";break;
        case "budapest":
        case "hungary":
          code="HU";break;
        case "dublin":
        case "ireland":
          code="IE";break;
        case "iceland":
        case "reykjavik":
          code="IS";break;
        case "italy_north":
        case "italy_south":
        case "rome":
          code="IT";break;
        case "liechtenstein":
          code="LI";break;
        case "lithuania":
        case "vilnius":
          code="LT";break;
        case "luxembourg":
        case "luxembourg_city":
          code="LU";break;
        case "latvia":
        case "riga":
          code="LV";break;
        case "montenegro":
        case "podgorica":
          code="ME";break;
        case "north_macedonia":
        case "skopje":
          code="MK";break;
        case "malta":
        case "valletta":
          code="MT";break;
        case "amsterdam":
        case "netherlands":
          code="NL";break;
        case "norway_north":
        case "norway_south_20260331":
        case "oslo":
          code="NO";break;
        case "poland_east":
        case "poland_west":
        case "warsaw":
          code="PL";break;
        case "lisbon":
        case "porto":
        case "portugal":
          code="PT";break;
        case "bucharest":
        case "romania_north":
        case "romania_south":
          code="RO";break;
        case "belgrade":
        case "serbia":
          code="RS";break;
        case "moscow":
        case "russia_central":
        case "russia_northwest":
        case "saint_petersburg":
          code="RU";break;
        case "stockholm":
        case "sweden_north":
        case "sweden_south":
          code="SE";break;
        case "ljubljana":
        case "slovenia":
          code="SI";break;
        case "bratislava":
        case "slovakia":
          code="SK";break;
        case "turkey_europe":
          code="TR";break;
        case "ukraine_east":
        case "ukraine_west":
          code="UA";break;
        case "kosovo":
          code="XK";break;
        default:return "🌐";
      }
    }
    int first=0x1f1e6+code.charAt(0)-'A',second=0x1f1e6+code.charAt(1)-'A';
    return new String(Character.toChars(first))+new String(Character.toChars(second));
  }
  // GitHub's pinned digest authenticates the published bytes, including a
  // damaged publication. This particular release asset has a zeroed header.
  String unavailableMapPack(JSONObject pack){
    if("invalid_source".equals(pack.optString("availability")))return "Source invalide · archive publiée sans en-tête PMTiles";
    if("france_northeast".equals(pack.optString("id"))
      &&"a59c5bcd94e402b4f4617ce39538b206fe57e42fa0a5a6a5f855d216c93a4c35".equalsIgnoreCase(pack.optString("sha256")))
      return "Source invalide · archive publiée sans en-tête PMTiles";
    return "";
  }
  void showOfflineMapDownloads(){
    restoreMapCatalogIfEmpty();
    int availableCount=0;for(int i=0;i<mapCatalog.length();i++){JSONObject item=mapCatalog.optJSONObject(i);if(item!=null&&!oldFrenchRegion(item.optString("id"))&&unavailableMapPack(item).isEmpty())availableCount++;}
    LinearLayout panel=column();panel.setPadding(pxDp(12),pxDp(14),pxDp(12),pxDp(14));panel.setBackgroundColor(0xff070d14);
    LinearLayout header=row();TextView title=label("◉  Télécharger les cartes",23);header.addView(title,new LinearLayout.LayoutParams(0,-2,1));
    TextView close=button("✕",()->{});close.setBackground(outlined(0xff161e27,0xfff72b51,12));header.addView(close,new LinearLayout.LayoutParams(pxDp(55),pxDp(48)));add(panel,header);
    TextView intro=label("Choisis un pays. Les cartes installées se rejoignent sur la même carte, même sans réseau. Les téléchargements continuent en arrière-plan.",13);intro.setTextColor(0xffacb9c8);add(panel,intro);
    File national=new File(new File(getFilesDir(),"pmtiles"),"france_openmaptiles.pmtiles");
    boolean nationalInstalled=national.isFile();
    if(nationalInstalled){long freed=MapPackDownloadService.pruneOldFrenchRegions(this);if(freed>0)invalidateInstalledMosaic();}
    TextView france=button(nationalInstalled?"🇫🇷  France complète · installée · voir":"🇫🇷  France complète · télécharger · 2,70 Gio",()->downloadFranceOneTap());
    france.setBackground(premiumPanel(nationalInstalled?0xff166b41:0xffb91737,nationalInstalled?0xff104c38:0xff610d25,nationalInstalled?0xff74f4a6:0xffff5a75,16));
    LinearLayout.LayoutParams franceLp=new LinearLayout.LayoutParams(-1,pxDp(54));franceLp.topMargin=pxDp(12);panel.addView(france,franceLp);
    if(MapPackDownloadService.isRunning()){TextView transfer=label("↧  "+MapPackDownloadService.status(this)+" · suivi dans les notifications",12);transfer.setTextColor(0xff80e7b5);transfer.setPadding(pxDp(9),pxDp(8),pxDp(9),pxDp(8));add(panel,transfer);}
    EditText search=new EditText(this);search.setSingleLine(true);search.setTextColor(Color.WHITE);search.setHintTextColor(0xff91a0b1);
    search.setHint("⌕  Rechercher parmi "+(availableCount+1)+" cartes…");search.setBackground(outlined(0xff101a24,0xff526779,12));
    search.setPadding(pxDp(12),0,pxDp(12),0);LinearLayout.LayoutParams searchLp=new LinearLayout.LayoutParams(-1,pxDp(52));searchLp.setMargins(0,pxDp(14),0,pxDp(10));panel.addView(search,searchLp);
    final int[] mapFilter={0};
    LinearLayout filters=row();TextView[] filterButtons=new TextView[]{button("Toutes",()->{}),button("Installées",()->{}),button("À télécharger",()->{})};
    for(TextView filterButton:filterButtons){filterButton.setTextSize(12);filterButton.setPadding(pxDp(4),pxDp(7),pxDp(4),pxDp(7));LinearLayout.LayoutParams filterLp=new LinearLayout.LayoutParams(0,pxDp(44),1);filterLp.setMargins(pxDp(2),0,pxDp(2),pxDp(9));filters.addView(filterButton,filterLp);}
    add(panel,filters);
    ScrollView scroll=new ScrollView(this);scroll.setFillViewport(false);LinearLayout list=column();scroll.addView(list);
    panel.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    TextView info=label("Vert : installée   ·   Rouge : télécharger   ·   Gris : source indisponible",12);info.setTextColor(0xffb4bfca);info.setPadding(0,pxDp(8),0,pxDp(8));add(panel,info);
    TextView finish=button("Fermer",()->{});finish.setBackground(outlined(0xff18232e,0xfff72b51,12));add(panel,finish);
    final android.app.Dialog dialog=new android.app.Dialog(this);
    dialog.setContentView(panel,new android.view.ViewGroup.LayoutParams(-1,-1));
    close.setOnClickListener(v->dialog.dismiss());finish.setOnClickListener(v->dialog.dismiss());
    france.setOnClickListener(v->{dialog.dismiss();downloadFranceOneTap();});
    Runnable populate=()->{
      list.removeAllViews();String filter=java.text.Normalizer.normalize(search.getText().toString(),java.text.Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT);
      for(int i=0;i<mapCatalog.length();i++){
        JSONObject pack=mapCatalog.optJSONObject(i);if(pack==null)continue;
        String id=pack.optString("id"),name=pack.optString("name",id);
        if(oldFrenchRegion(id))continue;
        String normalized=java.text.Normalizer.normalize(name+" "+id,java.text.Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT);
        if(!normalized.contains(filter))continue;
        boolean installed=new File(new File(getFilesDir(),"pmtiles"),id+".pmtiles").isFile();
        if(mapFilter[0]==1&&!installed||mapFilter[0]==2&&installed)continue;
        String unavailable=installed?"":unavailableMapPack(pack);
        LinearLayout line=row();line.setGravity(Gravity.CENTER_VERTICAL);line.setPadding(pxDp(12),pxDp(11),pxDp(10),pxDp(11));line.setBackground(premiumPanel(0xff1b2934,0xff101923,0xff344d59,15));
        LinearLayout.LayoutParams item=new LinearLayout.LayoutParams(-1,-2);item.bottomMargin=pxDp(7);list.addView(line,item);
        TextView flag=label(mapPackFlag(pack),26);line.addView(flag,new LinearLayout.LayoutParams(pxDp(46),-2));
        LinearLayout details=column();line.addView(details,new LinearLayout.LayoutParams(0,-2,1));
        TextView nameView=label(name,16);nameView.setMaxLines(2);add(details,nameView);
        TextView state=label(installed?"Disponible hors connexion · "+(pack.optLong("size")/1048576)+" Mio":unavailable.isEmpty()?"À télécharger · "+(pack.optLong("size")/1048576)+" Mio":unavailable,11);
        state.setMaxLines(2);state.setTextColor(installed?0xff65e994:unavailable.isEmpty()?0xffaebac8:0xffffb347);add(details,state);
        TextView action=button(installed?"✓ Voir":unavailable.isEmpty()?"↓ Télécharger":"Indisponible",()->{dialog.dismiss();if(installed)showDownloadedMapOptions(pack);else confirmMapDownload(pack);});
        action.setEnabled(unavailable.isEmpty());action.setTextSize(12);action.setBackground(outlined(installed?0xff176d3e:unavailable.isEmpty()?0xffad0b29:0xff36414b,installed?0xff6af0a0:unavailable.isEmpty()?0xfffb365a:0xff56616c,10));
        line.addView(action,new LinearLayout.LayoutParams(pxDp(124),pxDp(48)));
      }
    };
    for(int i=0;i<filterButtons.length;i++){final int index=i;filterButtons[i].setOnClickListener(v->{mapFilter[0]=index;for(int j=0;j<filterButtons.length;j++)filterButtons[j].setBackground(outlined(j==index?0xff8e1530:0xff18232e,j==index?0xffff6078:0xff526779,12));populate.run();});}
    filterButtons[0].setBackground(outlined(0xff8e1530,0xffff6078,12));
    search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){populate.run();}public void afterTextChanged(android.text.Editable e){}});
    populate.run();dialog.show();if(dialog.getWindow()!=null){dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);dialog.getWindow().setLayout(-1,-1);dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);}
  }
  static boolean oldFrenchRegion(String id){return id.matches("france_(northeast|northwest|southeast|southwest)");}
  void showDownloadedMapOptions(JSONObject pack){
    String id=pack.optString("id");File file=new File(new File(getFilesDir(),"pmtiles"),id+".pmtiles");
    new AlertDialog.Builder(this).setTitle(pack.optString("name",id))
      .setMessage("Carte présente sur le téléphone · "+(file.length()/1048576)+" Mio. Toutes les cartes téléchargées sont affichées ensemble sur la carte, en jour ou nuit.")
      .setPositiveButton("Afficher les cartes",(d,w)->{
        try{if(!new PmtilesReader(file).isVector())throw new IOException("Carte vectorielle requise");
          mapLibrePreview=true;offlineView=false;forceOnlineMapLibre=false;selected="Carte";render();
        }catch(Exception ex){Toast.makeText(this,"Carte illisible : "+ex.getMessage(),Toast.LENGTH_LONG).show();}
      })
      .setNeutralButton("Supprimer",(d,w)->new AlertDialog.Builder(this).setTitle("Supprimer "+pack.optString("name",id)+" ?")
        .setNegativeButton("Annuler",null).setPositiveButton("Supprimer",(d2,w2)->{
          if(!file.delete()){Toast.makeText(this,"Suppression impossible",Toast.LENGTH_LONG).show();return;}
          invalidateInstalledMosaic();
          mapLibrePreview=!installedMosaicRegions().isEmpty();
          Toast.makeText(this,"Carte supprimée",Toast.LENGTH_SHORT).show();
        }).show())
      .setNegativeButton("Fermer",null).show();
  }
  void showMapCatalog(LinearLayout content){
    if(mapCatalog.length()==0){add(content,label("Aucun pack disponible : le catalogue national ROADGUARD n’est pas encore hébergé.",13));return;}
    for(int i=0;i<mapCatalog.length();i++){
      JSONObject pack=mapCatalog.optJSONObject(i);if(pack==null)continue;
      String id=pack.optString("id"),name=pack.optString("name",id);
      if(oldFrenchRegion(id))continue;
      long size=pack.optLong("size",0);
      String kind=pack.optString("format","xyz-zip");
      boolean vector="pmtiles".equals(kind);
      File pmFile=new File(new File(getFilesDir(),"pmtiles"),id+".pmtiles");
      boolean installed=vector?pmFile.isFile():id.equals(getPreferences(0).getString("map_pack_id",""));
      String state=installed?(vector?"✓ PMTiles installé · ":"✓ Installé · "):"↓ Télécharger · ";
      add(content,button(state+name+" · "+(size/(1024*1024))+" Mo · "+kind,()->{
        if(installed){
          Toast.makeText(this,vector?"Archive vectorielle déjà téléchargée : sélectionnez son aperçu ci-dessous":"Cette carte est déjà installée",Toast.LENGTH_LONG).show();return;
        }
        confirmMapDownload(pack);
      }));
    }
    if(getPreferences(0).getInt("tile_count",0)>0)add(content,button("Supprimer le pack de carte installé",()->new AlertDialog.Builder(this).setTitle("Supprimer les cartes locales ?").setNegativeButton("Annuler",null).setPositiveButton("Supprimer",(d,w)->removeImportedTilePack()).show()));
  }
  void showPmtilesInstalled(LinearLayout content){
    File dir=new File(getFilesDir(),"pmtiles");File[] files=dir.listFiles((d,n)->n.endsWith(".pmtiles"));
    add(content,button("Importer une carte PMTiles déjà présente sur le téléphone · sans Internet",()->openExistingPmtiles()));
    String active=getPreferences(0).getString("active_raster_pmtiles","");
    String activeVector=getPreferences(0).getString("active_vector_pmtiles","");
    if(!active.isEmpty()||!activeVector.isEmpty())add(content,button("Revenir à la carte en ligne · désactiver l’aperçu PMTiles",()->{
      getPreferences(0).edit().remove("active_raster_pmtiles").remove("active_vector_pmtiles").apply();offlineView=false;mapLibrePreview=false;selected="Carte";render();
    }));
    if(files==null||files.length==0){add(content,label("Aucun pack PMTiles installé",13));return;}
    for(File f:files){
      String detail="";
      try{detail=pmtilesHeaderSummary(f);}catch(Exception ex){detail=" · fichier invalide : "+ex.getMessage();}
      add(content,label("Archive PMTiles : "+f.getName()+" · "+(f.length()/(1024*1024))+" Mo"+detail,13));
      try{
        PmtilesReader reader=new PmtilesReader(f);
        if(reader.isRaster()&&f.getName().matches("[A-Za-z0-9_-]{1,64}\\.pmtiles")){
          String id=f.getName().substring(0,f.getName().length()-8);
          add(content,button("Afficher ce pack raster sur la carte (aperçu)",()->{
            getPreferences(0).edit().putString("active_raster_pmtiles",id).remove("active_vector_pmtiles").apply();
            offlineView=true;mapLibrePreview=false;selected="Carte";render();
          }));
        }else if(reader.isVector()&&f.getName().matches("[A-Za-z0-9_-]{1,64}\\.pmtiles")){
          String id=f.getName().substring(0,f.getName().length()-8);
          add(content,button("Prévisualiser ce pack vectoriel (MapLibre local)",()->{
            getPreferences(0).edit().putString("active_vector_pmtiles",id).remove("active_raster_pmtiles").apply();
            offlineView=false;mapLibrePreview=true;forceOnlineMapLibre=false;selected="Carte";render();
          }));
          add(content,label("Pack graphique ROADGUARD Premium : choisir le thème avant de prévisualiser le pack vectoriel.",12));
          add(content,button("☾ Pack Premium Nuit (graphite / rouge)",()->{
            nightMap=true;getPreferences(0).edit().putBoolean("night_map",true).putString("active_vector_pmtiles",id).remove("active_raster_pmtiles").apply();
            offlineView=false;mapLibrePreview=true;forceOnlineMapLibre=false;selected="Carte";render();
          }));
          add(content,button("☀ Pack Premium Jour (clair / rouge)",()->{
            nightMap=false;getPreferences(0).edit().putBoolean("night_map",false).putString("active_vector_pmtiles",id).remove("active_raster_pmtiles").apply();
            offlineView=false;mapLibrePreview=true;forceOnlineMapLibre=false;selected="Carte";render();
          }));
          add(content,label("Nécessite une archive PMTiles VECTORIELLE installée et compatible avec les couches OpenMapTiles. Sans archive, la carte en ligne classique reste disponible. Vérifier le rendu sur Xiaomi.",12));
        }else add(content,label("Type PMTiles non pris en charge pour l'aperçu",12));
      }catch(Exception ignored){}
      add(content,button("Supprimer "+f.getName(),()->new AlertDialog.Builder(this).setTitle("Supprimer cette carte ?")
        .setMessage(f.getName()).setNegativeButton("Annuler",null)
        .setPositiveButton("Supprimer",(d,w)->{if(f.delete())render();else Toast.makeText(this,"Suppression impossible",Toast.LENGTH_LONG).show();}).show()));
    }
  }
  void openExistingPmtiles(){
    Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("*/*");
    // Android providers often expose .pmtiles as application/octet-stream or
    // application/unknown. Filtering MIME types hides valid archives.
    // Let the user browse all files; validate the PMTiles header on import.
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
    try{DocumentPicker.open(this,intent,61);}catch(Exception e){Toast.makeText(this,"Sélecteur de fichiers indisponible",Toast.LENGTH_LONG).show();}
  }
  void importExistingPmtiles(Uri source){
    if(source==null||pmtilesImportRunning)return;
    try{getContentResolver().takePersistableUriPermission(source,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}
    pmtilesImportRunning=true;pmtilesImportCancel=false;
    final android.app.ProgressDialog progress=new android.app.ProgressDialog(this);
    progress.setTitle("Import de la carte locale");progress.setMessage("Copie depuis le téléphone, sans téléchargement Internet…");
    progress.setProgressStyle(android.app.ProgressDialog.STYLE_HORIZONTAL);progress.setMax(100);progress.setCancelable(false);
    progress.setButton(android.app.AlertDialog.BUTTON_NEGATIVE,"Annuler",(d,w)->{pmtilesImportCancel=true;progress.setMessage("Annulation demandée…");});progress.show();
    new Thread(()->{
      File dir=new File(getFilesDir(),"pmtiles"),tmp=new File(getFilesDir(),"pmtiles_import.tmp");
      try{
        if(!dir.exists()&&!dir.mkdirs())throw new IOException("Stockage interne indisponible");
        long expected=-1;
        try(android.database.Cursor c=getContentResolver().query(source,new String[]{android.provider.OpenableColumns.SIZE},null,null,null)){
          if(c!=null&&c.moveToFirst()&&!c.isNull(0))expected=c.getLong(0);
        }
        if(expected>MAX_PMTILES_DOWNLOAD_BYTES)throw new IOException("Archive supérieure à 8 Gio");
        if(expected>0&&getFilesDir().getUsableSpace()<expected+64L*1024*1024)throw new IOException("Espace libre insuffisant pour importer cette archive sans toucher à l’originale");
        java.security.MessageDigest sha=java.security.MessageDigest.getInstance("SHA-256");long copied=0;
        try(InputStream in=getContentResolver().openInputStream(source);FileOutputStream out=new FileOutputStream(tmp)){
          if(in==null)throw new IOException("Fichier source inaccessible");byte[] buffer=new byte[1024*1024];int n,last=-1;
          while((n=in.read(buffer))!=-1){if(pmtilesImportCancel)throw new IOException("Import annulé");copied+=n;if(copied>MAX_PMTILES_DOWNLOAD_BYTES||(expected>0&&copied>expected))throw new IOException("Taille du fichier incohérente");if(getFilesDir().getUsableSpace()<n+64L*1024*1024)throw new IOException("Espace libre insuffisant pour terminer l’import sans saturer le stockage");out.write(buffer,0,n);sha.update(buffer,0,n);
            if(expected>0){int pct=(int)(copied*100/expected);if(pct!=last){last=pct;final int value=pct;runOnUiThread(()->progress.setProgress(value));}}
          }out.getFD().sync();
        }
        if(copied<127||(expected>0&&copied!=expected))throw new IOException("Fichier incomplet");
        pmtilesHeaderSummary(tmp);PmtilesReader reader=new PmtilesReader(tmp);
        if(!reader.isVector()&&!reader.isRaster())throw new IOException("Type de tuiles PMTiles non pris en charge");
        // Any valid PMTiles region may be imported for preview. Do not mislabel
        // a foreign/regional archive as the France+Corsica national pack.
        boolean isFrance=reader.isVector()&&pmtilesCoversFranceAndCorsica(tmp);
        if(pmtilesImportCancel)throw new IOException("Import annulé");
        StringBuilder digest=new StringBuilder();for(byte b:sha.digest())digest.append(String.format(Locale.US,"%02x",b&255));
        // Keep the previous map recoverable until the replacement is installed.
        // Never delete a pre-existing .old backup before confirming the new file.
        String packId=isFrance?"france_local":"region_local";
        File target=new File(dir,packId+".pmtiles");
        File old=new File(dir,packId+".pmtiles.old");
        File stage=new File(dir,packId+".pmtiles.new");
        if(stage.exists()&&!stage.delete())throw new IOException("Installation temporaire verrouillée");
        if(!tmp.renameTo(stage))throw new IOException("Impossible de préparer la nouvelle carte");
        boolean previousMoved=false;
        try{
          if(target.exists()){
            if(old.exists()&&!old.delete())throw new IOException("Sauvegarde précédente verrouillée");
            if(!target.renameTo(old))throw new IOException("Impossible de préserver l’ancienne carte");
            previousMoved=true;
          }
          if(!stage.renameTo(target))throw new IOException("Installation locale impossible");
          // Persist activation only after the archive is safely installed.
          getPreferences(0).edit().putString("pmtiles_sha256_"+packId,digest.toString())
            .putString("active_vector_pmtiles",reader.isVector()?packId:getPreferences(0).getString("active_vector_pmtiles",""))
            .apply();
          if(previousMoved)old.delete();
        }catch(Exception installError){
          if(previousMoved){
            if(target.exists())target.delete();
            if(!old.renameTo(target))throw new IOException("Restauration automatique impossible : ancienne carte conservée dans "+old.getName(),installError);
          }
          throw installError;
        }finally{if(stage.exists())stage.delete();}
        runOnUiThread(()->{progress.dismiss();pmtilesImportRunning=false;pmtilesImportCancel=false;Toast.makeText(this,"Archive PMTiles importée · sélectionne son aperçu dans les cartes",Toast.LENGTH_LONG).show();render();});
      }catch(Exception e){tmp.delete();String msg=e.getMessage();runOnUiThread(()->{progress.dismiss();pmtilesImportRunning=false;pmtilesImportCancel=false;Toast.makeText(this,"Import impossible : "+msg,Toast.LENGTH_LONG).show();});}
    },"roadguard-import-local-pmtiles").start();
  }
  static long pmtilesLE64(byte[] b,int at){
    long v=0;for(int i=7;i>=0;i--)v=(v<<8)|(b[at+i]&255L);return v;
  }
  static boolean pmtilesRangeOk(long offset,long length,long size,boolean required){
    return offset>=0&&length>=(required?1:0)&&offset<=size&&length<=size-offset;
  }
  static String pmtilesHeaderSummary(File file) throws IOException{
    if(file.length()<127)throw new IOException("en-tête incomplet");
    byte[] h=new byte[127];
    try(FileInputStream in=new FileInputStream(file)){
      int pos=0,n;while(pos<h.length&&(n=in.read(h,pos,h.length-pos))>0)pos+=n;
      if(pos!=h.length)throw new IOException("lecture incomplète");
    }
    if(!"PMTiles".equals(new String(h,0,7,"US-ASCII")))throw new IOException("signature incorrecte");
    if((h[7]&255)!=3)throw new IOException("version PMTiles non prise en charge : "+(h[7]&255));
    long directory=pmtilesLE64(h,8),directoryLength=pmtilesLE64(h,16);
    long metadata=pmtilesLE64(h,24),metadataLength=pmtilesLE64(h,32);
    long leaf=pmtilesLE64(h,40),leafLength=pmtilesLE64(h,48);
    long tiles=pmtilesLE64(h,56),tilesLength=pmtilesLE64(h,64);
    long size=file.length();
    // All PMTiles v3 byte ranges must remain inside the file. Subtraction
    // avoids signed long overflow on malformed/untrusted header offsets.
    if(!pmtilesRangeOk(directory,directoryLength,size,true)
      ||!pmtilesRangeOk(metadata,metadataLength,size,false)
      ||!pmtilesRangeOk(leaf,leafLength,size,false)
      ||!pmtilesRangeOk(tiles,tilesLength,size,false))
      throw new IOException("sections PMTiles hors fichier");
    int minZoom=h[100]&255,maxZoom=h[101]&255;
    int tileType=h[99]&255,internalCompression=h[97]&255,tileCompression=h[98]&255;
    if(minZoom>maxZoom||maxZoom>30)throw new IOException("niveaux de zoom invalides");
    if(tileType<1||tileType>5)throw new IOException("type de tuile PMTiles inconnu");
    if(internalCompression<1||internalCompression>3||tileCompression<1||tileCompression>3)
      throw new IOException("compression PMTiles inconnue");
    return " · PMTiles v3 · zoom "+minZoom+"–"+maxZoom+" · type "+tileType;
  }
  static boolean pmtilesCoversFranceAndCorsica(File file) throws IOException{
    byte[] h=new byte[127];try(FileInputStream in=new FileInputStream(file)){int p=0,n;while(p<h.length&&(n=in.read(h,p,h.length-p))>0)p+=n;if(p!=h.length)throw new IOException("en-tête PMTiles incomplet");}
    int minLon=java.nio.ByteBuffer.wrap(h,102,4).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
    int minLat=java.nio.ByteBuffer.wrap(h,106,4).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
    int maxLon=java.nio.ByteBuffer.wrap(h,110,4).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
    int maxLat=java.nio.ByteBuffer.wrap(h,114,4).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
    return minLon<=-50000000&&minLat<=416000000&&maxLon>=95000000&&maxLat>=510000000;
  }
  static boolean validPmtilesRange(String header,long start,long total){
    if(header==null||start<0||total<=0||start>=total)return false;
    java.util.regex.Matcher m=java.util.regex.Pattern.compile("bytes ([0-9]+)-([0-9]+)/([0-9]+)").matcher(header.trim());
    if(!m.matches())return false;
    try{
      long first=Long.parseLong(m.group(1)),last=Long.parseLong(m.group(2)),length=Long.parseLong(m.group(3));
      return first==start&&last>=first&&last<total&&length==total;
    }catch(NumberFormatException ex){return false;}
  }
  void downloadPmtilesPack(JSONObject pack){
    String unavailable=unavailableMapPack(pack);
    if(!unavailable.isEmpty()){Toast.makeText(this,unavailable,Toast.LENGTH_LONG).show();return;}
    if(MapPackDownloadService.isRunning()){new AlertDialog.Builder(this).setTitle("Carte en téléchargement")
      .setMessage(MapPackDownloadService.status(this)+"\nLe transfert continue en arrière-plan. Une notification permet de le suspendre.")
      .setNegativeButton("Fermer",null).setPositiveButton("Suspendre",(d,w)->MapPackDownloadService.cancel(this)).show();return;}
    try{MapPackDownloadService.begin(this,pack);
      Toast.makeText(this,"Téléchargement en arrière-plan · progression dans les notifications",Toast.LENGTH_LONG).show();}
    catch(RuntimeException denied){Toast.makeText(this,"Téléchargement impossible : "+denied.getMessage(),Toast.LENGTH_LONG).show();}
  }
  java.net.HttpURLConnection secureConnection(String url) throws Exception{
    return SecureHttp.open(url,15000,30000,"ROADGUARD Android download");
  }
  // GitHub redirects release assets to its dedicated HTTPS content host.
  // Follow only that fixed chain; never forward the download to an arbitrary URL.
  static java.net.HttpURLConnection openVerifiedMapAsset(java.net.URL initial,long offset)throws IOException{
    return openVerifiedMapAsset(initial,offset,false);
  }
  static java.net.HttpURLConnection openVerifiedMapAsset(java.net.URL initial,long offset,boolean headerOnly)throws IOException{
    String expectedPrefix="/p-souto/project-nomad-extra-maps/releases/download/v1/";
    boolean publicFrance=PUBLIC_FRANCE_PMTILES_URL.equals(initial.toString());
    boolean github="https".equalsIgnoreCase(initial.getProtocol())&&"github.com".equalsIgnoreCase(initial.getHost())
      &&initial.getPath().startsWith(expectedPrefix)&&initial.getPath().endsWith(".pmtiles")&&initial.getUserInfo()==null;
    if(!publicFrance&&!github)
      throw new IOException("Source de carte non autorisée");
    java.net.URL next=initial;
    for(int redirects=0;redirects<=5;redirects++){
      java.net.HttpURLConnection connection=(java.net.HttpURLConnection)next.openConnection();
      connection.setConnectTimeout(15000);connection.setReadTimeout(30000);connection.setInstanceFollowRedirects(false);
      connection.setRequestProperty("User-Agent","ROADGUARD-Maps/1.0");
      if(publicFrance)connection.setRequestProperty("If-Match",PUBLIC_FRANCE_PMTILES_ETAG);
      if(headerOnly)connection.setRequestProperty("Range","bytes=0-126");
      else if(offset>0)connection.setRequestProperty("Range","bytes="+offset+"-");
      int status=connection.getResponseCode();
      if(status!=301&&status!=302&&status!=303&&status!=307&&status!=308){
        if(publicFrance&&status>=200&&status<300&&!PUBLIC_FRANCE_PMTILES_ETAG.equals(connection.getHeaderField("ETag"))){connection.disconnect();throw new IOException("Archive France modifiée pendant le téléchargement");}
        return connection;
      }
      String location=connection.getHeaderField("Location");connection.disconnect();
      if(publicFrance)throw new IOException("Redirection inattendue de l'archive France");
      if(location==null||redirects==5)throw new IOException("Redirection de carte invalide");
      next=new java.net.URL(next,location);
      String host=next.getHost().toLowerCase(Locale.ROOT);
      if(!"https".equalsIgnoreCase(next.getProtocol())||next.getUserInfo()!=null
        ||!("github.com".equals(host)||"release-assets.githubusercontent.com".equals(host)
          ||"objects.githubusercontent.com".equals(host)))throw new IOException("Redirection hors GitHub bloquée");
    }
    throw new IOException("Trop de redirections");
  }
  static void checkRemotePmtilesHeader(java.net.URL url,long expected)throws IOException{
    java.net.HttpURLConnection c=openVerifiedMapAsset(url,0,true);
    try{
      int status=c.getResponseCode();
      if(status!=206)throw new IOException("En-tête distant : réponse HTTP "+status+" au lieu d'une plage partielle");
      if(!validPmtilesRange(c.getHeaderField("Content-Range"),0,expected))throw new IOException("En-tête distant : plage HTTP incorrecte");
      byte[] header=new byte[127];try(InputStream in=c.getInputStream()){
        int count=0,n;while(count<header.length&&(n=in.read(header,count,header.length-count))>0)count+=n;
        if(count!=header.length)throw new IOException("En-tête distant incomplet");
      }
      if(!"PMTiles".equals(new String(header,0,7,"US-ASCII"))||(header[7]&255)!=3)
        throw new IOException("Source de carte invalide : le fichier publié ne commence pas par PMTiles v3. Aucun gros téléchargement lancé");
      if(PUBLIC_FRANCE_PMTILES_URL.equals(url.toString())){
        java.nio.ByteBuffer b=java.nio.ByteBuffer.wrap(header).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        if(expected!=PUBLIC_FRANCE_PMTILES_BYTES||(header[99]&255)!=1||(header[100]&255)>10||(header[101]&255)<14
          ||b.getInt(102)>-50000000||b.getInt(106)>416000000||b.getInt(110)<95000000||b.getInt(114)<510000000
          ||b.getLong(8)<127||b.getLong(8)+b.getLong(16)>expected
          ||b.getLong(24)<127||b.getLong(24)+b.getLong(32)>expected
          ||b.getLong(56)<127||b.getLong(56)+b.getLong(64)>expected)
          throw new IOException("Archive nationale : en-tête ou emprise géographique invalide");
      }
    }finally{c.disconnect();}
  }
  // One non-blocking refresh per process when the library is first opened.
  // Does not retry in a render loop or interfere with navigation/GPS.
  boolean catalogAutoRefreshAttempted=false;
  void refreshCatalogOnceOnLibraryOpen(){
    if(catalogAutoRefreshAttempted)return;
    catalogAutoRefreshAttempted=true;
    String url=getPreferences(0).getString("map_catalog_url","");
    if(!url.isEmpty())refreshMapCatalog();
  }
  void refreshMapCatalog(){
    String url=getPreferences(0).getString("map_catalog_url","");
    if(url.isEmpty()){Toast.makeText(this,"Aucun serveur de cartes configuré",Toast.LENGTH_LONG).show();return;}
    new Thread(()->{
      try{
        java.net.HttpURLConnection c=secureConnection(url);byte[] bytes;
        try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
          byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1){if(out.size()+n>262144)throw new IOException("Catalogue trop volumineux");out.write(buf,0,n);}bytes=out.toByteArray();
        }finally{c.disconnect();}
        JSONObject json=new JSONObject(new String(bytes,"UTF-8"));
        if(json.optInt("schema")!=1)throw new IOException("Version de catalogue inconnue");
        JSONArray packs=json.getJSONArray("packs");if(packs.length()>100)throw new IOException("Catalogue trop grand");
        java.util.HashSet<String> ids=new java.util.HashSet<>();
        for(int i=0;i<packs.length();i++){
          JSONObject pack=packs.getJSONObject(i);String link=pack.getString("url"),hash=pack.getString("sha256");
          if(!ids.add(pack.getString("id")))throw new IOException("Identifiant de carte en double : "+pack.getString("id"));
          if(!"https".equalsIgnoreCase(new java.net.URL(link).getProtocol())||new java.net.URL(link).getUserInfo()!=null||!hash.matches("[a-fA-F0-9]{64}")||pack.optLong("size",0)<1||pack.optLong("size",0)>("pmtiles".equals(pack.optString("format"))?MAX_PMTILES_DOWNLOAD_BYTES:600L*1024*1024)||!(pack.optString("format","xyz-zip").equals("xyz-zip")||pack.optString("format").equals("pmtiles"))||!pack.getString("id").matches("[a-z0-9_-]{2,50}")||pack.optString("name","").length()>100)throw new IOException("Entrée de catalogue invalide");
        }
        runOnUiThread(()->{mapCatalog=packs;getPreferences(0).edit().putString("map_catalog_cache",packs.toString()).apply();render();Toast.makeText(this,packs.length()+" cartes dans le catalogue",Toast.LENGTH_SHORT).show();});
      }catch(Exception e){runOnUiThread(()->Toast.makeText(this,"Catalogue indisponible : "+e.getMessage(),Toast.LENGTH_LONG).show());}
    },"roadguard-catalog").start();
  }
  boolean isWifiForMapDownload(){
    android.net.ConnectivityManager cm=(android.net.ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
    if(cm==null)return false;
    android.net.Network active=cm.getActiveNetwork();if(active==null)return false;
    android.net.NetworkCapabilities caps=cm.getNetworkCapabilities(active);
    return caps!=null&&caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI);
  }
  void confirmMapDownload(JSONObject pack){
    String unavailable=unavailableMapPack(pack);
    if(!unavailable.isEmpty()){new AlertDialog.Builder(this).setTitle("Carte indisponible").setMessage(unavailable+". La source doit republier France Nord-Est ; les autres régions françaises restent accessibles.").setPositiveButton("OK",null).show();return;}
    if(getPreferences(0).getBoolean("maps_wifi_only",false)&&!isWifiForMapDownload()){
      new AlertDialog.Builder(this).setTitle("Wi-Fi nécessaire")
        .setMessage("Le téléchargement de cartes est limité au Wi-Fi dans tes réglages. Connecte-toi à un réseau Wi-Fi ou désactive cette option dans Bibliothèque de cartes.")
        .setPositiveButton("OK",null).show();return;
    }
    if(mapDownloadRunning||MapPackDownloadService.isRunning()){Toast.makeText(this,"Un téléchargement est déjà en cours",Toast.LENGTH_SHORT).show();return;}
    long bytes=pack.optLong("size",0),free=getFilesDir().getUsableSpace();
    long maxBytes="pmtiles".equals(pack.optString("format"))?MAX_PMTILES_DOWNLOAD_BYTES:600L*1024*1024;
    if(bytes<=0||bytes>maxBytes){Toast.makeText(this,"Taille de carte invalide",Toast.LENGTH_LONG).show();return;}
    long reserve=128L*1024*1024;
    // PMTiles: temporary file and final archive live in the same directory; rename does not duplicate bytes.
    // XYZ ZIP requires additional extraction space, retained conservatively.
    long required="pmtiles".equals(pack.optString("format"))?bytes+reserve:bytes*3;
    if(required>free){new AlertDialog.Builder(this).setTitle("Espace insuffisant").setMessage("Disponible : "+(free/(1024*1024))+" Mo. Espace nécessaire estimé : "+(required/(1024*1024))+" Mo.").setPositiveButton("OK",null).show();return;}
    new AlertDialog.Builder(this).setTitle("Télécharger "+pack.optString("name"))
      .setMessage("Les packs XYZ remplacent les anciennes tuiles ; les PMTiles restent séparés, avec aperçu raster ou vectoriel expérimental. Taille annoncée : "+(pack.optLong("size")/(1024*1024))+" Mo."+(isWifiForMapDownload()?"":"\n\nRéseau mobile : ce téléchargement utilisera ton forfait de données."))
      .setNegativeButton("Annuler",null).setPositiveButton("Télécharger",(d,w)->{if("pmtiles".equals(pack.optString("format")))downloadPmtilesPack(pack);else downloadMapPack(pack);}).show();
  }
  void downloadMapPack(JSONObject pack){
    if(getPreferences(0).getBoolean("maps_wifi_only",false)&&!isWifiForMapDownload()){
      Toast.makeText(this,"Connexion Wi-Fi nécessaire",Toast.LENGTH_LONG).show();return;
    }
    if(mapDownloadRunning)return;mapDownloadRunning=true;mapDownloadCancel=false;
    final android.app.ProgressDialog progress=new android.app.ProgressDialog(this);
    progress.setTitle("Carte : "+pack.optString("name"));progress.setMessage("Téléchargement sécurisé…");progress.setProgressStyle(android.app.ProgressDialog.STYLE_HORIZONTAL);progress.setMax(100);progress.setCancelable(false);progress.setButton(android.app.AlertDialog.BUTTON_NEGATIVE,"Annuler",(dialog,which)->{mapDownloadCancel=true;progress.setMessage("Annulation demandée…");});progress.show();
    new Thread(()->{
      File temp=new File(getCacheDir(),"roadguard-map-download.zip"),stage=new File(getFilesDir(),"tiles_new");
      try{
        java.net.HttpURLConnection c=secureConnection(pack.getString("url"));
        java.security.MessageDigest sha=java.security.MessageDigest.getInstance("SHA-256");long received=0,limit=pack.getLong("size");
        try(InputStream in=c.getInputStream();FileOutputStream out=new FileOutputStream(temp)){
          byte[] buf=new byte[32768];int n;int last=-1;
          while((n=in.read(buf))!=-1){if(mapDownloadCancel)throw new IOException("Téléchargement annulé");
            if(getPreferences(0).getBoolean("maps_wifi_only",false)&&!isWifiForMapDownload())throw new IOException("Wi-Fi perdu : téléchargement interrompu");
            received+=n;if(received>limit||received>600L*1024*1024)throw new IOException("Taille dépassée");out.write(buf,0,n);sha.update(buf,0,n);
            int pct=(int)(received*100/limit);if(pct!=last){last=pct;final int current=pct;runOnUiThread(()->progress.setProgress(current));}
          }
        }finally{c.disconnect();}
        if(received!=limit)throw new IOException("Taille du fichier incorrecte");
        StringBuilder hex=new StringBuilder();for(byte b:sha.digest())hex.append(String.format(Locale.US,"%02x",b&255));
        if(!hex.toString().equalsIgnoreCase(pack.getString("sha256")))throw new IOException("Empreinte SHA-256 incorrecte");
        deleteTree(stage);if(!stage.mkdirs())throw new IOException("Stockage inaccessible");
        int count=0;long uncompressed=0;int minZoom=99,maxZoom=-1;java.util.HashSet<String> seenTiles=new java.util.HashSet<>();
        try(ZipInputStream zip=new ZipInputStream(new FileInputStream(temp))){ZipEntry entry;byte[] buf=new byte[32768];
          while((entry=zip.getNextEntry())!=null){if(mapDownloadCancel)throw new IOException("Installation annulée");if(entry.isDirectory())continue;
            String name=entry.getName();if(name.startsWith("tiles/"))name=name.substring(6);
            if(!name.matches("[0-9]{1,2}/[0-9]{1,9}/[0-9]{1,9}\\.(png|jpg|jpeg)"))throw new IOException("Format de tuile incompatible");
            if(!seenTiles.add(name))throw new IOException("Tuile en double : "+name);
            if(++count>300000)throw new IOException("Trop de tuiles");
            String[] coords=name.substring(0,name.lastIndexOf('.')).split("/");
            int zoom=Integer.parseInt(coords[0]);long x=Long.parseLong(coords[1]),y=Long.parseLong(coords[2]);
            if(zoom>19||x>=(1L<<zoom)||y>=(1L<<zoom))throw new IOException("Coordonnées XYZ invalides");
            minZoom=Math.min(minZoom,zoom);maxZoom=Math.max(maxZoom,zoom);
            File target=new File(stage,name);if(!target.getCanonicalPath().startsWith(stage.getCanonicalPath()+File.separator))throw new IOException("Chemin invalide");
            target.getParentFile().mkdirs();try(FileOutputStream out=new FileOutputStream(target)){
              int n;while((n=zip.read(buf))!=-1){if(mapDownloadCancel)throw new IOException("Installation annulée");uncompressed+=n;if(uncompressed>1500L*1024*1024)throw new IOException("Archive décompressée trop volumineuse");out.write(buf,0,n);}
            }
          }
        }
        if(count==0)throw new IOException("Archive sans tuiles");
        if(mapDownloadCancel)throw new IOException("Installation annulée");
        File current=new File(getFilesDir(),"tiles"),old=new File(getFilesDir(),"tiles_old");deleteTree(old);
        if(current.exists()&&!current.renameTo(old))throw new IOException("Sauvegarde des anciennes cartes impossible");
        if(!stage.renameTo(current)){if(old.exists())old.renameTo(current);throw new IOException("Installation impossible");}
        deleteTree(old);final int installed=count;final int lowestZoom=minZoom,highestZoom=maxZoom;
        getPreferences(0).edit().putInt("tile_count",installed).putString("map_pack_id",pack.getString("id")).putInt("map_zoom_min",lowestZoom).putInt("map_zoom_max",highestZoom).apply();
        runOnUiThread(()->{progress.dismiss();mapDownloadRunning=false;mapDownloadCancel=false;Toast.makeText(this,installed+" tuiles installées · zoom "+lowestZoom+" à "+highestZoom,Toast.LENGTH_LONG).show();render();});
      }catch(Exception e){deleteTree(stage);final String error=e.getMessage();runOnUiThread(()->{progress.dismiss();mapDownloadRunning=false;mapDownloadCancel=false;Toast.makeText(this,"Carte non installée : "+error,Toast.LENGTH_LONG).show();});}
      finally{temp.delete();}
    },"roadguard-map-download").start();
  }
  // Destination manuelle, conservée localement. Distance à vol d'oiseau uniquement.
  boolean hasDestination(){return Double.isFinite(destinationLat)&&Double.isFinite(destinationLon);}
  void loadDestination(){String saved=getPreferences(0).getString("destination","");String[] parts=saved.split(",");if(parts.length==2)try{double a=Double.parseDouble(parts[0]),b=Double.parseDouble(parts[1]);if(Math.abs(a)<=90&&Math.abs(b)<=180){destinationLat=a;destinationLon=b;}}catch(NumberFormatException ignored){}}
  String destinationDistance(){if(!hasDestination())return "Non définie";if(Double.isNaN(lastLat)||Double.isNaN(lastLon))return "GPS indisponible";double meters=geoMeters(lastLat,lastLon,destinationLat,destinationLon);return meters<1000?String.format(Locale.FRANCE,"%.0f m à vol d'oiseau",meters):String.format(Locale.FRANCE,"%.1f km à vol d'oiseau",meters/1000);}
  // 11.14.24: route planning first, technical trip journal in a separate expandable area.
  void showPremiumRoute(LinearLayout content){
    sectionCard(content,"Parcours en 3 étapes","Voir le détail du trajet","route_timeline",false,()->premiumRouteTimeline(content));
    premiumGroup(content,"MON ITINÉRAIRE","Départ, destination et accès direct au guidage.");
    LinearLayout startCard=column();startCard.setPadding(pxDp(15),pxDp(13),pxDp(15),pxDp(13));
    startCard.setBackground(premiumPanel(0xff263444,0xff151c26,0xff48596c,18));startCard.setElevation(pxDp(1));
    TextView startLabel=label("DÉPART",11);startLabel.setTypeface(null,Typeface.BOLD);startLabel.setTextColor(0xffff6879);add(startCard,startLabel);
    TextView startValue=label("⌖  Ma position GPS",15);startValue.setTypeface(null,Typeface.BOLD);startValue.setTextColor(0xfff5f7fb);startValue.setPadding(0,pxDp(5),0,0);add(startCard,startValue);
    TextView destinationLabel=label("DESTINATION",11);destinationLabel.setTypeface(null,Typeface.BOLD);destinationLabel.setTextColor(0xffff6879);destinationLabel.setPadding(0,pxDp(13),0,0);add(startCard,destinationLabel);
    TextView destinationValue=label(hasDestination()?String.format(Locale.FRANCE,"➤  %.5f, %.5f",destinationLat,destinationLon):"⌕  Où va-t-on ?",16);
    destinationValue.setTextColor(hasDestination()?0xfff5f7fb:0xffc0cad7);destinationValue.setPadding(0,pxDp(5),0,0);add(startCard,destinationValue);
    startCard.setClickable(true);startCard.setFocusable(true);startCard.setContentDescription("Choisir une destination");startCard.setOnClickListener(v->askAddress());
    LinearLayout.LayoutParams startLp=new LinearLayout.LayoutParams(-1,-2);startLp.setMargins(0,0,0,pxDp(10));content.addView(startCard,startLp);
    boolean routeReady=navigationPoints.size()>1;
    LinearLayout routeState=column();routeState.setPadding(pxDp(14),pxDp(11),pxDp(14),pxDp(11));
    routeState.setBackground(premiumPanel(routeReady?0xff542236:hasDestination()?0xff2b3948:0xff253140,0xff281720,routeReady?0xffd95167:0xff48596c,18));routeState.setElevation(pxDp(1));
    TextView routeStateTitle=label(routeReady?"●  ITINÉRAIRE CALCULÉ":hasDestination()?"◉  DESTINATION PRÊTE":"○  EN ATTENTE D’UNE DESTINATION",11);routeStateTitle.setTypeface(null,Typeface.BOLD);routeStateTitle.setTextColor(routeReady?0xffff8794:0xffd7e1ec);add(routeState,routeStateTitle);
    TextView routeStateDetail=label(routeReady?String.format(Locale.FRANCE,"%.1f km · environ %d min",navigationMeters/1000,Math.max(1,Math.round(navigationSeconds/60))):hasDestination()?"Lance le calcul routier pour obtenir le guidage.":"Choisis une adresse, un favori ou des coordonnées.",12);routeStateDetail.setTextColor(0xffb8c5d4);routeStateDetail.setPadding(0,pxDp(4),0,0);add(routeState,routeStateDetail);
    LinearLayout.LayoutParams routeStateLp=new LinearLayout.LayoutParams(-1,-2);routeStateLp.setMargins(0,pxDp(1),0,pxDp(9));content.addView(routeState,routeStateLp);
    if(availableRoutes.size()>1){
      premiumGroup(content,"ITINÉRAIRES DISPONIBLES","Choisis parmi les parcours réellement retournés par le calcul routier.");
      for(int ri=0;ri<availableRoutes.size();ri++){final int routeIndex=ri;RouteOption option=availableRoutes.get(ri);String eta=Math.max(1,Math.round(option.seconds/60))+" min  ·  "+String.format(Locale.FRANCE,"%.1f km",option.meters/1000);TextView choice=button((ri==activeRouteIndex?"✓  ":"○  ")+option.label+"\n"+eta,()->applyRouteOption(routeIndex,true));choice.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);choice.setTextSize(14);choice.setTypeface(null,Typeface.BOLD);choice.setMinHeight(pxDp(61));choice.setMaxLines(2);choice.setBackground(premiumPanel(ri==activeRouteIndex?0xff5b2434:0xff1a2532,0xff111821,ri==activeRouteIndex?0xfff05569:0xff45566b,17));LinearLayout.LayoutParams optionLp=new LinearLayout.LayoutParams(-1,-2);optionLp.setMargins(0,0,0,pxDp(6));content.addView(choice,optionLp);}
    }
    if(navigationPoints.size()>1){
      add(content,label("Durée indicative : "+Math.max(1,Math.round(navigationSeconds/60))+" min · Le temps réel dépend du trafic.",13));
      premiumAction(content,"▶","Reprendre le guidage","Ouvrir la carte avec le parcours déjà calculé",()->{offlineView=false;selected="Carte";render();});
    }
    LinearLayout actions=row();
    TextView coordinates=button("Coordonnées",()->askDestination());
    LinearLayout.LayoutParams a=new LinearLayout.LayoutParams(0,-2,1);a.setMargins(0,0,4,0);actions.addView(coordinates,a);
    TextView favorites=button("Favoris",()->showDestinationFavorites());
    LinearLayout.LayoutParams b=new LinearLayout.LayoutParams(0,-2,1);b.setMargins(4,0,0,0);actions.addView(favorites,b);add(content,actions);
    // Keep the route's main action visible even before a destination is selected.
    if(hasDestination()){
      premiumPrimaryAction(content,navigationPoints.size()>1?"▶  Démarrer le guidage":"➤  Calculer et démarrer l’itinéraire",()->{
        if(navigationPoints.size()>1){offlineView=false;selected="Carte";render();}
        else if(!routeRequestPending)calculateRoadRoute();
        else Toast.makeText(this,"Calcul en cours…",Toast.LENGTH_SHORT).show();
      });
    }else premiumPrimaryAction(content,"⌕  Rechercher une destination",()->askAddress());
    if(hasDestination()){
      premiumGroup(content,"PARCOURS","Calcul routier en ligne lorsque le réseau est disponible.");
      add(content,label("Distance à vol d'oiseau : "+destinationDistance(),14));

      add(content,label("Le calcul nécessite une connexion. Le dernier trajet calculé reste consultable sans réseau tant qu'il n'est pas effacé.",12));
      if(!offlineRoads.isEmpty())premiumAction(content,"▤","Calcul local expérimental","Utiliser les routes importées",()->calculateOfflineRoute());
      premiumAction(content,"☆","Enregistrer en favori","Retrouver cette destination",()->saveDestinationFavorite());
      premiumAction(content,"×","Retirer la destination","Supprimer la destination et le trajet après confirmation",()->new AlertDialog.Builder(this).setTitle("Retirer la destination ?").setMessage("Le trajet calculé sera effacé. Les cartes et favoris seront conservés.").setNegativeButton("Annuler",null).setPositiveButton("Retirer",(dialog,which)->{destinationLat=Double.NaN;destinationLon=Double.NaN;clearNavigation();getPreferences(0).edit().remove("destination").apply();render();}).show());
    }
    if(!navigationPoints.isEmpty()){
      premiumGroup(content,"GUIDAGE",routeStatusText());
      add(content,label(String.format(Locale.FRANCE,"%.1f km · environ %.0f min (estimation)",navigationMeters/1000,navigationSeconds/60),17));
      premiumAction(content,"➜","Afficher le trajet sur la carte","Revenir au GPS et suivre le parcours",()->{offlineView=false;selected="Carte";render();});
      premiumAction(content,"↻","Recalculer le trajet","Depuis la position GPS actuelle",()->calculateRoadRoute());
      premiumAction(content,"▣","Conserver hors ligne","Garder ce trajet en mémoire",()->keepNavigationOffline());
      premiumAction(content,"×","Effacer le trajet","Demander confirmation avant suppression",()->new AlertDialog.Builder(this).setTitle("Effacer le trajet ?").setMessage("Le trajet calculé et son cache seront supprimés. Les cartes téléchargées, favoris et données GPS seront conservés.").setNegativeButton("Annuler",null).setPositiveButton("Effacer",(dialog,which)->{clearNavigation();render();}).show());
    }
    sectionCard(content,"Journal GPS","Statistiques et export GPX","trip_journal",false,()->showTrip(content));
    sectionCard(content,"Options avancées","Coordonnées, limites du guidage","routing_info",false,()->{
      add(content,label("Le calcul routier en ligne utilise un serveur OSRM de démonstration. Le calcul local expérimental ne respecte pas toutes les restrictions routières. Ne pas utiliser comme seule aide à la conduite.",13));
      add(content,button("Recalcul automatique : "+(automaticRerouteEnabled?"ACTIF":"DÉSACTIVÉ"),()->{automaticRerouteEnabled=!automaticRerouteEnabled;getPreferences(0).edit().putBoolean("auto_reroute",automaticRerouteEnabled).apply();render();}));
    });
  }
  void showDestination(LinearLayout content){add(content,label("OÙ ALLER ?",18));if(hasDestination()){add(content,label(String.format(Locale.FRANCE,"Coordonnées : %.5f, %.5f",destinationLat,destinationLon),14));add(content,label("Distance : "+destinationDistance(),16));add(content,label("Direction : "+destinationBearing(),14));}else add(content,label("Aucune destination enregistrée.",14));add(content,button("Rechercher une destination",()->askAddress()));add(content,button("Entrer des coordonnées (avancé)",()->askDestination()));if(hasDestination())add(content,button("Calculer un trajet routier (Internet)",()->calculateRoadRoute()));if(hasDestination()&&!offlineRoads.isEmpty())add(content,button("Calculer un trajet sur mes routes locales (sans Internet)",()->calculateOfflineRoute()));if(!navigationPoints.isEmpty()){add(content,label(routeStatusText(),15));add(content,button("Conserver ce trajet hors ligne",()->keepNavigationOffline()));add(content,button("Recalculer depuis ma position GPS (Internet)",()->calculateRoadRoute()));add(content,button("Recalcul automatique : "+(automaticRerouteEnabled?"ACTIVE":"DESACTIVE"),()->{automaticRerouteEnabled=!automaticRerouteEnabled;getPreferences(0).edit().putBoolean("auto_reroute",automaticRerouteEnabled).apply();render();}));add(content,label(String.format(Locale.FRANCE,"Trajet routier : %.1f km • environ %.0f min (estimation)",navigationMeters/1000,navigationSeconds/60),15));add(content,button("Afficher le trajet sur la carte",()->{offlineView=false;selected="Carte";render();}));add(content,button("Effacer le trajet calcule",()->{clearNavigation();render();}));}if(hasDestination())add(content,button("Enregistrer comme favori",()->saveDestinationFavorite()));add(content,button("Mes destinations favorites",()->showDestinationFavorites()));if(hasDestination())add(content,button("Supprimer la destination",()->new AlertDialog.Builder(this).setTitle("Supprimer la destination ?").setNegativeButton("Annuler",null).setPositiveButton("Supprimer",(d,w)->{destinationLat=Double.NaN;destinationLon=Double.NaN;clearNavigation();getPreferences(0).edit().remove("destination").apply();render();}).show()));sectionCard(content,"Informations techniques","Limites du guidage","routing_info",false,()->{add(content,label("Le calcul routier en ligne utilise un serveur de demonstration OSRM sans garantie de disponibilite. Le calcul hors ligne sur GeoJSON est un prototype sans prise en compte des sens uniques, interdictions ou restrictions de circulation : ne pas utiliser pour conduire. Instructions de manoeuvre indicatives issues du serveur OSRM, avec guidage vocal experimental au premier plan. Un bouton permet de recalculer manuellement le trajet depuis la position GPS. Ne pas utiliser comme unique aide a la conduite.",13));});}
  // Bearing is an approximate great-circle direction, not a road instruction.
  String destinationBearing(){if(!hasDestination()||!Double.isFinite(lastLat)||!Double.isFinite(lastLon))return "GPS indisponible";double a=Math.toRadians(lastLat),b=Math.toRadians(destinationLat),dl=Math.toRadians(destinationLon-lastLon);double y=Math.sin(dl)*Math.cos(b);double x=Math.cos(a)*Math.sin(b)-Math.sin(a)*Math.cos(b)*Math.cos(dl);double bearing=(Math.toDegrees(Math.atan2(y,x))+360)%360;String[] compass={"N","NE","E","SE","S","SO","O","NO"};return String.format(Locale.FRANCE,"%.0f° (%s) à vol d'oiseau",bearing,compass[((int)Math.round(bearing/45))%8]);}
  void pushDestination(){if(map==null||!mapReady)return;if(hasDestination())map.evaluateJavascript(String.format(Locale.US,"setDestination(%.7f,%.7f)",destinationLat,destinationLon),null);else map.evaluateJavascript("setDestination(null,null)",null);}
  void askDestination(){EditText input=new EditText(this);input.setSingleLine(true);input.setHint("46.75, 5.90");input.setInputType(android.text.InputType.TYPE_CLASS_TEXT);if(hasDestination())input.setText(String.format(Locale.US,"%.6f, %.6f",destinationLat,destinationLon));new AlertDialog.Builder(this).setTitle("Coordonnées de destination").setMessage("Saisir latitude, longitude (degrés décimaux)").setView(input).setNegativeButton("Annuler",null).setPositiveButton("Enregistrer",(d,w)->{String[] parts=input.getText().toString().trim().split("[,;\\s]+",-1);try{if(parts.length!=2)throw new IllegalArgumentException();double lat=Double.parseDouble(parts[0]),lon=Double.parseDouble(parts[1]);if(!Double.isFinite(lat)||!Double.isFinite(lon)||Math.abs(lat)>90||Math.abs(lon)>180)throw new IllegalArgumentException();destinationLat=lat;destinationLon=lon;clearNavigation();getPreferences(0).edit().putString("destination",String.format(Locale.US,"%.7f,%.7f",lat,lon)).remove("destination_label").apply();render();}catch(Exception e){Toast.makeText(this,"Coordonnées invalides. Exemple : 46.75, 5.90",Toast.LENGTH_LONG).show();}}).show();}
  // Geocodage Android : requiert un service de geocodage present sur le telephone et Internet dans la plupart des cas.
  void askAddress(){
    EditText input=new EditText(this);input.setSingleLine(true);input.setHint("Ex. Champagnole, Jura, France");
    new AlertDialog.Builder(this).setTitle("Rechercher une adresse").setMessage("Recherche via le service de geocodage Android. Une connexion peut etre necessaire.").setView(input)
      .setNegativeButton("Annuler",null).setPositiveButton("Rechercher",(dialog,which)->{
        final String query=input.getText().toString().trim();
        if(query.length()<3){Toast.makeText(this,"Saisis au moins 3 caracteres",Toast.LENGTH_LONG).show();return;}
        if(!Geocoder.isPresent()){new AlertDialog.Builder(this).setTitle("Recherche indisponible").setMessage("Aucun service de geocodage Android n'est installe sur ce telephone. Utilise les coordonnees manuelles.").setPositiveButton("OK",null).show();return;}
        Toast.makeText(this,"Recherche de l'adresse...",Toast.LENGTH_SHORT).show();
        new Thread(()->{
          try{
            final java.util.List<Address> found=new Geocoder(this,Locale.FRANCE).getFromLocationName(query,5);
            runOnUiThread(()->{
              if(isFinishing()||isDestroyed())return;
              if(found==null||found.isEmpty()){new AlertDialog.Builder(this).setTitle("Aucun resultat").setMessage("Essaie une adresse plus precise ou utilise les coordonnees.").setPositiveButton("OK",null).show();return;}
              String[] labels=new String[found.size()];
              for(int i=0;i<found.size();i++){Address a=found.get(i);String line=a.getAddressLine(0);labels[i]=(line!=null&&!line.isEmpty()?line:String.format(Locale.FRANCE,"%.5f, %.5f",a.getLatitude(),a.getLongitude()));}
              new AlertDialog.Builder(this).setTitle("Choisir une destination").setItems(labels,(d,index)->{
                Address a=found.get(index);double lat=a.getLatitude(),lon=a.getLongitude();
                if(!Double.isFinite(lat)||!Double.isFinite(lon)||Math.abs(lat)>90||Math.abs(lon)>180){Toast.makeText(this,"Coordonnees invalides",Toast.LENGTH_LONG).show();return;}
                destinationLat=lat;destinationLon=lon;clearNavigation();
                getPreferences(0).edit().putString("destination",String.format(Locale.US,"%.7f,%.7f",lat,lon)).apply();
                render();Toast.makeText(this,"Destination selectionnee",Toast.LENGTH_SHORT).show();
              }).setNegativeButton("Annuler",null).show();
            });
          }catch(Exception e){runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())new AlertDialog.Builder(this).setTitle("Recherche impossible").setMessage("Service indisponible ou connexion absente. Reessaie plus tard ou saisis les coordonnees.").setPositiveButton("OK",null).show();});}
        },"roadguard-geocoder").start();
      }).show();
  }
  // Estimation le long de la polyline OSRM. Ne remplace pas un guidage virage par virage.
  // Mesure simple de l'écart au trajet. Le point le plus proche est une approximation,
  // utile pour signaler un écart, mais pas pour décider automatiquement d'un demi-tour.
  double distanceFromNavigation(){
    if(navigationPoints.size()<2||!Double.isFinite(lastLat)||!Double.isFinite(lastLon))return Double.NaN;
    return RouteRequestPolicy.distanceToPolylineMeters(lastLat,lastLon,navigationPoints);
  }
  // Auto-reroute is conservative: repeated good GPS fixes and a 90-second cooldown.
  boolean automaticRerouteEnabled=false,routeRequestPending=false;volatile int routeCalculationId=0;
  final LatestRouteHttp routeHttp=new LatestRouteHttp();
  boolean onlineRouteRequest;
  void cancelRouteCalculation(){routeCalculationId++;routeRequestPending=false;onlineRouteRequest=false;routeHttp.invalidate(routeCalculationId);}
  void cancelUnavailableOnlineRoute(){
    if(!routeRequestPending||!onlineRouteRequest||mapNetworkAvailable())return;
    cancelRouteCalculation();reroutePolicy.resetDetection();ReferenceUi.refreshNow(this);
    Toast.makeText(this,"Calcul interrompu · trajet précédent conservé. Relance après le retour du réseau.",Toast.LENGTH_LONG).show();
  }
  int offRouteFixes=0,guidanceProgressSegment=-1;
  final CarReroutePolicy reroutePolicy=new CarReroutePolicy();
  void maybeAutoReroute(Location location){
    if(location==null)return;
    long now=SystemClock.elapsedRealtime();
    boolean enabled=mapNetworkAvailable()&&automaticRerouteEnabled&&hasDestination()&&navigationPoints.size()>1&&mapInternetAvailable()&&!gpsSignalSuspended&&AlertPresentation.fresh(location,System.currentTimeMillis(),SystemClock.elapsedRealtimeNanos());
    NavigationProgress.Position position=enabled?currentRoutePosition(routeProgressModel(),location):null;
    if(!reroutePolicy.due(location.getElapsedRealtimeNanos()/1000000L,now,position==null?Double.NaN:position.lateral,location.hasAccuracy()?location.getAccuracy():Double.NaN,gpsMotion.speedMetersPerSecond(now),enabled,routeRequestPending))return;
    Toast.makeText(this,"Écart confirmé : nouveau trajet en cours",Toast.LENGTH_SHORT).show();calculateRoadRoute();
  }
  void confirmStopGuidance(){new AlertDialog.Builder(this).setTitle("Arrêter le trajet ?").setMessage("Le guidage s’arrête. La destination reste disponible pour repartir plus tard.").setNegativeButton("Continuer",null).setPositiveButton("Arrêter",(d,w)->{clearNavigation();selected="Carte";render();Toast.makeText(this,"Guidage arrêté",Toast.LENGTH_SHORT).show();}).show();}
  void clearNavigation(){reroutePolicy.reset();lastManualMapView="";restoredSearchQuery=null;restoredDriveSheetLevel=-1;cancelRouteCalculation();if(tts!=null&&alertSpeechTracker.hasNavigation()&&!alertSpeechTracker.hasPending())tts.stop();alertSpeechTracker.cancelNavigation();navigationProgress=null;getPreferences(0).edit().remove("cached_navigation").apply();navigationPoints.clear();navigationSteps.clear();availableRoutes.clear();activeRouteIndex=0;navigationMeters=0;navigationSeconds=0;offRouteFixes=0;guidanceProgressSegment=-1;spokenStepIndex=-1;spokenThreshold=-1;}
  // Conserve automatiquement le dernier itineraire calcule dans la carte locale.
  String localDataSummary(){
    String roads=getPreferences(0).getString("offline_roads","");
    long roadBytes=roads.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
    long tileBytes=0;int tileFiles=0;
    java.util.ArrayDeque<File> pending=new java.util.ArrayDeque<>();
    File base=new File(getFilesDir(),"tiles");if(base.isDirectory())pending.add(base);
    while(!pending.isEmpty()&&tileFiles<250000){
      File current=pending.removeFirst();File[] children=current.listFiles();if(children==null)continue;
      for(File child:children){if(child.isDirectory())pending.add(child);else if(child.isFile()){tileFiles++;tileBytes+=child.length();}}
    }
    String routes=hasAutomaticOfflineRoute()?"Dernier itineraire : enregistre":"Dernier itineraire : absent";
    String tiles=tileFiles>0?"Tuiles de carte locales : "+tileFiles+" ("+(tileBytes/1024/1024)+" Mo)":"Tuiles de carte locales : aucune";
    return routes+"\nRoutes et traces : "+(roadBytes/1024)+" Ko\n"+tiles;
  }
  boolean hasAutomaticOfflineRoute(){
    try{
      String raw=getPreferences(0).getString("offline_roads","");
      if(raw.isEmpty())return false;
      JSONArray features=new JSONObject(raw).optJSONArray("features");
      if(features==null)return false;
      for(int i=0;i<features.length();i++){
        JSONObject f=features.optJSONObject(i);
        if(f!=null){JSONObject props=f.optJSONObject("properties");
          if(props!=null&&props.optBoolean("roadguardAutoRoute",false))return true;}
      }
    }catch(Exception ignored){}
    return false;
  }
  void deleteAutomaticOfflineRoute(){
    try{
      String raw=getPreferences(0).getString("offline_roads","");
      if(raw.isEmpty()){Toast.makeText(this,"Aucun trajet automatique sauvegarde",Toast.LENGTH_SHORT).show();return;}
      JSONArray existing=new JSONObject(raw).optJSONArray("features");
      JSONArray kept=new JSONArray();
      if(existing!=null)for(int i=0;i<existing.length();i++){
        JSONObject f=existing.optJSONObject(i);
        if(f==null)continue;
        JSONObject props=f.optJSONObject("properties");
        if(props==null||!props.optBoolean("roadguardAutoRoute",false))kept.put(f);
      }
      JSONObject collection=new JSONObject();collection.put("type","FeatureCollection");collection.put("features",kept);
      String json=collection.toString();
      if(!getPreferences(0).edit().putString("offline_roads",json).commit())throw new Exception("Ecriture impossible");
      offlineRoads.clear();
      if(kept.length()>0)parseRoads(json);
      Toast.makeText(this,"Dernier trajet automatique efface",Toast.LENGTH_SHORT).show();
      render();
    }catch(Exception e){Toast.makeText(this,"Suppression impossible : "+e.getMessage(),Toast.LENGTH_LONG).show();}
  }
  void saveRouteToOfflineMap(){
    if(navigationPoints.size()<2||navigationPoints.size()>4000)return;
    try{
      String old=getPreferences(0).getString("offline_roads","");
      JSONArray features=new JSONArray();
      if(!old.isEmpty()){
        JSONArray existing=new JSONObject(old).optJSONArray("features");
        if(existing!=null)for(int i=0;i<existing.length();i++){
          JSONObject f=existing.optJSONObject(i);if(f==null)continue;
          JSONObject props=f.optJSONObject("properties");
          if(props!=null&&props.optBoolean("roadguardAutoRoute",false))continue;
          features.put(f);
        }
      }
      JSONArray coords=new JSONArray();
      for(double[] pt:navigationPoints){JSONArray pair=new JSONArray();pair.put(pt[1]);pair.put(pt[0]);coords.put(pair);}
      JSONObject geometry=new JSONObject();geometry.put("type","LineString");geometry.put("coordinates",coords);
      JSONObject props=new JSONObject();props.put("roadguardAutoRoute",true);
      JSONObject feature=new JSONObject();feature.put("type","Feature");feature.put("properties",props);feature.put("geometry",geometry);
      features.put(feature);
      JSONObject collection=new JSONObject();collection.put("type","FeatureCollection");collection.put("features",features);
      String json=collection.toString();if(json.length()>400000)return;
      if(getPreferences(0).edit().putString("offline_roads",json).commit())parseRoads(json);
    }catch(Exception ignored){}
  }
  JSONObject routeOptionToJson(RouteOption option)throws Exception{JSONObject out=new JSONObject();out.put("label",option.label);out.put("meters",option.meters);out.put("seconds",option.seconds);JSONArray pts=new JSONArray();for(double[] p:option.points){JSONArray pair=new JSONArray();pair.put(p[0]);pair.put(p[1]);pts.put(pair);}out.put("points",pts);JSONArray turns=new JSONArray();for(NavStep n:option.steps){JSONObject step=new JSONObject();step.put("lat",n.lat);step.put("lon",n.lon);step.put("text",n.instruction);step.put("index",n.routeIndex);turns.put(step);}out.put("steps",turns);return out;}
  RouteOption routeOptionFromJson(JSONObject input)throws Exception{RouteOption option=new RouteOption();option.label=input.optString("label","Itinéraire");JSONArray pts=input.getJSONArray("points");if(pts.length()<2||pts.length()>RouteRequestPolicy.MAX_ROUTE_POINTS)throw new IOException("Géométrie enregistrée invalide");for(int i=0;i<pts.length();i++){JSONArray p=pts.getJSONArray(i);double lat=p.getDouble(0),lon=p.getDouble(1);if(!RouteRequestPolicy.coordinate(lat,lon))throw new IOException("Coordonnée enregistrée invalide");option.points.add(new double[]{lat,lon});}option.meters=input.getDouble("meters");option.seconds=input.getDouble("seconds");if(!RouteRequestPolicy.isValidRouteSummary(option.meters,option.seconds,option.points.size()))throw new IOException("Résumé enregistré invalide");JSONArray turns=input.optJSONArray("steps");if(turns!=null)for(int i=0;i<turns.length()&&i<300;i++){JSONObject a=turns.optJSONObject(i);if(a==null)continue;NavStep step=new NavStep(a.optDouble("lat",Double.NaN),a.optDouble("lon",Double.NaN),a.optString("text","Suivre la route"));step.routeIndex=Math.max(0,Math.min(option.points.size()-1,a.optInt("index",0)));if(RouteRequestPolicy.coordinate(step.lat,step.lon))option.steps.add(step);}return option;}
  void saveCachedNavigation(){try{JSONObject o=new JSONObject();o.put("lat",destinationLat);o.put("lon",destinationLon);o.put("meters",navigationMeters);o.put("seconds",navigationSeconds);o.put("selected_route",activeRouteIndex);JSONArray pts=new JSONArray();for(double[] p:navigationPoints){JSONArray a=new JSONArray();a.put(p[0]);a.put(p[1]);pts.put(a);}o.put("points",pts);JSONArray steps=new JSONArray();for(NavStep n:navigationSteps){JSONObject a=new JSONObject();a.put("lat",n.lat);a.put("lon",n.lon);a.put("text",n.instruction);a.put("index",n.routeIndex);steps.put(a);}o.put("steps",steps);JSONArray alternatives=new JSONArray();for(int i=0;i<availableRoutes.size()&&i<MAX_ROUTE_CHOICES;i++)alternatives.put(routeOptionToJson(availableRoutes.get(i)));o.put("alternatives",alternatives);getPreferences(0).edit().putString("cached_navigation",o.toString()).apply();saveRouteToOfflineMap();}catch(Exception ignored){}}
  void loadCachedNavigation(){
    try{
      String saved=getPreferences(0).getString("cached_navigation","");if(saved.isEmpty()||saved.length()>6000000||!hasDestination())return;
      JSONObject input=new JSONObject(saved);double lat=input.optDouble("lat",Double.NaN),lon=input.optDouble("lon",Double.NaN);
      if(!RouteRequestPolicy.coordinate(lat,lon)||Math.abs(lat-destinationLat)>0.000001||Math.abs(lon-destinationLon)>0.000001)return;
      RouteOption primary=routeOptionFromJson(input);primary.label="Trajet enregistré";
      ArrayList<RouteOption> restored=new ArrayList<>();JSONArray alternatives=input.optJSONArray("alternatives");int wanted=input.optInt("selected_route",0),selectedRestored=-1;
      if(alternatives!=null)for(int i=0;i<alternatives.length()&&i<MAX_ROUTE_CHOICES;i++)try{RouteOption option=routeOptionFromJson(alternatives.getJSONObject(i));if(i==wanted)selectedRestored=restored.size();restored.add(option);}catch(Exception ignored){}
      if(restored.isEmpty()){restored.add(primary);selectedRestored=0;}
      if(selectedRestored<0){restored.add(0,primary);selectedRestored=0;while(restored.size()>MAX_ROUTE_CHOICES)restored.remove(restored.size()-1);}
      RouteOption selectedOption=restored.get(selectedRestored);
      availableRoutes.clear();availableRoutes.addAll(restored);activeRouteIndex=selectedRestored;navigationPoints=selectedOption.points;navigationSteps=selectedOption.steps;navigationMeters=selectedOption.meters;navigationSeconds=selectedOption.seconds;
      navigationProgress=null;guidanceProgressSegment=-1;spokenStepIndex=-1;spokenThreshold=-1;
      if(restoredGuidanceSegment>=0&&restoredRouteSignature.equals(Integer.toHexString(saved.hashCode())))guidanceProgressSegment=Math.min(navigationPoints.size()-2,restoredGuidanceSegment);
      restoredGuidanceSegment=-1;restoredRouteSignature="";
    }catch(Exception invalid){android.util.Log.w("ROADGUARD","Trajet enregistré invalide : état courant conservé",invalid);}
  }
  String routeStatusText(){
    if(!hasDestination())return "Destination non définie";
    if(navigationPoints.size()<2)return "Destination : "+destinationDistance();
    if(gpsSignalSuspended||!AlertPresentation.fresh(previousAcceptedGps,System.currentTimeMillis(),SystemClock.elapsedRealtimeNanos()))return "Trajet calculé · GPS en attente";
    NavigationProgress model=routeProgressModel();NavigationProgress.Position position=currentRoutePosition(model,previousAcceptedGps);
    if(position==null)return "Trajet calculé · GPS en attente";
    if(position.lateral>100)return String.format(Locale.FRANCE,"Hors trajet (~%.0f m) : ouvre Trajet pour recalculer",position.lateral);
    if(position.arrived())return "Vous êtes à destination";
    String status=String.format(Locale.FRANCE,"Reste estimé : %.1f km sur le tracé",position.remaining/1000);
    for(NavStep step:navigationSteps)if(!NavigationProgress.departure(step.instruction)&&model.toStep(position,step.routeIndex)>=1)return status+String.format(Locale.FRANCE,"\nProchaine manœuvre (~%.0f m) : %s",model.toStep(position,step.routeIndex),step.instruction);
    return status;
  }
  // Offline GeoJSON graph: connected vertices are snapped to an approximately 1 m grid.
  // WARNING: source GeoJSON lacks one-way, turn, access, and vehicle restrictions.
  static class LocalEdge {int to;double meters;LocalEdge(int t,double m){to=t;meters=m;}}
  static class LocalNode {double lat,lon;ArrayList<LocalEdge> edges=new ArrayList<>();LocalNode(double a,double b){lat=a;lon=b;}}
  String localNodeKey(double lat,double lon){return Math.round(lat*100000)+":"+Math.round(lon*100000);}
  int localNode(java.util.HashMap<String,Integer> index,ArrayList<LocalNode> nodes,double[] p){
    String key=localNodeKey(p[0],p[1]);Integer old=index.get(key);if(old!=null)return old;
    int id=nodes.size();nodes.add(new LocalNode(p[0],p[1]));index.put(key,id);return id;
  }
  void calculateOfflineRoute(){
    if(routeRequestPending){Toast.makeText(this,"Calcul déjà en cours",Toast.LENGTH_SHORT).show();return;}
    if(!hasDestination()||!Double.isFinite(lastLat)||!Double.isFinite(lastLon)){
      Toast.makeText(this,"Position GPS et destination nécessaires",Toast.LENGTH_LONG).show();return;
    }
    if(offlineRoads.isEmpty()){Toast.makeText(this,"Importe d'abord des routes GeoJSON",Toast.LENGTH_LONG).show();return;}
    final double startLat=lastLat,startLon=lastLon,endLat=destinationLat,endLon=destinationLon;
    final int calculationId=++routeCalculationId;routeRequestPending=true;onlineRouteRequest=false;
    final ArrayList<ArrayList<double[]>> roads=new ArrayList<>(offlineRoads);
    Toast.makeText(this,"Calcul local en cours...",Toast.LENGTH_SHORT).show();
    new Thread(()->{
      try{
        ArrayList<LocalNode> nodes=new ArrayList<>();java.util.HashMap<String,Integer> index=new java.util.HashMap<>();
        // Work on a snapshot: avoid changing the graph while an import is running.
        for(ArrayList<double[]> line:roads){if(calculationId!=routeCalculationId)return;int previous=-1;
          for(double[] p:line){int current=localNode(index,nodes,p);
            if(previous>=0&&previous!=current){LocalNode a=nodes.get(previous),b=nodes.get(current);
              double meters=geoMeters(a.lat,a.lon,b.lat,b.lon);
              if(meters>0&&meters<50000){a.edges.add(new LocalEdge(current,meters));b.edges.add(new LocalEdge(previous,meters));}
            }previous=current;
          }
        }
        if(nodes.size()<2)throw new IOException("Réseau local insuffisant");
        int start=-1,end=-1;double ds=Double.MAX_VALUE,de=Double.MAX_VALUE;
        for(int i=0;i<nodes.size();i++){LocalNode n=nodes.get(i);
          double a=geoMeters(startLat,startLon,n.lat,n.lon),b=geoMeters(endLat,endLon,n.lat,n.lon);
          if(a<ds){ds=a;start=i;}if(b<de){de=b;end=i;}
        }
        if(ds>250||de>250)throw new IOException("Départ ou destination à plus de 250 m des routes importées");
        double[] dist=new double[nodes.size()];int[] prev=new int[nodes.size()];
        java.util.Arrays.fill(dist,Double.POSITIVE_INFINITY);java.util.Arrays.fill(prev,-1);
        java.util.PriorityQueue<double[]> queue=new java.util.PriorityQueue<>((a,b)->Double.compare(a[0],b[0]));
        dist[start]=0;queue.add(new double[]{0,start});
        while(!queue.isEmpty()){if(calculationId!=routeCalculationId)return;double[] item=queue.poll();int id=(int)item[1];if(item[0]>dist[id])continue;if(id==end)break;
          for(LocalEdge e:nodes.get(id).edges){double nd=dist[id]+e.meters;if(nd<dist[e.to]){dist[e.to]=nd;prev[e.to]=id;queue.add(new double[]{nd,e.to});}}
        }
        if(!Double.isFinite(dist[end]))throw new IOException("Réseau non connecté entre départ et arrivée");
        ArrayList<double[]> result=new ArrayList<>();int at=end;
        while(at>=0&&result.size()<=nodes.size()){LocalNode n=nodes.get(at);result.add(new double[]{n.lat,n.lon});at=prev[at];}
        java.util.Collections.reverse(result);
        if(result.size()<2)throw new IOException("Tracé local trop court");
        final double meters=dist[end]+ds+de;
        runOnUiThread(()->{if(isFinishing()||isDestroyed()||calculationId!=routeCalculationId)return;
          routeRequestPending=false;if(destinationLat!=endLat||destinationLon!=endLon)return;
          navigationPoints=result;navigationSteps.clear();availableRoutes.clear();activeRouteIndex=0;navigationMeters=meters;guidanceProgressSegment=-1;
          navigationSeconds=meters/13.89;spokenStepIndex=-1;spokenThreshold=-1;
          saveCachedNavigation();offlineView=true;selected="Carte";render();
          Toast.makeText(this,"Trajet local expérimental : restrictions routières inconnues",Toast.LENGTH_LONG).show();
        });
      }catch(Exception ex){String message=ex.getMessage();runOnUiThread(()->{if(calculationId!=routeCalculationId)return;routeRequestPending=false;ReferenceUi.refreshNow(this);if(!isFinishing()&&!isDestroyed())new AlertDialog.Builder(this).setTitle("Calcul local impossible").setMessage(message).setPositiveButton("OK",null).show();});}
    },"roadguard-offline-routing").start();
  }

  // Prototype de routage en ligne : serveur OSRM de demonstration, non adapte a une exploitation commerciale.
  void calculateRoadRoute(){
    if(routeRequestPending){Toast.makeText(this,"Calcul déjà en cours",Toast.LENGTH_SHORT).show();return;}
    if(!hasDestination()){Toast.makeText(this,"Choisis une destination",Toast.LENGTH_LONG).show();return;}
    if(!Double.isFinite(lastLat)||!Double.isFinite(lastLon)){new AlertDialog.Builder(this).setTitle("Position GPS nécessaire").setMessage("Ouvre la carte, autorise la localisation précise et attends l'apparition du point bleu. Tu pourras ensuite calculer ton itinéraire.").setNegativeButton("Fermer",null).setPositiveButton("Ouvrir la carte",(dialog,which)->{selected="Carte";render();}).show();return;}
    if(!AlertPresentation.fresh(previousAcceptedGps,System.currentTimeMillis(),SystemClock.elapsedRealtimeNanos())){Toast.makeText(this,"Attends une position GPS récente et précise avant de calculer le trajet",Toast.LENGTH_LONG).show();return;}
    if(!mapNetworkAvailable()){Toast.makeText(this,"Calcul en ligne indisponible · passe en Auto ou En ligne avec Internet. Le trajet enregistré reste disponible",Toast.LENGTH_LONG).show();return;}
    routeRequestPending=true;onlineRouteRequest=true;final int calculationId=++routeCalculationId;routeHttp.invalidate(calculationId);ReferenceUi.refreshNow(this);
    final double fromLat=lastLat,fromLon=lastLon,toLat=destinationLat,toLon=destinationLon;
    Toast.makeText(this,"Calcul du trajet en ligne...",Toast.LENGTH_SHORT).show();
    new Thread(()->{
      try{
        String url=RouteRequestPolicy.osrmUrl(fromLat,fromLon,toLat,toLon);
        JSONObject response=new JSONObject(routeHttp.read(url,calculationId,1500000));
        if(!"Ok".equals(response.optString("code")))throw new IOException("Aucun trajet routier trouvé");
        JSONArray routeArray=response.optJSONArray("routes");
        if(routeArray==null||routeArray.length()==0)throw new IOException("Aucun trajet routier trouvé");
        ArrayList<RouteOption> computed=new ArrayList<>();
        for(int ri=0;ri<routeArray.length()&&ri<MAX_ROUTE_CHOICES;ri++){
          try{computed.add(parseOsrmRoute(routeArray.getJSONObject(ri)));}
          catch(Exception invalid){if(ri==0)throw invalid;android.util.Log.w("ROADGUARD","Alternative OSRM ignorée car invalide",invalid);}
        }
        if(computed.isEmpty())throw new IOException("Aucun itinéraire valide dans la réponse");
        java.util.Collections.sort(computed,(a,b)->Double.compare(a.seconds,b.seconds));
        int shortest=0;for(int i=1;i<computed.size();i++)if(computed.get(i).meters<computed.get(shortest).meters)shortest=i;
        for(int i=0;i<computed.size();i++)computed.get(i).label=i==0?"Le plus rapide":i==shortest?"Le plus court":"Alternative "+(i+1);
        final ArrayList<RouteOption> routes=computed;
        runOnUiThread(()->{if(isFinishing()||isDestroyed()||calculationId!=routeCalculationId)return;if(destinationLat!=toLat||destinationLon!=toLon){routeRequestPending=false;onlineRouteRequest=false;ReferenceUi.refreshNow(this);Toast.makeText(this,"Destination modifiée pendant le calcul; relance l’itinéraire",Toast.LENGTH_LONG).show();return;}availableRoutes.clear();availableRoutes.addAll(routes);routeRequestPending=false;onlineRouteRequest=false;applyRouteOption(0,true);Toast.makeText(this,routes.size()>1?routes.size()+" itinéraires calculés · choisis celui qui te convient":"Trajet calculé et conservé sur le téléphone",Toast.LENGTH_LONG).show();});
      }catch(Exception e){final String message=e.getMessage();runOnUiThread(()->{if(calculationId!=routeCalculationId)return;routeRequestPending=false;onlineRouteRequest=false;ReferenceUi.refreshNow(this);if(!isFinishing()&&!isDestroyed())new AlertDialog.Builder(this).setTitle("Calcul indisponible").setMessage("Le serveur de démonstration ne répond pas ou aucun trajet n'est disponible. Le dernier itinéraire conservé reste disponible s'il correspond à cette destination. "+(message==null?"":message)).setPositiveButton("OK",null).show();});}
    },"roadguard-osrm-route").start();
  }
  RouteOption parseOsrmRoute(JSONObject route)throws Exception{
    RouteOption option=new RouteOption();JSONArray coordinates=route.getJSONObject("geometry").getJSONArray("coordinates");
    if(coordinates.length()<2||coordinates.length()>RouteRequestPolicy.MAX_ROUTE_POINTS)throw new IOException("Géométrie du trajet trop courte ou trop détaillée");
    for(int i=0;i<coordinates.length();i++){JSONArray pair=coordinates.getJSONArray(i);if(pair.length()<2)throw new IOException("Coordonnée de trajet incomplète");double lon=pair.getDouble(0),lat=pair.getDouble(1);if(!RouteRequestPolicy.coordinate(lat,lon))throw new IOException("Coordonnée de trajet hors limites");option.points.add(new double[]{lat,lon});}
    JSONArray legs=route.optJSONArray("legs");if(legs!=null)for(int li=0;li<legs.length();li++){JSONObject leg=legs.optJSONObject(li);JSONArray maneuvers=leg==null?null:leg.optJSONArray("steps");if(maneuvers==null)continue;for(int si=0;si<maneuvers.length()&&option.steps.size()<300;si++){JSONObject step=maneuvers.optJSONObject(si),m=step==null?null:step.optJSONObject("maneuver");if(m==null)continue;JSONArray loc=m.optJSONArray("location");if(loc==null||loc.length()<2)continue;String instruction=navInstruction(m.optString("type",""),m.optString("modifier",""),step.optString("name",""),m.optInt("exit",0));option.steps.add(new NavStep(loc.getDouble(1),loc.getDouble(0),instruction));}}
    int searchFrom=0;for(NavStep step:option.steps){int best=searchFrom;double min=Double.MAX_VALUE;for(int j=searchFrom;j<option.points.size();j++){double[] pt=option.points.get(j);double distance=geoMeters(step.lat,step.lon,pt[0],pt[1]);if(distance<min){min=distance;best=j;}}step.routeIndex=best;searchFrom=best;}
    option.meters=route.getDouble("distance");option.seconds=route.getDouble("duration");if(!RouteRequestPolicy.isValidRouteSummary(option.meters,option.seconds,option.points.size()))throw new IOException("Durée, distance ou géométrie du trajet invalide");return option;
  }
  void applyRouteOption(int index,boolean rerender){if(index<0||index>=availableRoutes.size())return;reroutePolicy.resetDetection();activeRouteIndex=index;navigationProgress=null;RouteOption option=availableRoutes.get(index);navigationPoints=option.points;navigationSteps=option.steps;navigationMeters=option.meters;navigationSeconds=option.seconds;guidanceProgressSegment=-1;spokenStepIndex=-1;spokenThreshold=-1;saveCachedNavigation();if(map!=null&&mapReady)pushRoute();if(rerender)render();}
  String navInstruction(String type,String modifier,String street){return navInstruction(type,modifier,street,0);}
  String navInstruction(String type,String modifier,String street,int exitNumber){
    String action;
    if("arrive".equals(type))action="Arrivée à destination";
    else if("depart".equals(type))action="Départ";
    else if("roundabout".equals(type)||"rotary".equals(type))action=exitNumber>0?"Dans le rond-point, prendre la sortie "+exitNumber:"Entrer dans le rond-point";
    else if("exit roundabout".equals(type)||"exit rotary".equals(type))action="Sortir du rond-point";
    else if("merge".equals(type))action="Rejoindre la voie";
    else if("on ramp".equals(type))action="Prendre la bretelle";
    else if("off ramp".equals(type))action="Prendre la sortie";
    else if("continue".equals(type)||"new name".equals(type))action="Continuer tout droit";
    else if("turn".equals(type)||"end of road".equals(type)||"fork".equals(type)){
      if(modifier.contains("uturn"))action="Faire demi-tour";
      else if(modifier.contains("slight left"))action="Obliquer légèrement à gauche";
      else if(modifier.contains("sharp left"))action="Tourner franchement à gauche";
      else if(modifier.contains("left"))action="Tourner à gauche";
      else if(modifier.contains("slight right"))action="Obliquer légèrement à droite";
      else if(modifier.contains("sharp right"))action="Tourner franchement à droite";
      else if(modifier.contains("right"))action="Tourner à droite";
      else if(modifier.contains("straight"))action="Continuer tout droit";
      else action="Continuer";
    }else action="Suivre la route";
    if(street!=null&&!street.trim().isEmpty())action+=" : "+street.substring(0,Math.min(60,street.length()));
    return action;
  }
  // Favoris de destinations stockes uniquement sur cet appareil, sans API externe.
  JSONArray destinationFavorites(){try{return new JSONArray(getPreferences(0).getString("destination_favorites","[]"));}catch(Exception e){return new JSONArray();}}
  void saveDestinationFavorite(){if(!hasDestination())return;EditText name=new EditText(this);name.setSingleLine(true);name.setHint("Ex. Maison, Travail");new AlertDialog.Builder(this).setTitle("Nom du favori").setView(name).setNegativeButton("Annuler",null).setPositiveButton("Enregistrer",(d,w)->{String n=name.getText().toString().trim();if(n.length()==0){Toast.makeText(this,"Donne un nom à ce favori",Toast.LENGTH_LONG).show();return;}try{JSONArray list=destinationFavorites();int replace=-1;for(int i=0;i<list.length();i++){JSONObject old=list.optJSONObject(i);if(old!=null&&FavoriteNames.same(n,old.optString("name"))){replace=i;break;}}if(replace<0&&list.length()>=30){Toast.makeText(this,"Maximum 30 favoris",Toast.LENGTH_LONG).show();return;}JSONObject item=new JSONObject();item.put("name",n.substring(0,Math.min(60,n.length())));item.put("lat",destinationLat);item.put("lon",destinationLon);if(replace>=0)list.put(replace,item);else list.put(item);getPreferences(0).edit().putString("destination_favorites",list.toString()).apply();Toast.makeText(this,"Favori enregistré",Toast.LENGTH_SHORT).show();ReferenceUi.refreshNow(this);}catch(Exception e){Toast.makeText(this,"Impossible d'enregistrer",Toast.LENGTH_LONG).show();}}).show();}
  void showDestinationFavorites(){legacySection=false;selected="Favoris";render();}
  void openNamedFavorite(String name){openNamedFavorite(name,false);}
  void openNamedFavorite(String name,boolean redefine){
    JSONArray list=destinationFavorites();if(!redefine)for(int i=0;i<list.length();i++){JSONObject item=list.optJSONObject(i);if(item!=null&&FavoriteNames.same(name,item.optString("name"))){chooseDestinationFavorite(i);return;}}
    boolean hasFix=previousAcceptedGps!=null&&System.currentTimeMillis()-previousAcceptedGps.getTime()<30000&&RouteRequestPolicy.coordinate(lastLat,lastLon);
    if(!hasDestination()&&!hasFix){Toast.makeText(this,"Attends une position GPS, puis enregistre "+name,Toast.LENGTH_LONG).show();return;}
    ArrayList<String> choices=new ArrayList<>();if(hasDestination())choices.add("Enregistrer la destination actuelle");if(hasFix)choices.add("Enregistrer ma position GPS actuelle");
    new AlertDialog.Builder(this).setTitle("Définir "+name).setItems(choices.toArray(new String[0]),(d,which)->{
      boolean current=choices.get(which).contains("GPS");double lat=current?lastLat:destinationLat,lon=current?lastLon:destinationLon;
      if(!RouteRequestPolicy.coordinate(lat,lon))return;
      try{JSONArray saved=destinationFavorites();JSONObject item=new JSONObject().put("name",name).put("lat",lat).put("lon",lon);int replace=-1;for(int i=0;i<saved.length();i++)if(FavoriteNames.same(name,saved.optJSONObject(i)==null?"":saved.optJSONObject(i).optString("name"))){replace=i;break;}if(replace>=0)saved.put(replace,item);else{if(saved.length()>=30){Toast.makeText(this,"Maximum 30 favoris : supprime un favori avant d’en ajouter",Toast.LENGTH_LONG).show();return;}saved.put(item);}getPreferences(0).edit().putString("destination_favorites",saved.toString()).apply();Toast.makeText(this,name+" enregistré · touche le raccourci pour y aller",Toast.LENGTH_LONG).show();ReferenceUi.refreshNow(this);}catch(Exception error){Toast.makeText(this,"Enregistrement impossible",Toast.LENGTH_LONG).show();}
    }).setNegativeButton("Annuler",null).show();
  }
  void chooseDestinationFavorite(int index){
    JSONObject o=destinationFavorites().optJSONObject(index);if(o==null)return;
    double lat=o.optDouble("lat",Double.NaN),lon=o.optDouble("lon",Double.NaN);
    if(!RouteRequestPolicy.coordinate(lat,lon))return;
    rememberRecentDestination(lat,lon,o.optString("name","Favori"));destinationLat=lat;destinationLon=lon;clearNavigation();
    getPreferences(0).edit().putString("destination",String.format(Locale.US,"%.7f,%.7f",lat,lon)).putString("destination_label",o.optString("name","Favori")).apply();
    legacySection=false;selected="Carte";render();
    if(previousAcceptedGps!=null&&!routeRequestPending)calculateRoadRoute();
  }
  void confirmRemoveDestination(){new AlertDialog.Builder(this).setTitle("Retirer la destination ?").setMessage("Le trajet calculé sera aussi effacé. Tes favoris sont conservés.").setNegativeButton("Annuler",null).setPositiveButton("Retirer",(d,w)->{destinationLat=Double.NaN;destinationLon=Double.NaN;clearNavigation();getPreferences(0).edit().remove("destination").remove("destination_label").apply();render();}).show();}

  void deleteDestinationFavorite(int index){JSONArray list=destinationFavorites(),updated=new JSONArray();for(int i=0;i<list.length();i++)if(i!=index)updated.put(list.opt(i));getPreferences(0).edit().putString("destination_favorites",updated.toString()).apply();Toast.makeText(this,"Favori supprimé",Toast.LENGTH_SHORT).show();}
  // Journal GPS prive : aucun serveur, enregistrement seulement lorsque la carte est ouverte.
  void loadTrip(){android.content.SharedPreferences pref=getPreferences(0);tripRecording=false;tripStart=pref.getLong("trip_start",0);tripMeters=Double.longBitsToDouble(pref.getLong("trip_meters",Double.doubleToLongBits(0)));tripSamples=pref.getInt("trip_samples",0);try{JSONArray a=new JSONArray(pref.getString("trip_track","[]"));for(int i=0;i<a.length()&&i<20000;i++){JSONArray pt=a.getJSONArray(i);tripTrack.add(new double[]{pt.getDouble(0),pt.getDouble(1),pt.getDouble(2)});}}catch(Exception ignored){tripTrack.clear();}}
  void saveTripTrack(){try{JSONArray a=new JSONArray();for(double[] pt:tripTrack){JSONArray one=new JSONArray();one.put(pt[0]);one.put(pt[1]);one.put((long)pt[2]);a.put(one);}getPreferences(0).edit().putString("trip_track",a.toString()).apply();}catch(org.json.JSONException e){android.util.Log.e("ROADGUARD","Erreur de sauvegarde du trajet GPS",e);}}
  void exportTripGpx(){saveTripTrack();Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/gpx+xml");i.putExtra(Intent.EXTRA_TITLE,"roadguard-trajet-enregistre.gpx");DocumentPicker.open(this,i,49);}
  void writeTripGpx(Uri uri)throws Exception{try(OutputStream out=getContentResolver().openOutputStream(uri);OutputStreamWriter w=new OutputStreamWriter(out,"UTF-8")){w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?><gpx version=\"1.1\" creator=\"ROADGUARD\" xmlns=\"http://www.topografix.com/GPX/1/1\"><trk><name>Trajet ROADGUARD</name><trkseg>");for(double[] pt:tripTrack){w.write(String.format(Locale.US,"<trkpt lat=\"%.7f\" lon=\"%.7f\"><time>%s</time></trkpt>",pt[0],pt[1],new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",Locale.US){{setTimeZone(java.util.TimeZone.getTimeZone("UTC"));}}.format(new Date((long)pt[2]))));}w.write("</trkseg></trk></gpx>");}}
  void recordTripPoint(Location l){if(!tripRecording||!l.hasAccuracy()||l.getAccuracy()>70)return;if(previousTripLocation!=null){float d=previousTripLocation.distanceTo(l);if(d<3)return;if(d>300||d>Math.max(25,(l.getTime()-previousTripLocation.getTime())/1000.0*65)) {previousTripLocation=new Location(l);return;}tripMeters+=d;}previousTripLocation=new Location(l);tripSamples++;if(tripTrack.size()<20000){tripTrack.add(new double[]{l.getLatitude(),l.getLongitude(),l.getTime()});if(tripTrack.size()%5==0)saveTripTrack();}getPreferences(0).edit().putLong("trip_meters",Double.doubleToLongBits(tripMeters)).putInt("trip_samples",tripSamples).apply();}
  String tripDistance(){return String.format(Locale.FRANCE,"%.2f km",tripMeters/1000.0);}
  // Mesures GPX indicatives, sans guidage routier ni calcul d'itineraire.
  double geoMeters(double lat1,double lon1,double lat2,double lon2){double r=6371000.0;double a=Math.sin(Math.toRadians(lat2-lat1)/2);double b=Math.sin(Math.toRadians(lon2-lon1)/2);double h=a*a+Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))*b*b;return 2*r*Math.asin(Math.min(1,Math.sqrt(Math.max(0,h))));}
  double gpxLengthMeters(){double total=0;for(int i=1;i<routePoints.size();i++){double[] a=routePoints.get(i-1),b=routePoints.get(i);total+=geoMeters(a[0],a[1],b[0],b[1]);}return total;}
  // Approximation sur une polyligne GPX, sans calcul d'itineraire routier.
  String gpxProgress(){
    if(routePoints.size()<2||Double.isNaN(lastLat)||Double.isNaN(lastLon))return "Progression GPX indisponible : position GPS requise.";
    double best=Double.POSITIVE_INFINITY,remaining=0,done=0,walked=0;
    double total=gpxLengthMeters();
    for(int i=1;i<routePoints.size();i++){
      double[] a=routePoints.get(i-1),b=routePoints.get(i);
      double latScale=111195.0,lonScale=latScale*Math.cos(Math.toRadians((a[0]+b[0])*0.5));
      double dx=(b[1]-a[1])*lonScale,dy=(b[0]-a[0])*latScale;
      double px=(lastLon-a[1])*lonScale,py=(lastLat-a[0])*latScale;
      double len2=dx*dx+dy*dy;
      double t=len2>0?Math.max(0,Math.min(1,(px*dx+py*dy)/len2)):0;
      double dist=Math.hypot(px-t*dx,py-t*dy);
      double segment=geoMeters(a[0],a[1],b[0],b[1]);
      if(dist<best){best=dist;done=walked+segment*t;remaining=Math.max(0,total-done);}
      walked+=segment;
    }
    return String.format(Locale.FRANCE,"Distance au trace : %.0f m\nProgression estimee : %.0f %%\nReste sur le trace : %.2f km%s",best,total>0?100*done/total:0,remaining/1000.0,best>150?"\nVous etes a plus de 150 m du trace.":"");
  }
  String gpxInfo(){if(routePoints.size()<2)return "Importez un GPX avec au moins deux points pour mesurer le trace.";double[] end=routePoints.get(routePoints.size()-1);String result=String.format(Locale.FRANCE,"Longueur du trace GPX : %.2f km • %d points",gpxLengthMeters()/1000.0,routePoints.size());if(!Double.isNaN(lastLat)){result+=String.format(Locale.FRANCE,"\nDistance a vol d'oiseau jusqu'au dernier point : %.2f km",geoMeters(lastLat,lastLon,end[0],end[1])/1000.0);}return result;}
  void showTrip(LinearLayout content){TextView hero=label("◈  MON TABLEAU DE BORD",16);hero.setTextColor(0xffffc5c9);hero.setTypeface(null,Typeface.BOLD);hero.setPadding(14,16,14,16);hero.setBackground(outlined(0xff211a22,0xff74323e,20));add(content,hero);add(content,label("Journal GPS local et privé",15));add(content,label("Distance : "+tripDistance(),21));add(content,label("Points GPS retenus : "+tripSamples,14));add(content,label("Etat : "+(tripRecording?"Enregistrement actif":"A l'arret"),15));add(content,label("Pour enregistrer, restez sur la carte : Android suspend le GPS lorsque cette page est ouverte ou l'application est en arriere-plan.",13));add(content,button(tripRecording?"Arreter le trajet":"Demarrer un nouveau trajet",()->{if(tripRecording){tripRecording=false;previousTripLocation=null;saveTripTrack();Toast.makeText(this,"Trajet arrete",Toast.LENGTH_SHORT).show();}else{new AlertDialog.Builder(this).setTitle("Demarrer un nouveau trajet ?").setMessage("La distance precedente sera remise a zero. Le GPS fonctionne uniquement sur la carte.").setNegativeButton("Annuler",null).setPositiveButton("Demarrer",(d,w)->{tripMeters=0;tripSamples=0;tripTrack.clear();saveTripTrack();tripStart=System.currentTimeMillis();previousTripLocation=null;tripRecording=true;getPreferences(0).edit().putLong("trip_start",tripStart).putLong("trip_meters",Double.doubleToLongBits(0)).putInt("trip_samples",0).apply();selected="Carte";render();Toast.makeText(this,"Trajet actif : laissez la carte ouverte",Toast.LENGTH_LONG).show();}).show();return;}render();}));if(!tripTrack.isEmpty())add(content,button("Exporter mon trajet enregistré (GPX)",()->exportTripGpx()));if(tripSamples>0)add(content,button("Effacer les statistiques",()->new AlertDialog.Builder(this).setTitle("Effacer le trajet ?").setNegativeButton("Annuler",null).setPositiveButton("Effacer",(d,w)->{tripRecording=false;tripMeters=0;tripSamples=0;tripTrack.clear();tripStart=0;previousTripLocation=null;getPreferences(0).edit().remove("trip_start").remove("trip_meters").remove("trip_samples").remove("trip_track").apply();render();}).show()));}
  // Export explicite : aucune transmission automatique, le fichier est choisi par l'utilisateur.
  void exportReports(){String saved=getPreferences(0).getString("report_history","");if(saved.isEmpty()){Toast.makeText(this,"Aucun signalement à exporter",Toast.LENGTH_SHORT).show();return;}Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("text/csv");intent.putExtra(Intent.EXTRA_TITLE,"roadguard-signalements.csv");DocumentPicker.open(this,intent,46);}
  String csvCell(String value){return "\""+value.replace("\"","\"\"").replace("\r"," ").replace("\n"," ")+"\"";}
  void writeReportsCsv(Uri uri)throws IOException{String saved=getPreferences(0).getString("report_history","");try(OutputStream stream=getContentResolver().openOutputStream(uri);OutputStreamWriter writer=new OutputStreamWriter(stream,"UTF-8")){writer.write("type;date;position\n");for(String entry:saved.split("\n")){String[] fields=entry.split(" • ",3);if(fields.length==3){writer.write(csvCell(fields[0])+";"+csvCell(fields[1])+";"+csvCell(fields[2])+"\n");}}writer.flush();}}
  // 11.14.63 : confirmation explicite avant tout enregistrement de danger.
  void confirmLocalReport(String kind){
    if(community!=null){refreshLastPosition();boolean connected=community.paired;
      new AlertDialog.Builder(this).setTitle(connected?"Partager : "+kind:"Signalement : "+kind).setMessage(connected?"Le signalement sera partagé avec la communauté à votre position GPS récente et précise. Vous pourrez le retirer depuis Communauté.":"Connectez votre compte pour partager avec les autres conducteurs. L’enregistrement privé reste disponible.").setNegativeButton("Annuler",null).setNeutralButton("Garder en privé",(dialog,which)->saveReport(kind)).setPositiveButton(connected?"Partager":"Connecter mon compte",(dialog,which)->{if(connected)community.publish(kind);else{selected="Communauté";render();community.link();}}).show();return;
    }

    refreshLastPosition();
    boolean gpsReady=hasFreshPreciseLocation();
    String gpsStatus=gpsReady?String.format(Locale.FRANCE,"Position disponible : %.5f, %.5f",lastLat,lastLon):"Position GPS indisponible : le signalement sera enregistré sans coordonnées.";
    new AlertDialog.Builder(this)
      .setTitle("Confirmer : "+kind)
      .setMessage(gpsStatus+"\n\nCe signalement est enregistré uniquement sur ce téléphone. Il n'est pas partagé en ligne.")
      .setNegativeButton("Annuler",null)
      .setPositiveButton("Enregistrer",(dialog,which)->saveReport(kind))
      .show();
  }
  void saveReport(String kind){refreshLastPosition();String when=new SimpleDateFormat("dd/MM HH:mm",Locale.FRANCE).format(new Date());String loc=!hasFreshPreciseLocation()?"position inconnue":String.format(Locale.US,"%.5f, %.5f",lastLat,lastLon);String report=kind+" • "+when+" • "+loc;
    String existing=getPreferences(0).getString("report_history","");
    StringBuilder history=new StringBuilder(report);
    if(!existing.isEmpty()){String[] previous=existing.split("\n");for(int i=0;i<previous.length&&i<49;i++)history.append("\n").append(previous[i]);}
    getPreferences(0).edit().putString("last_report",report).putString("report_history",history.toString()).apply();
    if(hasFreshPreciseLocation()){
      try{if("mobile".equals(CommunityPolicy.kind(kind))){ControlMemory.ingest(this,java.util.Collections.singletonList(new ControlMemory.Observation("local",java.util.UUID.randomUUID().toString(),"mobile",lastLat,lastLon,System.currentTimeMillis())),System.currentTimeMillis());reloadControlMemory(true);}else{
      JSONArray points=new JSONArray(getPreferences(0).getString("custom_radars","[]"));JSONObject point=new JSONObject();long now=System.currentTimeMillis();point.put("lat",lastLat);point.put("lon",lastLon);point.put("type",kind);point.put("created",now);point.put("expires",kind.contains("fixe")||kind.contains("feu rouge")?0:now+2*60*60*1000L);point.put("source","report");
        boolean duplicate=false;for(int i=0;i<points.length();i++){JSONObject existingPoint=points.optJSONObject(i);if(existingPoint!=null&&kind.equals(existingPoint.optString("type"))&&geoMeters(lastLat,lastLon,existingPoint.optDouble("lat",999),existingPoint.optDouble("lon",999))<30){points.put(i,point);duplicate=true;break;}}
        if(!duplicate){if(points.length()<500)points.put(point);else for(int i=0;i<points.length();i++){JSONObject older=points.optJSONObject(i);if(older!=null&&"report".equals(older.optString("source"))){points.put(i,point);break;}}}getPreferences(0).edit().putString("custom_radars",points.toString()).apply();requestRadarCatalogReload(true);BackgroundAlertService.requestRadarReload();
      }}catch(Exception error){Toast.makeText(this,"Marqueur non enregistré : "+error.getMessage(),Toast.LENGTH_LONG).show();}
    }
    Toast.makeText(this,"Signalement enregistré sur ce téléphone",Toast.LENGTH_LONG).show();render();}
  boolean hasFreshPreciseLocation(){if(previousAcceptedGps==null)return false;long age=System.currentTimeMillis()-previousAcceptedGps.getTime();return age>=-5000&&age<30000&&previousAcceptedGps.hasAccuracy()&&previousAcceptedGps.getAccuracy()<=60;}
  void refreshLastPosition(){try{if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED||checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED){Location fix=lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);Location network=lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);if(network!=null&&(fix==null||network.getTime()>fix.getTime())&&network.hasAccuracy()&&network.getAccuracy()<=60)fix=network;if(fix!=null&&System.currentTimeMillis()-fix.getTime()>=-5000&&System.currentTimeMillis()-fix.getTime()<30000&&fix.hasAccuracy()&&fix.getAccuracy()<=60){lastLat=fix.getLatitude();lastLon=fix.getLongitude();lastAccuracy=fix.getAccuracy();previousAcceptedGps=new Location(fix);}}}catch(Exception ignored){}}

  void openRoads(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);DocumentPicker.open(this,i,47);}
  // Conserve un itineraire deja calcule dans les donnees locales, sans telechargement manuel.
  void keepNavigationOffline(){
    if(navigationPoints.size()<2){Toast.makeText(this,"Calculez d'abord un trajet",Toast.LENGTH_SHORT).show();return;}
    int points=0;for(ArrayList<double[]> road:offlineRoads)points+=road.size();
    if(points+navigationPoints.size()>4000){Toast.makeText(this,"Limite de 4 000 points locaux atteinte",Toast.LENGTH_LONG).show();return;}
    try{
      JSONObject collection=new JSONObject();collection.put("type","FeatureCollection");
      JSONArray features=new JSONArray();
      for(ArrayList<double[]> line:offlineRoads){
        JSONArray coords=new JSONArray();for(double[] p:line){JSONArray xy=new JSONArray();xy.put(p[1]);xy.put(p[0]);coords.put(xy);}
        JSONObject geometry=new JSONObject();geometry.put("type","LineString");geometry.put("coordinates",coords);
        JSONObject feature=new JSONObject();feature.put("type","Feature");feature.put("geometry",geometry);features.put(feature);
      }
      JSONArray coords=new JSONArray();for(double[] p:navigationPoints){JSONArray xy=new JSONArray();xy.put(p[1]);xy.put(p[0]);coords.put(xy);}
      JSONObject geometry=new JSONObject();geometry.put("type","LineString");geometry.put("coordinates",coords);
      JSONObject feature=new JSONObject();feature.put("type","Feature");feature.put("geometry",geometry);features.put(feature);
      collection.put("features",features);
      String saved=collection.toString();
      if(!getPreferences(0).edit().putString("offline_roads",saved).commit())throw new IOException("Sauvegarde impossible");
      parseRoads(saved);
      Toast.makeText(this,"Trajet sauvegarde pour consultation hors ligne",Toast.LENGTH_LONG).show();
      pushRoads();render();
    }catch(Exception e){Toast.makeText(this,"Erreur : "+e.getMessage(),Toast.LENGTH_LONG).show();}
  }
  // Un GeoJSON livre dans assets est charge une seule fois, sans import manuel.
  // Les routes deja enregistrees par l'utilisateur ne sont jamais ecrasees.
  // Ajoute les routes embarquees sans effacer les traces et trajets deja conserves.
  // Un identifiant de version evite de dupliquer les routes au prochain lancement.
  void installBundledRoadsIfPresent(){
    final String asset="offline-roads.geojson";
    try(java.io.InputStream in=getAssets().open(asset)){
      java.io.ByteArrayOutputStream output=new java.io.ByteArrayOutputStream();
      byte[] buf=new byte[8192];int n,total=0;
      while((n=in.read(buf))!=-1){total+=n;if(total>350000)throw new IOException("GeoJSON embarque trop volumineux");output.write(buf,0,n);}
      String incoming=output.toString("UTF-8");
      JSONObject root=new JSONObject(incoming);
      if(!"FeatureCollection".equals(root.optString("type")))return;
      JSONArray newFeatures=root.optJSONArray("features");if(newFeatures==null||newFeatures.length()==0)return;
      String signature=Integer.toHexString(incoming.hashCode())+"-"+incoming.length();
      android.content.SharedPreferences prefs=getPreferences(0);
      if(signature.equals(prefs.getString("bundled_roads_signature","")))return;
      JSONArray combined=new JSONArray();
      String existing=prefs.getString("offline_roads","");
      if(!existing.isEmpty()){
        JSONArray previous=new JSONObject(existing).optJSONArray("features");
        if(previous!=null)for(int i=0;i<previous.length();i++){
          JSONObject f=previous.optJSONObject(i);if(f==null)continue;
          JSONObject props=f.optJSONObject("properties");
          // Retirer seulement les routes du precedent pack embarque.
          if(props!=null&&props.optBoolean("roadguardBundledRoad",false))continue;
          combined.put(f);
        }
      }
      for(int i=0;i<newFeatures.length();i++){
        JSONObject f=newFeatures.optJSONObject(i);if(f==null)continue;
        JSONObject props=f.optJSONObject("properties");if(props==null)props=new JSONObject();
        props.put("roadguardBundledRoad",true);f.put("properties",props);combined.put(f);
      }
      JSONObject result=new JSONObject();result.put("type","FeatureCollection");result.put("features",combined);
      String json=result.toString();
      if(json.getBytes("UTF-8").length>400000)return;
      // Validation sans modifier l'etat courant en cas d'erreur.
      ArrayList<ArrayList<double[]>> before=offlineRoads;
      int points;
      try{points=parseRoads(json);}catch(Exception invalid){offlineRoads=before;return;}
      if(points<2){offlineRoads=before;return;}
      if(prefs.edit().putString("offline_roads",json).putString("bundled_roads_signature",signature).commit()){
        pushRoads();
      }else offlineRoads=before;
    }catch(java.io.FileNotFoundException absent){/* Aucun pack fourni. */}
    catch(Exception ignored){/* Ne jamais bloquer le demarrage. */}
  }
  void loadRoads(){String saved=getPreferences(0).getString("offline_roads","");if(!saved.isEmpty())try{parseRoads(saved);}catch(Exception ignored){offlineRoads.clear();}}
  // Valide tout le GeoJSON avant de remplacer les routes en memoire.
  // Un fichier trop grand est refuse plutot que coupe silencieusement.
  int parseRoads(String json)throws Exception{
    JSONObject root=new JSONObject(json);
    JSONArray features=root.optJSONArray("features");
    if(features==null)throw new Exception("FeatureCollection GeoJSON requise");
    ArrayList<ArrayList<double[]>> parsed=new ArrayList<>();
    int[] count={0};
    for(int i=0;i<features.length();i++){
      JSONObject feature=features.optJSONObject(i);
      if(feature==null)continue;
      JSONObject geom=feature.optJSONObject("geometry");
      if(geom==null)continue;
      String type=geom.optString("type","");
      JSONArray coords=geom.optJSONArray("coordinates");
      if(coords==null)continue;
      if("LineString".equals(type))addRoadLine(coords,parsed,count);
      else if("MultiLineString".equals(type)){
        for(int j=0;j<coords.length();j++){
          JSONArray line=coords.optJSONArray(j);
          if(line!=null)addRoadLine(line,parsed,count);
        }
      }
    }
    if(parsed.isEmpty())throw new Exception("Aucune route valide dans ce GeoJSON");
    offlineRoads=parsed;
    return count[0];
  }
  void addRoadLine(JSONArray line,ArrayList<ArrayList<double[]>> parsed,int[] count)throws Exception{
    addRoadLine(line,parsed,count,4000);
  }
  void addRoadLine(JSONArray line,ArrayList<ArrayList<double[]>> parsed,int[] count,int maxPoints)throws Exception{
    ArrayList<double[]> road=new ArrayList<>();
    for(int k=0;k<line.length();k++){
      JSONArray pair=line.optJSONArray(k);
      if(pair==null||pair.length()<2)continue;
      double lon=pair.optDouble(0,Double.NaN),lat=pair.optDouble(1,Double.NaN);
      if(Double.isFinite(lat)&&Double.isFinite(lon)&&Math.abs(lat)<=90&&Math.abs(lon)<=180){
        if(count[0]>=maxPoints)throw new Exception("Trop de points routiers (maximum "+maxPoints+"). Reduire la zone OSM.");
        road.add(new double[]{lat,lon});count[0]++;
      }
    }
    if(road.size()>1)parsed.add(road);
    else count[0]-=road.size();
  }
  // Secteurs embarques : chargement a la demande, sans importer de fichier.
  String activeSectorKey="";
  ArrayList<ArrayList<double[]>> activeSectorRoads=new ArrayList<>();
  void refreshNearbySector(){
    if(!offlineView||Double.isNaN(lastLat)||Double.isNaN(lastLon))return;
    ArrayList<ArrayList<double[]>> next=new ArrayList<>();
    try(java.io.InputStream in=getAssets().open("osm-sectors/manifest.json")){
      java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();
      byte[] buf=new byte[4096];int n;
      while((n=in.read(buf))!=-1){if(bytes.size()+n>150000)throw new IOException("Index trop volumineux");bytes.write(buf,0,n);}
      JSONObject manifest=new JSONObject(bytes.toString("UTF-8"));
      if(!"roadguard-osm-sectors-v1".equals(manifest.optString("format")))throw new IOException("Format index non reconnu");
      // La grille provient de l'index genere par split_osm_sectors.py.
      double cell=manifest.optDouble("cell",0.05);
      if(!Double.isFinite(cell)||cell<0.001||cell>5.0)throw new IOException("Taille de cellule invalide");
      int y=(int)Math.floor(lastLat/cell),x=(int)Math.floor(lastLon/cell);
      String key=y+"_"+x+"_"+cell;
      if(key.equals(activeSectorKey))return;
      JSONArray names=manifest.optJSONArray("sectors");if(names==null||names.length()==0||names.length()>10000)throw new IOException("Index de secteurs invalide");
      int[] count={0};int loaded=0;
      java.util.regex.Pattern filename=java.util.regex.Pattern.compile("^secteur_(-?[0-9]+)_(-?[0-9]+)_([0-9]+)\\.geojson$");
      for(int i=0;i<names.length();i++){
        String name=names.optString(i,"");
        java.util.regex.Matcher matcher=filename.matcher(name);
        if(!matcher.matches())throw new IOException("Nom de secteur invalide : "+name);
        int sy=Integer.parseInt(matcher.group(1)),sx=Integer.parseInt(matcher.group(2));
        if(sy < -9000||sy > 9000||sx < -18000||sx > 18000)throw new IOException("Coordonnees de secteur invalides");
        if(Math.abs((long)sy-y)>1||Math.abs((long)sx-x)>1)continue;
        if(++loaded>40)throw new IOException("Trop de fichiers voisins");
        try(java.io.InputStream sector=getAssets().open("osm-sectors/"+name)){
          java.io.ByteArrayOutputStream raw=new java.io.ByteArrayOutputStream();
          while((n=sector.read(buf))!=-1){if(raw.size()+n>350000)throw new IOException("Secteur trop volumineux");raw.write(buf,0,n);}
          JSONArray features=new JSONObject(raw.toString("UTF-8")).optJSONArray("features");
          if(features==null)throw new IOException("GeoJSON sans features : "+name);
          for(int j=0;j<features.length();j++){
            JSONObject feature=features.optJSONObject(j);if(feature==null)continue;
            JSONObject geometry=feature.optJSONObject("geometry");if(geometry==null)continue;
            JSONArray coordinates=geometry.optJSONArray("coordinates");if(coordinates==null)continue;
            if("LineString".equals(geometry.optString("type")))addRoadLine(coordinates,next,count,12000);
            else if("MultiLineString".equals(geometry.optString("type")))for(int k=0;k<coordinates.length();k++){
              JSONArray line=coordinates.optJSONArray(k);if(line!=null)addRoadLine(line,next,count,12000);
            }
            if(count[0]>12000)throw new IOException("Trop de points routiers voisins");
          }
        }
      }
      activeSectorRoads=next;
      activeSectorKey=key;
    }catch(java.io.FileNotFoundException missing){
      // Un pack absent ou incomplet ne doit pas effacer la carte deja visible.
      android.util.Log.w("ROADGUARD","Pack de secteurs absent ou incomplet",missing);
      return;
    }catch(Exception error){
      android.util.Log.w("ROADGUARD","Secteurs OSM indisponibles",error);
      return;
    }
    pushRoads();
  }
  ArrayList<ArrayList<double[]>> allVisibleRoads(){ArrayList<ArrayList<double[]>> all=new ArrayList<>(offlineRoads);all.addAll(activeSectorRoads);return all;}
  void pushRoads(){if(map==null||!mapReady||!offlineView)return;StringBuilder js=new StringBuilder("setRoads([");int i=0;for(ArrayList<double[]> road:allVisibleRoads()){if(i++>0)js.append(',');js.append('[');int j=0;for(double[] p:road){if(j++>0)js.append(',');js.append('[').append(p[0]).append(',').append(p[1]).append(']');}js.append(']');}js.append("])");map.evaluateJavascript(js.toString(),null);}
  // L'inversion conserve les points sur le telephone et recalcule la progression depuis le nouveau depart.
  void reverseGpx(){
    if(routePoints.size()<2)return;
    java.util.Collections.reverse(routePoints);
    StringBuilder xml=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><gpx version=\"1.1\" creator=\"ROADGUARD\"><trk><trkseg>");
    for(double[] p:routePoints)xml.append(String.format(Locale.US,"<trkpt lat=\"%.7f\" lon=\"%.7f\"/>",p[0],p[1]));
    xml.append("</trkseg></trk></gpx>");
    getPreferences(0).edit().putString("route_gpx",xml.toString()).apply();
    Toast.makeText(this,"Sens de l'itineraire inverse",Toast.LENGTH_SHORT).show();
    render();
  }
  void exportGpx(){Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/gpx+xml");i.putExtra(Intent.EXTRA_TITLE,"roadguard-itineraire.gpx");DocumentPicker.open(this,i,48);}
  void writeGpx(Uri uri)throws Exception{try(OutputStream out=getContentResolver().openOutputStream(uri);OutputStreamWriter w=new OutputStreamWriter(out,"UTF-8")){w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?><gpx version=\"1.1\" creator=\"ROADGUARD\" xmlns=\"http://www.topografix.com/GPX/1/1\"><trk><name>ROADGUARD</name><trkseg>");for(double[] p:routePoints)w.write(String.format(Locale.US,"<trkpt lat=\"%.7f\" lon=\"%.7f\"/>",p[0],p[1]));w.write("</trkseg></trk></gpx>");}}
  static final String[] BACKUP_KEYS={"danger_csv","custom_radars","route_gpx","offline_roads","report_history","last_report","destination","cached_navigation","trip_track","trip_samples","trip_meters","trip_start","trip_recording","favorites","destination_favorites","auto_reroute","speed_offset_kmh","Alertes vocales","Guidage vocal","Alertes de danger","Mode nuit"};
  void exportBackup(){Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/json");i.putExtra(Intent.EXTRA_TITLE,"roadguard-sauvegarde.json");DocumentPicker.open(this,i,50);}
  void importBackup(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");DocumentPicker.open(this,i,51);}
  void writeBackup(Uri uri)throws Exception{
    JSONObject data=new JSONObject();android.content.SharedPreferences prefs=getPreferences(0);
    for(String key:BACKUP_KEYS){Object value=prefs.getAll().get(key);if(value instanceof String||value instanceof Boolean||value instanceof Integer||value instanceof Long||value instanceof Float)data.put(key,value);}
    JSONObject root=new JSONObject();root.put("format","ROADGUARD_BACKUP");root.put("schema",1);root.put("created",System.currentTimeMillis());root.put("data",data);
    try(OutputStream stream=getContentResolver().openOutputStream(uri);OutputStreamWriter writer=new OutputStreamWriter(stream,"UTF-8")){writer.write(root.toString(2));}
  }
  void restoreBackup(Uri uri)throws Exception{
    StringBuilder text=new StringBuilder();try(BufferedReader reader=new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri),"UTF-8"))){String line;while((line=reader.readLine())!=null){text.append(line).append('\n');if(text.length()>2000000)throw new Exception("Sauvegarde trop volumineuse (2 Mo maximum)");}}
    JSONObject root=new JSONObject(text.toString());if(!"ROADGUARD_BACKUP".equals(root.optString("format"))||root.optInt("schema")!=1)throw new Exception("Format de sauvegarde non reconnu");
    JSONObject data=root.getJSONObject("data");android.content.SharedPreferences.Editor editor=getPreferences(0).edit();int restored=0;
    for(String key:BACKUP_KEYS){if(!data.has(key)||data.isNull(key))continue;Object value=data.get(key);if(value instanceof String)editor.putString(key,(String)value);else if(value instanceof Boolean)editor.putBoolean(key,(Boolean)value);else if(value instanceof Integer)editor.putInt(key,(Integer)value);else if(value instanceof Long)editor.putLong(key,(Long)value);else if(value instanceof Number)editor.putFloat(key,((Number)value).floatValue());else continue;restored++;}
    if(restored==0)throw new Exception("Aucune donnée compatible trouvée");
    if(!editor.commit())throw new Exception("Enregistrement impossible");
    stopGPS();dangers.clear();routePoints.clear();offlineRoads.clear();navigationPoints.clear();navigationSteps.clear();tripTrack.clear();destinationLat=Double.NaN;destinationLon=Double.NaN;loadDangers();loadCustomRadars();loadRoute();loadRoads();loadTrip();loadDestination();loadCachedNavigation();automaticRerouteEnabled=getPreferences(0).getBoolean("auto_reroute",false);render();
    Toast.makeText(this,restored+" éléments restaurés",Toast.LENGTH_LONG).show();
  }
  void openGpx(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);DocumentPicker.open(this,i,45);}
  int parseRoute(String xml)throws Exception{ArrayList<double[]> points=new ArrayList<>();XmlPullParser parser=Xml.newPullParser();parser.setInput(new StringReader(xml));int event;while((event=parser.next())!=XmlPullParser.END_DOCUMENT){if(event==XmlPullParser.START_TAG && ("trkpt".equals(parser.getName())||"rtept".equals(parser.getName()))){String la=parser.getAttributeValue(null,"lat"),lo=parser.getAttributeValue(null,"lon");if(la==null||lo==null)continue;try{double a=Double.parseDouble(la),b=Double.parseDouble(lo);if(Double.isFinite(a)&&Double.isFinite(b)&&a>=-90&&a<=90&&b>=-180&&b<=180&&points.size()<10000)points.add(new double[]{a,b});}catch(NumberFormatException ignored){}}}if(points.isEmpty())throw new Exception("Aucun point GPX valide (trkpt/rtept)");routePoints=points;return points.size();}
  void loadRoute(){String saved=getPreferences(0).getString("route_gpx","");if(!saved.isEmpty())try{parseRoute(saved);}catch(Exception ignored){}}
  void pushRoute(){if(map==null||!mapReady)return;StringBuilder js=new StringBuilder("setRoute([");int count=0;for(double[] p:(navigationPoints.isEmpty()?routePoints:navigationPoints)){if(count++>0)js.append(',');js.append('[').append(p[0]).append(',').append(p[1]).append(']');}js.append("])");map.evaluateJavascript(js.toString(),null);}
  NavigationProgress routeProgressModel(){if(navigationProgress==null||navigationProgress.points!=navigationPoints||navigationProgress.cumulative.length!=navigationPoints.size())navigationProgress=new NavigationProgress(navigationPoints);return navigationProgress;}
  NavigationProgress.Position currentRoutePosition(NavigationProgress model,Location fix){
    if(fix==null||gpsSignalSuspended)return null;
    return model.track(fix.getLatitude(),fix.getLongitude(),Math.max(0,guidanceProgressSegment-2),fix.getElapsedRealtimeNanos()/1000000L,gpsMotion.speedMetersPerSecond(SystemClock.elapsedRealtime()),fix.hasAccuracy()?fix.getAccuracy():30);
  }
  final PhoneGuidanceSharing phoneGuidanceSharing=new PhoneGuidanceSharing();
  void updateManeuverCard(){
    phoneGuidanceSharing.update(this);
    if(maneuverTitle==null||maneuverDistance==null)return;
    if(navigationPoints.size()<2){maneuverTitle.setText("Destination sélectionnée");maneuverDistance.setText("Calculer un itinéraire dans Trajet");if(maneuverGlyph!=null)maneuverGlyph.setText("↑");return;}
    if(gpsSignalSuspended||!AlertPresentation.fresh(previousAcceptedGps,System.currentTimeMillis(),SystemClock.elapsedRealtimeNanos())){
      maneuverTitle.setText("Signal GPS indisponible");maneuverDistance.setText("Guidage en attente");if(maneuverGlyph!=null)maneuverGlyph.setText("⌖");if(tripArrival!=null)tripArrival.setText("--:--");return;
    }
    NavigationProgress model=routeProgressModel();NavigationProgress.Position pos=currentRoutePosition(model,previousAcceptedGps);
    if(pos==null){maneuverTitle.setText("Itinéraire prêt");maneuverDistance.setText("En attente du GPS");return;}
    if(pos.lateral>150)alertSpeechTracker.cancelNavigation();
    if(pos.lateral<=100)guidanceProgressSegment=Math.max(guidanceProgressSegment,pos.segment);
    double remainingSeconds=model.remainingSeconds(pos,navigationSeconds);
    if(tripSummary!=null)tripSummary.setText((pos.remaining>=1000?String.format(Locale.FRANCE,"%.1f km",pos.remaining/1000):String.format(Locale.FRANCE,"%.0f m",pos.remaining))+(pos.lateral>100?" · hors trajet":""));
    if(tripMinutes!=null)tripMinutes.setText(pos.lateral>100?"— min":(pos.arrived()?0:Math.max(1,Math.round(remainingSeconds/60)))+" min");
    if(tripArrival!=null)tripArrival.setText(pos.lateral>100?"--:--":new SimpleDateFormat("HH:mm",Locale.FRANCE).format(new Date(System.currentTimeMillis()+(long)(remainingSeconds*1000))));
    if(routeProgress!=null&&model.total>0&&pos.lateral<=100)routeProgress.setProgress((int)Math.max(0,Math.min(1000,1000*(1-pos.remaining/model.total))));
    if(pos.arrived()){maneuverTitle.setText("Vous êtes à destination");maneuverDistance.setText("Arrivée");if(maneuverGlyph!=null)maneuverGlyph.setText("⚑");return;}
    NavStep next=null;for(NavStep step:navigationSteps){if(!NavigationProgress.departure(step.instruction)&&step.routeIndex>=pos.segment&&model.toStep(pos,step.routeIndex)>=1){next=step;break;}}
    if(next==null){maneuverTitle.setText("Continuer vers la destination");maneuverDistance.setText(pos.lateral>100?"Rejoindre le trajet":"Suivre la route");if(maneuverGlyph!=null)maneuverGlyph.setText("↑");return;}
    double meters=model.toStep(pos,next.routeIndex);maneuverTitle.setText(next.instruction);
    maneuverDistance.setText(pos.lateral>100?"Hors trajet":meters>=1000?String.format(Locale.FRANCE,"Dans %.1f km",meters/1000):String.format(Locale.FRANCE,"Dans %.0f m",meters));
    if(maneuverGlyph!=null)maneuverGlyph.setText(NavigationProgress.glyph(next.instruction));
  }
  void selectMapPalette(String pack,boolean night){
    if(!MapAppearance.valid(pack))return;
    nightMap=night;
    getPreferences(0).edit().putString("map_appearance_pack",pack).putBoolean("night_map",night).putBoolean("theme_auto",false).apply();
    if(community!=null)community.markPreferencesChanged();
    applyMapTheme();ReferenceUi.refreshNow(this);
  }
  public void showMapPackPicker(){
    String[] packs=MapAppearance.IDS;String[] names=new String[packs.length*2];
    for(int i=0;i<packs.length;i++){names[i]=MapAppearance.NAMES[i]+" · Jour";names[i+packs.length]=MapAppearance.NAMES[i]+" · Nuit";}
    String current=getPreferences(0).getString("map_appearance_pack","graphite");int selectedPack=java.util.Arrays.asList(packs).indexOf(current);if(selectedPack<0)selectedPack=0;
    new AlertDialog.Builder(this).setTitle("Couleurs de la carte")
      .setSingleChoiceItems(names,selectedPack+(nightMap?packs.length:0),(dialog,which)->{selectMapPalette(packs[which%packs.length],which>=packs.length);dialog.dismiss();if("custom".equals(packs[which%packs.length]))showCustomMapColors();})
      .setNeutralButton("Personnaliser",(d,w)->showCustomMapColors()).setNegativeButton("Fermer",null).show();
  }
  void showCustomMapColors(){
    final boolean night=nightMap;final JSONObject draft=MapAppearance.current(this,night);LinearLayout content=column();content.setPadding(pxDp(18),pxDp(8),pxDp(18),pxDp(8));
    add(content,label("Couleurs "+(night?"de nuit":"de jour")+" · cartes téléchargées. Les fonds réseau en image utilisent la teinte globale ci-dessous.",13));
    for(int i=0;i<MapAppearance.KEYS.length;i++){final String key=MapAppearance.KEYS[i],name=MapAppearance.LABELS[i];TextView option=button(name+"  "+draft.optString(key),()->{});content.addView(option);option.setOnClickListener(v->{EditText input=new EditText(this);input.setSingleLine(true);input.setText(draft.optString(key));input.setHint("#RRGGBB");LinearLayout chooser=column();chooser.setPadding(pxDp(14),pxDp(6),pxDp(14),pxDp(6));String[] swatches={"#0a1422","#ffffff","#d8d8d8","#525f70","#f04458","#ff8844","#ffd550","#b3ee75","#16876a","#88c7e7","#2299ee","#15579c","#bea4ff","#8c54b8","#f68abe","#a36b42"};for(int row=0;row<4;row++){LinearLayout colors=row();for(int col=0;col<4;col++){String value=swatches[row*4+col];TextView swatch=label("●",24);swatch.setGravity(Gravity.CENTER);swatch.setTextColor(android.graphics.Color.parseColor(value));swatch.setContentDescription("Couleur "+value);LinearLayout.LayoutParams size=new LinearLayout.LayoutParams(0,pxDp(44),1);colors.addView(swatch,size);swatch.setOnClickListener(t->input.setText(value));}chooser.addView(colors);}chooser.addView(input);AlertDialog pick=new AlertDialog.Builder(this).setTitle(name).setView(chooser).setPositiveButton("Appliquer",null).setNegativeButton("Annuler",null).create();pick.setOnShowListener(d->pick.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(w->{String value=input.getText().toString().trim();if(!value.matches("#[a-fA-F0-9]{6}")){input.setError("Saisis une couleur comme #28B8ED");return;}try{draft.put(key,value);}catch(Exception ignored){}option.setText(name+"  "+value);pick.dismiss();}));pick.show();});}
    TextView hueText=label("Teinte globale des fonds réseau",13);add(content,hueText);SeekBar hue=new SeekBar(this);hue.setMax(360);hue.setProgress(draft.optInt("rasterHue",0));content.addView(hue);hue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}public void onProgressChanged(SeekBar b,int value,boolean user){try{draft.put("rasterHue",value);}catch(Exception ignored){}hueText.setText("Teinte globale : "+value+"°");}});
    ScrollView scroll=new ScrollView(this);scroll.addView(content);new AlertDialog.Builder(this).setTitle("Ma palette · "+(night?"nuit":"jour")).setView(scroll).setPositiveButton("Enregistrer",(d,w)->{getPreferences(0).edit().putString("map_palette_custom_"+(night?"night":"day"),draft.toString()).apply();selectMapPalette("custom",night);}).setNegativeButton("Annuler",null).show();
  }
  void applyMapTheme(){if(map!=null&&mapReady){map.evaluateJavascript("if(typeof setRoadguardCustomPalette==='function')setRoadguardCustomPalette("+MapAppearance.current(this,nightMap).toString()+")",null);map.evaluateJavascript("if(typeof setRoadguardMapPack===\'function\')setRoadguardMapPack("+JSONObject.quote(getPreferences(0).getString("map_appearance_pack","graphite"))+")",null);if(mapLibrePreview)map.evaluateJavascript("if(typeof setRoadguardVectorTheme==='function')setRoadguardVectorTheme('"+(nightMap?"night":"day")+"')",null);else map.evaluateJavascript("if(typeof setRoadguardNight==='function')setRoadguardNight("+(nightMap?"true":"false")+");if(typeof setRoadguardContrast==='function')setRoadguardContrast("+(highContrastMap?"true":"false")+")",null);}}
  // Réseau présent : comportement WebView normal. Réseau absent : essayer son cache HTTP existant.
  // Seules les tuiles déjà demandées sont disponibles ; aucun téléchargement anticipé.
  boolean networkAvailable(){
    ConnectivityManager cm=(ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
    if(cm==null)return false;
    try{NetworkInfo info=cm.getActiveNetworkInfo();return info!=null&&info.isConnected();}
    catch(Exception e){return false;}
  }
  void refreshMapNetworkCacheMode(){
    boolean online=mapNetworkAvailable();
    if(online&&!lastMapNetworkOnline&&visibleTileCache!=null)visibleTileCache.retryNetworkFailures();
    if(online)offlineFallbackChecked=false;
    lastMapNetworkOnline=online;if(map==null)return;
    // Keep the renderer, its zoom and its loaded images when connectivity changes.
    // A local vector fallback is requested only after an actual empty raster view.
    // The native cache serves intercepted raster requests both online and offline.
    // LOAD_CACHE_ONLY can otherwise retain a prior WebView error before native retry.
    int target=visibleTileCache!=null?WebSettings.LOAD_DEFAULT:online?WebSettings.LOAD_DEFAULT:WebSettings.LOAD_CACHE_ONLY;
    if(map.getSettings().getCacheMode()!=target)map.getSettings().setCacheMode(target);
    if(mapConnectionSwitch!=null)mapConnectionSwitch.setContentDescription("Mode carte "+mapConnectionMode+(online?" : réseau disponible":" : données locales et cache"));
    if(modeChip!=null&&!offlineView)modeChip.setText(mapLibrePreview&&!forceOnlineMapLibre
      ?(online?" · LOCAL + RÉSEAU":" · CARTES LOCALES · HORS RÉSEAU")
      :(online?" · EN LIGNE":" · CACHE · HORS RÉSEAU"));
    if(mapReady&&(lastPushedMapNetworkOnline==null||lastPushedMapNetworkOnline.booleanValue()!=online)){
      lastPushedMapNetworkOnline=online;
      map.evaluateJavascript("if(typeof setRoadguardNetworkState==='function')setRoadguardNetworkState("+online+")",null);
      if(!mapLibrePreview&&!offlineView){
        String raster=online||"online".equals(mapConnectionMode)?"":getPreferences(0).getString("active_raster_pmtiles","");
        try{if(LocalPmtilesResolver.completed(raster,new OfflineCellStore(new File(getFilesDir(),"offline-cells")),new File(getFilesDir(),"pmtiles"))==null)raster="";}catch(IOException missing){raster="";}
        map.evaluateJavascript("if(typeof setRasterPmtiles==='function')setRasterPmtiles("+JSONObject.quote(raster)+")",null);
      }
    }
  }
  void retryMapTiles(){vectorEngineDisabledForSession=false;if(visibleTileCache!=null)visibleTileCache.retryNetworkFailures();selected="Carte";render();}

  void pushPosition(){warmDownloadedMap();publishMapGpsState();syncCarRoute();dispatchNavigationIntent();refreshMapNetworkCacheMode();if(map!=null&&mapReady&&!Double.isNaN(lastLat))map.evaluateJavascript(String.format(Locale.US,"if(typeof setCameraAnchor===\'function\')setCameraAnchor(%d);updateGPS(%.7f,%.7f,%s,%.1f,%.1f,%s,%.1f,%s,%s)",Math.max(50,Math.min(75,getPreferences(0).getInt("camera_anchor_percent",50))),lastLat,lastLon,follow?"true":"false",lastBearing,lastAccuracy,headingUp?"true":"false",Double.isFinite(gpsMotion.speed(android.os.SystemClock.elapsedRealtime()))?lastSpeedKmh:-1f,adaptiveZoom?"true":"false",lookAhead?"true":"false"),null);}
  void startBackgroundAlerts(){
    if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
      requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},3);
      Toast.makeText(this,"Autorise la localisation, puis relance le suivi",Toast.LENGTH_LONG).show();return;
    }
    if(requestTrackingNotifications(1))return;
    try{Intent intent=new Intent(this,BackgroundAlertService.class);getPreferences(0).edit().putBoolean("monitor_armed",true).putBoolean("floating_auto",false).apply();startForegroundService(intent);Toast.makeText(this,"Mode auto armé · déclenchement au-dessus de 20 km/h",Toast.LENGTH_SHORT).show();}
    catch(Exception e){Toast.makeText(this,"Démarrage impossible : "+e.getMessage(),Toast.LENGTH_LONG).show();}
  }
  void updateFloatingChip(){if(floatingChip!=null)floatingChip.setText(getPreferences(0).getBoolean("floating_auto",false)?(android.provider.Settings.canDrawOverlays(this)?"◎  BULLE RADAR · AUTO":"◎  BULLE RADAR · AUTORISATION REQUISE"):"◎  BULLE RADAR · ACTIVER");}
  void showFloatingRadarControls(){
    if(getPreferences(0).getBoolean("floating_auto",false)){
      if(!BackgroundAlertService.running||!android.provider.Settings.canDrawOverlays(this)){startFloatingRadar();return;}
      new AlertDialog.Builder(this).setTitle("Bulle radar automatique")
        .setMessage("Le mode auto démarre au-dessus de 20 km/h. Après 1 minute immobile, les alertes et la bulle disparaissent. La localisation reste active pour détecter un nouveau départ. La bulle se masque dans ROADGUARD et sur l’écran verrouillé. Quitter la page principale laisse le suivi activé ; Arrêter ou retirer ROADGUARD des applications récentes termine le suivi.")
        .setNegativeButton("Garder",null)
        .setPositiveButton("Arrêter",(dialog,which)->{getPreferences(0).edit().putBoolean("floating_auto",false).putBoolean("monitor_armed",false).apply();BackgroundAlertService.stopMonitor(this);BackgroundAlertService.bubbleVisible=false;updateFloatingChip();}).show();
    }else startFloatingRadar();
  }
  void startFloatingRadar(){
    if(!android.provider.Settings.canDrawOverlays(this)){
      pendingFloatingActivation=true;
      Toast.makeText(this,"Autorise l’affichage superposé pour ROADGUARD, puis reviens ici",Toast.LENGTH_LONG).show();
      Intent permission=new Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
          Uri.parse("package:"+getPackageName()));
      try{startActivityForResult(permission,66);}catch(Exception unavailable){pendingFloatingActivation=false;Toast.makeText(this,"Ouvre les réglages Android et autorise l’affichage superposé pour ROADGUARD",Toast.LENGTH_LONG).show();}
      return;
    }
    if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
      pendingFloatingActivation=true;
      requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},31);
      return;
    }
    if(requestTrackingNotifications(2))return;
    pendingFloatingActivation=false;
    try{
      Intent service=new Intent(this,BackgroundAlertService.class);
      getPreferences(0).edit().putBoolean("floating_auto",true).putBoolean("monitor_armed",true).apply();
      startForegroundService(service);
      new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(()->{
        updateFloatingChip();
        Toast.makeText(this,BackgroundAlertService.running?"Mode auto prêt · bulle au-dessus de 20 km/h dans Waze ou sur l’accueil":"Suivi GPS indisponible · vérifie l’autorisation de localisation",Toast.LENGTH_LONG).show();
      },2500);
    }catch(Exception error){getPreferences(0).edit().putBoolean("floating_auto",false).apply();Toast.makeText(this,"Bulle indisponible : "+error.getMessage(),Toast.LENGTH_LONG).show();}
  }
  void stopGPS(){driveSearchHandler.removeCallbacks(gpsFreshnessPulse);if(listener!=null){try{lm.removeUpdates(listener);}catch(Exception ignored){}listener=null;}}

  void openTileZip(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("application/zip");i.addCategory(Intent.CATEGORY_OPENABLE);DocumentPicker.open(this,i,60);}
  void deleteTree(File f){if(f.isDirectory()){File[] children=f.listFiles();if(children!=null)for(File c:children)deleteTree(c);}f.delete();}
  private DownloadedMapStore downloadedStore;
  private synchronized DownloadedMapStore downloadedMapStore(){if(downloadedStore==null)downloadedStore=new DownloadedMapStore(getFilesDir(),visibleTileCache!=null?visibleTileCache:new VisibleTileCache(new File(getCacheDir(),"visible-tiles")));return downloadedStore;}
  private String lastLocalWarmKey="";private final java.util.concurrent.atomic.AtomicBoolean localWarmQueued=new java.util.concurrent.atomic.AtomicBoolean();
  void warmDownloadedMap(){if(Double.isNaN(lastLat)||Double.isNaN(lastLon)||!mapLibrePreview)return;String key=Math.round(lastLat*500)+"/"+Math.round(lastLon*500);if(key.equals(lastLocalWarmKey)||!localWarmQueued.compareAndSet(false,true))return;lastLocalWarmKey=key;final double lat=lastLat,lon=lastLon;try{initialDataExecutor.execute(()->{try{downloadedMapStore().warm(lat,lon);}finally{localWarmQueued.set(false);}});}catch(java.util.concurrent.RejectedExecutionException closed){localWarmQueued.set(false);}}
  private final Object pmtilesReaderLock=new Object();
  private final java.util.LinkedHashMap<String,PmtilesReader> cachedPmtilesReaders=
    new java.util.LinkedHashMap<String,PmtilesReader>(8,0.75f,true){
      @Override protected boolean removeEldestEntry(java.util.Map.Entry<String,PmtilesReader> entry){return size()>8;}
    };
  private long mosaicDirectoryModified=-1;
  private java.util.List<InstalledPmtilesMosaic.Region> mosaicRegions=java.util.Collections.emptyList();
  void invalidateInstalledMosaic(){
    synchronized(pmtilesReaderLock){mosaicDirectoryModified=-1;mosaicRegions=java.util.Collections.emptyList();cachedPmtilesReaders.clear();}
  }
  java.util.List<InstalledPmtilesMosaic.Region> installedMosaicRegions(){
    synchronized(pmtilesReaderLock){
      File directory=new File(getFilesDir(),"pmtiles"),verifiedDirectory=new File(getFilesDir(),"offline-cells");
      long modified=31*directory.lastModified()+verifiedDirectory.lastModified()+new File(verifiedDirectory,"completed-sectors.tsv").lastModified();
      if(mosaicDirectoryModified!=modified){mosaicRegions=InstalledPmtilesMosaic.installed(directory,verifiedDirectory);mosaicDirectoryModified=modified;}
      return mosaicRegions;
    }
  }
  PmtilesReader reusablePmtilesReader(File f)throws IOException{
    synchronized(pmtilesReaderLock){
      String key=f.getAbsolutePath()+":"+f.length()+":"+f.lastModified();
      PmtilesReader reader=cachedPmtilesReaders.get(key);
      if(reader==null){reader=new PmtilesReader(f);cachedPmtilesReaders.put(key,reader);}
      return reader;
    }
  }
  // Always answer requests for the local tile host ourselves, including missing tiles.
  // Returning null would let WebView try to fetch roadguard.local over the network.
  WebResourceResponse localTileNotFound(){
    return new WebResourceResponse("text/plain","UTF-8",404,"Not Found",
        localTileCorsHeaders(),
        new ByteArrayInputStream(new byte[0]));
  }
  java.util.Map<String,String> localTileCorsHeaders(){
    java.util.HashMap<String,String> headers=new java.util.HashMap<>();headers.put("Access-Control-Allow-Origin","*");
    headers.put("Access-Control-Allow-Methods","GET, HEAD, OPTIONS");headers.put("Access-Control-Allow-Headers","Range, Content-Type");return headers;
  }
  WebResourceResponse localTileResponse(String mime,InputStream body){return new WebResourceResponse(mime,null,200,"OK",localTileCorsHeaders(),body);}
  WebResourceResponse serveMapResource(WebResourceRequest request){
    String url=request.getUrl().toString();
    if("GET".equals(request.getMethod())&&VisibleTileCache.canonical(url)!=null){
      try{String[] xyz=Uri.parse(url).getPath().substring(1).replace(".png","").split("/");DownloadedMapStore.Raster local=downloadedMapStore().raster(Integer.parseInt(xyz[0]),Integer.parseInt(xyz[1]),Integer.parseInt(xyz[2]),mapNetworkAvailable(),mapLibrePreview&&!vectorEngineDisabledForSession);
        if(local!=null)return localTileResponse(local.mime,local.body);
      }catch(IOException ignored){}
      return new WebResourceResponse("text/plain","UTF-8",503,"Tile unavailable",localTileCorsHeaders(),new ByteArrayInputStream(new byte[0]));
    }
    if("offline".equals(mapConnectionMode)&&("https".equalsIgnoreCase(request.getUrl().getScheme())||"http".equalsIgnoreCase(request.getUrl().getScheme()))&&!"roadguard.local".equalsIgnoreCase(request.getUrl().getHost()))
      return new WebResourceResponse("text/plain","UTF-8",503,"Offline map",localTileCorsHeaders(),new ByteArrayInputStream(new byte[0]));
    return serveOfflineTile(request.getUrl());
  }
  WebResourceResponse serveOfflineTile(Uri uri){
    if(uri==null)return null;
    // MapLibre uses Fetch/XHR and Blob workers. A file:// page with locked file
    // settings cannot reliably load these local HTTPS resources. Keep all map
    // pages/scripts/tiles on one intercepted HTTPS origin instead.
    if("roadguard.local".equalsIgnoreCase(uri.getHost())&&uri.getPath()!=null&&uri.getPath().startsWith("/assets/")){
      try{
        LocalMapAssets.Resource asset=LocalMapAssets.open(uri.toString(),path->getAssets().open(path));
        if(asset==null)return localTileNotFound();
        java.util.Map<String,String> headers=localTileCorsHeaders();headers.put("Cache-Control","no-store");headers.put("X-Content-Type-Options","nosniff");
        localMapAssetsServed.incrementAndGet();
        return new WebResourceResponse(asset.mime,asset.mime.startsWith("image/")?null:"UTF-8",200,"OK",headers,asset.body);
      }catch(IOException missing){lastLocalMapFailure="Ressource embarquée absente : "+uri.getPath();return localTileNotFound();}
    }
    if("roadguard.local".equalsIgnoreCase(uri.getHost())&&!"https".equalsIgnoreCase(uri.getScheme()))
      return localTileNotFound();
    if("roadguard.local".equalsIgnoreCase(uri.getHost())&&uri.getPath()!=null&&uri.getPath().startsWith("/fonts/")){
      String glyph=uri.getPath().substring(7);
      if(!glyph.matches("Noto Sans (Regular|Bold)/[0-9]{1,6}-[0-9]{1,6}\\.pbf"))return localTileNotFound();
      try{return localTileResponse("application/x-protobuf",getAssets().open("glyphs/"+glyph));}catch(Exception missing){return localTileNotFound();}
    }
    if("roadguard.local".equalsIgnoreCase(uri.getHost())&&uri.getPath()!=null&&uri.getPath().startsWith("/pmtiles/")){
      try{
        localMapTileRequests.incrementAndGet();
        String path=uri.getPath().substring(9);
        if(!path.matches("[A-Za-z0-9_-]{1,64}/[0-9]{1,2}/[0-9]{1,9}/[0-9]{1,9}\\.(png|pbf)"))return localTileNotFound();
        boolean vector=path.endsWith(".pbf");
        String[] p=path.substring(0,path.length()-4).split("/");
        if("all_downloaded".equals(p[0])){
          if(!vector)return localTileNotFound();
          int z=Integer.parseInt(p[1]),x=Integer.parseInt(p[2]),y=Integer.parseInt(p[3]);
          byte[] tile=downloadedMapStore().vector(z,x,y);if(tile!=null){localMapTilesServed.incrementAndGet();return localTileResponse("application/vnd.mapbox-vector-tile",new ByteArrayInputStream(tile));}
          // A valid empty vector tile lets MapLibre show the online/cached
          // backdrop beyond downloaded regions without a 404 error banner.
          return localTileResponse("application/vnd.mapbox-vector-tile",new ByteArrayInputStream(new byte[0]));
        }
        // Prefer the checksum-verified GPS library over any legacy/manual copy.
        // A stale file in pmtiles/ must not mask a newer downloaded sector.
        OfflineCellStore store=new OfflineCellStore(new File(getFilesDir(),"offline-cells"));
        File f=LocalPmtilesResolver.resolve(p[0],store,new File(getFilesDir(),"pmtiles"));
        if(f==null||!f.isFile())return localTileNotFound();
        PmtilesReader reader=reusablePmtilesReader(f);
        byte[] data=vector?reader.vectorTile(Integer.parseInt(p[1]),Integer.parseInt(p[2]),Integer.parseInt(p[3])):
            reader.rasterTile(Integer.parseInt(p[1]),Integer.parseInt(p[2]),Integer.parseInt(p[3]));
        if(data!=null)localMapTilesServed.incrementAndGet();
        return data==null?localTileNotFound():localTileResponse(vector?reader.vectorMimeType():reader.mimeType(),new ByteArrayInputStream(data));
      }catch(Exception ex){lastLocalMapFailure=ex.getClass().getSimpleName()+" : "+ex.getMessage();return localTileNotFound();}
    }
    if(!"roadguard.local".equalsIgnoreCase(uri.getHost()))return null;
    if(uri.getPath()==null||!uri.getPath().startsWith("/tiles/"))return localTileNotFound();try{String relative=uri.getPath().substring(7);if(!relative.matches("[0-9]{1,2}/[0-9]{1,9}/[0-9]{1,9}\\.(png|jpg|jpeg)"))return localTileNotFound();File base=new File(getFilesDir(),"tiles");File tile=new File(base,relative);if(!tile.getCanonicalPath().startsWith(base.getCanonicalPath()+File.separator)||!tile.isFile())return localTileNotFound();return new WebResourceResponse(relative.endsWith("png")?"image/png":"image/jpeg",null,new FileInputStream(tile));}catch(Exception e){return localTileNotFound();}}
  // Installe une archive XYZ fournie dans les assets de l'APK, sans import utilisateur.
  // Aucune carte regionale n'est fournie par defaut dans cette version.
  void installBundledMapIfPresent(){
    try{getAssets().open("offline-region.zip").close();}catch(IOException absent){return;}
    if(getPreferences(0).getBoolean("bundled_map_installed_v1",false))return;
    new Thread(()->{
      File base=new File(getFilesDir(),"tiles"),staging=new File(getFilesDir(),"tiles_bundled_staging");
      int count=0;long total=0;
      try{
        deleteTree(staging);if(!staging.mkdirs())throw new IOException("Stockage inaccessible");
        try(ZipInputStream zin=new ZipInputStream(getAssets().open("offline-region.zip"))){
          ZipEntry entry;byte[] buffer=new byte[8192];
          while((entry=zin.getNextEntry())!=null){
            if(entry.isDirectory())continue;
            String name=entry.getName();if(name.startsWith("tiles/"))name=name.substring(6);
            if(!name.matches("[0-9]{1,2}/[0-9]{1,9}/[0-9]{1,9}\\.(png|jpg|jpeg)"))continue;
            String[] xyz=name.split("/");
            int zoom=Integer.parseInt(xyz[0]);long x=Long.parseLong(xyz[1]);
            String ypart=xyz[2].substring(0,xyz[2].indexOf('.'));
            long y=Long.parseLong(ypart);
            if(zoom<0||zoom>18||x>=(1L<<zoom)||y>=(1L<<zoom))throw new IOException("Coordonnees de tuile invalides");
            if(++count>20000)throw new IOException("Trop de tuiles");
            File target=new File(staging,name);
            if(!target.getCanonicalPath().startsWith(staging.getCanonicalPath()+File.separator))throw new IOException("Chemin invalide");
            if(!target.getParentFile().exists()&&!target.getParentFile().mkdirs())throw new IOException("Dossier inaccessible");
            try(FileOutputStream out=new FileOutputStream(target)){
              int n;while((n=zin.read(buffer))!=-1){total+=n;if(total>100L*1024*1024)throw new IOException("Archive trop volumineuse");out.write(buffer,0,n);}
            }
          }
        }
        if(count==0)throw new IOException("Archive sans tuiles XYZ");
        // Refuser les packs qui contiennent des fichiers vides ou anormalement petits.
        if(total<100)throw new IOException("Pack de carte vide ou incomplet");
        // Ne remplace jamais les tuiles deja presentes : fusion uniquement des fichiers absents.
        if(!base.exists()&&!base.mkdirs())throw new IOException("Stockage des tuiles inaccessible");
        java.util.ArrayDeque<File> pending=new java.util.ArrayDeque<>();pending.add(staging);
        while(!pending.isEmpty()){
          File dir=pending.removeFirst();File[] children=dir.listFiles();if(children==null)continue;
          for(File f:children){if(f.isDirectory())pending.add(f);else{
            String relative=f.getAbsolutePath().substring(staging.getAbsolutePath().length()+1);
            File target=new File(base,relative);if(target.exists())continue;
            if(!target.getParentFile().exists()&&!target.getParentFile().mkdirs())throw new IOException("Dossier inaccessible");
            try(InputStream in=new FileInputStream(f);OutputStream out=new FileOutputStream(target)){
              byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);
            }
          }}
        }
        deleteTree(staging);
        getPreferences(0).edit().putBoolean("bundled_map_installed_v1",true).apply();
        final int installedCount = count;
        runOnUiThread(()->Toast.makeText(this,"Pack cartographique installe : "+installedCount+" tuiles",Toast.LENGTH_LONG).show());
      }catch(Exception e){deleteTree(staging);runOnUiThread(()->Toast.makeText(this,"Carte incluse non installee : "+e.getMessage(),Toast.LENGTH_LONG).show());}
    }).start();
  }
  void removeImportedTilePack(){
    Toast.makeText(this,"Suppression des cartes locales…",Toast.LENGTH_SHORT).show();
    new Thread(()->{try{TileArchiveInstaller.clear(new File(getFilesDir(),"tiles"));getPreferences(0).edit().remove("tile_count").remove("map_pack_id").apply();runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())render();});}
      catch(IOException failed){runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())Toast.makeText(this,"Suppression impossible : "+failed.getMessage(),Toast.LENGTH_LONG).show();});}
    },"roadguard-tiles-remove").start();
  }
  void importTileZip(Uri uri){new Thread(()->{
    try{
      TileArchiveInstaller.Result installed;
      try(InputStream input=getContentResolver().openInputStream(uri)){installed=TileArchiveInstaller.install(input,new File(getFilesDir(),"tiles"));}
      getPreferences(0).edit().putInt("tile_count",installed.count).putInt("map_zoom_min",installed.minZoom).putInt("map_zoom_max",installed.maxZoom).apply();
      runOnUiThread(()->{if(isFinishing()||isDestroyed())return;Toast.makeText(this,installed.count+" tuiles installées · zoom "+installed.minZoom+" à "+installed.maxZoom,Toast.LENGTH_LONG).show();render();});
    }catch(Exception failed){runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())Toast.makeText(this,"Import impossible : "+failed.getMessage(),Toast.LENGTH_LONG).show();});}
  },"roadguard-tiles-import").start();}
  // Hysteresis avoids flicker around the speed threshold. Essential guidance and report controls stay visible.
  void updateDrivingDeclutter(boolean speedAvailable){
    if(!autoDeclutter||!speedAvailable){drivingDecluttered=false;}
    else if(!drivingDecluttered&&lastSpeedKmh>=18f){drivingDecluttered=true;}
    else if(drivingDecluttered&&lastSpeedKmh<=8f){drivingDecluttered=false;}
    int visibility=drivingDecluttered?View.GONE:View.VISIBLE;
    if(searchWidget!=null)searchWidget.setVisibility(visibility);
    if(gpsWidget!=null)gpsWidget.setVisibility(visibility);
    // Close only when entering driving mode; never repeatedly relayout on each GPS fix.
    if(driveSheet!=null&&drivingDecluttered&&driveSheetLevel!=0&&!driveSheetUserOverride)setDriveSheetLevel(0);
  }
  final Runnable gpsFreshnessPulse=new Runnable(){public void run(){
    if(listener==null)return;updateSpeedLabels();
    if(!Double.isFinite(gpsMotion.speed(android.os.SystemClock.elapsedRealtime()))){if(gps!=null)gps.setText("⌖  GPS · mesure en attente");radarAudio.stop();}if(gpsSignalSuspended||!AlertPresentation.fresh(previousAcceptedGps,System.currentTimeMillis(),SystemClock.elapsedRealtimeNanos())){clearDangerWarning();if(tts!=null&&alertSpeechTracker.hasNavigation()&&!alertSpeechTracker.hasPending())tts.stop();alertSpeechTracker.cancelNavigation();updateManeuverCard();}
    publishMapGpsState();driveSearchHandler.postDelayed(this,1000);
  }};
  void updateSpeedLabels(){String value=gpsSpeedText();if(speed!=null)speed.setText(value);if(inlineMapSpeed!=null){inlineMapSpeed.setText(value+" km/h");inlineMapSpeed.setContentDescription(value.equals("--")?"Vitesse GPS indisponible":value+" kilomètres par heure");}if(speedHudFrame!=null)speedHudFrame.setContentDescription(value.equals("--")?"Vitesse GPS indisponible":value+" kilomètres par heure");}
  String gpsSpeedText(){double v=GpsMotion.displaySpeed(gpsMotion.speed(android.os.SystemClock.elapsedRealtime()),getPreferences(0).getInt("speed_offset_kmh",0));return Double.isFinite(v)?String.format(Locale.FRANCE,"%.0f",v):"--";}
  void showGpsSettings(){
    LinearLayout content=column();pad(content,pxDp(18));
    TextView info=label("Correction du compteur uniquement. La position, le cap, le zoom et les alertes gardent la mesure GPS brute. À l’arrêt, la vitesse reste à zéro.",15);add(content,info);
    TextView value=label("",22);add(content,value);SeekBar slider=new SeekBar(this);slider.setMax(30);slider.setProgress(Math.max(0,Math.min(30,getPreferences(0).getInt("speed_offset_kmh",0)+15)));add(content,slider);
    Runnable preview=()->{int offset=slider.getProgress()-15;double raw=gpsMotion.speed(android.os.SystemClock.elapsedRealtime());value.setText(String.format(Locale.FRANCE,"%+d km/h · GPS %s → %s",offset,Double.isFinite(raw)?String.format(Locale.FRANCE,"%.0f",raw):"--",Double.isFinite(raw)?String.format(Locale.FRANCE,"%.0f",GpsMotion.displaySpeed(raw,offset)):"--"));};preview.run();
    slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}public void onProgressChanged(SeekBar b,int n,boolean user){preview.run();}});
    TextView help=label("Direction : le cap est mesuré en roulant au-dessus de 10 km/h, puis conservé à basse vitesse. Attends un signal dégagé. Le GPS ne se calibre pas avec un mouvement en huit : ce geste concerne la boussole du téléphone. Si le signal manque, -- remplace une fausse vitesse nulle.",14);add(content,help);
    add(content,button("Réinitialiser le cap",()->{gpsMotion.resetCourse();reliableBearing=false;Toast.makeText(this,"Cap en attente d’un déplacement GPS fiable (plus de 10 km/h)",Toast.LENGTH_LONG).show();}));
    ScrollView gpsScroll=new ScrollView(this);gpsScroll.addView(content);
    new AlertDialog.Builder(this).setTitle("GPS, vitesse et direction").setView(gpsScroll).setNeutralButton("Remettre à 0",(d,w)->{getPreferences(0).edit().putInt("speed_offset_kmh",0).apply();updateSpeedLabels();}).setNegativeButton("Annuler",null).setPositiveButton("Enregistrer",(d,w)->{getPreferences(0).edit().putInt("speed_offset_kmh",slider.getProgress()-15).apply();updateSpeedLabels();ReferenceUi.refresh(this);}).show();
  }
  void startGPS(){AverageSpeedZoneUi.loadAsync(this);if(!accountConnected()||!activityResumed||listener!=null)return;if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED&&checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},3);return;}listener=new LocationListener(){public void onLocationChanged(Location l){if(l.hasAccuracy()&&l.getAccuracy()>2000)return;if(System.currentTimeMillis()-l.getTime()>30000||l.getTime()-System.currentTimeMillis()>5000)return;
 if(previousAcceptedGps!=null){long delta=l.getElapsedRealtimeNanos()-previousAcceptedGps.getElapsedRealtimeNanos();
 if(delta<=0)return;
 float seconds=delta/1000000000f;
 float gap=l.distanceTo(previousAcceptedGps);
 float accuracyAllowance=(l.hasAccuracy()?l.getAccuracy():30f)+(previousAcceptedGps.hasAccuracy()?previousAcceptedGps.getAccuracy():30f);
 float expectedSpeed=Math.max(l.hasSpeed()?l.getSpeed():0f,previousAcceptedGps.hasSpeed()?previousAcceptedGps.getSpeed():0f);
 if(seconds<12f&&gap>Math.max(35f,expectedSpeed*seconds*2.5f+Math.min(65f,accuracyAllowance)))return;
 }
 long motionAt=l.getElapsedRealtimeNanos()/1000000L;
 if(!gpsMotion.accept(motionAt,LocationManager.GPS_PROVIDER.equals(l.getProvider()),l.hasAccuracy()?l.getAccuracy():2000,
   l.hasSpeed()?l.getSpeed():Double.NaN,l.hasSpeedAccuracy()?l.getSpeedAccuracyMetersPerSecond():Double.NaN,
   l.hasBearing()?l.getBearing():Double.NaN,l.hasBearingAccuracy()?l.getBearingAccuracyDegrees():Double.NaN))return;
 previousAcceptedGps=new Location(l);lastLat=l.getLatitude();lastLon=l.getLongitude();
 double motionSpeed=gpsMotion.speed(android.os.SystemClock.elapsedRealtime());
 lastSpeedKmh=Double.isFinite(motionSpeed)?(float)motionSpeed:0f;
 if(Double.isFinite(gpsMotion.course()))lastBearing=(float)gpsMotion.course();
 reliableBearing=gpsMotion.courseReliable(android.os.SystemClock.elapsedRealtime())&&motionSpeed>=10;
 updateSpeedLabels();AverageSpeedZoneUi.update(MainActivity.this,l);if(accuracyChip!=null)accuracyChip.setText(l.hasAccuracy()?String.format(Locale.FRANCE,"PRÉCISION GPS  ·  ±%.0f m",l.getAccuracy()):"PRÉCISION GPS  ·  INCONNUE");
 if(headingChip!=null)headingChip.setText(getPreferences(0).getInt("speed_offset_kmh",0)==0?"km/h":"km/h corrigés");
 if(gps!=null)gps.setText(offlineView?"⌖  GPS ACTIF · LOCAL":"⌖  GPS ACTIF");updateDrivingDeclutter(Double.isFinite(motionSpeed));
 gpsSignalSuspended=false;lastAccuracy=l.hasAccuracy()?l.getAccuracy():0f;getPreferences(0).edit().putLong("foreground_gps_heartbeat",System.currentTimeMillis()).apply();recordTripPoint(l);refreshNearbySector();pushPosition();maybeAutoReroute(l);announceNextManeuver(l);if(routeStatus!=null)routeStatus.setText(routeStatusText());updateManeuverCard();checkDangers();ReferenceUi.refresh(MainActivity.this);}@Override public void onProviderEnabled(String provider){}@Override public void onProviderDisabled(String provider){if(!LocationManager.GPS_PROVIDER.equals(provider))return;gpsSignalSuspended=true;gpsMotion.invalidateMotion();reliableBearing=false;clearDangerWarning();updateSpeedLabels();if(gps!=null)gps.setText("GPS désactivé · active la localisation");if(tts!=null&&alertSpeechTracker.hasNavigation()&&!alertSpeechTracker.hasPending())tts.stop();alertSpeechTracker.cancelNavigation();updateManeuverCard();}@Override public void onStatusChanged(String provider,int status,Bundle extras){}};boolean precise=checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED;boolean subscribed=false;
    if(precise)try{lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000,0,listener);subscribed=true;}catch(Exception ignored){}
    try{if(lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,2500,5,listener);subscribed=true;}}catch(Exception ignored){}
    if(!subscribed){listener=null;if(gps!=null)gps.setText("GPS indisponible · activer la localisation");return;}
    driveSearchHandler.removeCallbacks(gpsFreshnessPulse);driveSearchHandler.post(gpsFreshnessPulse);
    Location last=null;try{if(precise)last=lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);}catch(Exception ignored){}
    try{Location network=lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);if(network!=null&&(last==null||System.currentTimeMillis()-last.getTime()>6000&&network.getTime()>last.getTime()))last=network;}catch(Exception ignored){}
    if(last!=null&&System.currentTimeMillis()-last.getTime()<120000)listener.onLocationChanged(last);
  }
  @Override public void onRequestPermissionsResult(int request,String[] p,int[] r){super.onRequestPermissionsResult(request,p,r);if(request==32){int next=pendingNotificationAction;pendingNotificationAction=0;if(next==1)startBackgroundAlerts();if(next==2)startFloatingRadar();return;}boolean granted=r.length>0&&r[0]==PackageManager.PERMISSION_GRANTED;if(request==3&&granted)startGPS();if(request==31){if(granted&&pendingFloatingActivation)startFloatingRadar();else{pendingFloatingActivation=false;Toast.makeText(this,"La localisation est nécessaire pour la bulle radar",Toast.LENGTH_LONG).show();}}}
  void openCsv(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);DocumentPicker.open(this,i,44);}
  @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==66){driveSearchHandler.postDelayed(()->{if(pendingFloatingActivation&&android.provider.Settings.canDrawOverlays(this))startFloatingRadar();else if(pendingFloatingActivation)Toast.makeText(this,"Autorise l’affichage superposé dans les réglages, puis reviens à ROADGUARD",Toast.LENGTH_LONG).show();},500);return;}if(request==63&&result==RESULT_OK&&data!=null&&data.getData()!=null){importLufopSwissCsvZip(data.getData());return;}if(request==64&&result==RESULT_OK&&data!=null&&data.getData()!=null){importPoiWarnerSwissZip(data.getData());return;}if(request==62&&result==RESULT_OK&&data!=null&&data.getData()!=null){importRadarCountryCatalog(data.getData());return;}if(request==61&&result==RESULT_OK&&data!=null){importExistingPmtiles(data.getData());return;}if(request==60&&result==RESULT_OK&&data!=null){importTileZip(data.getData());return;}if(request==50&&result==RESULT_OK&&data!=null){try{writeBackup(data.getData());Toast.makeText(this,"Sauvegarde exportée",Toast.LENGTH_LONG).show();}catch(Exception e){Toast.makeText(this,"Sauvegarde impossible : "+e.getMessage(),Toast.LENGTH_LONG).show();}return;}if(request==51&&result==RESULT_OK&&data!=null){new AlertDialog.Builder(this).setTitle("Restaurer les données ?").setMessage("Les valeurs présentes dans la sauvegarde remplaceront les données correspondantes sur cet appareil.").setNegativeButton("Annuler",null).setPositiveButton("Restaurer",(d,w)->{try{restoreBackup(data.getData());}catch(Exception e){Toast.makeText(this,"Restauration impossible : "+e.getMessage(),Toast.LENGTH_LONG).show();}}).show();return;}if(request==49&&result==RESULT_OK&&data!=null){try{writeTripGpx(data.getData());Toast.makeText(this,"Trajet GPS exporté",Toast.LENGTH_LONG).show();}catch(Exception e){Toast.makeText(this,"Export impossible : "+e.getMessage(),Toast.LENGTH_LONG).show();}return;}if(request==48&&result==RESULT_OK&&data!=null){try{writeGpx(data.getData());Toast.makeText(this,"GPX exporté",Toast.LENGTH_LONG).show();}catch(Exception e){Toast.makeText(this,"Export GPX impossible : "+e.getMessage(),Toast.LENGTH_LONG).show();}return;}if(request==46&&result==RESULT_OK&&data!=null){try{writeReportsCsv(data.getData());Toast.makeText(this,"CSV exporté",Toast.LENGTH_LONG).show();}catch(Exception e){Toast.makeText(this,"Export impossible : "+e.getMessage(),Toast.LENGTH_LONG).show();}return;}if((request==44||request==45||request==47)&&result==RESULT_OK&&data!=null){try{BufferedReader r=new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(data.getData()),"UTF-8"));StringBuilder sb=new StringBuilder();String line;int lines=0;while((line=r.readLine())!=null&&lines<20000&&sb.length()<1000000){sb.append(line).append('\n');lines++;}r.close();if(lines>=20000||sb.length()>=1000000)throw new Exception("Fichier trop volumineux (limite 1 Mo / 20 000 lignes)");if(request==47){int count=parseRoads(sb.toString());getPreferences(0).edit().putString("offline_roads",sb.toString()).apply();Toast.makeText(this,count+" points routiers importés",Toast.LENGTH_LONG).show();}else if(request==45){int count=parseRoute(sb.toString());getPreferences(0).edit().putString("route_gpx",sb.toString()).apply();Toast.makeText(this,count+" points GPX importés",Toast.LENGTH_LONG).show();}else{int count=RadarCsvInput.count(sb.toString());if(count<=0){throw new Exception("Aucun point valide. L’import précédent est conservé.");}getPreferences(0).edit().putString("danger_csv",sb.toString()).apply();requestRadarCatalogReload(true);BackgroundAlertService.requestRadarReload();Toast.makeText(this,count+" points personnels importés · bases conservées",Toast.LENGTH_LONG).show();}render();}catch(Exception e){Toast.makeText(this,"Import impossible : "+e.getMessage(),Toast.LENGTH_LONG).show();}}}
  int parsePrivateDangers(String csv){ArrayList<Danger> parsed=new ArrayList<>();for(String line:csv.split("\n")){String[] cols=line.trim().split("[,;]",3);if(cols.length<2)continue;try{double lat=Double.parseDouble(cols[0].trim());double lon=Double.parseDouble(cols[1].trim());if(!Double.isFinite(lat)||!Double.isFinite(lon)||Math.abs(lat)>90||Math.abs(lon)>180)continue;String type=cols.length>2?cols[2].trim():"Zone de danger";if(type.isEmpty()||type.length()>80)continue;parsed.add(new Danger(lat,lon,type));}catch(NumberFormatException ignored){}}dangers=parsed;officialRadarCount=0;rebuildDangerSpatialIndex();return parsed.size();}
  int parseDangers(String csv){int count=parsePrivateDangers(csv);loadOfficialRadarDatabase();return count;}
  void loadDangers(){parseDangers(getPreferences(0).getString("danger_csv",""));}
  void rebuildDangerSpatialIndex(){double[] lat=new double[dangers.size()],lon=new double[dangers.size()];for(int i=0;i<dangers.size();i++){lat[i]=dangers.get(i).lat;lon[i]=dangers.get(i).lon;}dangerSpatialIndex=new DangerSpatialIndex(lat,lon);}
  void loadOfficialRadarDatabase(){if(loadBundledRadarDatabase()){rebuildDangerSpatialIndex();return;}try{BufferedReader reader=new BufferedReader(new InputStreamReader(getAssets().open("radars_france_20251230.csv"),"UTF-8"));String line;reader.readLine();int loaded=0;while((line=reader.readLine())!=null&&loaded<4000){String[] c=line.split(";",-1);if(c.length<5)continue;try{double lat=Double.parseDouble(c[2].trim()),lon=Double.parseDouble(c[3].trim());if(!Double.isFinite(lat)||!Double.isFinite(lon)||Math.abs(lat)>90||Math.abs(lon)>180)continue;String code=c[1].trim().toUpperCase(Locale.ROOT);String type=officialRadarName(code);if(type==null)continue;officialRadarCount++;if(hasNearbyRadar(lat,lon,type))continue;Danger fallbackPoint=new Danger(lat,lon,type,RadarAlertPolicy.limit(c[4]));fallbackPoint.catalogPoint=true;dangers.add(fallbackPoint);loaded++;}catch(NumberFormatException ignored){}}reader.close();}catch(Exception e){android.util.Log.e("ROADGUARD","Base radar officielle indisponible",e);}rebuildDangerSpatialIndex();loadInstalledRadarCountries();}
  void loadInstalledRadarCountries(){JSONArray installed;try{installed=new JSONArray(getPreferences(0).getString("radar_country_installed","[]"));}catch(Exception e){return;}File dir=new File(getFilesDir(),"radar_countries");int total=0;for(int i=0;i<installed.length()&&total<150000;i++){JSONObject item=installed.optJSONObject(i);if(item==null)continue;String country=item.optString("country","");String id=item.optString("id","");if(RadarSourcePolicy.quarantined(id))continue;if(!id.matches("[a-z0-9_-]{2,50}"))continue;File file=new File(dir,id+".csv");try(BufferedReader reader=new BufferedReader(new InputStreamReader(new FileInputStream(file),"UTF-8"))){String line=reader.readLine();if(!"id;type;lat;lon".equalsIgnoreCase(line==null?"":line.trim()))continue;int lines=0,accepted=0;while((line=reader.readLine())!=null&&lines++<100000&&total<150000){String[] c=line.split(";",-1);if(c.length<4)continue;try{double lat=Double.parseDouble(c[2].trim()),lon=Double.parseDouble(c[3].trim());String type=c[1].trim();if(!RadarSourcePolicy.include(country,type,getPreferences(0).getBoolean(BundledRadarCatalog.SWISS_HISTORICAL_PREFERENCE,true)))continue;type=RadarSourcePolicy.displayLabel(country,type);if(!Double.isFinite(lat)||!Double.isFinite(lon)||Math.abs(lat)>90||Math.abs(lon)>180||!DangerTypePolicy.isRadar(type)||hasNearbyRadar(lat,lon,type))continue;Danger supplement=new Danger(lat,lon,type);supplement.catalogPoint=true;dangers.add(supplement);total++;if(++accepted%500==0)rebuildDangerSpatialIndex();}catch(NumberFormatException ignored){}}rebuildDangerSpatialIndex();}catch(Exception e){android.util.Log.w("ROADGUARD","Base radar pays ignorée : "+id,e);}}}
  String officialRadarName(String code){switch(code){case "ETF":return "Radar fixe";case "ETD":return "Radar discriminant";case "ETT":return "Radar tourelle";case "ETU":return "Radar urbain";case "ETVM":return "Radar vitesse moyenne";case "ETFR":return "Radar feu rouge";case "ETPN":return "Radar passage à niveau";default:return null;}}
  void loadCustomRadars(){try{JSONArray arr=new JSONArray(getPreferences(0).getString("custom_radars","[]"));long now=System.currentTimeMillis();for(int i=0;i<arr.length();i++){JSONObject o=arr.optJSONObject(i);if(o==null)continue;long expiry=o.optLong("expires",0L);if(expiry>0&&expiry<=now)continue;double a=o.optDouble("lat",Double.NaN),b=o.optDouble("lon",Double.NaN);String type=o.optString("type","").trim();if(Double.isFinite(a)&&Double.isFinite(b)&&Math.abs(a)<=90&&Math.abs(b)<=180&&DangerTypePolicy.isValidLabel(type)&&(!DangerTypePolicy.isRadar(type)||!hasNearbyRadar(a,b,type)))dangers.add(new Danger(a,b,type));}rebuildDangerSpatialIndex();}catch(Exception ignored){}if(community!=null)community.appendDangers(dangers);ControlMemory.append(this,dangers);rebuildDangerSpatialIndex();}
  boolean hasNearbyRadar(double lat,double lon,String type){
    for(int index:dangerSpatialIndex.query(lat,lon,40))if(index>=0&&index<dangers.size()){
      Danger point=dangers.get(index);if(!isRadarDanger(point.type)||!RadarIdentity.type(type).equals(RadarIdentity.type(point.type)))continue;
      if(geoMeters(lat,lon,point.lat,point.lon)<40)return true;
    }
    return false;
  }
  boolean hasNearbyRadar(double lat,double lon){for(int i:dangerSpatialIndex.query(lat,lon,40)){if(i<0||i>=dangers.size())continue;Danger d=dangers.get(i);if(!isRadarDanger(d.type))continue;float[] distance=new float[1];Location.distanceBetween(lat,lon,d.lat,d.lon,distance);if(distance[0]<40)return true;}return false;}
  void addCustomRadar(){if(!Double.isFinite(lastLat)||!Double.isFinite(lastLon)||lastAccuracy<=0||lastAccuracy>60){Toast.makeText(this,"Position GPS précise requise (±60 m maximum)",Toast.LENGTH_LONG).show();return;}
    final String[] types={"Radar fixe","Radar feu rouge","Contrôle mobile (2 h)"};new AlertDialog.Builder(this).setTitle("Enregistrer un radar ici ?").setMessage(String.format(Locale.FRANCE,"Position GPS : %.5f, %.5f (±%.0f m). Vérifie que tu es à l'arrêt.",lastLat,lastLon,lastAccuracy)).setItems(types,(dialog,choice)->{
      try{JSONArray arr=new JSONArray(getPreferences(0).getString("custom_radars","[]"));long now=System.currentTimeMillis();for(int i=0;i<arr.length();i++){JSONObject o=arr.optJSONObject(i);if(o==null)continue;float[] dist=new float[1];Location.distanceBetween(lastLat,lastLon,o.optDouble("lat"),o.optDouble("lon"),dist);if(dist[0]<40){Toast.makeText(this,"Radar déjà enregistré à proximité (moins de 40 m)",Toast.LENGTH_LONG).show();return;}}
      JSONObject o=new JSONObject();o.put("lat",lastLat);o.put("lon",lastLon);o.put("type",types[choice]);o.put("created",now);o.put("expires",choice==2?now+7200000L:0);arr.put(o);getPreferences(0).edit().putString("custom_radars",arr.toString()).apply();requestRadarCatalogReload(true);BackgroundAlertService.requestRadarReload();Toast.makeText(this,"Radar enregistré sur cet appareil",Toast.LENGTH_LONG).show();}catch(Exception e){Toast.makeText(this,"Enregistrement impossible",Toast.LENGTH_LONG).show();}}).setNegativeButton("Annuler",null).show();}
  int countCustomRadars(String filter,long now){int count=0;try{JSONArray arr=new JSONArray(getPreferences(0).getString("custom_radars","[]"));for(int i=0;i<arr.length();i++){JSONObject o=arr.optJSONObject(i);if(o==null)continue;long expiry=o.optLong("expires",0L);if(expiry>0&&expiry<=now)continue;double lat=o.optDouble("lat",Double.NaN),lon=o.optDouble("lon",Double.NaN);String type=o.optString("type","").trim();if(!Double.isFinite(lat)||!Double.isFinite(lon)||Math.abs(lat)>90||Math.abs(lon)>180||type.isEmpty()||type.length()>80)continue;if(radarMatchesFilter(type,filter))count++;}}catch(Exception ignored){}return count;}
  boolean radarMatchesFilter(String type,String filter){return DangerTypePolicy.matchesFilter(type,filter);}
  int countActiveRadars(String filter){int count=0;for(Danger point:dangers)if(DangerTypePolicy.isRadar(point.type)&&radarMatchesFilter(point.type,filter))count++;return count;}
  void manageCustomRadars(){
    try{
      JSONArray arr=new JSONArray(getPreferences(0).getString("custom_radars","[]"));
      String filter=getPreferences(0).getString("radar_ui_filter","all");
      ArrayList<Integer> visibleIndices=new ArrayList<>();ArrayList<String> labels=new ArrayList<>();
      for(int i=0;i<arr.length();i++){
        JSONObject o=arr.optJSONObject(i);
        if(o==null||!radarMatchesFilter(o.optString("type",""),filter))continue;
        visibleIndices.add(i);
        labels.add(String.format(Locale.FRANCE,"%s · %.5f, %.5f",o.optString("type","Radar"),o.optDouble("lat"),o.optDouble("lon")));
      }
      if(labels.isEmpty()){Toast.makeText(this,"Aucun radar pour ce filtre",Toast.LENGTH_LONG).show();return;}
      new AlertDialog.Builder(this).setTitle("Mes radars · toucher pour supprimer")
        .setItems(labels.toArray(new String[0]),(d,which)->{
          final int originalIndex=visibleIndices.get(which);
          new AlertDialog.Builder(this).setTitle("Supprimer ce radar ?")
            .setNegativeButton("Annuler",null)
            .setPositiveButton("Supprimer",(dd,w)->{
              try{
                JSONArray updated=new JSONArray();
                for(int i=0;i<arr.length();i++)if(i!=originalIndex)updated.put(arr.get(i));
                getPreferences(0).edit().putString("custom_radars",updated.toString()).apply();
                requestRadarCatalogReload(true);BackgroundAlertService.requestRadarReload();render();
              }catch(Exception ignored){}
            }).show();
        }).setNegativeButton("Fermer",null).show();
    }catch(Exception e){Toast.makeText(this,"Liste indisponible",Toast.LENGTH_SHORT).show();}
  }

  void removeExpiredRadars(){try{JSONArray arr=new JSONArray(getPreferences(0).getString("custom_radars","[]")),keep=new JSONArray();long now=System.currentTimeMillis();boolean changed=false;for(int i=0;i<arr.length();i++){JSONObject o=arr.optJSONObject(i);if(o==null)continue;long exp=o.optLong("expires",0);if(exp>0&&exp<=now)changed=true;else keep.put(o);}if(changed){getPreferences(0).edit().putString("custom_radars",keep.toString()).apply();requestRadarCatalogReload(true);BackgroundAlertService.requestRadarReload();}}catch(Exception ignored){}}

  long lastControlMemoryRead=0;
  void reloadControlMemory(boolean force){long now=System.currentTimeMillis();if(!force&&now-lastControlMemoryRead<15000)return;lastControlMemoryRead=now;ArrayList<Danger> next=new ArrayList<>();for(Danger d:dangers)if(d.controlId.isEmpty())next.add(d);ControlMemory.append(this,next);dangers=next;rebuildDangerSpatialIndex();pushDangers();}

  void reloadCommunityDangers(){requestRadarCatalogReload(true);reloadCommunityReports();}
  void reloadCommunityReports(){
    // Public snapshots change every two minutes. Do not reparse the complete local inventory.
    ArrayList<Danger> next=new ArrayList<>();for(Danger point:dangers)if(point.communityId==null||point.communityId.isEmpty())next.add(point);
    if(community!=null)community.appendReports(next);dangers=next;rebuildDangerSpatialIndex();pushDangers();
  }
  boolean expireCommunityDangers(){long now=System.currentTimeMillis();boolean changed=false;for(int i=dangers.size()-1;i>=0;i--){Danger d=dangers.get(i);if(d.expires>0&&d.expires<=now){dangers.remove(i);changed=true;}}if(changed){rebuildDangerSpatialIndex();pushDangers();}return changed;}
  void applyNativeVoice(){if(tts==null)return;String name=getPreferences(0).getString("native_voice","");try{if(!name.isEmpty()&&tts.getVoices()!=null)for(android.speech.tts.Voice v:tts.getVoices())if(name.equals(v.getName())){tts.setVoice(v);return;}}catch(Exception ignored){} }
  void tuneNativeVoice(){
    LinearLayout panel=column();panel.setPadding(pxDp(22),pxDp(8),pxDp(22),pxDp(8));
    int rate=getPreferences(0).getInt("voice_rate",100),pitch=getPreferences(0).getInt("voice_pitch",100);
    TextView rateLabel=label("Débit · "+rate+" %",16);add(panel,rateLabel);
    android.widget.SeekBar rateBar=new android.widget.SeekBar(this);rateBar.setMax(140);rateBar.setProgress(Math.max(0,Math.min(140,rate-60)));add(panel,rateBar);
    TextView pitchLabel=label("Hauteur · "+pitch+" %",16);add(panel,pitchLabel);
    android.widget.SeekBar pitchBar=new android.widget.SeekBar(this);pitchBar.setMax(100);pitchBar.setProgress(Math.max(0,Math.min(100,pitch-50)));add(panel,pitchBar);
    rateBar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(android.widget.SeekBar b,int value,boolean user){rateLabel.setText("Débit · "+(60+value)+" %");}public void onStartTrackingTouch(android.widget.SeekBar b){}public void onStopTrackingTouch(android.widget.SeekBar b){}});
    pitchBar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(android.widget.SeekBar b,int value,boolean user){pitchLabel.setText("Hauteur · "+(50+value)+" %");}public void onStartTrackingTouch(android.widget.SeekBar b){}public void onStopTrackingTouch(android.widget.SeekBar b){}});
    new AlertDialog.Builder(this).setTitle("Personnaliser la voix").setView(panel).setNegativeButton("Annuler",null).setNeutralButton("Écouter",(d,w)->{if(tts!=null){tts.setSpeechRate((60+rateBar.getProgress())/100f);tts.setPitch((50+pitchBar.getProgress())/100f);if(!alertSpeechTracker.speakOther(tts,"Attention, radar dans cinq cents mètres.",GuidanceAudioPolicy.PREVIEW))Toast.makeText(this,"Une annonce de conduite est en cours",Toast.LENGTH_SHORT).show();}tuneNativeVoice();}).setPositiveButton("Enregistrer",(d,w)->{int speed=60+rateBar.getProgress(),tone=50+pitchBar.getProgress();getPreferences(0).edit().putInt("voice_rate",speed).putInt("voice_pitch",tone).apply();if(tts!=null){tts.setSpeechRate(speed/100f);tts.setPitch(tone/100f);}ReferenceUi.refreshNow(this);}).show();
  }
  void chooseNativeVoice(){if(!voiceReady||tts==null){Toast.makeText(this,"La synthèse vocale n’est pas encore prête",Toast.LENGTH_LONG).show();return;}ArrayList<android.speech.tts.Voice> voices=new ArrayList<>();try{if(tts.getVoices()!=null)for(android.speech.tts.Voice v:tts.getVoices())if("fr".equals(v.getLocale().getLanguage())&&(!"CA".equals(v.getLocale().getCountry())))voices.add(v);}catch(Exception ignored){}java.util.Collections.sort(voices,(x,y)->x.getName().compareTo(y.getName()));String[] names=new String[voices.size()+1];names[0]="Voix française de l’appareil";int selectedVoice=0;for(int i=0;i<voices.size();i++){android.speech.tts.Voice v=voices.get(i);names[i+1]=v.getName()+(v.isNetworkConnectionRequired()?" · connexion":" · locale");if(v.getName().equals(getPreferences(0).getString("native_voice","")))selectedVoice=i+1;}new AlertDialog.Builder(this).setTitle("Choisir la voix française").setSingleChoiceItems(names,selectedVoice,(d,w)->{if(w==0){getPreferences(0).edit().remove("native_voice").putString("native_voice_label",names[0]).apply();tts.setLanguage(Locale.FRANCE);}else{tts.setVoice(voices.get(w-1));getPreferences(0).edit().putString("native_voice",voices.get(w-1).getName()).putString("native_voice_label",names[w]).apply();}if(!alertSpeechTracker.speakOther(tts,"Bienvenue sur ROADGUARD. Votre route, en confiance.",GuidanceAudioPolicy.PREVIEW))Toast.makeText(this,"Une annonce de conduite est en cours",Toast.LENGTH_SHORT).show();ReferenceUi.refreshNow(this);}).setPositiveButton("Terminer",null).show();}
  final class MapRadarBridge {final WebView owner;MapRadarBridge(WebView view){owner=view;}
    @JavascriptInterface public String mapBootstrap(){try{return new JSONObject().put("local",!vectorEngineDisabledForSession&&downloadedMapStore().hasVectors()).put("maxZoom",downloadedMapStore().maxZoom()).put("online",mapNetworkAvailable()).put("night",nightMap).put("pack",getPreferences(0).getString("map_appearance_pack","graphite")).put("palette",MapAppearance.current(MainActivity.this,nightMap)).toString();}catch(Exception unavailable){return "{}";}}

    @JavascriptInterface public void storeViewState(String value){
      if(value==null||value.length()>512)return;
      runOnUiThread(()->{if(map!=owner||isDestroyed()||!LocalPagePolicy.mapPage(owner.getUrl()))return;
        if("null".equals(value)){lastManualMapView="";return;}
        try{JSONObject state=new JSONObject(value);double lat=state.getDouble("lat"),lon=state.getDouble("lon"),zoom=state.getDouble("zoom");
          if(!state.optBoolean("paused",false)||!RouteRequestPolicy.coordinate(lat,lon)||!Double.isFinite(zoom)||zoom<0||zoom>22)return;
          lastManualMapView=new JSONObject().put("paused",true).put("lat",lat).put("lon",lon).put("zoom",zoom).toString();
        }catch(Exception invalid){}
      });
    }
    @JavascriptInterface public void engineState(String state,String detail){final String bounded=detail==null?"":detail.substring(0,Math.min(detail.length(),1000));runOnUiThread(()->handleMapEngineState(owner,state,bounded));}
    @JavascriptInterface public void storeViewport(String json){
      if(json==null||json.length()>256)return;
      try{JSONObject value=new JSONObject(json);double lat=value.getDouble("lat"),lon=value.getDouble("lon"),zoom=value.getDouble("zoom");
        if(!RouteRequestPolicy.coordinate(lat,lon)||!Double.isFinite(zoom)||zoom<0||zoom>22)return;
        final String safe=new JSONObject().put("lat",lat).put("lon",lon).put("zoom",zoom).toString();
        runOnUiThread(()->{if(map==owner&&!isDestroyed())getPreferences(0).edit().putString("map_last_viewport",safe).apply();});
      }catch(Exception invalid){}
    }
    @JavascriptInterface public void offlineTilesMissing(double lat,double lon){runOnUiThread(()->{
      if(map!=owner||!mapReady||mapNetworkAvailable()||mapLibrePreview||vectorEngineDisabledForSession||offlineFallbackChecked||!RouteRequestPolicy.coordinate(lat,lon)||!mapLibreAssetsPresent())return;
      for(InstalledPmtilesMosaic.Region region:installedMosaicRegions())if(region.west<=lon&&region.east>=lon&&region.south<=lat&&region.north>=lat){
        offlineFallbackChecked=true;mapLibrePreview=true;forceOnlineMapLibre=false;render();break;
      }
    });}
    @JavascriptInterface public void refreshRadars(){runOnUiThread(()->{if(map==owner&&mapReady&&!isFinishing()&&!isDestroyed()&&LocalPagePolicy.mapPage(owner.getUrl()))pushDangers();});}
  }
  /** Send a bounded viewport slice to WebView; retain the complete index for GPS alerts. */
  void pushDangers(){if(map==null||!mapReady)return;final WebView owner=map;final long request=++mapRadarRequestGeneration;
    owner.evaluateJavascript(RadarViewport.QUERY,result->{
      if(map!=owner||!mapReady||isDestroyed()||request!=mapRadarRequestGeneration)return;
      double latitude=lastLat,longitude=lastLon,radius=12000;
      try{JSONArray viewport=new JSONArray(result);latitude=viewport.getDouble(0);longitude=viewport.getDouble(1);radius=viewport.getDouble(2);}catch(Exception unavailable){}
      StringBuilder js=new StringBuilder("setDangers([");int count=0;
      if(Double.isFinite(latitude)&&Double.isFinite(longitude))for(Integer index:dangerSpatialIndex.queryLimited(latitude,longitude,radius,600)){
        if(index<0||index>=dangers.size())continue;Danger d=dangers.get(index);if(count++>0)js.append(',');
        js.append('[').append(d.lat).append(',').append(d.lon).append(',').append(JSONObject.quote(d.type)).append(',').append(d.expires).append(',').append(JSONObject.quote(d.communityId)).append(',').append(JSONObject.quote(d.controlId)).append(',').append(d.sourceDirection==null?"null":String.valueOf(d.sourceDirection)).append(',').append(d.speedLimit).append(',').append(d.catalogPoint).append(']');
      }
      js.append("]);setRadarMarkersVisible("+radarMarkersVisible+")").append(";setMapMarkerScale("+mapMarkerSize+")");owner.evaluateJavascript(js.toString(),null);
    });
  }
  // Guidage vocal experimental : annonces ponctuelles, uniquement pendant l'affichage Carte.
  // Les seuils 500 m / 150 m / 40 m sont franchis une seule fois par manoeuvre.
  void announceNextManeuver(Location fix){
    if(!voiceReady||tts==null||navigationPoints.size()<2||navigationSteps.isEmpty()||!selected.equals("Carte")||!getPreferences(0).getBoolean("Alertes vocales",true)||!getPreferences(0).getBoolean("Guidage vocal",true))return;
    if(!AlertPresentation.fresh(fix,System.currentTimeMillis(),SystemClock.elapsedRealtimeNanos())||!fix.hasAccuracy()||fix.getAccuracy()>40||alertSpeechTracker.hasPending()||alertSpeechTracker.hasNavigation())return;
    NavigationProgress model=routeProgressModel();NavigationProgress.Position position=currentRoutePosition(model,fix);
    if(position==null||position.lateral>100)return;
    guidanceProgressSegment=Math.max(guidanceProgressSegment,position.segment);
    int stepIndex=-1;
    for(int i=0;i<navigationSteps.size();i++){NavStep step=navigationSteps.get(i);if(!NavigationProgress.departure(step.instruction)&&model.toStep(position,step.routeIndex)>=1){stepIndex=i;break;}}
    if(stepIndex<0)return;
    NavStep next=navigationSteps.get(stepIndex);double along=model.toStep(position,next.routeIndex);
    int threshold=along<=40?0:along<=150?1:along<=500?2:-1;
    if(threshold<0)return;
    if(stepIndex!=spokenStepIndex){spokenStepIndex=stepIndex;spokenThreshold=3;}
    if(threshold>=spokenThreshold)return;
    String message=threshold==0?next.instruction:("Dans environ "+(threshold==2?500:150)+" mètres, "+next.instruction);
    final int step=stepIndex,milestone=threshold,calculation=routeCalculationId;
    final String utterance="roadguard-nav-"+calculation+"-"+step+"-"+milestone;
    final int previous=spokenThreshold;
    spokenThreshold=threshold;
    alertSpeechTracker.speakNavigation(tts,message,utterance,()->runOnUiThread(()->{if(!isDestroyed()&&calculation==routeCalculationId&&spokenStepIndex==step&&spokenThreshold==milestone)spokenThreshold=previous;}));
  }
  String corridorName(){int mode=getPreferences(0).getInt("route_corridor_width",1);return mode==0?"Étroit (35 m)":(mode==2?"Large (85 m)":"Normal (55 m)");}
  double corridorWidthMeters(){int mode=getPreferences(0).getInt("route_corridor_width",1);return mode==0?35.0:(mode==2?85.0:55.0);}
  String radarEffectName(){return getPreferences(0).getInt("radar_effect",1)==0?"Désactivé":"Halo rouge fixe dès 1 km";}
  boolean isOfficialRadarType(String type){return type!=null&&(type.equals("Radar fixe")||type.equals("Radar discriminant")||type.equals("Radar tourelle")||type.equals("Radar urbain")||type.equals("Radar vitesse moyenne")||type.equals("Radar feu rouge")||type.equals("Radar passage à niveau"));}
  boolean isRadarDanger(String type){return DangerTypePolicy.isRadar(type);}
  android.view.View radarBlueHalo;
  final RadarAlertAudio radarAudio=new RadarAlertAudio();
  boolean radarHaloVisible;
  void showRadarHalo(boolean show){
    radarHaloVisible=show;
    if(show&&radarBlueHalo==null){
      android.widget.FrameLayout content=findViewById(android.R.id.content);
      if(content!=null){radarBlueHalo=new android.view.View(this);radarBlueHalo.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);radarBlueHalo.setClickable(false);radarBlueHalo.setFocusable(false);GradientDrawable halo=new GradientDrawable();halo.setColor(0x12e9274e);halo.setStroke(pxDp(5),0xffe9274e);halo.setCornerRadius(pxDp(18));radarBlueHalo.setBackground(halo);content.addView(radarBlueHalo,new android.widget.FrameLayout.LayoutParams(-1,-1));}
    }
    if(radarBlueHalo!=null){radarBlueHalo.setVisibility(show?View.VISIBLE:View.GONE);if(show)radarBlueHalo.bringToFront();}
  }
  void stopRadarPulse(){showRadarHalo(false);radarAudio.stop();dangerTones.stop();if(radarPulse!=null){radarPulse.cancel();radarPulse=null;}activeRadarKey="";if(dangerBanner!=null)dangerBanner.setAlpha(1f);}
  void styleRadarWarning(Danger d,float meters){
    if(dangerBanner==null)return;
    boolean fixedHalo=isRadarDanger(d.type)&&RadarAlertPolicy.halo(meters,radarHaloVisible)&&getPreferences(0).getInt("radar_effect",1)!=0;
    // Stop any animation inherited from previous versions: the blue halo must remain steady.
    if(radarPulse!=null){radarPulse.cancel();radarPulse=null;}
    dangerBanner.setAlpha(1f);
    showRadarHalo(fixedHalo);
    if(fixedHalo){
      activeRadarKey=d.lat+":"+d.lon;
      dangerBanner.setTextColor(0xfffff6f7);
      GradientDrawable blueHalo=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xff5a1226,0xff24121b});blueHalo.setCornerRadius(pxDp(24));blueHalo.setStroke(pxDp(2),0xfff02d54);dangerBanner.setBackground(blueHalo);
    }else{
      activeRadarKey="";
      dangerBanner.setTextColor(0xffffffff);
      dangerBanner.setBackground(accentWarning(19));
    }
  }
  // Approximate distance to a route polyline in local metres. No lane-level accuracy.
  double routeDistanceMeters(double lat,double lon){
    if(navigationPoints.size()<2)return Double.MAX_VALUE;
    double cos=Math.cos(Math.toRadians(lat));double best=Double.MAX_VALUE;
    for(int i=1;i<navigationPoints.size();i++){
      double[] a=navigationPoints.get(i-1),b=navigationPoints.get(i);
      double ax=(a[1]-lon)*111320*cos,ay=(a[0]-lat)*111320;
      double bx=(b[1]-lon)*111320,by=(b[0]-lat)*111320;
      double dx=bx-ax,dy=by-ay,len2=dx*dx+dy*dy;
      double t=len2<0.001?0:Math.max(0,Math.min(1,-(ax*dx+ay*dy)/len2));
      double x=ax+t*dx,y=ay+t*dy;
      best=Math.min(best,Math.sqrt(x*x+y*y));
    }
    return best;
  }
  final GuidanceTone dangerTones=new GuidanceTone();
  void playDangerBeeps(String type){
    if(!getPreferences(0).getBoolean("danger_beeps",true))return;
    String t=type==null?"":type.toLowerCase(Locale.ROOT);
    dangerTones.play(this,isRadarDanger(type)?2:(t.contains("travaux")||t.contains("bouchon")||t.contains("ralentissement")?1:3));
  }
  String activeWarningPoint="";int lastWarningStage=0;long lastRadarVoiceAt=0L;
  // Keep per-radar milestones across short GPS dropouts and nearby radar switches.
  final java.util.HashMap<String,Integer> radarMilestones=new java.util.HashMap<>();
  final java.util.HashMap<String,Long> radarLastSeen=new java.util.HashMap<>();
  void clearDangerWarning(){activeRadarUi=null;if(dangerBanner!=null)dangerBanner.setVisibility(View.GONE);stopRadarPulse();}
  android.app.AlertDialog spotifyDialog,audioStudioDialog,musicSourceDialog;
  android.content.SharedPreferences speechPreferences;
  final android.content.SharedPreferences.OnSharedPreferenceChangeListener speechPreferenceListener=(prefs,key)->{
    if(key==null||"Alertes vocales".equals(key)){if(!prefs.getBoolean("Alertes vocales",true))this.alertSpeechTracker.cancelAll();}
    if(key==null||"Guidage vocal".equals(key)){if(!prefs.getBoolean("Guidage vocal",true))this.alertSpeechTracker.cancelNavigation();}
    if(key==null||"danger_beeps".equals(key)){if(!prefs.getBoolean("danger_beeps",true)){radarAudio.stop();dangerTones.stop();}}
    if("auto_reroute".equals(key)){automaticRerouteEnabled=prefs.getBoolean("auto_reroute",false);reroutePolicy.resetDetection();}
  };
  final AlertSpeechTracker alertSpeechTracker=new AlertSpeechTracker(this);
  void checkDangers(){removeExpiredRadars();reloadControlMemory(false);
    if(getPreferences(0).getBoolean("monitor_armed",false)&&!BackgroundAlertService.monitoring){clearDangerWarning();return;}
    if(gpsSignalSuspended||dangers.isEmpty()||!AlertPresentation.fresh(previousAcceptedGps,System.currentTimeMillis(),SystemClock.elapsedRealtimeNanos())||!getPreferences(0).getBoolean("Alertes de danger",true)){
      clearDangerWarning();
      return;
    }
    if(lastAccuracy>0&&lastAccuracy<=50)AlertMilestones.rearmDistant(this,lastLat,lastLon);
    ControlFeed.tick(this,previousAcceptedGps,false);
    Danger closest=null, pendingDanger=null, closestRadar=null;
    float radarBest=Float.MAX_VALUE;
    float best=Float.MAX_VALUE, pendingDistance=Float.MAX_VALUE;
    boolean forwardEnabled=getPreferences(0).getBoolean("forward_dangers",true);
    boolean directional=forwardEnabled&&reliableBearing;
    NavigationProgress corridorModel=navigationPoints.size()>1?routeProgressModel():null;
    NavigationProgress.Position corridorPosition=corridorModel==null?null:currentRoutePosition(corridorModel,previousAcceptedGps);
    boolean corridor=getPreferences(0).getBoolean("route_corridor_alerts",true)&&RadarApproachFilter.usesCorridor(corridorModel,corridorPosition,lastAccuracy,corridorWidthMeters(),forwardEnabled,reliableBearing,lastBearing);
    RadarApproachFilter.Projection vehicleOnRoute=corridor?RadarApproachFilter.projection(corridorPosition):null;
    for(int candidate:dangerSpatialIndex.query(lastLat,lastLon,1020)){if(candidate<0||candidate>=dangers.size())continue;Danger d=dangers.get(candidate);
      if(d.type.startsWith("Lieu de contrôle mémorisé")||d.type.startsWith("Lieu de radar mobile mémorisé"))continue;
      float[] res=new float[2];
      Location.distanceBetween(lastLat,lastLon,d.lat,d.lon,res);
      float meters=res[0];
      if(meters>(isRadarDanger(d.type)?1020:700))continue;
      if(isRadarDanger(d.type)&&forwardEnabled&&!reliableBearing)continue;
      if(corridor){double routeMeters=RadarApproachFilter.routeDistance(vehicleOnRoute,RadarApproachFilter.projection(corridorModel.upcoming(d.lat,d.lon,corridorPosition,35,5500)),corridorWidthMeters());if(!Double.isFinite(routeMeters))continue;meters=(float)routeMeters;if(meters>(isRadarDanger(d.type)?1020:700))continue;}
      if(directional&&!corridor&&!RadarApproachFilter.ahead(meters,res[1],lastBearing,lastAccuracy))continue;
      if(isRadarDanger(d.type)){double compensated=corridor?RadarLeadDistance.onRoute(meters):RadarLeadDistance.effective(meters,res[1],lastBearing,directional);if(!Double.isFinite(compensated)||compensated<0)continue;meters=(float)compensated;}
      if(isRadarDanger(d.type)&&meters<radarBest){radarBest=meters;closestRadar=d;}
      if(meters<best){best=meters;closest=d;}
      int candidateStage=AlertMilestones.stage(isRadarDanger(d.type),meters);
      if(AlertMilestones.pending(this,d.lat,d.lon,d.type,candidateStage)&&meters<pendingDistance){pendingDanger=d;pendingDistance=meters;}
    }
    // Announce only the first relevant radar. A second radar waits until the
    // nearest has been passed; other immediate road hazards keep their priority.
    if(pendingDanger!=null&&isRadarDanger(pendingDanger.type)&&pendingDanger!=closestRadar){pendingDanger=null;pendingDistance=Float.MAX_VALUE;}
    if(closestRadar!=null&&AlertMilestones.pending(this,closestRadar.lat,closestRadar.lon,closestRadar.type,AlertMilestones.stage(true,radarBest))
      &&(pendingDanger==null||radarBest<pendingDistance)){pendingDanger=closestRadar;pendingDistance=radarBest;}
    if(closestRadar!=null&&RadarAlertPolicy.halo(radarBest,radarHaloVisible)){closest=closestRadar;best=radarBest;boolean voiceAvailable=voiceReady&&tts!=null&&getPreferences(0).getBoolean("Alertes vocales",true)&&!alertSpeechTracker.radarSpeechFailed();boolean announced=!AlertMilestones.pending(this,closest.lat,closest.lon,closest.type,1);boolean handoffReady=RadarAnnouncementHandoff.allowBeeps(voiceAvailable,announced,alertSpeechTracker.hasPending());radarAudio.update(this,closest.lat,closest.lon,radarBest,gpsMotion.speed(SystemClock.elapsedRealtime()),RadarAlertPolicy.applicableLimit(closest.type,closest.speedLimit),handoffReady);if(announced)OverspeedSpeech.warn(this,alertSpeechTracker,tts,voiceReady,radarBest,gpsMotion.speed(SystemClock.elapsedRealtime()),RadarAlertPolicy.applicableLimit(closest.type,closest.speedLimit),SystemClock.elapsedRealtime());}else radarAudio.stop();
    if(closest==null){clearDangerWarning();return;}
    boolean sameWarning=activeRadarUi!=null&&RadarIdentity.point(activeRadarUi.lat,activeRadarUi.lon,activeRadarUi.type).equals(RadarIdentity.point(closest.lat,closest.lon,closest.type));
    boolean bannerNearby=AlertPresentation.nearby(best,isRadarDanger(closest.type),sameWarning&&dangerBanner!=null&&dangerBanner.getVisibility()==View.VISIBLE);
    if(!bannerNearby){clearDangerWarning();}
    if(dangerBanner!=null&&bannerNearby){
      activeRadarUi=isRadarDanger(closest.type)?closest:null;dangerBanner.setClickable(activeRadarUi!=null);dangerBanner.setFocusable(activeRadarUi!=null);
      int limit=RadarAlertPolicy.applicableLimit(closest.type,closest.speedLimit);
      String description=(isRadarDanger(closest.type)?"◉  ":"⚠  ")+RadarAlertPolicy.displayType(closest.type)+"  ·  "+AlertPresentation.text(best)+(limit>0?" · "+limit+" km/h":"");
      dangerBanner.setText(description);dangerBanner.setContentDescription(description);
      dangerBanner.setMaxLines(2);dangerBanner.setEllipsize(android.text.TextUtils.TruncateAt.END);
      styleRadarWarning(closest,best);dangerBanner.setVisibility(View.VISIBLE);
    }
    // Radar: one voice announcement inside 1 km, then proximity tones.
    // Other hazards keep their short-range two-stage warnings.
    // The visual halo tracks the nearest relevant radar, while the audio
    // queue can announce a second radar whose milestone is still pending.
    if(pendingDanger==null)return;
    boolean radar=isRadarDanger(pendingDanger.type);
    int stage=AlertMilestones.stage(radar,pendingDistance);
    String key=pendingDanger.lat+":"+pendingDanger.lon;
    long now=System.currentTimeMillis();
    String stageKey=key+":"+stage;
    boolean voiceEnabled=voiceReady&&tts!=null&&getPreferences(0).getBoolean("Alertes vocales",true);
    // A radar distance waits for another radar, but can preempt a lower-priority instruction.
    if(radar&&voiceEnabled&&(alertSpeechTracker.hasPending()||!alertSpeechTracker.hasNavigation()&&!alertSpeechTracker.hasOther()&&now-lastRadarVoiceAt<1800L))return;
    if(radar&&!voiceEnabled)return;
    AlertMilestones.Claim claim=radar?AlertMilestones.reserve(this,pendingDanger.lat,pendingDanger.lon,pendingDanger.type,stage):null;
    int announcement=radar?(claim==null?AlertMilestones.NONE:claim.kind):AlertMilestones.claimKind(this,pendingDanger.lat,pendingDanger.lon,pendingDanger.type,stage);
    if(announcement==AlertMilestones.NONE)return;
    lastAlertKey=stageKey;lastAlertAt=now;
    String distance=AlertPresentation.voice(pendingDistance);
    String label=RadarAlertPolicy.displayType(pendingDanger.type);
    String message=radar?(announcement==AlertMilestones.FIRST?RadarAlertPolicy.voiceType(label)+" à "+distance:distance):((stage==2?"Danger proche, ":"Attention, ")+label+" à environ "+distance);
    if(!radar)Toast.makeText(this,message,Toast.LENGTH_LONG).show();
    if(!radar)playDangerBeeps(pendingDanger.type);
    if(voiceEnabled){if(radar){if(alertSpeechTracker.speak(tts,message,claim))lastRadarVoiceAt=now;}
      else alertSpeechTracker.speakOther(tts,message,GuidanceAudioPolicy.HAZARD);}


  }

  @Override protected void onDestroy(){PhoneNavigationStopReceiver.detach(this);if(!BackgroundAlertService.running)phoneGuidanceSharing.stop(this);SpotifyControls.close(this);MobileAudioStudio.close(this);MusicSources.close(this);if(speechPreferences!=null)speechPreferences.unregisterOnSharedPreferenceChangeListener(speechPreferenceListener);nextDriveSearchToken();driveSearchHandler.removeCallbacksAndMessages(null);cancelRouteCalculation();activityResumed=false;if(mapNetworkCallback!=null){try{((ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE)).unregisterNetworkCallback(mapNetworkCallback);}catch(Exception ignored){}mapNetworkCallback=null;}initialDataExecutor.shutdownNow();destinationSearchExecutor.shutdownNow();if(community!=null)community.destroy();stopRadarPulse();stopGPS();if(tts!=null){tts.stop();alertSpeechTracker.cancelAll();tts.shutdown();}if(map!=null)map.destroy();super.onDestroy();}
  @Override protected void onStart(){super.onStart();}
  @Override protected void onStop(){super.onStop();}
  @Override protected void onPause(){super.onPause();activityResumed=false;if(community!=null)community.pause();clearDangerWarning();if(tts!=null)tts.stop();alertSpeechTracker.cancelAll();getPreferences(0).edit().remove("foreground_gps_heartbeat").apply();stopGPS();previousTripLocation=null;RoadguardVisibility.paused(this);}
  @Override protected void onResume(){super.onResume();activityResumed=true;requestRadarCatalogReload();RoadguardVisibility.resumed(this);if(accountConnected()&&pendingFloatingActivation)driveSearchHandler.postDelayed(()->{if(accountConnected()&&pendingFloatingActivation&&android.provider.Settings.canDrawOverlays(this))startFloatingRadar();},900);if(accountConnected()&&(getPreferences(0).getBoolean("monitor_armed",false)||getPreferences(0).getBoolean("floating_auto",false))&&!BackgroundAlertService.running&&checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED)try{startForegroundService(new Intent(this,BackgroundAlertService.class));}catch(Exception e){android.util.Log.e("ROADGUARD","Redémarrage de la surveillance refusé",e);}updateFloatingChip();long carRevisionBefore=lastCarRouteRevision;syncCarRoute();if(carRevisionBefore!=lastCarRouteRevision&&map!=null)render();if(community!=null)community.resume();if(radarDataReady)SiteAppUpdate.check(this,false);if(accountConnected()&&(map!=null||referenceWebView!=null)){refreshMapNetworkCacheMode();if(listener==null)startGPS();}}
}
