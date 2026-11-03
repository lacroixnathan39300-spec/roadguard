package fr.roadguard.app;
import java.io.*;
import java.util.*;
public final class TestRadarSelection {
  public static void main(String[] args)throws Exception{
    BundledRadarCatalog.Source source=name->new FileInputStream(new File(args[0],name));
    List<BundledRadarCatalog.Point> all=BundledRadarCatalog.active(source,true,true),limited=BundledRadarCatalog.active(source,false,true);
    int fr=0,permanent=0,historical=0;Set<String> ids=new HashSet<>();
    for(BundledRadarCatalog.Point p:all){if(!ids.add(p.id))throw new AssertionError("doublon");if("FR".equals(p.country)){fr++;if(p.historical)historical++;else permanent++;}}
    if(fr!=24429||permanent!=7325||historical!=17104||all.size()!=26206||limited.size()!=9102)throw new AssertionError("sélection incorrecte");
    System.out.println("PASS real catalog: FR24429 = permanent7325 + historical17104; CH1777; total26206 unique; disabling FR history returns9102");
  }
}
