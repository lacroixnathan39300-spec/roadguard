package fr.roadguard.app;

import android.content.*;
import android.content.res.AssetManager;
import android.location.Location;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Real catalog parsers and real bundled files, with explicit JVM adapters for Android boundaries. */
public final class TestCatalogCore {
  static int scenarios,assertions;
  static void check(boolean value,String label){assertions++;if(!value)throw new AssertionError(label);}
  static final class Prefs implements SharedPreferences {
    final Map<String,Object> values=new HashMap<>();
    public synchronized Map<String,?> getAll(){return new HashMap<>(values);}
    public synchronized long getLong(String key,long fallback){return values.containsKey(key)?((Number)values.get(key)).longValue():fallback;}
    public synchronized int getInt(String key,int fallback){return values.containsKey(key)?((Number)values.get(key)).intValue():fallback;}
    public synchronized boolean getBoolean(String key,boolean fallback){return values.containsKey(key)?(Boolean)values.get(key):fallback;}
    public synchronized String getString(String key,String fallback){return values.containsKey(key)?(String)values.get(key):fallback;}
    public Editor edit(){return new Editor(){
      final Map<String,Object> pending=new HashMap<>();boolean clear;
      public Editor putLong(String key,long value){pending.put(key,value);return this;}
      public Editor putInt(String key,int value){pending.put(key,value);return this;}
      public Editor putBoolean(String key,boolean value){pending.put(key,value);return this;}
      public Editor putString(String key,String value){pending.put(key,value);return this;}
      public Editor clear(){clear=true;return this;}
      public boolean commit(){synchronized(Prefs.this){if(clear)values.clear();values.putAll(pending);}return true;}
      public void apply(){commit();}
    };}
  }
  static final class FilesContext extends Context {
    final File directory;final AssetManager assets;final Map<String,Prefs> preferences=new HashMap<>();
    FilesContext(File directory,File assets){this.directory=directory;this.assets=new AssetManager(assets);}
    public File getFilesDir(){return directory;}public AssetManager getAssets(){return assets;}
    public synchronized SharedPreferences getSharedPreferences(String name,int mode){if(!preferences.containsKey(name))preferences.put(name,new Prefs());return preferences.get(name);}
  }
  static Set<String> ids(List<BundledRadarCatalog.Point> points){Set<String> result=new HashSet<>();for(BundledRadarCatalog.Point p:points)result.add(p.id);return result;}
  static void retained(List<BundledRadarCatalog.Point> originals,List<BundledRadarCatalog.Point> combined){Set<String> present=ids(combined);for(BundledRadarCatalog.Point p:originals)check(present.contains(p.id),"original removed: "+p.id);}
  static void write(File path,String text)throws Exception{Files.write(path.toPath(),text.getBytes("UTF-8"));}
  public static void main(String[] args)throws Exception{
    File directory=Files.createTempDirectory("roadguard-catalog-test-").toFile();FilesContext context=new FilesContext(directory,new File(args[0]));
    try{
      List<BundledRadarCatalog.Point> baseEurope=LufopEuropeCatalog.all(context);
      List<BundledRadarCatalog.Point> fr=BundledRadarCatalog.france(context.getAssets()::open),ch=BundledRadarCatalog.switzerland(context.getAssets()::open);
      List<BundledRadarCatalog.Point> initial=UpdatedRadarCatalog.active(context,true,true);
      retained(fr,initial);retained(ch,initial);check(baseEurope.size()>80000,"real Europe inventory loaded");scenarios++;
      File updates=UpdatedRadarCatalog.directory(context);check(updates.mkdirs(),"create update directory");
      File overlay=new File(updates,"lufop_europe_asc_overlay.csv");
      String header="country;type;lat;lon;speed_kmh;id;label\n";
      write(overlay,header+"FR;radar_fixe;0.123456;-150.87654;0;unit-test-only;Synthetic test fixture\n");LufopEuropeCatalog.invalidate();
      List<BundledRadarCatalog.Point> partial=LufopEuropeCatalog.all(context);
      retained(baseEurope,partial);check(partial.size()==baseEurope.size()+1,"partial import must ADD to original Europe");check(ids(partial).contains("lufop-unit-test-only"),"new overlay missing");scenarios++;
      BundledRadarCatalog.Point first=baseEurope.get(0);
      write(overlay,header+first.country+";"+first.category+";"+first.lat+";"+first.lon+";"+first.speedKmh+";duplicate-test;duplicate\n");LufopEuropeCatalog.invalidate();
      check(LufopEuropeCatalog.all(context).size()==baseEurope.size(),"duplicate supplemental point added");scenarios++;
      File backup=new File(updates,"lufop_europe_asc_overlay.backup"),older=new File(updates,"lufop_europe_asc_overlay.backup.previous");
      write(overlay,"broken");write(backup,header+"CH;radar_fixe;0.123456;-150.87654;0;backup-unit-test;Synthetic fixture\n");LufopEuropeCatalog.invalidate();
      List<BundledRadarCatalog.Point> restored=LufopEuropeCatalog.all(context);retained(baseEurope,restored);check(ids(restored).contains("lufop-backup-unit-test"),"known-good backup not used");scenarios++;
      write(backup,"broken backup");write(older,header+"DE;radar_fixe;0.123456;-150.87654;0;older-unit-test;Synthetic fixture\n");LufopEuropeCatalog.invalidate();
      check(ids(LufopEuropeCatalog.all(context)).contains("lufop-older-unit-test"),"second backup not used");scenarios++;
      write(older,"also broken");LufopEuropeCatalog.invalidate();retained(baseEurope,LufopEuropeCatalog.all(context));check(LufopEuropeCatalog.all(context).size()==baseEurope.size(),"damaged updates disabled originals");scenarios++;
      overlay.delete();backup.delete();older.delete();LufopEuropeCatalog.invalidate();
      File authorized=new File(updates,"authorized_radars_fr.csv");
      String authorizedHeader="id;type;lat;lon;country;category;speed_kmh;direction_deg;status;sources;source_ids;speed_conflicts\n";
      Set<String> redPositions=new HashSet<>();for(BundledRadarCatalog.Point p:fr)if(RadarAlertPolicy.isRedLight(p.type))redPositions.add(AlertMilestones.pointKey(p.lat,p.lon));
      BundledRadarCatalog.Point colocated=null;for(BundledRadarCatalog.Point p:fr)if(!RadarAlertPolicy.isRedLight(p.type)&&!redPositions.contains(AlertMilestones.pointKey(p.lat,p.lon))){colocated=p;break;}
      check(colocated!=null,"co-located type fixture");
      write(authorized,authorizedHeader+"rg-01234567890123456789;Radar feu rouge;"+colocated.lat+";"+colocated.lon+";FR;redlight;0;;inventory;unit-test;test:1;\n");
      List<BundledRadarCatalog.Point> authorizedActive=UpdatedRadarCatalog.active(context,true,true);retained(fr,authorizedActive);retained(ch,authorizedActive);
      check(ids(authorizedActive).contains("rg-01234567890123456789"),"another camera type at one position was removed");scenarios++;
      write(authorized,"broken");List<BundledRadarCatalog.Point> afterCorruption=UpdatedRadarCatalog.active(context,true,true);retained(fr,afterCorruption);retained(ch,afterCorruption);scenarios++;
      String revision=RadarCatalogRevision.current(context);RadarCatalogRevision.changed(context);check(!revision.equals(RadarCatalogRevision.current(context)),"revision event ignored");scenarios++;
      revision=RadarCatalogRevision.current(context);context.getSharedPreferences("radar_country_visibility",0).edit().putBoolean("FR",false).commit();check(!revision.equals(RadarCatalogRevision.current(context)),"visibility not in revision");
      List<BundledRadarCatalog.Point> hidden=UpdatedRadarCatalog.active(context,true,true);retained(fr,hidden);retained(ch,hidden);for(BundledRadarCatalog.Point p:hidden)check(!("FR".equals(p.country)&&"Lufop ASC".equals(p.sources)),"disabled French Lufop point active");scenarios++;
      SharedPreferences settings=context.getSharedPreferences("MainActivity",0);revision=RadarCatalogRevision.current(context);settings.edit().putBoolean(BundledRadarCatalog.HISTORICAL_PREFERENCE,true).commit();check(!revision.equals(RadarCatalogRevision.current(context)),"historical flag not in revision");scenarios++;
      Location fix=new Location("unit-test");fix.setLatitude(46.75);fix.setLongitude(5.9);fix.setAccuracy(5);fix.setTime(100000);fix.setElapsedRealtimeNanos(100000000000L);
      check(AlertPresentation.fresh(fix,105000,105000000000L),"fresh fix refused");check(!AlertPresentation.fresh(fix,116000,116000000000L),"stale fix accepted");check(!AlertPresentation.fresh(fix,94000,105000000000L),"future wall clock fix accepted");check(!AlertPresentation.fresh(fix,105000,99000000000L),"future monotonic fix accepted");fix.setAccuracy(Float.NaN);check(!AlertPresentation.fresh(fix,105000,105000000000L),"invalid accuracy");scenarios++;
      check(AlertMilestones.claim(context,46.75,5.9,"Radar fixe",6),"first milestone refused");check(!AlertMilestones.claim(context,46.75,5.9,"ETF",6),"same warning repeated");check(AlertMilestones.claim(context,46.75,5.9,"Radar feu rouge",6),"different type suppressed");check(AlertMilestones.claim(context,46.75,5.9,"Radar fixe",7),"closer milestone refused");scenarios++;
      check(AlertMilestones.claimKind(context,46.76,5.9,"Radar fixe",1)==AlertMilestones.FIRST,"first type announcement missing");
      check(AlertMilestones.claimKind(context,46.76,5.9,"ETF",1)==AlertMilestones.NONE,"first stage repeated");
      check(AlertMilestones.claimKind(context,46.76,5.9,"Radar fixe",2)==AlertMilestones.FOLLOWUP,"distance-only follow-up missing");scenarios++;
      ExecutorService pool=Executors.newFixedThreadPool(4);try{List<Future<Boolean>> futures=new ArrayList<>();for(int i=0;i<8;i++)futures.add(pool.submit(()->AlertMilestones.claim(context,46.75,6.2,"Radar fixe",6)));int claims=0;for(Future<Boolean> f:futures)if(f.get())claims++;check(claims==1,"simultaneous foreground/background milestone claims");}finally{pool.shutdownNow();}scenarios++;
      System.out.println("PASS catalog/policy core: "+scenarios+" scenarios, "+assertions+" assertions; real bundled files (Europe="+baseEurope.size()+", FR="+fr.size()+", CH="+ch.size()+", active="+initial.size()+"); JVM Android adapters, NOT Android runtime");
    }finally{LufopEuropeCatalog.invalidate();try(java.util.stream.Stream<Path> paths=Files.walk(directory.toPath())){paths.sorted(Comparator.reverseOrder()).forEach(p->{try{Files.deleteIfExists(p);}catch(IOException error){throw new UncheckedIOException(error);}});}}
  }
}
