package fr.roadguard.app;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.PowerManager;
/** Read-only Android state, collected on demand for the final device checks. */
final class DeviceDiagnostics {
  private DeviceDiagnostics(){}
  private static String yes(boolean value){return value?"oui":"non";}
  static String describe(Context context){
    StringBuilder text=new StringBuilder("Android ").append(Build.VERSION.RELEASE).append(" · ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL);
    text.append("\nLocalisation précise autorisée : ").append(yes(context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED));
    try{LocationManager location=(LocationManager)context.getSystemService(Context.LOCATION_SERVICE);text.append("\nGPS activé : ").append(yes(location!=null&&location.isProviderEnabled(LocationManager.GPS_PROVIDER)));}catch(RuntimeException unavailable){text.append("\nGPS activé : état indisponible");}
    text.append("\nNotifications autorisées : ").append(yes(Build.VERSION.SDK_INT<33||context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED));
    text.append("\nAffichage superposé autorisé : ").append(yes(android.provider.Settings.canDrawOverlays(context)));
    try{PowerManager power=(PowerManager)context.getSystemService(Context.POWER_SERVICE);if(power!=null){text.append("\nÉconomie de batterie active : ").append(yes(power.isPowerSaveMode()));text.append("\nExemption d’optimisation batterie : ").append(yes(power.isIgnoringBatteryOptimizations(context.getPackageName())));}}catch(RuntimeException unavailable){text.append("\nBatterie : état indisponible");}
    return text.toString();
  }
}
