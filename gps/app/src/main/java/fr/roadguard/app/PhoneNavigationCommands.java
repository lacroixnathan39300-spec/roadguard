package fr.roadguard.app;

import android.content.*;
import android.location.Location;
import android.os.*;
import fr.roadguard.ipc.INavigationListener;
import org.json.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;

/** Authenticated bounded commands. No activity launch, extra location subscription or hidden FGS start. */
class PhoneNavigationCommands {
  final Context context; final SharedPreferences prefs;
  final Handler main=new Handler(Looper.getMainLooper());
  final ThreadPoolExecutor io=new ThreadPoolExecutor(1,1,0,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(2));
  final Set<String> cancelled=Collections.synchronizedSet(new LinkedHashSet<>()),seen=new LinkedHashSet<>();
  volatile boolean closed; volatile int consentGeneration;
  void revoke(){consentGeneration++;}
  interface Places {JSONArray find(String query)throws Exception;}
  interface Calculate {JSONObject route(Location fix,double lat,double lon)throws Exception;}
  Places places=q->new JSONObject(CarRouteData.get(CommunitySync.ORIGIN+"/api/v1/places?q="+java.net.URLEncoder.encode(q,"UTF-8"),200000)).getJSONArray("places");
  Calculate calculate=CarRouteData::calculate;
  PhoneNavigationCommands(Context c){context=c;prefs=c.getSharedPreferences("MainActivity",0);}
  void cancel(String scope){synchronized(cancelled){cancelled.add(scope);while(cancelled.size()>64)cancelled.remove(cancelled.iterator().next());}}
  void close(){closed=true;io.shutdownNow();main.removeCallbacksAndMessages(null);}
  boolean permitted(){return prefs.getBoolean("phone_guidance_share",false)&&context.getSharedPreferences("roadguard_community",0).getBoolean("approved",false);}
  boolean current(String scope,long until,BooleanSupplier trusted){return !closed&&!cancelled.contains(scope)&&SystemClock.elapsedRealtime()<until&&permitted()&&trusted.getAsBoolean();}
  static boolean identifier(String s){return s!=null&&s.matches("[A-Za-z0-9-]{1,64}");}
  static JSONObject result(String id,String kind,String status,JSONArray rows,String route)throws Exception {
    return new JSONObject().put("id",id).put("kind",kind).put("status",status).put("places",rows==null?new JSONArray():rows).put("routeId",route==null?"":route);
  }
  void deliver(INavigationListener cb,JSONObject value){try{cb.onResult(value.toString());}catch(RemoteException ignored){}}
  void answer(INavigationListener cb,String id,String kind,String status){try{deliver(cb,result(id,kind,status,null,null));}catch(Exception ignored){}}
  static JSONArray validPlaces(JSONArray input,String fallback){
    JSONArray rows=new JSONArray();Set<String> positions=new HashSet<>();
    for(int i=0;i<Math.min(input.length(),128)&&rows.length()<30;i++)try{
      JSONObject p=input.getJSONObject(i);double a=p.getDouble("lat"),b=p.has("lng")?p.getDouble("lng"):p.getDouble("lon");if(!RouteRequestPolicy.coordinate(a,b))continue;
      String name=p.optString("name",fallback).trim();if(name.isEmpty())name=fallback;
      String key=String.format(Locale.US,"%.6f,%.6f",a,b);if(!positions.add(key))continue;
      rows.put(new JSONObject().put("lat",a).put("lng",b).put("name",name.substring(0,Math.min(100,name.length()))));
    }catch(Exception invalid){}return rows;
  }
  Location fix(){Location a=PhoneNavigationStopReceiver.currentFix(),b=BackgroundAlertService.navigationFix();
    if(a==null||b!=null&&b.getElapsedRealtimeNanos()>a.getElapsedRealtimeNanos())a=b;
    return AlertPresentation.fresh(a,System.currentTimeMillis(),SystemClock.elapsedRealtimeNanos())?a:null;}
  boolean guidanceAvailable(){return PhoneNavigationStopReceiver.foregroundGuidance()||BackgroundAlertService.running;}
  void submit(String raw,INavigationListener cb,BooleanSupplier trusted){
    String id="",kind="";
    try{
      JSONObject request=new JSONObject(raw);id=request.getString("id");kind=request.getString("kind");final String scope=request.getString("scope");
      if(!identifier(id)||!identifier(scope)||!(kind.equals("search")||kind.equals("favorites")||kind.equals("start")))throw new IllegalArgumentException();
      final int epoch=consentGeneration;final BooleanSupplier authorized=()->trusted.getAsBoolean()&&epoch==consentGeneration;
      final String requestId=id,action=kind;final long until=SystemClock.elapsedRealtime()+25000;
      if(!current(scope,until,authorized)){answer(cb,id,kind,"unavailable");return;}
      String key=scope+":"+id;if(!seen.add(key)){answer(cb,id,kind,"duplicate");return;}while(seen.size()>64)seen.remove(seen.iterator().next());
      if(kind.equals("favorites")){deliver(cb,result(id,kind,"ok",validPlaces(new JSONArray(prefs.getString("destination_favorites","[]")),"Favori"),null));return;}
      if(kind.equals("search")){
        final String query=request.getString("query").trim();if(query.length()<2||query.length()>160)throw new IllegalArgumentException();
        double[] point=NavigationIntentInput.coordinates(query);
        if(point!=null){JSONArray rows=new JSONArray().put(new JSONObject().put("lat",point[0]).put("lng",point[1]).put("name",query.substring(0,Math.min(100,query.length()))));deliver(cb,result(id,kind,"ok",rows,null));return;}
        if("offline".equals(MapConnectionPolicy.normalize(prefs.getString("map_connection_mode","auto")))){answer(cb,id,kind,"offline");return;}
        io.execute(()->{JSONArray found=null;try{if(current(scope,until,authorized)&&!"offline".equals(MapConnectionPolicy.normalize(prefs.getString("map_connection_mode","auto"))))found=validPlaces(places.find(query),query);}catch(Exception failed){}final JSONArray rows=found;
          main.post(()->{if(current(scope,until,authorized))try{deliver(cb,result(requestId,action,rows==null?"network_error":"ok",rows,null));}catch(Exception ignored){}});
        });return;
      }
      final double lat=request.getDouble("lat"),lon=request.getDouble("lng");final String label=request.getString("label").trim();
      if(!RouteRequestPolicy.coordinate(lat,lon)||label.isEmpty()||label.length()>100)throw new IllegalArgumentException();
      if("offline".equals(MapConnectionPolicy.normalize(prefs.getString("map_connection_mode","auto")))){answer(cb,id,kind,"offline");return;}
      if(!guidanceAvailable()){answer(cb,id,kind,"guidance_unavailable");return;}
      Location from=fix();if(from==null){answer(cb,id,kind,"gps_unavailable");return;}
      final String previous=prefs.getString("cached_navigation",""),target=prefs.getString("destination","");final long revision=prefs.getLong("car_route_revision",0);
      io.execute(()->{JSONObject route=null;try{if(current(scope,until,authorized)&&!"offline".equals(MapConnectionPolicy.normalize(prefs.getString("map_connection_mode","auto"))))route=calculate.route(from,lat,lon);}catch(Exception failed){}final JSONObject built=route;
        main.post(()->{
          if(!current(scope,until,authorized))return;
          if(built==null){answer(cb,requestId,action,"network_error");return;}
          if(!guidanceAvailable()||fix()==null){answer(cb,requestId,action,"guidance_unavailable");return;}
          if(!previous.equals(prefs.getString("cached_navigation",""))||!target.equals(prefs.getString("destination",""))||revision!=prefs.getLong("car_route_revision",0)){answer(cb,requestId,action,"route_changed");return;}
          try{
            // Never discard the old route on an unsuccessful calculation.
            String dest=String.format(Locale.US,"%.7f,%.7f",lat,lon),saved=built.toString();
            if(!prefs.edit().putString("destination",dest).putString("destination_label",label).putString("cached_navigation",saved).putLong("car_route_revision",revision+1).commit()){answer(cb,requestId,action,"storage_error");return;}
            PhoneNavigationStopReceiver.routeCommitted();new PhoneGuidanceSharing().publish(context,"Trajet calculé · attente des prochaines consignes",true);
            deliver(cb,result(requestId,action,"started",null,PhoneRouteGeometry.identity(saved,dest,label)));
          }catch(Exception failed){answer(cb,requestId,action,"failed");}
        });
      });
    }catch(RejectedExecutionException busy){answer(cb,id,kind,"busy");}
     catch(Exception invalid){answer(cb,id,kind,"invalid");}
  }
}
