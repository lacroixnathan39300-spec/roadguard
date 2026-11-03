package fr.roadguard.app;
import java.io.*;
import java.nio.file.Files;
import java.util.*;

/** Executes the production supplement merger on real files and bundled inventory. */
public final class TestRadarSupplements {
  static int checks,scenarios;
  static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
  static BundledRadarCatalog.Point point(String id,double lat,double lon){return new BundledRadarCatalog.Point(id,"Radar fixe",lat,lon,"fixed",false,"fixture","FR");}
  static void write(File file,String value)throws Exception{Files.write(file.toPath(),value.getBytes("UTF-8"));}
  static List<BundledRadarCatalog.Point> merge(List<BundledRadarCatalog.Point> base,File file,String id,String country,List<BundledRadarCatalog.Point> extra,boolean history)throws Exception{
    return RadarSupplementCatalog.merge(base,Collections.singletonList(new RadarSupplementCatalog.Source(id,country,file)),extra,history);
  }
  public static void main(String[] args)throws Exception{
    File dir=Files.createTempDirectory("roadguard-supplement-").toFile(),file=new File(dir,"fr.csv");
    try{
      List<BundledRadarCatalog.Point> originals=BundledRadarCatalog.france(name->new FileInputStream(new File(args[0],name)));
      BundledRadarCatalog.Point first=originals.stream().filter(p->RadarIdentity.type(p.type).equals("fixed")).findFirst().get();
      write(file,"id;type;lat;lon\nnear;Radar fixe;"+first.lat+";"+first.lon+"\nnew;Radar fixe;0.12345;-150.45678\n");
      List<BundledRadarCatalog.Point> combined=merge(originals,file,"verified_fr","FR",Collections.emptyList(),true);
      Set<String> present=new HashSet<>();for(BundledRadarCatalog.Point p:combined)present.add(p.id);
      for(BundledRadarCatalog.Point p:originals)check(present.contains(p.id),"Original lost: "+p.id);
      check(combined.size()==originals.size()+1,"Only distinct supplement added");scenarios++;
      write(file,"broken header\n0,0,broken");check(merge(originals,file,"verified_fr","FR",Collections.emptyList(),true).size()==originals.size(),"Corrupt supplement removed originals");scenarios++;
      write(file,"id;type;lat;lon\nbad;Radar fixe;NaN;5\nbad;Radar fixe;99;5\nbad;Accident;0;0\nbad;Radar fixe;x;0\n");check(merge(originals,file,"verified_fr","FR",Collections.emptyList(),true).size()==originals.size(),"Invalid points accepted");scenarios++;
      write(file,"id;type;lat;lon\nnew;Radar fixe;0.12345;-150.45678\n");check(merge(originals,file,"poi_warner_ch","CH",Collections.emptyList(),true).size()==originals.size(),"Quarantined source activated");scenarios++;
      write(file,"id;type;lat;lon\nm;Radar mobile;0.12345;-150.45678\n");check(merge(originals,file,"verified_ch","CH",Collections.emptyList(),false).size()==originals.size(),"Swiss history filter ignored");
      check(merge(originals,file,"verified_ch","CH",Collections.emptyList(),true).size()==originals.size()+1,"Swiss authorized history not loaded");scenarios++;
      write(file,"bad");List<BundledRadarCatalog.Point> community=Arrays.asList(point("duplicate",first.lat,first.lon),point("extra",-10,-100));
      check(merge(originals,file,"verified_fr","FR",community,true).size()==originals.size()+1,"Community duplicates not filtered");scenarios++;
      List<BundledRadarCatalog.Point> wrap=Collections.singletonList(point("date-line",0,179.9999));
      check(merge(wrap,file,"verified_fr","FR",Collections.singletonList(point("near-date-line",0,-179.9999)),true).size()==1,"Date-line duplicate not recognized");scenarios++;
      List<BundledRadarCatalog.Point> fixed=Collections.singletonList(point("fixed",46.75,5.9));
      write(file,"id;type;lat;lon\nred;Radar feu rouge;46.75;5.9\n");
      check(merge(fixed,file,"verified_fr","FR",Collections.emptyList(),true).size()==2,"Different co-located radar type lost");scenarios++;
      write(file,"bad");
      BundledRadarCatalog.Point otherCountry=new BundledRadarCatalog.Point("border","Radar fixe",46.75,5.9,"fixed",false,"fixture","CH");
      check(merge(fixed,file,"verified_fr","FR",Collections.singletonList(otherCountry),true).size()==2,"Cross-border source suppressed");scenarios++;
      Thread.currentThread().interrupt();try{merge(originals,file,"verified_fr","FR",community,true);throw new AssertionError("Cancellation ignored");}catch(InterruptedIOException expected){check(true,"cancelled");}finally{Thread.interrupted();}scenarios++;
      List<BundledRadarCatalog.Point> immutable=merge(originals,file,"verified_fr","FR",Collections.emptyList(),true);
      try{immutable.clear();throw new AssertionError("Snapshot mutable");}catch(UnsupportedOperationException expected){check(originals.size()>0,"Input catalog changed");}scenarios++;
      write(file,"id;type;lat;lon\na;Radar fixe;0.12345;-150.45678\nb;Radar fixe;0.12346;-150.45678\n");check(merge(originals,file,"verified_fr","FR",Collections.emptyList(),true).size()==originals.size()+1,"Duplicate additions accepted");scenarios++;
      System.out.println("PASS "+scenarios+" radar supplement scenarios / "+checks+" assertions; originals preserved, real files, no Android runtime");
    }finally{file.delete();dir.delete();}
  }
}
