package fr.roadguard.app;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Real SQLite fixtures and real files, through explicit JVM Android boundary adapters. */
public final class TestCanonicalRadar {
  static int scenarios,assertions;
  static void check(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
  static void expectIo(Action action)throws Exception{try{action.run();throw new AssertionError("invalid input accepted");}catch(IOException expected){}}
  interface Action {void run()throws Exception;}
  static CanonicalRadar record(String id,double latitude)throws Exception{return new CanonicalRadar("FR",id,latitude,-150.87654,CanonicalRadar.Kind.FIXED,0,null,"Unit-test local","","import_local",Collections.singletonMap("raw_type","speed_camera"));}
  static String sha(File file)throws Exception{return Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file.toPath())));}
  public static void main(String[] args)throws Exception{
    File directory=Files.createTempDirectory("roadguard-canonical-").toFile(),updates=new File(directory,"radar_updates");updates.mkdirs();File fixtures=new File(args[0]);
    try{
      CanonicalRadar first=record("1",0.123456),second=record("2",0.234567),third=record("3",0.345678);
      CanonicalRadar decoded=CanonicalRadar.decode(first.encode());check(decoded.id.equals(first.id)&&decoded.speedKmh==0&&decoded.direction==null,"unknown speed/direction changed");scenarios++;
      Map<String,String> metadata=new TreeMap<>();metadata.put("raw_type","unknown <type>;\ntexte éè");metadata.put("extra","\\n and ; and \u0000");
      CanonicalRadar raw=new CanonicalRadar("FR","99",0.456,-150.87,CanonicalRadar.Kind.UNKNOWN,0,null,"Import local","","import_local",metadata);
      check(CanonicalRadar.decode(raw.encode()).metadata.equals(metadata),"unknown metadata lost or escaped incorrectly");check(raw.point().speedKmh==0&&raw.label().contains("non renseigné"),"unknown type invented");scenarios++;
      expectIo(()->CanonicalRadar.decode(first.encode().replace(first.id,"sa-00000000000000000000000000000000")));scenarios++;
      expectIo(()->new CanonicalRadar("FR","1",Double.NaN,1,CanonicalRadar.Kind.FIXED,0,null,"local","","local",metadata));scenarios++;
      expectIo(()->new CanonicalRadar("FR","1",1,1,CanonicalRadar.Kind.FIXED,0,360d,"local","","local",metadata));scenarios++;
      File active=CanonicalRadarStore.install(updates,"FR",Arrays.asList(first),CanonicalRadarStore.NONE);
      check(CanonicalRadarStore.load(updates,"FR").records.get(0).id.equals(first.id),"initial install");scenarios++;
      String previous=sha(active);expectIo(()->CanonicalRadarStore.install(updates,"FR",Arrays.asList(first,first),CanonicalRadarStore.NONE));check(sha(active).equals(previous),"duplicate import changed current");scenarios++;
      expectIo(()->CanonicalRadarStore.install(updates,"CH",Arrays.asList(first),CanonicalRadarStore.NONE));check(sha(active).equals(previous),"wrong country changed current");scenarios++;
      expectIo(()->CanonicalRadarStore.install(updates,"FR",Arrays.asList(second),()->{throw new IOException("cancel");}));check(sha(active).equals(previous),"early cancel changed current");scenarios++;
      final int[] checks={0};expectIo(()->CanonicalRadarStore.install(updates,"FR",Arrays.asList(second),()->{if(++checks[0]>=3)throw new IOException("cancel during backup");}));check(sha(active).equals(previous),"backup cancel changed current");scenarios++;
      CanonicalRadarStore.install(updates,"FR",Arrays.asList(second),CanonicalRadarStore.NONE);CanonicalRadarStore.install(updates,"FR",Arrays.asList(third),CanonicalRadarStore.NONE);
      File backup=new File(active+".backup"),older=new File(active+".backup.previous");check(CanonicalRadarStore.read(backup,"FR").get(0).id.equals(second.id),"latest backup");check(CanonicalRadarStore.read(older,"FR").get(0).id.equals(first.id),"previous backup");scenarios++;
      Files.write(active.toPath(),"corrupt".getBytes("UTF-8"));CanonicalRadarStore.Snapshot snapshot=CanonicalRadarStore.load(updates,"FR");check(snapshot.backup&&snapshot.records.get(0).id.equals(second.id),"first fallback");scenarios++;
      Files.write(backup.toPath(),"corrupt backup".getBytes("UTF-8"));snapshot=CanonicalRadarStore.load(updates,"FR");check(snapshot.backup&&snapshot.records.get(0).id.equals(first.id),"second fallback");scenarios++;
      CanonicalRadarStore.install(updates,"FR",Arrays.asList(third),CanonicalRadarStore.NONE);check(CanonicalRadarStore.read(older,"FR").get(0).id.equals(first.id),"repair destroyed known good older backup");scenarios++;
      File[] leftovers=active.getParentFile().listFiles((dir,name)->name.endsWith(".pending"));check(leftovers!=null&&leftovers.length==0,"pending leak");scenarios++;
      File sqlite=new File(fixtures,"valid.sqlite");String sourceHash=sha(sqlite);List<CanonicalRadar> imported=SpeedAlertSqliteImporter.readLocal(sqlite,"FR",CanonicalRadarStore.NONE);
      check(imported.size()==3&&sha(sqlite).equals(sourceHash),"real SQLite count or source changed");check(imported.get(0).kind==CanonicalRadar.Kind.FIXED&&imported.get(1).kind==CanonicalRadar.Kind.RED_LIGHT&&imported.get(2).kind==CanonicalRadar.Kind.UNKNOWN,"kind mapping");
      for(CanonicalRadar item:imported)check(item.speedKmh==0&&item.direction==null&&item.metadata.containsKey("sqlite_sha256"),"invented source speed/direction or missing provenance");scenarios++;
      CanonicalRadar extra=SpeedAlertSqliteImporter.readLocal(new File(fixtures,"extra.sqlite"),"CH",CanonicalRadarStore.NONE).get(0);
      check(extra.metadata.get("sqlite.note").contains(";\n")&&"90".equals(extra.metadata.get("sqlite.speed")),"unknown SQLite columns not retained");check(extra.speedKmh==0,"unmapped speed column became a speed limit");scenarios++;
      for(String name:new String[]{"missing","wrong_table","view","empty","bad_coordinates","duplicate","text_coordinates","null_type","huge_type","blob","large","invalid"}){
        File bad=new File(fixtures,name+".sqlite");String hash=sha(bad);expectIo(()->SpeedAlertSqliteImporter.readLocal(bad,"FR",CanonicalRadarStore.NONE));check(sha(bad).equals(hash),"invalid SQLite input modified: "+name);scenarios++;
      }
      expectIo(()->SpeedAlertSqliteImporter.readLocal(sqlite,"FR",()->{throw new IOException("cancel");}));scenarios++;
      CanonicalRadarStore.install(updates,"FR",imported,CanonicalRadarStore.NONE);
      TestCatalogCore.FilesContext context=new TestCatalogCore.FilesContext(directory,new File(args[1]));
      List<BundledRadarCatalog.Point> base=BundledRadarCatalog.france(context.getAssets()::open),all=UpdatedRadarCatalog.active(context,true,true);
      TestCatalogCore.retained(base,all);for(CanonicalRadar item:imported)check(TestCatalogCore.ids(all).contains(item.id),"import not read by common catalog");scenarios++;
      BundledRadarCatalog.Point original=base.get(0);
      CanonicalRadar unspecified=new CanonicalRadar("FR","unspecified-test",original.lat,original.lon,CanonicalRadar.Kind.UNKNOWN,0,null,"Unit-test local","","import_local",Collections.singletonMap("raw_type","unspecified"));
      List<CanonicalRadar> withUnspecified=new ArrayList<>(imported);withUnspecified.add(unspecified);CanonicalRadarStore.install(updates,"FR",withUnspecified,CanonicalRadarStore.NONE);
      all=UpdatedRadarCatalog.active(context,true,true);check(!TestCatalogCore.ids(all).contains(unspecified.id),"unspecified duplicate displaced or duplicated a precise original");TestCatalogCore.retained(base,all);scenarios++;
      check(RadarActivationState.loadedCount(context,"FR")==0,"import was declared active before an engine read");String revision=RadarCatalogRevision.current(context);RadarActivationState.consumed(context,"phone",revision,all);check(RadarActivationState.loadedCount(context,"FR")==3,"engine receipt not recognized");scenarios++;
      RadarCatalogRevision.changed(context);check(RadarActivationState.loadedCount(context,"FR")==0,"stale activation receipt accepted");scenarios++;
      context.getSharedPreferences("radar_country_visibility",0).edit().putBoolean("FR",false).commit();List<BundledRadarCatalog.Point> hidden=UpdatedRadarCatalog.active(context,true,true);
      TestCatalogCore.retained(base,hidden);for(CanonicalRadar item:imported)check(!TestCatalogCore.ids(hidden).contains(item.id),"disabled supplemental country retained");scenarios++;
      System.out.println("PASS canonical/SQLite: "+scenarios+" scenarios, "+assertions+" assertions; real SQLite read-only via JVM adapter, atomic files/backups, additive engine catalog and activation receipts; Android native runtime NOT executed");
    }finally{LufopEuropeCatalog.invalidate();try(java.util.stream.Stream<Path> paths=Files.walk(directory.toPath())){paths.sorted(Comparator.reverseOrder()).forEach(p->{try{Files.deleteIfExists(p);}catch(IOException error){throw new UncheckedIOException(error);}});}}
  }
}
