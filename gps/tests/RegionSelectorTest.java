import fr.roadguard.app.*;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
public class RegionSelectorTest {
  static void yes(boolean b,String msg){if(!b)throw new AssertionError(msg);}
  static String hash(byte[] data)throws Exception{byte[] digest=MessageDigest.getInstance("SHA-256").digest(data);StringBuilder b=new StringBuilder();for(byte x:digest)b.append(String.format("%02x",x&255));return b.toString();}
  public static void main(String[] args)throws Exception {
    Path root=Files.createTempDirectory("rg-regions-");OfflineCellStore store=new OfflineCellStore(root.toFile());
    byte[] bytes="valid fixture".getBytes("UTF-8");String sha=hash(bytes);
    VerifiedMapDownloader.Pack jura=new VerifiedMapDownloader.Pack("jura","https://example.org/jura.pmtiles",sha,1000);
    VerifiedMapDownloader.Pack lyon=new VerifiedMapDownloader.Pack("lyon","https://example.org/lyon.pmtiles",sha,1000);
    VerifiedMapDownloader.Pack far=new VerifiedMapDownloader.Pack("marseille","https://example.org/marseille.pmtiles",sha,1000);
    List<OfflinePackRegionSelector.Region> catalog=Arrays.asList(
      new OfflinePackRegionSelector.Region(far,43.2,5.2,43.5,5.6),
      new OfflinePackRegionSelector.Region(lyon,45.6,4.7,45.9,5.0),
      new OfflinePackRegionSelector.Region(jura,46.7,5.6,47.1,6.0));
    List<OfflinePackRegionSelector.Region> near=OfflinePackRegionSelector.missingNearby(46.9,5.9,50,catalog,store);
    yes(near.size()==1&&near.get(0).pack.id.equals("jura"),"Jura only");
    store.importVerified("jura",new ByteArrayInputStream(bytes),sha,1000);
    yes(OfflinePackRegionSelector.missingNearby(46.9,5.9,50,catalog,new OfflineCellStore(root.toFile())).isEmpty(),"No redownload after restart");
    yes(OfflinePackRegionSelector.missingNearby(45.76,4.84,50,catalog,store).get(0).pack.id.equals("lyon"),"Lyon becomes pending");
    yes(OfflinePackRegionSelector.missingNearby(43.3,5.4,50,catalog,store).get(0).pack.id.equals("marseille"),"Marseille becomes pending");
    OfflinePackRegionSelector.Region wrap=new OfflinePackRegionSelector.Region(jura,-2,170,2,-170);
    yes(OfflinePackRegionSelector.distanceToRegionKm(0,179,wrap)==0,"Dateline east");
    yes(OfflinePackRegionSelector.distanceToRegionKm(0,-179,wrap)==0,"Dateline west");
    System.out.println("OK: GPS 50km selection, cumulative memory, deduplication, antimeridian");
  }
}
