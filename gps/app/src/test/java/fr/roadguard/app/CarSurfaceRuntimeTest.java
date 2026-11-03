package fr.roadguard.app;
import android.graphics.SurfaceTexture;
import android.view.Surface;
import androidx.car.app.SurfaceContainer;
import androidx.car.app.testing.TestCarContext;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={26,34,36})
@LooperMode(LooperMode.Mode.PAUSED)
public class CarSurfaceRuntimeTest {
  RoadguardCarScreen screen;
  @Before public void setup(){screen=new RoadguardCarScreen(TestCarContext.createCarContext(RuntimeEnvironment.getApplication()));screen.main.removeCallbacks(screen.pulse);}
  @After public void close(){screen.close();}
  Surface surface(){return new Surface(new SurfaceTexture(0));}
  @Test public void resizingAndDisconnectionReleaseEveryReceivedSurface(){
    Surface a=surface(),b=surface();assertTrue(a.isValid());assertTrue(b.isValid());
    screen.map.onSurfaceAvailable(new SurfaceContainer(a,320,240,160));
    screen.map.onSurfaceAvailable(new SurfaceContainer(b,640,480,240));assertFalse(a.isValid());assertSame(b,screen.map.surface);
    screen.map.onSurfaceDestroyed(new SurfaceContainer(b,640,480,240));assertFalse(b.isValid());assertNull(screen.map.surface);
  }
  @Test public void lateSurfaceAfterSessionCloseIsReleasedImmediately(){
    screen.close();Surface late=surface();screen.map.onSurfaceAvailable(new SurfaceContainer(late,320,240,160));assertFalse(late.isValid());assertNull(screen.map.surface);
  }
  @Test public void closingActiveSessionReleasesMapSurface(){
    Surface active=surface();screen.map.onSurfaceAvailable(new SurfaceContainer(active,320,240,160));screen.close();assertFalse(active.isValid());assertTrue(screen.map.io.isShutdown());
  }
}
