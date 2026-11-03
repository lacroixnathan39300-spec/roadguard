package fr.roadguard.app;

import java.util.ArrayList;
import java.util.HashMap;

/** Small in-memory grid index for reducing GPS proximity checks over the bundled radar set. */
final class DangerSpatialIndex {
  private static final double CELL_DEGREES=0.10;
  private static final int LONGITUDE_CELLS=(int)(360.0/CELL_DEGREES);
  private final HashMap<Long,ArrayList<Integer>> cells=new HashMap<>();
  private final int size;

  DangerSpatialIndex(double[] latitudes,double[] longitudes){
    size=Math.min(latitudes.length,longitudes.length);
    for(int i=0;i<size;i++){
      double lat=latitudes[i],lon=longitudes[i];
      if(!Double.isFinite(lat)||!Double.isFinite(lon)||Math.abs(lat)>90||Math.abs(lon)>180)continue;
      long key=key(cell(lat),longitudeCell(lon));
      ArrayList<Integer> bucket=cells.get(key);
      if(bucket==null){bucket=new ArrayList<>();cells.put(key,bucket);}
      bucket.add(i);
    }
  }

  ArrayList<Integer> query(double latitude,double longitude,double radiusMeters){
    return queryLimited(latitude,longitude,radiusMeters,Integer.MAX_VALUE);
  }

  ArrayList<Integer> queryLimited(double latitude,double longitude,double radiusMeters,int maximum){
    ArrayList<Integer> result=new ArrayList<>();
    if(maximum<=0)return result;
    if(size==0||!Double.isFinite(latitude)||!Double.isFinite(longitude)||Math.abs(latitude)>90||Math.abs(longitude)>180||!Double.isFinite(radiusMeters)||radiusMeters<0)return result;
    double latSpan=radiusMeters/110574.0;
    double cosine=Math.max(0.01,Math.cos(Math.toRadians(latitude)));
    double lonSpan=Math.min(360.0,radiusMeters/(111320.0*cosine));
    int minLat=cell(Math.max(-90,latitude-latSpan)),maxLat=cell(Math.min(90,latitude+latSpan));
    int centerLon=longitudeCell(longitude);
    int lonCells=lonSpan>=180?LONGITUDE_CELLS/2:(int)Math.ceil(lonSpan/CELL_DEGREES)+1;
    for(int y=minLat;y<=maxLat;y++){
      if(lonCells>=LONGITUDE_CELLS/2){for(int x=0;x<LONGITUDE_CELLS;x++){ArrayList<Integer> bucket=cells.get(key(y,x));if(bucket!=null)for(Integer value:bucket){result.add(value);if(result.size()>=maximum)return result;}}}
      else for(int dx=-lonCells;dx<=lonCells;dx++){int x=(centerLon+dx)%LONGITUDE_CELLS;if(x<0)x+=LONGITUDE_CELLS;ArrayList<Integer> bucket=cells.get(key(y,x));if(bucket!=null)for(Integer value:bucket){result.add(value);if(result.size()>=maximum)return result;}}
    }
    return result;
  }

  int indexedSize(){int count=0;for(ArrayList<Integer> bucket:cells.values())count+=bucket.size();return count;}
  private static int cell(double coordinate){return (int)Math.floor((coordinate+180.0)/CELL_DEGREES);}
  private static int longitudeCell(double longitude){double wrapped=((longitude+180.0)%360.0+360.0)%360.0-180.0;return Math.min(LONGITUDE_CELLS-1,Math.max(0,(int)Math.floor((wrapped+180.0)/CELL_DEGREES)));}
  private static long key(int latCell,int lonCell){return (((long)latCell)<<32)|(lonCell&0xffffffffL);}
}
