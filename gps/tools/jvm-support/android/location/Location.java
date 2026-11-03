package android.location;
/** Data holder for testing freshness arithmetic, with no Android location implementation. */
public final class Location {
  private double lat,lon;private float accuracy;private boolean hasAccuracy;private long time,elapsed;
  public Location(String provider){}
  public double getLatitude(){return lat;}public void setLatitude(double value){lat=value;}
  public double getLongitude(){return lon;}public void setLongitude(double value){lon=value;}
  public boolean hasAccuracy(){return hasAccuracy;}public float getAccuracy(){return accuracy;}
  public void setAccuracy(float value){accuracy=value;hasAccuracy=true;}
  public long getTime(){return time;}public void setTime(long value){time=value;}
  public long getElapsedRealtimeNanos(){return elapsed;}public void setElapsedRealtimeNanos(long value){elapsed=value;}
}
