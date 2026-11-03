package fr.roadguard.app;

import android.app.Application;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class RadarAnnouncementHandoffTest {
  private Application app;
  @Before public void setup(){app=RuntimeEnvironment.getApplication();app.getSharedPreferences("roadguard_alert_milestones",0).edit().clear().commit();}
  @Test public void oneVoiceWarningWithinOneKilometreThenNoRepeatedDistances(){
    assertEquals(0,AlertMilestones.stage(true,1001));
    assertEquals(1,AlertMilestones.stage(true,1000));
    assertEquals(1,AlertMilestones.stage(true,50));
    assertEquals(0,AlertMilestones.stage(true,-1));
    assertTrue(AlertMilestones.pending(app,46.75,5.9,"Radar fixe",1));
    assertEquals(AlertMilestones.FIRST,AlertMilestones.claimKind(app,46.75,5.9,"Radar fixe",1));
    assertFalse(AlertMilestones.pending(app,46.75,5.9,"Radar fixe",AlertMilestones.stage(true,100)));
  }
  @Test public void beepWaitsUntilSpeechHasFinished(){
    assertFalse(RadarAnnouncementHandoff.allowBeeps(true,false,false));
    assertFalse(RadarAnnouncementHandoff.allowBeeps(true,true,true));
    assertTrue(RadarAnnouncementHandoff.allowBeeps(true,true,false));
    assertTrue(RadarAnnouncementHandoff.allowBeeps(false,false,false));
  }
  @Test public void failedSpeechReleasesClaimAndFallsBackToBeeps(){
    AlertSpeechTracker tracker=new AlertSpeechTracker(app,(task,delay)->()->{});
    tracker.useAudio(new AlertSpeechTracker.Audio(){
      public boolean acquire(String id,int priority,Runnable interrupted){return false;}
      public void release(String id){}
      public void cancel(String id){}
    });
    AlertMilestones.Claim claim=AlertMilestones.reserve(app,46.75,5.9,"Radar fixe",1);
    assertFalse(tracker.speak(null,"Radar dans un kilomètre",claim));
    assertTrue(tracker.radarSpeechFailed());
    assertTrue(AlertMilestones.pending(app,46.75,5.9,"Radar fixe",1));
    assertTrue(RadarAnnouncementHandoff.allowBeeps(!tracker.radarSpeechFailed(),false,tracker.hasPending()));
  }
}
