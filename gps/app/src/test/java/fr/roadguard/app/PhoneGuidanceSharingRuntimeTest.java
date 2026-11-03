package fr.roadguard.app;
import android.app.*;import android.content.*;import android.os.*;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.*;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk={26,36},application=Application.class)
public class PhoneGuidanceSharingRuntimeTest {
 public static class Host extends MainActivity { @Override public void onCreate(Bundle b){} @Override boolean accountConnected(){return true;} @Override String routeStatusText(){return "À droite dans 100 m";} @Override public android.content.SharedPreferences getPreferences(int m){return getSharedPreferences("MainActivity",m);}}
 Host host;NotificationManager nm;
 @Before public void setup(){host=Robolectric.buildActivity(Host.class).create().get();host.getPreferences(0).edit().clear().commit();nm=(NotificationManager)host.getSystemService(Context.NOTIFICATION_SERVICE);nm.cancelAll();host.navigationPoints.add(new double[]{46,6});host.navigationPoints.add(new double[]{46.1,6});}
 @Test public void sharingIsOffByDefault(){new PhoneGuidanceSharing().update(host);assertEquals(0,nm.getActiveNotifications().length);}
 @Test public void optedInGuidanceContainsOnlyMarkedNavigationAndIsCancelledWhenStopped(){host.getPreferences(0).edit().putBoolean("phone_guidance_share",true).commit();PhoneGuidanceSharing share=new PhoneGuidanceSharing();share.update(host);assertEquals(1,nm.getActiveNotifications().length);Notification n=nm.getActiveNotifications()[0].getNotification();assertEquals(PhoneGuidanceSharing.CHANNEL,n.getChannelId());assertTrue(n.extras.getBoolean("roadguard_guidance"));assertEquals("À droite dans 100 m",n.extras.getCharSequence(Notification.EXTRA_TEXT).toString());assertEquals(Notification.VISIBILITY_PRIVATE,n.visibility);host.navigationPoints.clear();share.update(host);assertEquals(0,nm.getActiveNotifications().length);}
}
