package fr.roadguard.app;

import android.app.Application;
import android.content.*;
import android.os.Bundle;
import java.util.ArrayList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowToast;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={26,36},application=Application.class)
public class DocumentEntryRuntimeTest {
    public static class Host extends MainActivity {
        boolean missing, denied, contentOnly;
        final ArrayList<Intent> attempts=new ArrayList<>();
        int requestCode=-1;
        @Override public void onCreate(Bundle state) {}
        @Override public void startActivityForResult(Intent intent,int request) {
            attempts.add(new Intent(intent)); requestCode=request;
            if(denied)throw new SecurityException("fixture");
            if(missing || contentOnly&&!Intent.ACTION_GET_CONTENT.equals(intent.getAction()))throw new ActivityNotFoundException("fixture");
        }
    }
    Host host(){return Robolectric.buildActivity(Host.class).create().get();}
    @Test public void csvFallsBackToTheAvailableReadPickerWithoutChangingTheResultContract(){
        Host a=host();a.contentOnly=true;a.openCsv();
        assertEquals(2,a.attempts.size());assertEquals(44,a.requestCode);
        Intent used=a.attempts.get(1);assertEquals(Intent.ACTION_GET_CONTENT,used.getAction());
        assertEquals("*/*",used.getType());assertTrue(used.hasCategory(Intent.CATEGORY_OPENABLE));
    }
    @Test public void absentReadPickersDoNotCloseTheLauncherOrErasePersonalRadars(){
        Host a=host();a.missing=true;
        a.getPreferences(0).edit().putString("danger_csv","46;6;Radar fixe").commit();
        for(Runnable open:new Runnable[]{a::openCsv,a::openGpx,a::openRoads,a::importBackup,a::openTileZip,a::openExistingPmtiles,a::chooseRadarCountryCatalogFile,a::chooseLufopSwissCsvZip,a::choosePoiWarnerSwissZip})open.run();
        assertFalse(a.isFinishing());assertEquals("46;6;Radar fixe",a.getPreferences(0).getString("danger_csv",""));
        assertNotNull(ShadowToast.getTextOfLatestToast());
    }
    @Test public void absentWritePickersNeverFallBackToAReadOperation(){
        Host a=host();a.missing=true;a.exportBackup();a.exportGpx();a.exportTripGpx();
        a.getPreferences(0).edit().putString("report_history","[]").commit();a.exportReports();
        assertFalse(a.isFinishing());assertEquals(4,a.attempts.size());
        for(Intent attempt:a.attempts)assertEquals(Intent.ACTION_CREATE_DOCUMENT,attempt.getAction());
    }
    @Test public void deniedPickerAccessIsReportedWithoutThrowing(){
        Host a=host();a.denied=true;a.openCsv();assertFalse(a.isFinishing());assertNotNull(ShadowToast.getTextOfLatestToast());
    }
    @Test public void pmtilesReadFallbackDoesNotClaimAPersistableGrant(){
        Host a=host();a.contentOnly=true;a.openExistingPmtiles();
        assertEquals(2,a.attempts.size());assertEquals(61,a.requestCode);
        assertEquals(0,a.attempts.get(1).getFlags()&Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        assertNotEquals(0,a.attempts.get(1).getFlags()&Intent.FLAG_GRANT_READ_URI_PERMISSION);
    }
    @Test public void anAvailableDocumentPickerKeepsItsNormalActionAndRequestCode(){
        Host a=host();a.openCsv();assertEquals(1,a.attempts.size());
        assertEquals(Intent.ACTION_OPEN_DOCUMENT,a.attempts.get(0).getAction());assertEquals(44,a.requestCode);
    }
}
