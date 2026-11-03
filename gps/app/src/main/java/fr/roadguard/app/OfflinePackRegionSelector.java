package fr.roadguard.app;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Chooses authorized regional packs overlapping the GPS radius.
 * Metadata must originate from a trusted catalog; never synthesizes a download URL.
 * Handles antimeridian-crossing boxes and excludes verified packs.
 */
public final class OfflinePackRegionSelector {
  private OfflinePackRegionSelector() {}
  public static final class Region {
    public final VerifiedMapDownloader.Pack pack;
    public final double south,west,north,east;
    public Region(VerifiedMapDownloader.Pack pack,double south,double west,double north,double east){
      if(pack==null||!finite(south,west,north,east)||south < -90||north > 90||south>north||west < -180||west > 180||east < -180||east > 180)
        throw new IllegalArgumentException("Invalid map region");
      this.pack=pack;this.south=south;this.west=west;this.north=north;this.east=east;
    }
  }
  private static boolean finite(double... values){for(double v:values)if(!Double.isFinite(v))return false;return true;}
  private static double longitudeDelta(double a,double b){return Math.abs((a-b+540)%360-180);}
  private static double clamp(double v,double low,double high){return Math.max(low,Math.min(high,v));}
  private static double nearestLongitude(double lon,double west,double east){
    if(west<=east){if(lon>=west&&lon<=east)return lon;return longitudeDelta(lon,west)<=longitudeDelta(lon,east)?west:east;}
    if(lon>=west||lon<=east)return lon;
    return longitudeDelta(lon,west)<=longitudeDelta(lon,east)?west:east;
  }
  public static double distanceToRegionKm(double lat,double lon,Region r){
    if(!finite(lat,lon)||lat < -90||lat>90||lon < -180||lon>180)throw new IllegalArgumentException("Invalid GPS");
    double nearestLat=clamp(lat,r.south,r.north);
    double nearestLon=nearestLongitude(lon,r.west,r.east);
    // Conservative estimate for mid-latitude regional boxes; exact for a GPS inside the box.
    return NearbyOfflinePlanner.distanceKm(lat,lon,nearestLat,nearestLon);
  }
  public static List<Region> missingNearby(double lat,double lon,double radiusKm,List<Region> catalog,OfflineCellStore store) throws IOException {
    if(!Double.isFinite(radiusKm)||radiusKm<=0||radiusKm>1000||catalog==null||store==null)throw new IllegalArgumentException("Invalid request");
    ArrayList<Region> out=new ArrayList<>();
    for(Region r:catalog){
      if(distanceToRegionKm(lat,lon,r)>radiusKm)continue;
      if(!store.hasVerified(r.pack.id))out.add(r);
    }
    out.sort(Comparator.comparingDouble(r->distanceToRegionKm(lat,lon,r)));
    return out;
  }
}
