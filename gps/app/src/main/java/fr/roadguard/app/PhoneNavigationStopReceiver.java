package fr.roadguard.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import java.lang.ref.WeakReference;

/** Only the immutable PendingIntent in GPS's opt-in navigation notification can invoke this externally. */
public final class PhoneNavigationStopReceiver extends BroadcastReceiver {
  private static WeakReference<MainActivity> activity = new WeakReference<>(null);
  static void attach(MainActivity owner) { activity = new WeakReference<>(owner); }
  static void detach(MainActivity owner) { if (activity.get() == owner) activity.clear(); }

  static MainActivity owner() {MainActivity a=activity.get();return a!=null&&!a.isDestroyed()?a:null;}
  static android.location.Location currentFix() {MainActivity a=owner();return a==null||a.previousAcceptedGps==null?null:new android.location.Location(a.previousAcceptedGps);}
  static boolean foregroundGuidance() {MainActivity a=owner();return a!=null&&a.activityResumed&&a.accountConnected();}
  static void routeCommitted() {MainActivity a=owner();if(a!=null){a.cancelRouteCalculation();a.pendingNavigationQuery=null;a.navigationIntentGeneration++;a.syncCarRoute();if(a.activityResumed)a.render();}}
  @Override public void onReceive(Context context, Intent intent) {
    if (intent == null || !"fr.roadguard.app.STOP_SHARED_NAVIGATION".equals(intent.getAction())) return;
    stop(context, intent.getStringExtra("route_id"));
  }

  static boolean matches(Context context, String expected) {
    SharedPreferences prefs = context.getSharedPreferences("MainActivity", 0);
    String snapshot = prefs.getString("cached_navigation", "");
    return expected != null && expected.length() <= 128 && !snapshot.isEmpty() &&
      prefs.getBoolean("phone_guidance_share", false) && PhoneRouteGeometry.identity(snapshot,
        prefs.getString("destination", ""), prefs.getString("destination_label", "Destination")).equals(expected);
  }

  /** Called on the main thread; rechecks identity after any queued recalculation. */
  static void stop(Context context, String expected) {
    if (!matches(context, expected)) return;
    SharedPreferences prefs = context.getSharedPreferences("MainActivity", 0);
    // A stale action must never stop a newly calculated route. Keep destination and all local map packs.
    prefs.edit().remove("cached_navigation").putLong("car_route_revision",
        prefs.getLong("car_route_revision", 0) + 1).apply();
    MainActivity owner = activity.get();
    if (owner != null && !owner.isDestroyed()) {
      owner.clearNavigation();
      if (owner.activityResumed) owner.render();
    }
    new PhoneGuidanceSharing().stop(context);
  }
}
