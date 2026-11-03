package fr.roadguard.app;

import android.content.res.Configuration;
import android.app.AlertDialog;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.widget.FrameLayout;
import android.widget.Toast;
import android.view.View;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/** Compact ROADGUARD screens following the ten-screen approved composition.
 * Advanced controls remain reachable through the previous native pages. */
final class ReferenceUi {
  private ReferenceUi(){}

  static void show(MainActivity a){
    WebView view=new WebView(a);a.referenceWebView=view;
    view.setBackgroundColor(0xff061019);
    LocalPagePolicy.configure(view,false);
    view.addJavascriptInterface(new Bridge(a,view),"Roadguard");
    view.setWebViewClient(new WebViewClient(){
      @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){return true;}
      @Override public boolean shouldOverrideUrlLoading(WebView v,String url){return true;}
      @Override public void onPageFinished(WebView v,String url){
        if(a.referenceWebView==v&&LocalPagePolicy.referencePage(url)){
          v.evaluateJavascript("roadguardUpdate("+JSONObject.quote(data(a).toString())+")",null);
        }
      }
    });
    a.frame.addView(view,new FrameLayout.LayoutParams(-1,-1));
    if(a.accountConnected()&&(a.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)==android.content.pm.PackageManager.PERMISSION_GRANTED||a.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)==android.content.pm.PackageManager.PERMISSION_GRANTED))a.startGPS();
    view.loadUrl("file:///android_asset/reference_ui.html");
  }

  static void refresh(MainActivity a){long now=android.os.SystemClock.elapsedRealtime();if(now-a.lastReferenceRefreshAt<2500)return;a.lastReferenceRefreshAt=now;refreshNow(a);}
  static void refreshNow(MainActivity a){WebView view=a.referenceWebView;if(view!=null&&LocalPagePolicy.referencePage(view.getUrl())&&!a.isFinishing()&&!a.isDestroyed())view.evaluateJavascript("roadguardUpdate("+JSONObject.quote(data(a).toString())+")",null);}
  static JSONObject data(MainActivity a){
    JSONObject j=new JSONObject();
    try{
      boolean freshGps=a.previousAcceptedGps!=null&&System.currentTimeMillis()-a.previousAcceptedGps.getTime()<30000;
      j.put("version",a.getPackageManager().getPackageInfo(a.getPackageName(),0).versionName);
      j.put("community",a.community==null?new JSONObject():a.community.data());
      j.put("screen",a.selected).put("gps",freshGps).put("reportLocationReady",a.hasFreshPreciseLocation());
      j.put("updateVersion",a.getPreferences(0).getString("update_available_version",""));
      j.put("radarLoading",a.radarLoadStatus).put("mapPack",a.getPreferences(0).getString("map_appearance_pack","graphite"));
      j.put("night",a.nightMap).put("auto",a.getPreferences(0).getBoolean("theme_auto",false));
      String saved=a.getPreferences(0).getString("destination_label","");
      j.put("destination",a.hasDestination()?(saved.isEmpty()?String.format(Locale.FRANCE,"%.5f, %.5f",a.destinationLat,a.destinationLon):saved):"");
      j.put("routeReady",a.navigationPoints.size()>1).put("routeActive",a.activeRouteIndex).put("routePending",a.routeRequestPending);
      j.put("routeSummary",String.format(Locale.FRANCE,"%d min · %.1f km",Math.max(1,Math.round(a.navigationSeconds/60)),a.navigationMeters/1000));
      j.put("network",a.networkAvailable()).put("recording",a.tripRecording).put("tripDistance",a.tripDistance()).put("tripSamples",a.tripSamples);
      j.put("mapTransfer",MapPackDownloadService.isRunning()?MapPackDownloadService.status(a):"");
      j.put("reports",a.getPreferences(0).getString("report_history", ""));
      JSONArray favorites=new JSONArray();JSONArray savedFavorites=a.destinationFavorites();for(int i=0;i<savedFavorites.length();i++){JSONObject favorite=savedFavorites.optJSONObject(i);if(favorite==null)continue;double lat=favorite.optDouble("lat",Double.NaN),lon=favorite.optDouble("lon",Double.NaN);if(!RouteRequestPolicy.coordinate(lat,lon))continue;JSONObject item=new JSONObject();item.put("index",i).put("name",favorite.optString("name","Favori")).put("coords",String.format(Locale.FRANCE,"%.5f, %.5f",lat,lon));favorites.put(item);}j.put("favorites",favorites);
      if(a.visibleTileCache!=null&&"Hors ligne".equals(a.selected)){VisibleTileCache.Snapshot cache=a.visibleTileCache.snapshot();j.put("cacheFiles",cache.files).put("cacheMb",Math.round(cache.bytes/1024.0/1024.0)).put("cacheLimitMb",Math.round(a.visibleTileCache.limitBytes()/1024.0/1024.0)).put("cacheReserveMb",Math.round(a.visibleTileCache.reserveBytes()/1024.0/1024.0)).put("cacheHits",cache.hits).put("cacheDownloads",cache.downloads).put("cacheMisses",cache.misses).put("cacheAttempts",cache.attempts).put("cacheErrors",cache.failures).put("cacheLastError",cache.lastFailure);}
      j.put("savedRoute",a.navigationPoints.size()>1);
      JSONArray options=new JSONArray();
      for(MainActivity.RouteOption route:a.availableRoutes){
        JSONObject item=new JSONObject();
        item.put("label",route.label).put("summary",String.format(Locale.FRANCE,"%d min · %.1f km",Math.max(1,Math.round(route.seconds/60)),route.meters/1000));
        options.put(item);
      }
      j.put("routeOptions",options);
      j.put("frCount",a.bundledLoadedFranceCount).put("chCount",a.bundledLoadedSwissCount);
      JSONObject bases=new JSONObject(),countries=a.bundledRadarManifest().optJSONObject("countries");
      if(countries!=null){for(String code:new String[]{"FR","CH"}){JSONObject country=countries.optJSONObject(code);if(country!=null)bases.put(code,new JSONObject().put("permanent",country.optInt("static_count")).put("historical",country.optInt("historical_count")));}}
      bases.put("historyFr",a.getPreferences(0).getBoolean(BundledRadarCatalog.HISTORICAL_PREFERENCE,false)).put("historyCh",a.getPreferences(0).getBoolean(BundledRadarCatalog.SWISS_HISTORICAL_PREFERENCE,true));
      JSONArray extra=new JSONArray(),installed=a.installedRadarCountries();for(int i=0;i<installed.length();i++){JSONObject item=installed.optJSONObject(i);if(item!=null)extra.put(new JSONObject().put("name",item.optString("name")).put("count",item.optInt("count")).put("quarantined",RadarSourcePolicy.quarantined(item.optString("id"))));}bases.put("extra",extra);j.put("radarBases",bases);
      JSONArray neighbors=new JSONArray();try(java.io.InputStream in=a.getAssets().open("radar_neighbors_manifest.json")){java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1)bytes.write(buffer,0,n);JSONObject bundled=new JSONObject(bytes.toString("UTF-8")).optJSONObject("countries");if(bundled!=null)for(String code:new String[]{"BE","DE","ES","IT","LU"}){JSONObject pack=bundled.optJSONObject(code);if(pack!=null){String label=new java.util.Locale("",code).getDisplayCountry(Locale.FRANCE);JSONObject update=a.installedRadarCountry(a.mapAtlasPackId(code));neighbors.put(new JSONObject().put("code",code).put("name",label).put("count",pack.optInt("count")).put("snapshot",pack.optString("snapshot")).put("updateCount",update==null?0:update.optInt("count")).put("updated",update!=null));}}}catch(Exception ignored){}bases.put("neighbors",neighbors);
      j.put("markerSize",a.mapMarkerSizeLabel());
      JSONObject settings=new JSONObject();
      settings.put("gps",freshGps).put("background",BackgroundAlertService.running).put("autoReroute",a.automaticRerouteEnabled).put("contrast",a.highContrastMap).put("zoom",a.adaptiveZoom).put("ahead",a.lookAhead);
      settings.put("radar",a.getPreferences(0).getBoolean("Alertes de danger",true));
      settings.put("phoneMaps",a.getPreferences(0).getBoolean("phone_map_share",false));
      settings.put("phoneGuidance",a.getPreferences(0).getBoolean("phone_guidance_share",false));
      settings.put("overspeedVoice",a.getPreferences(0).getBoolean("overspeed_voice",true));
      settings.put("voice",a.getPreferences(0).getBoolean("Alertes vocales",true)).put("guidance",a.getPreferences(0).getBoolean("Guidage vocal",true));
      settings.put("night",a.nightMap).put("markers",a.radarMarkersVisible).put("heading",a.headingUp);
      j.put("settings",settings);
      a.referenceRadarItems.clear();
      boolean directional=a.getPreferences(0).getBoolean("forward_dangers",true);
      boolean bearingReady=freshGps&&a.reliableBearing;
      boolean routeFilter=bearingReady&&a.getPreferences(0).getBoolean("route_corridor_alerts",true)
        &&a.navigationPoints.size()>1&&a.lastAccuracy>0&&a.lastAccuracy<=30
        &&a.routeDistanceMeters(a.lastLat,a.lastLon)<Math.max(65,a.corridorWidthMeters()+10);
      RadarApproachFilter.Projection vehicle=routeFilter?RadarApproachFilter.onRoute(a.navigationPoints,a.lastLat,a.lastLon):null;
      j.put("radarDirectionStatus",directional&&!bearingReady?"Cap GPS fiable en attente · roule quelques mètres pour afficher les radars devant toi":routeFilter?"Radars probables sur le trajet devant toi · chaussée non garantie":directional?"Radars devant toi selon le cap GPS · route non confirmée":"Tous les radars proches · filtre de direction désactivé");
      if(freshGps&&Double.isFinite(a.lastLat)&&Double.isFinite(a.lastLon)){
        ArrayList<Integer> candidates=a.dangerSpatialIndex.query(a.lastLat,a.lastLon,50000);
        Collections.sort(candidates,(x,y)->Double.compare(
          a.geoMeters(a.lastLat,a.lastLon,a.dangers.get(x).lat,a.dangers.get(x).lon),
          a.geoMeters(a.lastLat,a.lastLon,a.dangers.get(y).lat,a.dangers.get(y).lon)));
        for(int index:candidates){
          if(index<0||index>=a.dangers.size())continue;
          MainActivity.Danger point=a.dangers.get(index);
          if(!DangerTypePolicy.isRadar(point.type))continue;
          if(directional){if(!bearingReady)continue;if(!routeFilter){float[] heading=new float[2];android.location.Location.distanceBetween(a.lastLat,a.lastLon,point.lat,point.lon,heading);
            if(!RadarApproachFilter.ahead(heading[0],heading[1],a.lastBearing,a.lastAccuracy))continue;}}
          if(routeFilter&&!RadarApproachFilter.onUpcomingRoute(vehicle,RadarApproachFilter.onRoute(a.navigationPoints,point.lat,point.lon),a.corridorWidthMeters()))continue;
          a.referenceRadarItems.add(point);
          if(a.referenceRadarItems.size()>=200)break;
        }
      }
      if("Radar".equals(a.selected)&&a.activeRadarUi!=null){a.referenceRadarItems.removeIf(p->p.lat==a.activeRadarUi.lat&&p.lon==a.activeRadarUi.lon);a.referenceRadarItems.add(0,a.activeRadarUi);if(a.referenceRadarItems.size()>200)a.referenceRadarItems.remove(a.referenceRadarItems.size()-1);}
      JSONArray nearby=new JSONArray();
      for(int i=0;i<a.referenceRadarItems.size();i++){
        MainActivity.Danger point=a.referenceRadarItems.get(i);String type=point.type==null?"Radar":point.type;
        double meters=a.geoMeters(a.lastLat,a.lastLon,point.lat,point.lon);
        JSONObject radar=new JSONObject();radar.put("index",i).put("type",type);
        radar.put("kind",radarKind(type));radar.put("historical",RadarSourcePolicy.historical(type));radar.put("meters",Double.isFinite(meters)?meters:-1);
        radar.put("distance",!Double.isFinite(meters)?"GPS en attente":meters>=1000?String.format(Locale.FRANCE,"%.1f km",meters/1000):String.format(Locale.FRANCE,"%.0f m",meters));
        radar.put("coords",String.format(Locale.FRANCE,"%.5f, %.5f",point.lat,point.lon));
        String limit="";java.util.regex.Matcher match=java.util.regex.Pattern.compile("(?<![0-9])([0-9]{2,3}) km/h").matcher(type);if(match.find())limit=match.group(1);
        if(point.sourceDirection!=null)radar.put("sourceDirection",point.sourceDirection);
        radar.put("sourceName",point.sourceName).put("sourceReliability",point.sourceReliability);
        radar.put("limit",limit);radar.put("place",routeFilter?"Probable sur ton trajet · à vérifier":"Devant toi selon le GPS · route à vérifier");nearby.put(radar);
      }
      j.put("radars",nearby).put("alertIndex",0);
    }catch(Exception error){android.util.Log.w("ROADGUARD","Reference UI data",error);}
    return j;
  }

  static String radarKind(String type){
    if(DangerTypePolicy.matchesFilter(type,"redlight"))return "redlight";
    if(DangerTypePolicy.matchesFilter(type,"works"))return "works";
    if(DangerTypePolicy.matchesFilter(type,"danger"))return "danger";
    if(DangerTypePolicy.matchesFilter(type,"mobile"))return "mobile";
    if(DangerTypePolicy.matchesFilter(type,"average"))return "average";
    if(DangerTypePolicy.matchesFilter(type,"crossing"))return "crossing";
    if(DangerTypePolicy.matchesFilter(type,"tunnel"))return "tunnel";
    return "fixed";
  }
  static final class Bridge {
    final MainActivity a;final WebView owner;
    Bridge(MainActivity activity,WebView view){a=activity;owner=view;}
    @JavascriptInterface public void action(String name,String value){
      if(name==null||name.length()>40||value!=null&&value.length()>500)return;
      a.runOnUiThread(()->{if(a.referenceWebView==owner&&LocalPagePolicy.referencePage(owner.getUrl())&&!a.isFinishing()&&!a.isDestroyed())handle(name,value==null?"":value);});
    }
    void handle(String name,String value){
      if(!a.accountConnected()&&!("communityLink".equals(name)||"communityWeb".equals(name)||"screen".equals(name)&&"Compte".equals(value)))return;
      switch(name){
        case "screen":
          if(!java.util.Arrays.asList("Accueil","Carte","Trajet","Alertes","Signaler","Hors ligne","Paramètres","Apparence","Radar","Favoris","Historique","Journal","Communauté","Compte","Bases radar","Gestion radars","Android Auto").contains(value))return;
          a.legacySection=false;a.selected=value;a.render();if("Communauté".equals(value)&&a.community!=null)a.community.loadOwn();break;
        case "spotify":SpotifyControls.show(a);break;
        case "driveAuto":DriveAutoUi.show(a);break;
        case "controlMemory":ControlMemoryUi.show(a);break;
        case "advanced":a.legacySection=true;a.legacySectionScreen=a.selected;a.render();break;
        case "destination":a.openInlineDestinationSearch();break;
        case "coordinates":a.askDestination();break;
        case "favoriteSave":a.saveDestinationFavorite();break;
        case "favoriteChoose":try{a.chooseDestinationFavorite(Integer.parseInt(value));}catch(NumberFormatException ignored){}break;
        case "favoriteDelete":try{int index=Integer.parseInt(value);if(index>=0&&index<a.destinationFavorites().length())new android.app.AlertDialog.Builder(a).setTitle("Supprimer ce favori ?").setNegativeButton("Annuler",null).setPositiveButton("Supprimer",(d,w)->{a.deleteDestinationFavorite(index);refreshNow(a);}).show();}catch(NumberFormatException ignored){}break;
        case "retryMapTiles":a.retryMapTiles();break;
        case "offlineMapDownloads":a.showOfflineMapDownloads();break;
        case "appUpdate":a.showAppUpdate();break;
        case "mapColors":a.showMapPackPicker();return;
        case "mapPalette":{
          String[] parts=value.split(":",-1);
          if(parts.length!=2||!MapAppearance.valid(parts[0])||!java.util.Arrays.asList("day","night").contains(parts[1]))return;
          a.selectMapPalette(parts[0],"night".equals(parts[1]));break;
        }
        case "mapPack":
          if(!MapAppearance.valid(value))return;
          a.getPreferences(0).edit().putString("map_appearance_pack",value).apply();a.applyMapTheme();refreshNow(a);break;
        case "routeRecalculate":if(a.hasDestination()&&!a.routeRequestPending){a.calculateRoadRoute();refreshNow(a);}break;
        case "destinationRemove":a.confirmRemoveDestination();break;
        case "routeClear":a.confirmStopGuidance();break;
        case "reportsExport":a.exportReports();break;
        case "reportsClear":new android.app.AlertDialog.Builder(a).setTitle("Effacer les signalements locaux ?").setMessage("L’historique et les marqueurs issus de ces signalements seront retirés. Les bases radar et les points ajoutés séparément sont conservés.").setNegativeButton("Annuler",null).setPositiveButton("Effacer",(d,w)->{a.getPreferences(0).edit().remove("report_history").remove("last_report").apply();try{JSONArray all=new JSONArray(a.getPreferences(0).getString("custom_radars","[]")),keep=new JSONArray();for(int i=0;i<all.length();i++){JSONObject item=all.optJSONObject(i);if(item!=null&&!"report".equals(item.optString("source")))keep.put(item);}a.getPreferences(0).edit().putString("custom_radars",keep.toString()).apply();a.loadDangers();a.loadCustomRadars();}catch(Exception ignored){}refreshNow(a);}).show();break;
        case "tripToggle":a.tripRecording=!a.tripRecording;if(a.tripRecording&&a.tripStart==0)a.tripStart=System.currentTimeMillis();a.previousTripLocation=null;a.getPreferences(0).edit().putLong("trip_start",a.tripStart).apply();refreshNow(a);break;
        case "tripExport":a.exportTripGpx();break;
        case "markerSize":a.mapMarkerSize=(a.mapMarkerSize+1)%3;a.getPreferences(0).edit().putInt("map_marker_size",a.mapMarkerSize).apply();refreshNow(a);break;
        case "routeOption":
          try{int i=Integer.parseInt(value);if(i>=0&&i<a.availableRoutes.size()){a.applyRouteOption(i,true);a.render();}}catch(NumberFormatException ignored){}break;
        case "routeStart":
          if(!a.hasDestination())a.openInlineDestinationSearch();
          else if(a.navigationPoints.size()>1){a.selected="Carte";a.render();}
          else if(!a.routeRequestPending){a.calculateRoadRoute();refreshNow(a);}
          break;
        case "radarOnMap":if(a.activeRadarUi!=null)a.focusRadarOnMap(a.activeRadarUi);break;
        case "radarSeen":if(a.activeRadarUi!=null)AlertMilestones.claim(a,a.activeRadarUi.lat,a.activeRadarUi.lon,a.activeRadarUi.type,8);a.selected="Carte";a.render();break;
        case "radarDetail":
          try{int i=Integer.parseInt(value);if(i>=0&&i<a.referenceRadarItems.size())a.showRadarDetails(a.referenceRadarItems.get(i));}catch(NumberFormatException ignored){}break;
        case "notificationSettings":a.showNotificationSettings();break;
        case "gpsSettings":a.showGpsSettings();break;
        case "radarFranceFull":a.getPreferences(0).edit().putBoolean(BundledRadarCatalog.HISTORICAL_PREFERENCE,true).apply();a.reloadCommunityDangers();refreshNow(a);break;
        case "radarUpdate":if(a.community!=null)a.community.updateRadarInventory();break;
        case "radarCountries":a.openSimpleRadarCountryPicker();break;
        case "radarSources":a.showBundledRadarSources();break;
        case "radarAdvanced":a.selected="Alertes";a.legacySection=true;a.legacySectionScreen=a.selected;a.render();break;
        case "radarHistorical":
          String historicalKey="FR".equals(value)?BundledRadarCatalog.HISTORICAL_PREFERENCE:"CH".equals(value)?BundledRadarCatalog.SWISS_HISTORICAL_PREFERENCE:null;
          if(historicalKey==null)return;boolean fallback="CH".equals(value);a.getPreferences(0).edit().putBoolean(historicalKey,!a.getPreferences(0).getBoolean(historicalKey,fallback)).apply();a.reloadCommunityDangers();refreshNow(a);break;
        case "communityLink":if(a.community!=null)a.community.link();break;
        case "communityAdmin":if(a.community!=null)a.community.manageAccess();break;
        case "communitySync":if(a.community!=null)a.community.refreshNow();break;
        case "communityWeb":if(a.community!=null)a.community.openWeb("/");break;
        case "communityDisconnect":if(a.community!=null)a.community.disconnect();break;
        case "communityPresence":if(a.community!=null)a.community.sharing();break;
        case "communityMembers":if(a.community!=null)a.community.showMembers();break;
        case "communityDelete":if(a.community!=null)a.community.remove(value);break;
        case "communityVote":if(a.community!=null){String[] parts=value.split(":",2);if(parts.length==2)a.community.vote(parts[0],parts[1]);}break;
        case "accent":if(a.community!=null)a.community.preference("accent",value);break;
        case "cursor":if(a.community!=null)a.community.preference("cursor",value);break;
        case "routeColor":if(a.community!=null)a.community.preference("route_color",value);break;
        case "menuPhoto":if(a.community!=null)a.community.menuPhoto();break;
        case "voiceChoose":a.chooseNativeVoice();break;
        case "voiceTune":a.tuneNativeVoice();break;
        case "report":
          if(!java.util.Arrays.asList("Radar fixe","Radar mobile","Contrôle routier","Feu rouge","Zone dangereuse","Accident","Véhicule arrêté","Bouchon","Obstacles","Travaux","Autre").contains(value))return;
          String type=value.equals("Feu rouge")?"Radar feu rouge":value.equals("Zone dangereuse")?"Danger":value.equals("Obstacles")?"Objet sur la route":value;
          if(a.community!=null&&a.community.paired){new AlertDialog.Builder(a).setTitle("Partager « "+type+" » ?").setMessage("Ce signalement et votre nom affiché seront transmis aux autres membres. Vérifiez que le point GPS correspond au lieu observé.").setNegativeButton("Annuler",null).setNeutralButton("Garder sur ce téléphone",(d,w)->a.confirmLocalReport(type)).setPositiveButton("Partager",(d,w)->a.community.publish(type)).show();}
          else new AlertDialog.Builder(a).setTitle("Comment signaler ?").setMessage("Connectez votre numéro pour partager avec les autres membres, ou gardez ce point sur ce téléphone.").setNegativeButton("Annuler",null).setNeutralButton("Garder sur ce téléphone",(d,w)->a.confirmLocalReport(type)).setPositiveButton("Créer mon compte",(d,w)->{if(a.community!=null)a.community.link();}).show();break;
        case "theme":
          if(!java.util.Arrays.asList("auto","day","night").contains(value))return;
          boolean automatic=value.equals("auto");
          boolean night=automatic?((a.getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES):value.equals("night");
          a.nightMap=night;a.getPreferences(0).edit().putBoolean("theme_auto",automatic).putBoolean("night_map",night).apply();if(a.community!=null)a.community.markPreferencesChanged();a.render();break;
        case "toggle":
          switch(value){
            case "gps":a.selected="Carte";a.render();return;
            case "background":if(BackgroundAlertService.running)a.stopService(new android.content.Intent(a,BackgroundAlertService.class));else a.startBackgroundAlerts();a.driveSearchHandler.postDelayed(()->refreshNow(a),500);return;
            case "autoReroute":a.automaticRerouteEnabled=!a.automaticRerouteEnabled;a.getPreferences(0).edit().putBoolean("auto_reroute",a.automaticRerouteEnabled).apply();refreshNow(a);return;
            case "contrast":a.highContrastMap=!a.highContrastMap;a.getPreferences(0).edit().putBoolean("high_contrast_map",a.highContrastMap).apply();refreshNow(a);return;
            case "zoom":a.adaptiveZoom=!a.adaptiveZoom;a.getPreferences(0).edit().putBoolean("adaptive_zoom",a.adaptiveZoom).apply();refreshNow(a);return;
            case "ahead":a.lookAhead=!a.lookAhead;a.getPreferences(0).edit().putBoolean("look_ahead",a.lookAhead).apply();refreshNow(a);return;
            case "radar":togglePreference("Alertes de danger",true);return;
            case "voice":togglePreference("Alertes vocales",true);return;
            case "overspeedVoice":togglePreference("overspeed_voice",true);return;
            case "phoneMaps":togglePreference("phone_map_share",false);a.getPreferences(0).edit().putLong("phone_map_epoch",System.currentTimeMillis()).apply();return;
            case "phoneGuidance":togglePreference("phone_guidance_share",false);a.phoneGuidanceSharing.update(a);return;
            case "guidance":togglePreference("Guidage vocal",true);return;
            case "night":a.nightMap=!a.nightMap;a.getPreferences(0).edit().putBoolean("theme_auto",false).putBoolean("night_map",a.nightMap).apply();if(a.community!=null)a.community.markPreferencesChanged();a.render();return;
            case "markers":a.radarMarkersVisible=!a.radarMarkersVisible;a.getPreferences(0).edit().putBoolean("radar_markers_visible",a.radarMarkersVisible).apply();a.render();return;
            case "heading":a.headingUp=!a.headingUp;a.getPreferences(0).edit().putBoolean("heading_up",a.headingUp).apply();a.render();return;
          }break;
        case "unavailable":Toast.makeText(a,value+" indisponible dans le moteur actuel",Toast.LENGTH_SHORT).show();break;
      }
    }
    void togglePreference(String key,boolean fallback){boolean before=a.getPreferences(0).getBoolean(key,fallback);a.getPreferences(0).edit().putBoolean(key,!before).apply();if("Alertes vocales".equals(key)&&a.community!=null)a.community.markPreferencesChanged();a.render();}
  }
}
