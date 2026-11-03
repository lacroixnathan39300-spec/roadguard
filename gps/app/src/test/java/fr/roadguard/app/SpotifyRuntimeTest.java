package fr.roadguard.app;
import android.app.*;
import android.content.*;
import android.media.session.*;
import android.os.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={26,36},shadows={SpotifyRuntimeTest.ControllerState.class})
@LooperMode(LooperMode.Mode.PAUSED)
public class SpotifyRuntimeTest {
  @Implements(MediaController.class) public static class ControllerState {
    @org.robolectric.annotation.RealObject MediaController real;
    static final Map<MediaController,String> packages=new IdentityHashMap<>();
    static final Map<MediaController,PlaybackState> states=new IdentityHashMap<>();
    @Implementation protected String getPackageName(){return packages.getOrDefault(real,"unrelated.player");}
    @Implementation protected PlaybackState getPlaybackState(){return states.get(real);}
  }
  Application app;List<MediaSession> sessions=new ArrayList<>();
  @Before public void setup(){app=RuntimeEnvironment.getApplication();ControllerState.packages.clear();ControllerState.states.clear();}
  @After public void close(){for(MediaSession session:sessions)session.release();}
  MediaController controller(String pkg,int state,long actions){MediaSession s=new MediaSession(app,"test"+sessions.size());sessions.add(s);MediaController c=s.getController();ControllerState.packages.put(c,pkg);ControllerState.states.put(c,new PlaybackState.Builder().setState(state,0,1).setActions(actions).build());return c;}
  @Test public void sessionSelectionNeverTargetsAnotherMusicApp(){MediaController other=controller("unrelated.player",PlaybackState.STATE_PLAYING,PlaybackState.ACTION_PAUSE),spotify=controller(SpotifyControls.PACKAGE,PlaybackState.STATE_PAUSED,PlaybackState.ACTION_PLAY);assertSame(spotify,SpotifyControls.select(Arrays.asList(other,spotify)));assertNull(SpotifyControls.select(Collections.singletonList(other)));assertFalse(SpotifyControls.send(other,"toggle"));}
  @Test public void activeSpotifySessionWinsOverAnOldPausedSession(){MediaController paused=controller(SpotifyControls.PACKAGE,PlaybackState.STATE_PAUSED,PlaybackState.ACTION_PLAY),active=controller(SpotifyControls.PACKAGE,PlaybackState.STATE_PLAYING,PlaybackState.ACTION_PAUSE);assertSame(active,SpotifyControls.select(Arrays.asList(paused,active)));}
  @Test public void seekRequiresTheSpotifySessionCapability(){MediaController c=controller(SpotifyControls.PACKAGE,PlaybackState.STATE_PLAYING,PlaybackState.ACTION_SEEK_TO);assertTrue(SpotifyControls.supported(c,"seek"));assertFalse(SpotifyControls.supported(c,"next"));}
  @Test public void unsupportedSpotifyActionsAreDisabled(){MediaController c=controller(SpotifyControls.PACKAGE,PlaybackState.STATE_PLAYING,PlaybackState.ACTION_PAUSE);assertTrue(SpotifyControls.supported(c,"toggle"));assertFalse(SpotifyControls.supported(c,"next"));assertFalse(SpotifyControls.send(c,"next"));assertFalse(SpotifyControls.send(c,"unknown"));}
  @Test public void pauseAndPlayFollowCurrentSpotifyState(){MediaController c=controller(SpotifyControls.PACKAGE,PlaybackState.STATE_PLAYING,PlaybackState.ACTION_PAUSE);assertTrue(SpotifyControls.send(c,"toggle"));ControllerState.states.put(c,new PlaybackState.Builder().setState(PlaybackState.STATE_PAUSED,0,1).setActions(PlaybackState.ACTION_PLAY).build());assertTrue(SpotifyControls.send(c,"toggle"));}
  @Test public void previousAndNextRequireTheirOwnSupportedActions(){MediaController c=controller(SpotifyControls.PACKAGE,PlaybackState.STATE_PLAYING,PlaybackState.ACTION_SKIP_TO_NEXT|PlaybackState.ACTION_SKIP_TO_PREVIOUS);assertTrue(SpotifyControls.send(c,"next"));assertTrue(SpotifyControls.send(c,"previous"));assertFalse(SpotifyControls.supported(c,"toggle"));}
  @Test public void noMediaAccessMeansNoControllerAndNoAccountRequest(){assertFalse(SpotifyControls.authorized(app));assertNull(SpotifyControls.current(app));assertFalse(SpotifyControls.send(null,"toggle"));assertEquals(SpotifyControls.PACKAGE,SpotifyControls.launchIntent().getPackage());assertEquals("spotify",SpotifyControls.launchIntent().getData().getScheme());}
  @Test public void nativeSpotifyPanelBuildsWithoutSessionOrSecret()throws Exception{
    org.robolectric.android.controller.ActivityController<PhoneRuntimeTest.OfflineActivity> c=Robolectric.buildActivity(PhoneRuntimeTest.OfflineActivity.class).setup();AlertDialog d=null;try{PhoneRuntimeTest.OfflineActivity a=c.get();a.initialLoad.get(15,java.util.concurrent.TimeUnit.SECONDS);Shadows.shadowOf(Looper.getMainLooper()).idle();SpotifyControls.show(a);Shadows.shadowOf(Looper.getMainLooper()).idle();d=(AlertDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();assertNotNull(d);assertTrue(d.isShowing());assertEquals("Fermer",d.getButton(AlertDialog.BUTTON_POSITIVE).getText().toString());SpotifyControls.show(a);assertSame(d,a.spotifyDialog);SpotifyControls.close(a);assertNull(a.spotifyDialog);}finally{if(d!=null)d.dismiss();c.pause().stop().destroy();}
  }
}
