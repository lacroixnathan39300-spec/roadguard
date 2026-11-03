package fr.roadguard.app;

/** Monotonic-clock gate. Unknown GPS speed never proves that a vehicle has stopped. */
final class DriveAutoPolicy {
  static final double START_KMH=20, STOP_KMH=3;
  static final long STOP_MS=60000, FRESH_MS=10000;
  private boolean driving;
  private long last=-1,stoppedAt=-1,fastAt=-1;
  void update(long at,double kmh){
    if(at<0||last>=0&&at<=last)return;
    if(last>=0&&at-last>FRESH_MS)reset();
    last=at;
    if(!Double.isFinite(kmh)||kmh<0||kmh>396){reset();last=at;return;}
    if(!driving){
      if(kmh>START_KMH){if(fastAt<0)fastAt=at;else if(at-fastAt>=500)driving=true;}
      else fastAt=-1;
    }
    if(driving){if(kmh<=STOP_KMH){if(stoppedAt<0)stoppedAt=at;}else stoppedAt=-1;}
  }
  boolean active(long now){
    if(last<0||now<last||now-last>FRESH_MS){reset();return false;}
    if(driving&&stoppedAt>=0&&now-stoppedAt>=STOP_MS){driving=false;fastAt=-1;stoppedAt=-1;}
    return driving;
  }
  private void reset(){driving=false;stoppedAt=-1;fastAt=-1;}
}
