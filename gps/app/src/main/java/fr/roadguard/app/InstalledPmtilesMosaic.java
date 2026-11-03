package fr.roadguard.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Chooses the local vector archive covering each requested map tile. */
final class InstalledPmtilesMosaic {
  private InstalledPmtilesMosaic() {}

  static final class Region {
    final File file;
    final double west,south,east,north,area;
    final int maxZoom;
    Region(File file,double west,double south,double east,double north,int maxZoom){
      this.file=file;this.west=west;this.south=south;this.east=east;this.north=north;
      this.area=(east-west)*(north-south);this.maxZoom=maxZoom;
    }
  }

  static List<Region> installed(File directory){
    File[] files=directory.listFiles((parent,name)->name.matches("[A-Za-z0-9_-]{1,64}\\.pmtiles"));
    if(files==null)return Collections.emptyList();
    ArrayList<Region> regions=new ArrayList<>();
    for(File file:files){
      if(regions.size()>=100)break;
      try{Region region=read(file);if(region!=null)regions.add(region);}catch(IOException ignored){}
    }
    // Once a complete France map exists, use it throughout France. Mixing it
    // with older regional tiles creates a visible straight seam on the map.
    boolean completeFrance=false;
    for(Region region:regions)if("france_openmaptiles.pmtiles".equals(region.file.getName()))completeFrance=true;
    if(completeFrance)regions.removeIf(region->region.file.getName().matches("france_(northeast|northwest|southeast|southwest)\\.pmtiles"));
    regions.sort(Comparator.comparing(r->r.file.getName()));
    return regions;
  }

  /** Discover both download stores. Only registered, completed sectors are eligible; never staging files. */
  static List<Region> installed(File legacy,File verifiedDirectory){
    java.util.TreeMap<String,Region> result=new java.util.TreeMap<>();
    for(Region region:installed(legacy))result.put(region.file.getName(),region);
    try{
      OfflineCellStore store=new OfflineCellStore(verifiedDirectory);
      File[] files=verifiedDirectory.listFiles();
      if(files!=null)for(File file:files){
        String name=file.getName();if(!name.matches("[A-Za-z0-9_-]{1,64}\\.pmtiles"))continue;
        File verified=store.completedFile(name.substring(0,name.length()-8));
        if(verified!=null){Region region=read(verified);if(region!=null)result.put(name,region);}
      }
    }catch(IOException unavailable){}
    return new ArrayList<>(result.values());
  }

  static Region read(File file)throws IOException{
    if(!file.isFile()||file.length()<127)return null;
    byte[] h=new byte[127];
    try(FileInputStream in=new FileInputStream(file)){
      int offset=0,n;while(offset<127&&(n=in.read(h,offset,127-offset))>0)offset+=n;
      if(offset!=127)throw new IOException("En-tête PMTiles incomplet");
    }
    if(!"PMTiles".equals(new String(h,0,7,"US-ASCII"))||(h[7]&255)!=3||(h[99]&255)!=1)return null;
    double west=le32(h,102)/10000000d,south=le32(h,106)/10000000d;
    double east=le32(h,110)/10000000d,north=le32(h,114)/10000000d;
    int zoom=h[101]&255;
    if(west< -180||east>180||south< -90||north>90||west>=east||south>=north||zoom>22)return null;
    return new Region(file,west,south,east,north,zoom);
  }

  private static int le32(byte[] b,int offset){
    return (b[offset]&255)|((b[offset+1]&255)<<8)|((b[offset+2]&255)<<16)|(b[offset+3]<<24);
  }

  static List<Region> candidates(List<Region> regions,int z,int x,int y){
    if(z<0||z>22||x<0||y<0||x>=(1<<z)||y>=(1<<z))return Collections.emptyList();
    if(z<6)return regions;
    double n=Math.scalb(1.0,z),west=x/n*360-180,east=(x+1)/n*360-180;
    double north=tileLatitude(y/n),south=tileLatitude((y+1)/n);
    double centerLon=(west+east)/2,centerLat=(south+north)/2;
    ArrayList<Region> matching=new ArrayList<>();
    for(Region r:regions)if(r.maxZoom>=z&&r.west<east&&r.east>west&&r.south<north&&r.north>south)matching.add(r);
    matching.sort((a,b)->{
      boolean aCenter=a.west<=centerLon&&a.east>=centerLon&&a.south<=centerLat&&a.north>=centerLat;
      boolean bCenter=b.west<=centerLon&&b.east>=centerLon&&b.south<=centerLat&&b.north>=centerLat;
      if(aCenter!=bCenter)return aCenter?-1:1;
      int area=Double.compare(a.area,b.area);return area!=0?area:a.file.getName().compareTo(b.file.getName());
    });
    return matching;
  }

  private static double tileLatitude(double y){return Math.toDegrees(Math.atan(Math.sinh(Math.PI*(1-2*y))));}
}
