package fr.roadguard.app;
import java.io.*;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

/** Not executed by run_targeted_checks.py; requires the Android/Robolectric environment. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk={26,36})
@SQLiteMode(SQLiteMode.Mode.NATIVE)
public class RealSevenZipRuntimeTest {
  @Test public void packagedArchivesExtractAndInstallUsingAndroidSQLite()throws Exception{
    android.content.Context context=RuntimeEnvironment.getApplication();
    File dir=new File(context.getCacheDir(),"real-sevenzip-"+UUID.randomUUID());assertTrue(dir.mkdirs());
    try{
      for(String country:new String[]{"FR","CH"}){
        RadarDownloadSource source=RadarDownloadSource.find(country);
        File archive=RadarArchiveInstaller.bundled(source,context.getAssets()::open,new File(dir,"downloads"),CanonicalRadarStore.NONE);
        assertNotNull(archive);assertTrue(source.verify(archive));
        int count=RadarArchiveInstaller.install(archive,source,new File(dir,"updates"),new File(dir,"tmp"),CanonicalRadarStore.NONE);
        assertEquals("FR".equals(country)?3824:593,count);
        assertEquals(count,CanonicalRadarStore.load(new File(dir,"updates"),country).records.size());
      }
    }finally{delete(dir);}
  }
  static void delete(File file){File[] files=file.listFiles();if(files!=null)for(File child:files)delete(child);file.delete();}
}
