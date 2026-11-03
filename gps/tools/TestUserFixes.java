package fr.roadguard.app;
import java.io.*;import java.nio.file.*;import java.util.*;
/** The actual Champagnole records reported in the screenshots, plus non-merge safeguards. */
public final class TestUserFixes {
 static int checks;
 static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
 static BundledRadarCatalog.Point p(String id,double lat,String type,int speed,Double direction,String source,boolean history){return new BundledRadarCatalog.Point(id,type,lat,5.92218,"fixed",history,source,"FR",speed,direction,"");}
 public static void main(String[] args)throws Exception{
  File temp=Files.createTempDirectory("user-map-fixes-").toFile();
  try{
   TestCatalogCore.FilesContext context=new TestCatalogCore.FilesContext(temp,new File(args[0]));
   List<BundledRadarCatalog.Point> base=BundledRadarCatalog.france(context.getAssets()::open),overlay=LufopEuropeCatalog.all(context);
   BundledRadarCatalog.Point fixed=null,zone=null,lf=null,lz=null;
   for(BundledRadarCatalog.Point p:base){if(p.id.equals("rg-4fe984fd9019cc59dd43"))fixed=p;if(p.id.equals("rg-54eb07386dfc96e47cc7"))zone=p;}
   for(BundledRadarCatalog.Point p:overlay){if(p.id.equals("lufop-8c035dfad849ff990dd3"))lf=p;if(p.id.equals("lufop-eea67d7875fb594c9f0b"))lz=p;}
   check(fixed!=null&&zone!=null&&lf!=null&&lz!=null,"screenshot fixtures missing");
   RadarCatalogMerge map=new RadarCatalogMerge(Arrays.asList(fixed,zone));map.add(lf);map.add(lz);
   check(map.snapshot().size()==2,"screenshot locations still appear twice");
   check(map.snapshot().get(0).id.equals(fixed.id)&&map.snapshot().get(0).lat==fixed.lat&&map.snapshot().get(0).direction.equals(fixed.direction),"original identity, geometry or bearing replaced");
   check(map.snapshot().get(0).speedKmh==80&&map.snapshot().get(0).sources.contains("Lufop ASC"),"speed/provenance lost");
   check(lz.historical&&RadarIdentity.type(zone.type).equals(RadarIdentity.type(lz.type)),"archived zone promoted to current installation");
   map.add(lf);map.add(lz);check(map.snapshot().size()==2,"repeat reload multiplied observations");
   BundledRadarCatalog.Point incoming=p("lufop-test",46.74810,"Radar fixe",80,null,"Lufop ASC",false);
   for(BundledRadarCatalog.Point distinct:Arrays.asList(p("different-source",46.74826,"Radar fixe",80,null,"osm",false),p("different-speed",46.74826,"Radar fixe",90,null,"lufop",false),p("different-type",46.74826,"Radar feu rouge",80,null,"lufop",false),p("historical",46.74826,"Radar fixe",80,null,"lufop",true),p("far",46.75,"Radar fixe",80,null,"lufop",false))){
    RadarCatalogMerge retain=new RadarCatalogMerge(Collections.singletonList(distinct));retain.add(incoming);check(retain.snapshot().size()==2,"independent or conflicting camera hidden: "+distinct.id);
   }
   RadarCatalogMerge ambiguous=new RadarCatalogMerge(Arrays.asList(p("north",46.74826,"Radar fixe",80,0d,"lufop",false),p("south",46.74820,"Radar fixe",80,180d,"lufop",false)));ambiguous.add(incoming);check(ambiguous.snapshot().size()==3,"unknown direction arbitrarily matched one of two cameras");
   RadarCatalogMerge opposite=new RadarCatalogMerge(Collections.singletonList(p("north",46.74826,"Radar fixe",80,0d,"lufop",false)));opposite.add(p("lufop-opposite",46.74810,"Radar fixe",80,180d,"Lufop ASC",false));check(opposite.snapshot().size()==2,"opposite orientations merged");
   List<BundledRadarCatalog.Point> active=UpdatedRadarCatalog.active(context,true,true);int fixedCopies=0,zoneCopies=0;Set<String> ids=new HashSet<>();for(BundledRadarCatalog.Point p:active){ids.add(p.id);if(p.id.equals(fixed.id)||p.id.equals(lf.id))fixedCopies++;if(p.id.equals(zone.id)||p.id.equals(lz.id))zoneCopies++;}
   check(fixedCopies==1&&zoneCopies==1,"full catalog still duplicates screenshot observations");for(BundledRadarCatalog.Point p:base)check(ids.contains(p.id),"original FR record removed");
   check(!RadarIdentity.type("Contrôle routier").equals(RadarIdentity.type("Radar fixe")),"police control acquired fixed-camera identity");
   System.out.println("PASS user screenshot catalog regression: "+checks+" checks; both real Champagnole duplicates reconciled, all FR originals retained, conflicts/ambiguity preserved");
  }finally{LufopEuropeCatalog.invalidate();try(java.util.stream.Stream<Path> files=Files.walk(temp.toPath())){files.sorted(Comparator.reverseOrder()).forEach(f->{try{Files.delete(f);}catch(IOException e){throw new UncheckedIOException(e);}});}}
 }
}
