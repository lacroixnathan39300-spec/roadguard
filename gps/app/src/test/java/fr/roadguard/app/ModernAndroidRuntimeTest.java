package fr.roadguard.app;
import android.Manifest;
import android.app.Application;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Insets;
import android.view.View;
import android.view.WindowInsets;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={36})
@LooperMode(LooperMode.Mode.PAUSED)
public class ModernAndroidRuntimeTest {
  public static class ModernActivity extends PhoneRuntimeTest.OfflineActivity {
    int trackingRequests=0,floatingRequests=0;
    @Override void startBackgroundAlerts(){trackingRequests++;}
    @Override void startFloatingRadar(){floatingRequests++;}
  }
  ActivityController<ModernActivity> controller;ModernActivity activity;
  @Before public void setup()throws Exception{
    Shadows.shadowOf((Application)RuntimeEnvironment.getApplication()).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION);
    controller=Robolectric.buildActivity(ModernActivity.class).setup();activity=controller.get();activity.initialLoad.get(15,java.util.concurrent.TimeUnit.SECONDS);Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
  }
  @After public void close(){if(controller!=null)controller.pause().stop().destroy();}
  @Test public void systemBarsCutoutAndKeyboardNeverCoverContent(){
    View content=activity.findViewById(android.R.id.content);
    WindowInsets normal=new WindowInsets.Builder().setInsets(WindowInsets.Type.systemBars(),Insets.of(0,28,0,24)).setVisible(WindowInsets.Type.systemBars(),true).setInsets(WindowInsets.Type.displayCutout(),Insets.of(12,40,0,0)).setVisible(WindowInsets.Type.displayCutout(),true).build();
    content.dispatchApplyWindowInsets(normal);assertEquals(12,content.getPaddingLeft());assertEquals(40,content.getPaddingTop());assertEquals(24,content.getPaddingBottom());
    WindowInsets keyboard=new WindowInsets.Builder(normal).setInsets(WindowInsets.Type.ime(),Insets.of(0,0,0,310)).setVisible(WindowInsets.Type.ime(),true).build();
    content.dispatchApplyWindowInsets(keyboard);assertEquals(310,content.getPaddingBottom());
    content.dispatchApplyWindowInsets(normal);assertEquals(24,content.getPaddingBottom());assertEquals(40,content.getPaddingTop());
  }
  @Test public void notificationRefusalContinuesRequestedTrackingWithoutPromptLoop(){
    Shadows.shadowOf((Application)activity.getApplication()).denyPermissions(Manifest.permission.POST_NOTIFICATIONS);
    assertTrue(activity.requestTrackingNotifications(1));
    activity.onRequestPermissionsResult(32,new String[]{Manifest.permission.POST_NOTIFICATIONS},new int[]{PackageManager.PERMISSION_DENIED});
    assertEquals(1,activity.trackingRequests);assertEquals(0,activity.pendingNotificationAction);assertFalse(activity.requestTrackingNotifications(1));
  }
  @Test public void notificationApprovalResumesFloatingActivation(){
    Shadows.shadowOf((Application)activity.getApplication()).denyPermissions(Manifest.permission.POST_NOTIFICATIONS);
    assertTrue(activity.requestTrackingNotifications(2));
    activity.onRequestPermissionsResult(32,new String[]{Manifest.permission.POST_NOTIFICATIONS},new int[]{PackageManager.PERMISSION_GRANTED});
    assertEquals(1,activity.floatingRequests);assertEquals(0,activity.pendingNotificationAction);
  }
  @Test public void backReturnsFromSecondaryPageAndThenExitsNavigationStack(){
    activity.selected="Paramètres";activity.render();assertTrue(activity.handleRoadguardBack());assertEquals("Carte",activity.selected);assertFalse(activity.handleRoadguardBack());
  }
  @Test public void locationServicesStopCleanlyWithoutPrecisePermission(){
    Shadows.shadowOf((Application)activity.getApplication()).denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION);
    org.robolectric.android.controller.ServiceController<BackgroundAlertService> b=Robolectric.buildService(BackgroundAlertService.class).create();
    assertFalse(BackgroundAlertService.running);assertTrue(Shadows.shadowOf(b.get()).isStoppedBySelf());b.destroy();
    org.robolectric.android.controller.ServiceController<CarLocationService> c=Robolectric.buildService(CarLocationService.class).create();
    c.get().onStartCommand(new Intent(),0,1);assertTrue(Shadows.shadowOf(c.get()).isStoppedBySelf());c.destroy();
  }
}
