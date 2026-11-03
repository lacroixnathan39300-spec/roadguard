package fr.roadguard.app;
import android.app.*;
import android.content.Intent;
import android.os.IBinder;
/** Keeps location available only for the active car session. Never restarts itself. */
public final class CarLocationService extends Service {
  @Override public int onStartCommand(Intent intent,int flags,int id){
    if(checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)!=android.content.pm.PackageManager.PERMISSION_GRANTED){stopSelf();return START_NOT_STICKY;}
    NotificationManager manager=getSystemService(NotificationManager.class);
    manager.createNotificationChannel(new NotificationChannel("roadguard_car","Navigation Android Auto",NotificationManager.IMPORTANCE_LOW));
    PendingIntent open=PendingIntent.getActivity(this,179,new Intent(this,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    Notification notice=new Notification.Builder(this,"roadguard_car").setSmallIcon(android.R.drawable.ic_menu_mylocation).setContentTitle("ROADGUARD · Android Auto").setContentText("GPS actif pendant la session voiture").setContentIntent(open).setOngoing(true).setCategory(android.os.Build.VERSION.SDK_INT>=28?Notification.CATEGORY_NAVIGATION:Notification.CATEGORY_SERVICE).build();
    try{if(android.os.Build.VERSION.SDK_INT>=29)startForeground(179,notice,android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);else startForeground(179,notice);}catch(RuntimeException denied){android.util.Log.w("ROADGUARD","Session GPS voiture refusée par Android",denied);stopSelf();}
    return START_NOT_STICKY;
  }
  @Override public IBinder onBind(Intent i){return null;}
}
