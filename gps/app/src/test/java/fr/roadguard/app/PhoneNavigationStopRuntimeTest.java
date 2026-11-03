package fr.roadguard.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE)
public class PhoneNavigationStopRuntimeTest {
  private Context context;
  private SharedPreferences prefs;
  private final String route = "{\"lat\":46.8,\"lon\":5.7,\"points\":[[46.7,5.6],[46.8,5.7]]}";
  @Before public void prepare() {
    context = RuntimeEnvironment.getApplication();
    prefs = context.getSharedPreferences("MainActivity", 0);
    prefs.edit().clear().putBoolean("phone_guidance_share", true)
      .putString("cached_navigation", route).putString("destination", "46.8,5.7")
      .putString("destination_label", "Maison").putString("installed_map", "France")
      .putLong("car_route_revision", 7).commit();
  }
  private Intent stop(String snapshot) {
    return new Intent("fr.roadguard.app.STOP_SHARED_NAVIGATION").putExtra("route_id",
      PhoneRouteGeometry.identity(snapshot, "46.8,5.7", "Maison"));
  }
  @Test public void validStopClearsRouteButPreservesDestinationAndMap() {
    new PhoneNavigationStopReceiver().onReceive(context, stop(route));
    assertFalse(prefs.contains("cached_navigation"));
    assertEquals(8, prefs.getLong("car_route_revision", 0));
    assertEquals("46.8,5.7", prefs.getString("destination", ""));
    assertEquals("France", prefs.getString("installed_map", ""));
  }
  @Test public void staleActionCannotStopRecalculatedRoute() {
    Intent stale = stop(route);
    prefs.edit().putString("cached_navigation", route + " ").commit();
    new PhoneNavigationStopReceiver().onReceive(context, stale);
    assertEquals(route + " ", prefs.getString("cached_navigation", ""));
    assertEquals(7, prefs.getLong("car_route_revision", 0));
  }
  @Test public void disabledSharingRejectsDelegatedStop() {
    prefs.edit().putBoolean("phone_guidance_share", false).commit();
    new PhoneNavigationStopReceiver().onReceive(context, stop(route));
    assertEquals(route, prefs.getString("cached_navigation", ""));
  }
  @Test public void missingOrWrongActionCannotStopRoute() {
    PhoneNavigationStopReceiver receiver = new PhoneNavigationStopReceiver();
    receiver.onReceive(context, null);
    receiver.onReceive(context, new Intent("other.action"));
    receiver.onReceive(context, new Intent("fr.roadguard.app.STOP_SHARED_NAVIGATION"));
    assertEquals(route, prefs.getString("cached_navigation", ""));
  }
}
