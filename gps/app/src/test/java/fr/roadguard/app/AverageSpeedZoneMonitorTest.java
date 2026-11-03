package fr.roadguard.app;

import org.junit.Test;
import java.io.StringReader;
import java.util.ArrayList;
import static org.junit.Assert.*;

public class AverageSpeedZoneMonitorTest {
  @Test public void pairsOnlyVerifiedEndpointsAndReportsAverage()throws Exception {
    String csv="id;type;lat;lon;country;category;speed_kmh;direction_deg;status;sources;source_ids;speed_conflicts\n"
      +"one;Radar tronçon début;46.0;6.0;CH;average_start;;;inventory;osm;osm_live:relation/123/from/node/1;\n"
      +"two;Radar vitesse moyenne;46.0;6.0;CH;average;80;;inventory;osm;osm_live:relation/123/device/node/1;\n"
      +"three;Radar tronçon fin;46.01;6.0;CH;average_end;;;inventory;osm;osm_live:relation/123/to/node/2;\n";
    ArrayList<AverageSpeedZoneMonitor.Zone> zones=AverageSpeedZoneMonitor.parse(new StringReader(csv));
    assertEquals(1,zones.size());
    AverageSpeedZoneMonitor monitor=new AverageSpeedZoneMonitor(zones);
    AverageSpeedZoneMonitor.Status start=monitor.update(46.0,6.0,70,5,0,1000);
    assertNotNull(start);assertTrue(start.active);
    AverageSpeedZoneMonitor.Status middle=monitor.update(46.005,6.0,70,5,0,31000);
    assertNull(middle); // Missing fixes invalidate an average; never claim OK.
    start=monitor.update(46.0,6.0,70,5,0,32000);
    assertNotNull(start);
    long at=32000;
    AverageSpeedZoneMonitor.Status status=start;
    for(int i=1;i<=20;i++){
      at+=2000;
      status=monitor.update(46.0+i*0.0005,6.0,110,5,0,at);
    }
    assertTrue(status.finished);
    assertTrue(status.overLimit);
    assertTrue(status.text.contains("au-dessus"));
  }
  @Test public void doesNotStartInWrongDirectionOrWithoutAccurateGps(){
    ArrayList<AverageSpeedZoneMonitor.Zone> zones=new ArrayList<>();
    zones.add(new AverageSpeedZoneMonitor.Zone("z",46.0,6.0,46.01,6.0,80));
    AverageSpeedZoneMonitor monitor=new AverageSpeedZoneMonitor(zones);
    assertNull(monitor.update(46.0,6.0,90,5,180,1000));
    assertNull(monitor.update(46.0,6.0,90,80,0,2000));
    assertEquals(1,monitor.zoneCount());
  }
}
