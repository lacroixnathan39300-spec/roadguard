package fr.roadguard.app;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import fr.roadguard.vendor.commons.compress.archivers.sevenz.*;

/** Real Commons Compress/XZ archives, real SQLite, real atomic installation; no Android runtime. */
public final class TestSevenZipImport {
  static int scenarios,assertions;
  interface Work{void run()throws Exception;}
  static void check(boolean ok,String why){assertions++;if(!ok)throw new AssertionError(why);}
  static void rejected(Work work)throws Exception{try{work.run();throw new AssertionError("Invalid archive accepted");}catch(IOException expected){assertions++;}}
  static byte[] payload(){byte[] data=new byte[1024];byte[] header="SQLite format 3\0".getBytes(java.nio.charset.StandardCharsets.US_ASCII);System.arraycopy(header,0,data,0,header.length);return data;}
  static File archive(File dir,String name,String[] entries,byte[] data,SevenZMethod method)throws Exception{
    File file=new File(dir,name);try(SevenZOutputFile seven=new SevenZOutputFile(file)){
      seven.setContentCompression(method);
      for(String entryName:entries){SevenZArchiveEntry entry=new SevenZArchiveEntry();entry.setName(entryName);entry.setDirectory(entryName.endsWith("/"));seven.putArchiveEntry(entry);if(!entry.isDirectory())seven.write(data);seven.closeArchiveEntry();}
    }return file;
  }
  static void noTemps(File dir){File[] files=dir.listFiles((d,n)->n.endsWith(".sqlite"));check(files==null||files.length==0,"Leaked extracted SQLite");}
  public static void main(String[] args)throws Exception{
    File sourceDir=new File(args[0]),assets=new File(args[1]),dir=Files.createTempDirectory("roadguard-real-7z-").toFile(),tmp=new File(dir,"tmp");tmp.mkdirs();
    Map<String,Object> additions=new TreeMap<>();
    try{
      for(String country:new String[]{"FR","CH"}){
        RadarDownloadSource source=RadarDownloadSource.find(country);File input=new File(sourceDir,country.toLowerCase(Locale.ROOT)+"-camera.sqlite.7z");String hash=TestCanonicalRadar.sha(input);
        File extracted=RadarSevenZipExtractor.extract(input,country,tmp,CanonicalRadarStore.NONE);
        check(extracted.length()==("FR".equals(country)?258048:57344),"Unexpected SQLite size");
        check(TestCanonicalRadar.sha(input).equals(hash),"Source archive changed");check(!extracted.getName().equals(country.toLowerCase(Locale.ROOT)+"-camera.sqlite"),"Predictable output name");extracted.delete();scenarios++;
        File engineDir=new File(dir,country);engineDir.mkdirs();File updates=new File(engineDir,"radar_updates");
        TestCatalogCore.FilesContext context=new TestCatalogCore.FilesContext(engineDir,assets);List<BundledRadarCatalog.Point> before=UpdatedRadarCatalog.active(context,true,true);
        int count=RadarArchiveInstaller.install(input,source,updates,tmp,CanonicalRadarStore.NONE);
        check(count==("FR".equals(country)?3824:593),"Real source count mismatch");
        for(CanonicalRadar p:CanonicalRadarStore.load(updates,country).records){check(p.speedKmh==0&&p.direction==null&&p.kind==CanonicalRadar.Kind.UNKNOWN,"Unknown source speed/subtype invented");check("speed".equals(p.metadata.get("raw_type")),"Raw type lost");}
        List<BundledRadarCatalog.Point> after=UpdatedRadarCatalog.active(context,true,true);TestCatalogCore.retained(before,after);
        check(RadarActivationState.loadedCount(context,country)==0,"Import falsely marked active");String revision=RadarCatalogRevision.current(context);RadarActivationState.consumed(context,"phone",revision,after);
        int consumed=RadarActivationState.loadedCount(context,country);check(consumed==after.size()-before.size(),"Consumed unique count mismatch");additions.put(country,consumed);noTemps(tmp);scenarios++;
        File target=CanonicalRadarStore.file(updates,country);String current=TestCanonicalRadar.sha(target);
        rejected(()->RadarArchiveInstaller.install(input,source,updates,tmp,()->{throw new IOException("cancel");}));check(TestCanonicalRadar.sha(target).equals(current),"Cancelled import replaced good data");noTemps(tmp);scenarios++;
      }
      File input=new File(sourceDir,"fr-camera.sqlite.7z");
      File seedUpdates=new File(dir,"seed-updates");
      check(RadarArchiveInstaller.install(input,RadarDownloadSource.find("FR"),seedUpdates,tmp,CanonicalRadarStore.NONE,
        name->new FileInputStream(new File(assets,name)))==3824,"Bundled FR SQLite fallback failed");scenarios++;
      rejected(()->RadarArchiveInstaller.install(input,RadarDownloadSource.find("FR"),seedUpdates,tmp,CanonicalRadarStore.NONE,
        name->new ByteArrayInputStream(new byte[1024])));scenarios++;
      rejected(()->RadarSevenZipExtractor.extract(input,"CH",tmp,CanonicalRadarStore.NONE));noTemps(tmp);scenarios++;
      rejected(()->RadarSevenZipExtractor.extract(input,"../FR",tmp,CanonicalRadarStore.NONE));scenarios++;
      AtomicInteger calls=new AtomicInteger();rejected(()->RadarSevenZipExtractor.extract(input,"FR",tmp,()->{if(calls.incrementAndGet()>=4)throw new IOException("cancel during extraction");}));noTemps(tmp);scenarios++;
      File bad=archive(dir,"traversal.7z",new String[]{"../fr-camera.sqlite"},payload(),SevenZMethod.LZMA2);rejected(()->RadarSevenZipExtractor.extract(bad,"FR",tmp,CanonicalRadarStore.NONE));noTemps(tmp);check(!new File(dir,"fr-camera.sqlite").exists(),"Traversal wrote outside tmp");scenarios++;
      File absolute=archive(dir,"absolute.7z",new String[]{"/fr-camera.sqlite"},payload(),SevenZMethod.LZMA2);rejected(()->RadarSevenZipExtractor.extract(absolute,"FR",tmp,CanonicalRadarStore.NONE));noTemps(tmp);scenarios++;
      File multiple=archive(dir,"multiple.7z",new String[]{"fr-camera.sqlite","extra"},payload(),SevenZMethod.LZMA2);rejected(()->RadarSevenZipExtractor.extract(multiple,"FR",tmp,CanonicalRadarStore.NONE));noTemps(tmp);scenarios++;
      File empty=archive(dir,"empty.7z",new String[0],payload(),SevenZMethod.LZMA2);rejected(()->RadarSevenZipExtractor.extract(empty,"FR",tmp,CanonicalRadarStore.NONE));noTemps(tmp);scenarios++;
      File directory=archive(dir,"directory.7z",new String[]{"fr-camera.sqlite/"},payload(),SevenZMethod.LZMA2);rejected(()->RadarSevenZipExtractor.extract(directory,"FR",tmp,CanonicalRadarStore.NONE));noTemps(tmp);scenarios++;
      File shortFile=archive(dir,"short.7z",new String[]{"fr-camera.sqlite"},new byte[128],SevenZMethod.LZMA2);rejected(()->RadarSevenZipExtractor.extract(shortFile,"FR",tmp,CanonicalRadarStore.NONE));noTemps(tmp);scenarios++;
      File notSqlite=archive(dir,"not-sqlite.7z",new String[]{"fr-camera.sqlite"},new byte[1024],SevenZMethod.LZMA2);rejected(()->RadarSevenZipExtractor.extract(notSqlite,"FR",tmp,CanonicalRadarStore.NONE));noTemps(tmp);scenarios++;
      File unsupported=archive(dir,"deflate.7z",new String[]{"fr-camera.sqlite"},payload(),SevenZMethod.DEFLATE);rejected(()->RadarSevenZipExtractor.extract(unsupported,"FR",tmp,CanonicalRadarStore.NONE));noTemps(tmp);scenarios++;
      File memory=new File(dir,"dictionary40m.7z");
      try(SevenZOutputFile seven=new SevenZOutputFile(memory)){
        fr.roadguard.vendor.xz.LZMA2Options options=new fr.roadguard.vendor.xz.LZMA2Options();options.setDictSize(40*1024*1024);options.setMode(fr.roadguard.vendor.xz.LZMA2Options.MODE_UNCOMPRESSED);
        seven.setContentMethods(Collections.singletonList(new SevenZMethodConfiguration(SevenZMethod.LZMA2,options)));
        SevenZArchiveEntry entry=new SevenZArchiveEntry();entry.setName("fr-camera.sqlite");seven.putArchiveEntry(entry);seven.write(payload());seven.closeArchiveEntry();
      }
      rejected(()->RadarSevenZipExtractor.extract(memory,"FR",tmp,CanonicalRadarStore.NONE));noTemps(tmp);scenarios++;
      File large=archive(dir,"large.7z",new String[]{"fr-camera.sqlite"},new byte[(int)CanonicalRadarStore.MAX_BYTES+1],SevenZMethod.COPY);rejected(()->RadarSevenZipExtractor.extract(large,"FR",tmp,CanonicalRadarStore.NONE));noTemps(tmp);scenarios++;
      File corrupted=new File(dir,"corrupt.7z");byte[] bytes=Files.readAllBytes(input.toPath());bytes[40]^=1;Files.write(corrupted.toPath(),bytes);rejected(()->RadarSevenZipExtractor.extract(corrupted,"FR",tmp,CanonicalRadarStore.NONE));noTemps(tmp);scenarios++;
      File trunc=new File(dir,"truncated.7z");Files.write(trunc.toPath(),Arrays.copyOf(Files.readAllBytes(input.toPath()),1000));rejected(()->RadarSevenZipExtractor.extract(trunc,"FR",tmp,CanonicalRadarStore.NONE));noTemps(tmp);scenarios++;
      File downloads=new File(dir,"bundled");File bundled=RadarArchiveInstaller.bundled(RadarDownloadSource.find("FR"),name->new FileInputStream(new File(assets,name)),downloads,CanonicalRadarStore.NONE);check(bundled!=null&&RadarDownloadSource.find("FR").verify(bundled),"FR asset unavailable");scenarios++;
      check(RadarArchiveInstaller.bundled(RadarDownloadSource.find("DE"),name->new FileInputStream(new File(assets,name)),downloads,CanonicalRadarStore.NONE)==null,"Missing seed must allow network route");scenarios++;
      rejected(()->RadarArchiveInstaller.bundled(RadarDownloadSource.find("FR"),name->new ByteArrayInputStream(new byte[1024]),downloads,CanonicalRadarStore.NONE));check(RadarDownloadSource.find("FR").verify(bundled),"Corrupt bundled seed replaced valid archive");scenarios++;
      rejected(()->RadarArchiveInstaller.install(input,RadarDownloadSource.find("CH"),new File(dir,"wrong"),tmp,CanonicalRadarStore.NONE));noTemps(tmp);scenarios++;
      System.out.println("PASS real 7z/SQLite pipeline: "+scenarios+" scenarios / "+assertions+" assertions; FR=3824 CH=593; newly consumed after dedup="+additions+"; real libraries and files, JVM Android SQLite adapters, no Android native execution");
    }finally{try(java.util.stream.Stream<Path> paths=Files.walk(dir.toPath())){paths.sorted(Comparator.reverseOrder()).forEach(p->{try{Files.deleteIfExists(p);}catch(IOException e){throw new UncheckedIOException(e);}});}LufopEuropeCatalog.invalidate();}
  }
}
