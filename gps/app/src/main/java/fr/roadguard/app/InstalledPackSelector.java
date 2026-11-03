package fr.roadguard.app;

import java.io.File;
import java.io.IOException;
import java.util.List;

/** Select a real, checksum-verified vector archive covering the current GPS point. */
public final class InstalledPackSelector {
  private InstalledPackSelector() {}
  public static final class Choice {
    public final String id;
    public final File file;
    Choice(String id, File file) { this.id=id;this.file=file; }
  }
  public static Choice select(double lat,double lon,List<OfflinePackRegionSelector.Region> regions,
      OfflineCellStore store) throws IOException {
    if (!Double.isFinite(lat)||!Double.isFinite(lon)||regions==null||store==null)return null;
    for(OfflinePackRegionSelector.Region region:regions){
      if(OfflinePackRegionSelector.distanceToRegionKm(lat,lon,region)>0.001)continue;
      File archive=store.verifiedFile(region.pack.id);
      if(archive==null)continue;
      try {
        if(new PmtilesReader(archive).isVector())return new Choice(region.pack.id,archive);
      }catch(IOException ignored){/* Invalid map: do not activate it. */}
    }
    return null;
  }
}
