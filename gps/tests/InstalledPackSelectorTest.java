import fr.roadguard.app.*;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
public class InstalledPackSelectorTest {
 static String sha(File f)throws Exception{MessageDigest d=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(f)){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)d.update(b,0,n);}StringBuilder s=new StringBuilder();for(byte x:d.digest())s.append(String.format("%02x",x&255));return s.toString();}
 public static void main(String[] args)throws Exception{
  File source=new File(args[0]);if(!source.isFile())throw new AssertionError("Real sample missing");
  File root=Files.createTempDirectory("rg-real-vector").toFile();
  OfflineCellStore store=new OfflineCellStore(root);
  store.importVerified("firenze",new FileInputStream(source),sha(source),source.length()+1);
  OfflinePackRegionSelector.Region region=new OfflinePackRegionSelector.Region(new VerifiedMapDownloader.Pack("firenze","https://example.org/firenze.pmtiles",sha(source),source.length()+1),43.6,11.1,43.9,11.5);
  List<OfflinePackRegionSelector.Region> catalog=Collections.singletonList(region);
  InstalledPackSelector.Choice found=InstalledPackSelector.select(43.77,11.25,catalog,store);
  if(found==null||!"firenze".equals(found.id)||!found.file.isFile())throw new AssertionError("Installed pack not selected");
  PmtilesReader reader=new PmtilesReader(found.file);
  if(!reader.isVector())throw new AssertionError("Real sample not vector");
  if(InstalledPackSelector.select(46.7,5.9,catalog,store)!=null)throw new AssertionError("Wrong GPS region activated");
  if(InstalledPackSelector.select(43.77,11.25,Collections.emptyList(),store)!=null)throw new AssertionError("Missing catalog must not activate");
  System.out.println("PASS: real vector PMTiles verified, selected by GPS, other GPS excluded, missing catalog excluded");
 }
}
