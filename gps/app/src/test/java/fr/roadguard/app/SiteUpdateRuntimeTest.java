package fr.roadguard.app;

import android.app.*;
import android.content.*;
import android.os.*;
import android.provider.Settings;
import android.view.View;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowToast;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={26,36},application=Application.class)
public class SiteUpdateRuntimeTest {
    public static class Host extends MainActivity {
        boolean missing,securityOnly;
        final ArrayList<Intent> attempts=new ArrayList<>();
        @Override public void onCreate(Bundle b){}
        @Override public void startActivity(Intent i){
            attempts.add(new Intent(i));
            if(missing||securityOnly&&!Settings.ACTION_SECURITY_SETTINGS.equals(i.getAction()))throw new ActivityNotFoundException("fixture");
        }
    }
    Host host(){return Robolectric.buildActivity(Host.class).create().get();}
    @Test public void vendorWithoutPerAppInstallSettingsGetsTheSecuritySettingsFallback(){
        Host a=host();a.securityOnly=true;assertTrue(SiteAppUpdate.openInstallPermission(a));
        assertEquals(2,a.attempts.size());assertEquals(Settings.ACTION_SECURITY_SETTINGS,a.attempts.get(1).getAction());
    }
    @Test public void missingSettingsNeverCloseTheLauncher(){
        Host a=host();a.missing=true;assertFalse(SiteAppUpdate.openInstallPermission(a));
        assertFalse(a.isFinishing());assertNotNull(ShadowToast.getTextOfLatestToast());
    }
    @Test public void normalSettingsKeepTheCurrentApplicationPackage(){
        Host a=host();assertTrue(SiteAppUpdate.openInstallPermission(a));assertEquals(1,a.attempts.size());
        assertEquals(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,a.attempts.get(0).getAction());
        assertEquals("package:"+a.getPackageName(),a.attempts.get(0).getDataString());
    }
    @Test public void downloadHasAVisibleWorkingCancelButton(){
        Host a=host();AtomicBoolean cancel=new AtomicBoolean();ProgressDialog dialog=SiteAppUpdate.downloadProgress(a,cancel);
        assertNotNull(dialog.getButton(DialogInterface.BUTTON_NEGATIVE));
        assertEquals(View.VISIBLE,dialog.getButton(DialogInterface.BUTTON_NEGATIVE).getVisibility());
        assertFalse(cancel.get());dialog.getButton(DialogInterface.BUTTON_NEGATIVE).performClick();
        Shadows.shadowOf(Looper.getMainLooper()).idle();assertTrue(cancel.get());dialog.dismiss();
    }
}
