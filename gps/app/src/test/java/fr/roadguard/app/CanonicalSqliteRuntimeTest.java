package fr.roadguard.app;

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

/** Native Android SQLite integration, to run when SDK/Robolectric are available. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk={26,36})
@SQLiteMode(SQLiteMode.Mode.NATIVE)
public class CanonicalSqliteRuntimeTest {
  Application application;File source;
  @Before public void prepare()throws Exception{
    application=RuntimeEnvironment.getApplication();source=File.createTempFile("sqlite-source-",".sqlite",application.getCacheDir());source.delete();
    try(SQLiteDatabase database=SQLiteDatabase.openOrCreateDatabase(source,null)){
      database.execSQL("CREATE TABLE cameras(id INTEGER PRIMARY KEY,lat REAL NOT NULL,lon REAL NOT NULL,type TEXT NOT NULL,note TEXT)");
      database.execSQL("INSERT INTO cameras VALUES(?,?,?,?,?)",new Object[]{1,0.123456,-150.87654,"speed_camera","unknown metadata"});
      database.execSQL("INSERT INTO cameras VALUES(?,?,?,?,?)",new Object[]{2,0.123456,-150.87654,"red_light","no speed supplied"});
    }
  }
  @After public void cleanup(){source.delete();File file=CanonicalRadarStore.file(UpdatedRadarCatalog.directory(application),"FR");file.delete();new File(file+".backup").delete();new File(file+".backup.previous").delete();}
  byte[] hash()throws Exception{MessageDigest digest=MessageDigest.getInstance("SHA-256");try(InputStream input=new FileInputStream(source)){byte[] bytes=new byte[8192];int n;while((n=input.read(bytes))!=-1)digest.update(bytes,0,n);}return digest.digest();}
  @Test public void nativeReadOnlyImportPreservesFileMetadataAndUnknownLimits()throws Exception{
    byte[] original=hash();List<CanonicalRadar> records=SpeedAlertSqliteImporter.readLocal(source,"FR",CanonicalRadarStore.NONE);
    assertArrayEquals(original,hash());assertEquals(2,records.size());
    assertEquals("unknown metadata",records.get(0).metadata.get("sqlite.note"));assertEquals(0,records.get(1).speedKmh);assertNull(records.get(1).direction);
    CanonicalRadarStore.install(UpdatedRadarCatalog.directory(application),"FR",records,CanonicalRadarStore.NONE);
    List<BundledRadarCatalog.Point> catalog=UpdatedRadarCatalog.active(application,true,true);
    for(CanonicalRadar record:records)assertTrue(catalog.stream().anyMatch(point->point.id.equals(record.id)));
    assertEquals(0,RadarActivationState.loadedCount(application,"FR"));
    RadarActivationState.consumed(application,"phone",RadarCatalogRevision.current(application),catalog);assertEquals(2,RadarActivationState.loadedCount(application,"FR"));
  }
}
