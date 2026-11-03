package fr.roadguard.app;
import android.app.Application;import android.content.*;import android.os.Bundle;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.*;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk={27,36},application=Application.class)
public class RadarDownloadEntryTest {
 public static class Host extends MainActivity {boolean fail;Intent opened;@Override public void onCreate(Bundle b){} @Override public void startActivity(Intent i){if(fail)throw new ActivityNotFoundException("fixture");opened=i;}}
 @Test public void correctScreenIsLaunched(){Host a=Robolectric.buildActivity(Host.class).create().get();assertTrue(RadarDownloadEntry.open(a));assertEquals(RadarDownloadActivity.class.getName(),a.opened.getComponent().getClassName());}
 @Test public void missingScreenDoesNotCloseMapOrEraseUserData(){Host a=Robolectric.buildActivity(Host.class).create().get();a.getPreferences(0).edit().putString("danger_csv","46;6;Radar fixe").commit();a.fail=true;assertFalse(RadarDownloadEntry.open(a));assertFalse(a.isFinishing());assertEquals("46;6;Radar fixe",a.getPreferences(0).getString("danger_csv",""));}
}
