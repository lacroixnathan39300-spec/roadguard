package fr.roadguard.app;
import java.io.*;import java.util.*;
public class TestRadarMetadata {
 static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
 public static void main(String[] args)throws Exception{
  CanonicalRadar source=new CanonicalRadar("FR","test",46.7,6,CanonicalRadar.Kind.FIXED,80,90d,"Source test","","import_local",Collections.emptyMap());
  BundledRadarCatalog.Point point=CanonicalRadar.decode(source.encode()).point();check(point.direction!=null&&point.direction==90d,"direction lost during codec/live conversion");check(point.reliability.equals("import_local"),"source qualification lost");
  CanonicalRadar unknown=new CanonicalRadar("CH","unknown",46.8,6.1,CanonicalRadar.Kind.UNKNOWN,0,null,"Source inconnue","","import_local",Collections.emptyMap());check(unknown.point().direction==null,"unknown direction must stay unknown");
  List<BundledRadarCatalog.Point> france=BundledRadarCatalog.france(name->new FileInputStream(new File(args[0],name)));
  List<BundledRadarCatalog.Point> swiss=BundledRadarCatalog.switzerland(name->new FileInputStream(new File(args[0],name)));
  check(!france.isEmpty()&&!swiss.isEmpty(),"existing catalogs no longer load");
  for(BundledRadarCatalog.Point p:france)check(p.direction==null||p.direction>=0&&p.direction<360,"invalid direction leaked");
  for(BundledRadarCatalog.Point p:swiss)check(p.direction==null||p.direction>=0&&p.direction<360,"invalid direction leaked");
  System.out.println("PASS source orientation/qualification codec, unknown values, and existing FR/CH catalogs ("+(france.size()+swiss.size())+" points)");
 }
}
