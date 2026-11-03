package fr.roadguard.app;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Real merge/catalog code and real assets; Android file/preferences boundaries use JVM adapters. */
public final class TestRadarMerge {
 static int checks;
 static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
 static BundledRadarCatalog.Point point(String id,String type,boolean history,int speed,Double direction,String source,String quality){return new BundledRadarCatalog.Point(id,type,46.75,5.9,"fixed",history,source,"FR",speed,direction,quality);}
 static BundledRadarCatalog.Point merged(BundledRadarCatalog.Point a,BundledRadarCatalog.Point b){RadarCatalogMerge merge=new RadarCatalogMerge(Collections.singletonList(a));merge.add(b);check(merge.snapshot().size()==1,"same identity created another point");return merge.snapshot().get(0);}
 public static void main(String[] args)throws Exception{
  BundledRadarCatalog.Point original=point("original","Radar fixe",false,0,null,"embarquée","");
  BundledRadarCatalog.Point incoming=point("import","ETF",false,80,90d,"import", "import_local");
  BundledRadarCatalog.Point p=merged(original,incoming);
  check(p.id.equals("original")&&p.type.equals("Radar fixe")&&p.lat==original.lat,"original identity altered");
  check(p.speedKmh==80&&p.direction==90d&&p.reliability.equals("import_local"),"missing metadata not filled");
  check(p.sources.equals("embarquée · import"),"corroborating sources lost");
  p=merged(p,incoming);check(p.sources.equals("embarquée · import"),"repeated imports multiply provenance");
  BundledRadarCatalog.Point known=point("original","Radar fixe",false,80,90d,"original", "verified");
  p=merged(known,point("conflict","ETF",false,90,90d,"conflict", "unknown"));
  check(p==known,"conflicting limits overwritten or falsely corroborated");
  p=merged(known,point("opposite","ETF",false,80,270d,"opposite", "unknown"));check(p==known,"opposite directions combined");
  p=merged(original,point("history","ETF",true,80,90d,"history", "unknown"));check(p==original,"historical evidence qualified active point");
  p=merged(point("wrap","ETF",false,80,359.999d,"a","a"),point("wrap2","ETF",false,80,.001d,"b","b"));check(p.sources.equals("a · b"),"north orientation wrap rejected");
  RadarCatalogMerge multiple=new RadarCatalogMerge(Collections.singletonList(original));multiple.add(point("redlight","Radar feu rouge",false,0,null,"import",""));check(multiple.snapshot().size()==2,"different co-located type discarded");
  List<BundledRadarCatalog.Point> snapshot=multiple.snapshot();multiple.add(new BundledRadarCatalog.Point("ch","Radar fixe",46.75,5.9,"fixed",false,"import","CH"));check(snapshot.size()==2&&multiple.snapshot().size()==3,"snapshot changed after publication or country identity lost");
  try{snapshot.clear();throw new AssertionError("mutable snapshot");}catch(UnsupportedOperationException expected){checks++;}
  RadarCatalogMerge retained=new RadarCatalogMerge(Arrays.asList(original,original));retained.add(incoming);check(retained.snapshot().size()==2,"pre-existing records removed");check(original.speedKmh==0&&original.direction==null,"original object mutated");
  File temp=Files.createTempDirectory("radar-merge-").toFile();
  try{
   TestCatalogCore.FilesContext context=new TestCatalogCore.FilesContext(temp,new File(args[0]));
   File updates=UpdatedRadarCatalog.directory(context);
   String revision=RadarCatalogRevision.current(context);
   context.getSharedPreferences("MainActivity",0).edit().putString("radar_country_installed","[{installed}]").commit();
   check(!revision.equals(RadarCatalogRevision.current(context)),"installed membership missing from revision");
   File legacy=new File(new File(temp,"radar_countries"),"test_fr.csv");legacy.getParentFile().mkdirs();
   revision=RadarCatalogRevision.current(context);Files.write(legacy.toPath(),"header".getBytes("UTF-8"));
   check(!revision.equals(RadarCatalogRevision.current(context)),"installed file missing from revision");
   revision=RadarCatalogRevision.current(context);Files.write(legacy.toPath(),"replacement content".getBytes("UTF-8"));
   check(!revision.equals(RadarCatalogRevision.current(context)),"replacement file missing from revision");
   revision=RadarCatalogRevision.current(context);legacy.delete();
   check(!revision.equals(RadarCatalogRevision.current(context)),"deleted file missing from revision");
   List<BundledRadarCatalog.Point> fr=BundledRadarCatalog.france(context.getAssets()::open);
   BundledRadarCatalog.Point target=null;for(BundledRadarCatalog.Point candidate:fr)if(!candidate.historical&&candidate.direction==null&&RadarIdentity.type(candidate.type).equals("fixed")){target=candidate;break;}
   check(target!=null,"real metadata fixture absent");
   CanonicalRadar record=new CanonicalRadar("FR","metadata",target.lat,target.lon,CanonicalRadar.Kind.FIXED,target.speedKmh,90d,"import test","","import_local",Collections.emptyMap());
   CanonicalRadarStore.install(updates,"FR",Collections.singletonList(record),CanonicalRadarStore.NONE);
   CanonicalRadar ch=new CanonicalRadar("CH","country",46.8,6.1,CanonicalRadar.Kind.FIXED,80,null,"test","","import_local",Collections.emptyMap());
   CanonicalRadarStore.install(updates,"CH",Collections.singletonList(ch),CanonicalRadarStore.NONE);
   Files.write(CanonicalRadarStore.file(updates,"CH").toPath(),"corrupt".getBytes("UTF-8"));
   List<BundledRadarCatalog.Point> active=UpdatedRadarCatalog.active(context,true,true);
   TestCatalogCore.retained(fr,active);
   boolean found=false;for(BundledRadarCatalog.Point a:active)if(a.id.equals(target.id)){found=true;check(a.direction!=null&&a.direction==90d&&a.reliability.contains("import_local")&&a.sources.contains("import test"),"canonical metadata lost in live additive catalog");}
   check(found,"enriched original lost");
   check(CanonicalRadarCatalog.all(context).size()==1,"damaged country discarded valid other country");
   CanonicalRadarStore.install(updates,"CH",Collections.singletonList(ch),CanonicalRadarStore.NONE);
   CanonicalRadarStore.install(updates,"CH",Collections.singletonList(ch),CanonicalRadarStore.NONE);
   Files.write(CanonicalRadarStore.file(updates,"CH").toPath(),"corrupt again".getBytes("UTF-8"));
   check(CanonicalRadarCatalog.all(context).size()==2,"backup or other country lost");
  }finally{LufopEuropeCatalog.invalidate();try(java.util.stream.Stream<Path> files=Files.walk(temp.toPath())){files.sorted(Comparator.reverseOrder()).forEach(f->{try{Files.delete(f);}catch(IOException e){throw new UncheckedIOException(e);}});}}
  System.out.println("PASS radar metadata merge / "+checks+" checks plus retention of all FR originals; real assets, JVM boundaries only");
 }
}
