package fr.roadguard.app;
import android.app.*;import android.os.*;import android.content.*;
/** Direct opt-in IPC plus legacy navigation notification. */
final class PhoneGuidanceSharing {
 static final String CHANNEL="roadguard_phone_guidance"; static final int ID=8421;
 private long lastAt=-10000;
 private String previousSnapshot,previousTarget,previousLabel;
 private org.json.JSONObject previousRoute;
 void update(MainActivity a){publish(a,a.routeStatusText(),a.accountConnected()&&a.navigationPoints.size()>=2);}
 void publish(Context a,String text,boolean active){
  NotificationManager nm=(NotificationManager)a.getSystemService(Context.NOTIFICATION_SERVICE);
  if(!active||!a.getSharedPreferences("MainActivity",0).getBoolean("phone_guidance_share",false)){stop(a);return;}
  org.json.JSONObject route=routeSnapshot(a);
  PhoneGuidanceBridgeService.publish(a,text,route);
  long now=SystemClock.elapsedRealtime();if(now-lastAt<5000)return;
  if(text.length()>1000)text=text.substring(0,1000);
  nm.createNotificationChannel(new NotificationChannel(CHANNEL,"Guidage partagé avec Compagnon",NotificationManager.IMPORTANCE_LOW));
  Bundle extras=new Bundle();extras.putBoolean("roadguard_guidance",true);extras.putLong("roadguard_guidance_at",System.currentTimeMillis());
  if(route!=null)extras.putString("roadguard_route_v1",route.toString());
  Intent open=new Intent(a,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
  Notification.Builder builder=new Notification.Builder(a,CHANNEL).setSmallIcon(android.R.drawable.ic_menu_directions)
   .setContentTitle("ROADGUARD · Guidage").setContentText(text).setStyle(new Notification.BigTextStyle().bigText(text))
   .setCategory("navigation").setVisibility(Notification.VISIBILITY_PRIVATE).setOngoing(true).setOnlyAlertOnce(true)
   .setContentIntent(PendingIntent.getActivity(a,ID,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE)).addExtras(extras);
  if(route!=null){
   String id=route.optString("routeId");
   Intent stop=new Intent(a,PhoneNavigationStopReceiver.class).setAction("fr.roadguard.app.STOP_SHARED_NAVIGATION")
    .setData(android.net.Uri.parse("roadguard://stop/"+id)).putExtra("route_id",id);
   PendingIntent action=PendingIntent.getBroadcast(a,ID,stop,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
   Bundle marker=new Bundle();marker.putBoolean("roadguard_stop_v1",true);
   builder.addAction(new Notification.Action.Builder(android.R.drawable.ic_media_pause,"Arrêter le trajet",action).addExtras(marker).build());
  }
  Notification n=builder.build();
  try{nm.notify(ID,n);lastAt=now;}catch(SecurityException ignored){}
 }
 void stop(Context a){PhoneGuidanceBridgeService.publish(a,"",null);((NotificationManager)a.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(ID);lastAt=-10000;}
 /** Read the saved route, shared by foreground GPS and background guidance. Never expose the full cache. */
 org.json.JSONObject routeSnapshot(Context context){
  try{
   android.content.SharedPreferences prefs=context.getSharedPreferences("MainActivity",0);
   String raw=prefs.getString("cached_navigation","");
   if(raw.isEmpty()||raw.length()>6000000)return null;
   String target=prefs.getString("destination",""),name=prefs.getString("destination_label","Destination");
   if(raw.equals(previousSnapshot)&&target.equals(previousTarget)&&name.equals(previousLabel))return previousRoute;
   previousSnapshot=raw;previousTarget=target;previousLabel=name;previousRoute=null;
   org.json.JSONObject saved=new org.json.JSONObject(raw);String[] destination=target.split(",");
   if(destination.length!=2)return null;
   double targetLat=Double.parseDouble(destination[0]),targetLon=Double.parseDouble(destination[1]);
   double savedLat=saved.getDouble("lat"),savedLon=saved.getDouble("lon");
   if(!RouteRequestPolicy.coordinate(targetLat,targetLon)||!RouteRequestPolicy.coordinate(savedLat,savedLon)||
      Math.abs(savedLat-targetLat)>0.000001||Math.abs(savedLon-targetLon)>0.000001)return null;
   org.json.JSONArray points=saved.getJSONArray("points");
   if(points.length()<2||points.length()>PhoneRouteGeometry.MAX_INPUT_POINTS)return null;
   java.util.List<double[]> geometry=new java.util.ArrayList<>(points.length());
   for(int i=0;i<points.length();i++){org.json.JSONArray p=points.getJSONArray(i);geometry.add(new double[]{p.getDouble(0),p.getDouble(1)});}
   org.json.JSONArray coordinates=new org.json.JSONArray();
   for(double[] p:PhoneRouteGeometry.bounded(geometry))coordinates.put(new org.json.JSONObject().put("lat",p[0]).put("lng",p[1]));
   org.json.JSONObject result=new org.json.JSONObject().put("routeId",PhoneRouteGeometry.identity(raw,target,name)).put("revision",0)
    .put("coordinates",coordinates).put("destinationLabel",name.substring(0,Math.min(256,name.length()))).put("active",true);
   previousRoute=result.toString().length()<=160000?result:null;
   return previousRoute;
  }catch(Exception invalid){return null;}
 }
}
