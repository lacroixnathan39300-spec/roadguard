package fr.roadguard.app;
import android.Manifest;
import android.content.*;
import android.content.pm.PackageManager;
import android.location.*;
import android.media.*;
import android.os.*;
import android.speech.tts.*;
import androidx.car.app.*;
import androidx.car.app.model.*;
import androidx.car.app.navigation.*;
import androidx.car.app.navigation.model.*;
import androidx.core.graphics.drawable.IconCompat;
import androidx.lifecycle.*;
import org.json.*;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.*;

/** Android Auto navigation UI; no location sharing or private-account requests. */
final class RoadguardCarScreen extends Screen {
  final Handler main=new Handler(Looper.getMainLooper());final ExecutorService io=Executors.newFixedThreadPool(2);
  interface RouteCalculator { JSONObject calculate(Location from,double lat,double lon)throws Exception; }
  RouteCalculator routeCalculator=CarRouteData::calculate;
  final CarReroutePolicy reroutePolicy=new CarReroutePolicy();
  final SharedPreferences prefs;final GpsMotion motion=new GpsMotion();final CarMapSurface map;
  CarRouteData data;Location fix;LocationManager lm;LocationListener listener;NavigationProgress progress;NavigationProgress.Position position;
  NavigationManager navigation;volatile boolean closed=false;boolean hostReady=false,routeStopped=false,navigating=false,loading=false,dataLoading=false,simulation=false;
  int generation=0,simIndex=0,spoken=-1;long lastLoad=0,lastReports=0,lastSpokenAt=0;String routeKey="",pendingQuery=null,message="Choisis un favori ou prépare ton trajet sur le téléphone";
  TextToSpeech tts;boolean voiceReady=false;
  final RadarAlertAudio radarAudio=new RadarAlertAudio();
  final AlertSpeechTracker carSpeech;
  boolean radarBlueActive;
  Step currentStep;double stepMeters=0,remainingSeconds=0;CarRouteData.Radar nearby;
  RoadguardCarScreen(CarContext context){super(context);carSpeech=new AlertSpeechTracker(context);prefs=context.getSharedPreferences("MainActivity",0);map=new CarMapSurface(this);
    context.getCarService(AppManager.class).setSurfaceCallback(map);navigation=context.getCarService(NavigationManager.class);
    navigation.setNavigationManagerCallback(new NavigationManagerCallback(){@Override public void onStopNavigation(){stopNavigation();}@Override public void onAutoDriveEnabled(){simulation=true;simIndex=0;message="Simulation · aucune position partagée";}});
    getLifecycle().addObserver((LifecycleEventObserver)(owner,event)->{if(event==Lifecycle.Event.ON_START){hostReady=true;main.post(()->{if(!closed){updateRoute();map.draw();invalidate();}});}if(event==Lifecycle.Event.ON_DESTROY)close();});
    tts=new TextToSpeech(context,status->{if(status==TextToSpeech.SUCCESS)main.post(()->{if(closed)return;int french=tts.setLanguage(Locale.FRANCE);tts.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build());String voice=prefs.getString("native_voice","");if(tts.getVoices()!=null)for(Voice v:tts.getVoices())if(v.getName().equals(voice)&&!v.isNetworkConnectionRequired())tts.setVoice(v);voiceReady=french>=TextToSpeech.LANG_AVAILABLE;});});
    carSpeech.useAudio(GuidanceAudio.get(context).speech(tts));carSpeech.install(tts);
    loadData();startGps();main.post(pulse);
  }
  String appearance(String key,String fallback){String value=getCarContext().getSharedPreferences("roadguard_community",0).getString(key,fallback);return key.equals("cursor")?value:CommunityPolicy.color(value,fallback);}
  void loadData(){
    if(closed||dataLoading)return;dataLoading=true;lastLoad=System.currentTimeMillis();
    try{io.execute(()->{
      CarRouteData built=null;
      try{CarRouteData candidate=new CarRouteData(getCarContext());candidate.loadRoute();if(candidate.loadRadars())built=candidate;}
      catch(Exception unavailable){android.util.Log.w("ROADGUARD_RADARS","Rechargement voiture échoué ; état précédent conservé",unavailable);}
      final CarRouteData ready=built;
      main.post(()->{
        dataLoading=false;if(closed||ready==null)return;
        if(!ready.catalogRevision.equals(RadarCatalogRevision.current(getCarContext()))){loadData();return;}
        data=ready;RadarActivationState.consumed(getCarContext(),"car",ready.catalogRevision,ready.catalog);
        updateRoute();map.draw();invalidate();
      });
    });}catch(RejectedExecutionException stopped){dataLoading=false;}
  }
  void startGps(){
    if(closed)return;if(getCarContext().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){stopGps();fix=null;return;}if(listener!=null)return;
    try{getCarContext().startForegroundService(new Intent(getCarContext(),CarLocationService.class));lm=(LocationManager)getCarContext().getSystemService(Context.LOCATION_SERVICE);
      listener=new LocationListener(){@Override public void onLocationChanged(Location l){accept(l);}@Override public void onProviderEnabled(String provider){}@Override public void onProviderDisabled(String provider){fix=null;message="GPS désactivé · active la localisation";updateRoute();map.draw();invalidate();}@Override public void onStatusChanged(String provider,int status,Bundle extras){}};lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000,0,listener,Looper.getMainLooper());Location known=lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);if(known!=null)accept(known);
    }catch(Exception e){if(lm!=null&&listener!=null)try{lm.removeUpdates(listener);}catch(Exception ignored){}listener=null;message="Active le GPS et l’autorisation précise sur le téléphone";}
  }
  void stopGps(){if(lm!=null&&listener!=null)try{lm.removeUpdates(listener);}catch(Exception ignored){}listener=null;try{getCarContext().stopService(new Intent(getCarContext(),CarLocationService.class));}catch(RuntimeException ignored){}}
  void accept(Location location){if(closed||simulation||!CommunityPolicy.freshFix(location.getTime(),location.hasAccuracy()?location.getAccuracy():1000,System.currentTimeMillis()))return;
    if(fix!=null){long delta=location.getElapsedRealtimeNanos()-fix.getElapsedRealtimeNanos();if(delta<=0)return;double seconds=delta/1e9;
      double speed=Math.max(location.hasSpeed()?location.getSpeed():0,fix.hasSpeed()?fix.getSpeed():0);
      double accuracy=(location.hasAccuracy()?location.getAccuracy():30)+(fix.hasAccuracy()?fix.getAccuracy():30);
      if(seconds<12&&location.distanceTo(fix)>Math.max(35,speed*seconds*2.5+Math.min(65,accuracy)))return;
    }
    if(!motion.accept(location.getElapsedRealtimeNanos()/1000000L,true,location.hasAccuracy()?location.getAccuracy():1000,location.hasSpeed()?location.getSpeed():Double.NaN,location.hasSpeedAccuracy()?location.getSpeedAccuracyMetersPerSecond():Double.NaN,location.hasBearing()?location.getBearing():Double.NaN,location.hasBearingAccuracy()?location.getBearingAccuracyDegrees():Double.NaN))return;
    fix=new Location(location);ControlFeed.tick(getCarContext(),fix,false);prefs.edit().putLong("foreground_gps_heartbeat",System.currentTimeMillis()).apply();dispatchQuery();updateRoute();map.draw();invalidate();
  }
  final Runnable pulse=new Runnable(){public void run(){if(closed)return;long now=System.currentTimeMillis();startGps();
    if(simulation&&data!=null&&data.route.size()>1){double[] a=data.route.get(Math.min(simIndex++,data.route.size()-1));fix=new Location("simulation");fix.setLatitude(a[0]);fix.setLongitude(a[1]);fix.setAccuracy(5);fix.setTime(now);fix.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());fix.setSpeed(10);if(simIndex<data.route.size()){double[] b=data.route.get(simIndex);Location to=new Location(fix);to.setLatitude(b[0]);to.setLongitude(b[1]);fix.setBearing(fix.bearingTo(to));}motion.accept(SystemClock.elapsedRealtime(),true,5,10,1,fix.getBearing(),1);updateRoute();}
    if(!simulation&&(now-lastLoad>=30000||data!=null&&!data.catalogRevision.equals(RadarCatalogRevision.current(getCarContext()))))loadData();if(fresh()&&!simulation&&now-lastReports>=120000){lastReports=now;refreshReports();}if(!fresh()&&currentStep!=null)updateRoute();map.draw();invalidate();main.postDelayed(this,2000);
  }};
  boolean fresh(){return fix!=null&&(simulation||AlertPresentation.fresh(fix,System.currentTimeMillis(),SystemClock.elapsedRealtimeNanos()));}
  void updateRoute(){
    if(!hostReady||data==null)return;if(!fresh()){reroutePolicy.resetDetection();radarBlueActive=false;radarAudio.stop();currentStep=null;nearby=null;if(navigating)navigation.updateTrip(new Trip.Builder().setLoading(true).build());if(tts!=null)tts.stop();spoken=-1;releaseAudio();return;}String key=prefs.getString("cached_navigation","");if(routeStopped&&key.equals(routeKey)){currentStep=null;progress=null;data.route=new ArrayList<>();return;}routeStopped=false;if(!key.equals(routeKey)||!key.equals(data.snapshotKey)){data.loadRoute();routeKey=key;progress=data.route.size()>1?new NavigationProgress(data.route):null;position=null;spoken=-1;}
    if(progress==null&&data.route.size()>1)progress=new NavigationProgress(data.route);
    if(progress!=null){position=progress.track(fix.getLatitude(),fix.getLongitude(),position==null?0:Math.max(0,position.segment-2),fix.getElapsedRealtimeNanos()/1000000L,motion.speedMetersPerSecond(SystemClock.elapsedRealtime()),fix.hasAccuracy()?fix.getAccuracy():30);if(position!=null){
      if(position.lateral<=100)reroutePolicy.resetDetection();if(position.lateral>150)carSpeech.cancelNavigation();
      if(position.lateral>100){
        currentStep=null;message="Hors itinéraire · revenir vers le trajet";
        if(navigating)navigation.updateTrip(new Trip.Builder().setLoading(true).build());
        if(!simulation&&reroutePolicy.due(fix.getElapsedRealtimeNanos()/1000000L,SystemClock.elapsedRealtime(),position.lateral,fix.hasAccuracy()?fix.getAccuracy():1000,motion.speedMetersPerSecond(SystemClock.elapsedRealtime()),prefs.getBoolean("auto_reroute",false),loading))recalculateDestination();
      }else if(position.arrived()){if(navigating){navigation.navigationEnded();navigating=false;}currentStep=null;message="Arrivée · vous êtes à destination";carSpeech.cancelNavigation();}
      else{if(!navigating){navigation.navigationStarted();navigating=true;}CarRouteData.Turn turn=null;int idx=-1;for(int i=0;i<data.turns.size();i++){CarRouteData.Turn t=data.turns.get(i);if(t.index>position.segment&&!NavigationProgress.departure(t.text)){turn=t;idx=i;break;}}
        if(turn==null){turn=new CarRouteData.Turn(data.route.size()-1,"Arrivée à destination");idx=data.turns.size();}stepMeters=progress.toStep(position,turn.index);remainingSeconds=progress.remainingSeconds(position,data.seconds);currentStep=new Step.Builder(turn.text).setManeuver(CarManeuvers.from(turn.text)).build();navigation.updateTrip(new Trip.Builder().addStep(currentStep,estimate(stepMeters,Math.min(remainingSeconds,stepMeters/Math.max(2,Double.isFinite(motion.speed(SystemClock.elapsedRealtime()))?motion.speed(SystemClock.elapsedRealtime())/3.6:8.33)))).addDestination(new Destination.Builder().setName(data.label).build(),estimate(position.remaining,remainingSeconds)).build());
        if(idx!=spoken&&stepMeters<=250&&prefs.getBoolean("Guidage vocal",true)&&speak(turn.text))spoken=idx;
      }
    }}else{currentStep=null;if(navigating){navigation.navigationEnded();navigating=false;}}
    radarBlueActive=false;CarRouteData.Radar approach=data.nearestRadar(fix);if(approach!=null&&!simulation&&prefs.getBoolean("Alertes de danger",true)&&fix.hasAccuracy()&&fix.getAccuracy()<=60){Location camera=new Location("radar");camera.setLatitude(approach.lat);camera.setLongitude(approach.lon);double meters=RadarLeadDistance.effective(fix.distanceTo(camera),fix.bearingTo(camera),fix.getBearing(),fix.hasBearing()&&fix.hasSpeed()&&fix.getSpeed()>2.8f);if(meters>=0){radarBlueActive=meters<=500&&prefs.getInt("radar_effect",1)!=0;boolean voiceAvailable=voiceReady&&tts!=null&&prefs.getBoolean("Alertes vocales",true)&&!carSpeech.radarSpeechFailed();boolean announced=!AlertMilestones.pending(getCarContext(),approach.lat,approach.lon,approach.type,1);boolean handoffReady=RadarAnnouncementHandoff.allowBeeps(voiceAvailable,announced,carSpeech.hasPending());radarAudio.update(getCarContext(),approach.lat,approach.lon,meters,motion.speed(SystemClock.elapsedRealtime()),approach.speedLimit,handoffReady);}else radarAudio.stop();}else radarAudio.stop();nearby=data.nearest(fix);if(nearby!=null&&!simulation&&prefs.getBoolean("Alertes de danger",true)&&fix.hasAccuracy()&&fix.getAccuracy()<=60){Location radar=new Location("radar");radar.setLatitude(nearby.lat);radar.setLongitude(nearby.lon);float d=fix.distanceTo(radar);if(DangerTypePolicy.isRadar(nearby.type))d=(float)RadarLeadDistance.effective(d,fix.bearingTo(radar),fix.getBearing(),fix.hasBearing()&&fix.hasSpeed()&&fix.getSpeed()>2.8f);int milestone=d>=0&&d<=1000?AlertMilestones.stage(DangerTypePolicy.isRadar(nearby.type),d):0;if(milestone>0&&AlertMilestones.pending(getCarContext(),nearby.lat,nearby.lon,nearby.type,milestone)){
      if(DangerTypePolicy.isRadar(nearby.type))speakRadar(nearby,d,milestone);
      else if(voiceReady&&prefs.getBoolean("Alertes vocales",true)){if(carSpeech.speakOther(tts,RadarAlertPolicy.displayType(nearby.type)+", à "+AlertPresentation.voice(d),GuidanceAudioPolicy.HAZARD))AlertMilestones.claim(getCarContext(),nearby.lat,nearby.lon,nearby.type,milestone);}
    }}
  }
  TravelEstimate estimate(double meters,double seconds){long duration=Double.isFinite(seconds)?Math.max(0,Math.round(seconds)):0;return new TravelEstimate.Builder(distance(meters),DateTimeWithZone.create(System.currentTimeMillis()+duration*1000,TimeZone.getDefault())).setRemainingTimeSeconds(duration).build();}
  Distance distance(double meters){return meters>=1000?Distance.create(meters/1000,Distance.UNIT_KILOMETERS):Distance.create(Math.max(0,Double.isFinite(meters)?meters:0),Distance.UNIT_METERS);}
  Action icon(int resource,Runnable run){return new Action.Builder().setIcon(new CarIcon.Builder(IconCompat.createWithResource(getCarContext(),resource)).build()).setOnClickListener(run::run).build();}
  @Override public Template onGetTemplate(){
    if(getCarContext().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)return new MessageTemplate.Builder("Autorise le GPS précis pour ROADGUARD sur le téléphone").setTitle("ROADGUARD").setHeaderAction(Action.APP_ICON).addAction(new Action.Builder().setTitle("Autoriser").setOnClickListener(()->getCarContext().requestPermissions(Arrays.asList(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION),(approved,rejected)->{startGps();invalidate();})).build()).build();
    ActionStrip actions=new ActionStrip.Builder().addAction(icon(android.R.drawable.ic_menu_search,()->getScreenManager().push(new CarSearchScreen(getCarContext(),this::startRoute)))).addAction(new Action.Builder().setTitle("Favoris").setOnClickListener(this::favorites).build()).addAction(icon(android.R.drawable.ic_menu_directions,this::recalculateDestination)).addAction(icon(android.R.drawable.ic_menu_close_clear_cancel,this::stopNavigation)).build();
    NavigationTemplate.Builder view=new NavigationTemplate.Builder().setActionStrip(actions).setMapActionStrip(new ActionStrip.Builder().addAction(icon(android.R.drawable.ic_menu_zoom,()->map.zoom(1))).addAction(icon(android.R.drawable.ic_menu_revert,()->map.zoom(-1))).build());
    if(loading)view.setNavigationInfo(new RoutingInfo.Builder().setLoading(true).build());
    else if(currentStep!=null&&position!=null&&fresh()){view.setNavigationInfo(new RoutingInfo.Builder().setCurrentStep(currentStep,distance(stepMeters)).build());view.setDestinationTravelEstimate(estimate(position.remaining,remainingSeconds));}
    else{String title=!fresh()?"GPS en attente":message;if(nearby!=null&&fresh()&&currentStep==null&&progress==null){Location r=new Location("radar");r.setLatitude(nearby.lat);r.setLongitude(nearby.lon);title=nearby.type+" · "+Math.round(fix.distanceTo(r))+" m";}view.setNavigationInfo(new MessageInfo.Builder(title).setText(simulation?"Simulation · aucune position partagée":"ROADGUARD · carte et alertes").build());}
    return view.build();
  }
  void favorites(){getScreenManager().push(new CarFavoritesScreen(getCarContext(),this::startRoute));}

  void recalculateDestination(){double[] point=NavigationIntentInput.coordinates(prefs.getString("destination",""));if(point==null){message="Choisis d’abord un favori";invalidate();return;}startRoute(point[0],point[1],prefs.getString("destination_label","Destination"));}
  void startRoute(double lat,double lon,String label){if(!fresh()){pendingQuery=lat+","+lon;message="Destination en attente du GPS";invalidate();return;}final int token=++generation;final String priorDestination=prefs.getString("destination","");loading=true;invalidate();final Location from=new Location(fix);
    io.execute(()->{try{JSONObject route=routeCalculator.calculate(from,lat,lon);main.post(()->{if(closed||generation!=token)return;loading=false;if(!priorDestination.equals(prefs.getString("destination",""))){message="Destination modifiée sur le téléphone";invalidate();return;}routeStopped=false;prefs.edit().putString("destination",String.format(Locale.US,"%.7f,%.7f",lat,lon)).putString("destination_label",label).putString("cached_navigation",route.toString()).putLong("car_route_revision",System.currentTimeMillis()).apply();position=null;loadData();invalidate();});}catch(Exception e){main.post(()->{if(closed||generation!=token)return;loading=false;message="Calcul impossible · trajet précédent conservé";CarToast.makeText(getCarContext(),message,CarToast.LENGTH_LONG).show();invalidate();});}});
  }
  void handleIntent(Intent intent){String q=NavigationIntentInput.query(intent==null?null:intent.getDataString());if(q!=null){generation++;pendingQuery=q;dispatchQuery();}}
  void dispatchQuery(){if(pendingQuery==null||!fresh())return;String q=pendingQuery;pendingQuery=null;double[] point=NavigationIntentInput.coordinates(q);if(point!=null){startRoute(point[0],point[1],q);return;}final int token=++generation;loading=true;invalidate();io.execute(()->{try{JSONObject root=new JSONObject(CarRouteData.get(CommunitySync.ORIGIN+"/api/v1/places?q="+URLEncoder.encode(q,"UTF-8"),200000));JSONArray places=root.getJSONArray("places");JSONObject first=places.getJSONObject(0);double a=first.getDouble("lat"),b=first.getDouble("lng");if(!RouteRequestPolicy.coordinate(a,b))throw new Exception();main.post(()->{if(!closed&&token==generation){loading=false;startRoute(a,b,first.optString("name",q));}});}catch(Exception e){main.post(()->{if(!closed&&token==generation){loading=false;message="Destination introuvable";invalidate();}});}});}
  void stopNavigation(){reroutePolicy.reset();generation++;routeStopped=true;loading=false;pendingQuery=null;if(!simulation)prefs.edit().remove("cached_navigation").putLong("car_route_revision",System.currentTimeMillis()).apply();if(navigating)navigation.navigationEnded();navigating=false;currentStep=null;progress=null;position=null;routeKey=prefs.getString("cached_navigation","");if(data!=null){data.route=new ArrayList<>();data.turns=new ArrayList<>();}if(tts!=null)tts.stop();releaseAudio();message="Guidage arrêté";map.draw();invalidate();}
  boolean speak(String text){
    if(closed||simulation||!voiceReady||!prefs.getBoolean("Alertes vocales",true)||carSpeech.hasNavigation()||carSpeech.hasPending())return false;
    long now=System.currentTimeMillis();if(now-lastSpokenAt<1800)return false;final int token=generation;
    boolean accepted=carSpeech.speakNavigation(tts,text,"car-nav-"+token,()->main.post(()->{if(!closed&&generation==token)spoken=-1;}));
    if(accepted)lastSpokenAt=now;return accepted;
  }
  void speakRadar(CarRouteData.Radar radar,double meters,int milestone){
    if(closed||simulation||!voiceReady||!prefs.getBoolean("Alertes vocales",true)||carSpeech.hasPending())return;
    AlertMilestones.Claim claim=AlertMilestones.reserve(getCarContext(),radar.lat,radar.lon,radar.type,milestone);if(claim==null)return;
    String message=claim.kind==AlertMilestones.FIRST?RadarAlertPolicy.voiceType(radar.type)+" à "+AlertPresentation.voice(meters):AlertPresentation.voice(meters);
    if(carSpeech.speak(tts,message,claim))lastSpokenAt=System.currentTimeMillis();
  }
  void releaseAudio(){carSpeech.cancelAll();}
  void refreshReports(){final double lat=fix.getLatitude(),lon=fix.getLongitude();io.execute(()->{try{JSONArray all=new JSONArray();String cursor="";long deadline=System.currentTimeMillis()+20000;for(int page=0;page<20;page++){if(closed||System.currentTimeMillis()>deadline)return;String url=CommunitySync.ORIGIN+"/api/v1/reports?bbox="+CommunityPolicy.bbox(lat,lon)+(cursor.isEmpty()?"":"&cursor="+URLEncoder.encode(cursor,"UTF-8"));JSONObject response=new JSONObject(CarRouteData.get(url,1500000));JSONArray rows=response.getJSONArray("reports");for(int i=0;i<rows.length();i++){JSONObject r=rows.getJSONObject(i);if(!CommunityPolicy.uuid(r.optString("id"))||!CommunityPolicy.coordinate(r.optDouble("lat",Double.NaN),r.optDouble("lng",Double.NaN)))throw new Exception("Signalement invalide");if(r.optLong("expires")>System.currentTimeMillis()&&"active".equals(r.optString("status","active")))all.put(r);}if(all.length()>10000)throw new Exception("Trop de points");cursor=response.isNull("nextCursor")?"":response.optString("nextCursor","");if(cursor.isEmpty()){if(closed)return;JSONObject cache=new JSONObject().put("reports",all).put("lat",lat).put("lng",lon).put("downloaded",System.currentTimeMillis());byte[] bytes=cache.toString().getBytes("UTF-8");if(bytes.length>1500000)return;CommunityFiles.write(getCarContext().getFilesDir(),"community-reports.json",bytes);main.post(()->{if(!closed)loadData();});return;}}}catch(Exception ignored){}});}
  void close(){radarAudio.stop();radarBlueActive=false;if(closed)return;closed=true;generation++;main.removeCallbacksAndMessages(null);stopGps();if(navigating)try{navigation.navigationEnded();}catch(Exception ignored){}navigating=false;try{navigation.clearNavigationManagerCallback();}catch(RuntimeException ignored){}try{getCarContext().getCarService(AppManager.class).setSurfaceCallback(null);}catch(RuntimeException ignored){}if(tts!=null){tts.stop();tts.shutdown();}releaseAudio();map.close();io.shutdownNow();}
}
