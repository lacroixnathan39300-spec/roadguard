package fr.roadguard.app;
import java.util.ArrayList;
public class TestRadarMapSlice {
  public static void main(String[] args){
    double[] lat=new double[100000],lon=new double[100000];
    for(int i=0;i<100000;i++){lat[i]=46.75+(i%500)*.0001;lon[i]=5.9+(i/500)*.0001;}
    DangerSpatialIndex index=new DangerSpatialIndex(lat,lon);
    if(index.indexedSize()!=100000)throw new AssertionError("GPS alert index lost points");
    ArrayList<Integer> near=index.queryLimited(46.75,5.9,60000,600);
    if(near.size()!=600||index.queryLimited(0,0,5000,600).size()!=0||index.queryLimited(46.75,5.9,60000,0).size()!=0)throw new AssertionError("viewport limit");
    System.out.println("100000 GPS radars indexed; map slice 600; distant and zero-cap slices empty");
  }
}
