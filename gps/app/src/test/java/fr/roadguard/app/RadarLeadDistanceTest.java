package fr.roadguard.app;

import org.junit.Test;
import static org.junit.Assert.*;

public class RadarLeadDistanceTest {
  @Test public void advancesKilometreWarningByTwentyMetres(){
    assertEquals(1000,RadarLeadDistance.effective(1020,90,90,true),0.01);
    assertEquals(1000,RadarLeadDistance.onRoute(1020),0.01);
  }
  @Test public void stopsAfterTheAdvancedPassingPoint(){
    assertEquals(0,RadarLeadDistance.effective(20,90,90,true),0.01);
    assertEquals(-1,RadarLeadDistance.effective(19,90,90,true),0.01);
    assertEquals(-1,RadarLeadDistance.onRoute(20),0.01);
  }
  @Test public void retainsCrossTrackDistance(){
    assertEquals(Math.hypot(80,100),RadarLeadDistance.effective(Math.hypot(100,100),45,0,true),0.01);
    assertEquals(980,RadarLeadDistance.effective(1000,90,0,false),0.01);
    assertTrue(Double.isNaN(RadarLeadDistance.effective(Double.NaN,0,0,true)));
  }
}
