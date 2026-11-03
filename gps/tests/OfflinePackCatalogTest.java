import fr.roadguard.app.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
public class OfflinePackCatalogTest {
 static final String SHA=String.join("",Collections.nCopies(64,"a"));
 static String row(String id,String url){return id+"\t"+url+"\t"+SHA+"\t1000000\t46.7\t5.6\t47.1\t6.0\tpmtiles\n";}
 static List<OfflinePackRegionSelector.Region> parse(String s)throws Exception{return OfflinePackCatalog.read(new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8)));}
 static void rejects(String s)throws Exception{try{parse(s);throw new AssertionError("Accepted invalid catalog");}catch(IOException expected){}}
 public static void main(String[] args)throws Exception{
  if(parse("# ROADGUARD\n"+row("jura","https://maps.example.org/jura.pmtiles")).size()!=1)throw new AssertionError();
  rejects(row("a","http://maps.example.org/a.pmtiles"));
  rejects(row("a","https://maps.example.org/a.pmtiles")+row("a","https://maps.example.org/b.pmtiles"));
  rejects(row("../a","https://maps.example.org/a.pmtiles"));
  rejects(row("a","https://user:pass@maps.example.org/a.pmtiles"));
  rejects(row("a","https://maps.example.org/a.pmtiles#fragment"));
  rejects(row("a","https://maps.example.org/a.pmtiles").replace("\tpmtiles","\tpng"));
  rejects("#"+String.join("",Collections.nCopies(1048577,"x")));
  System.out.println("OK: strict HTTPS catalog, unique IDs, PMTiles, bounds, size limit");
 }
}
