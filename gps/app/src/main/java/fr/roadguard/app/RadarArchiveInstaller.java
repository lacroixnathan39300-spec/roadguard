package fr.roadguard.app;

import java.io.*;
import java.util.List;
import java.util.Locale;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;

/** Verified archive -> bounded extraction -> read-only SQLite -> atomic additive overlay. */
final class RadarArchiveInstaller {
  /** FR/CH seeds are shipped with the app; other countries may use the downloader. */
  static File bundled(RadarDownloadSource source,BundledRadarCatalog.Source assets,File folder,
      CanonicalRadarStore.Cancellation cancellation)throws IOException{
    String name=source.country.toLowerCase(Locale.ROOT)+"-camera.sqlite.7z";InputStream input;
    try{input=assets.open("radar_archives/"+name);}catch(FileNotFoundException absent){return null;}
    if(!folder.isDirectory()&&!folder.mkdirs()){input.close();throw new IOException("Stockage des archives inaccessible");}
    File pending=null;
    try(InputStream in=input){
      pending=File.createTempFile("bundled-",".pending",folder);
      try(FileOutputStream out=new FileOutputStream(pending)){
        long total=0;byte[] bytes=new byte[16384];int n;
        while((n=in.read(bytes))!=-1){cancellation.check();total+=n;if(total>source.bytes)throw new IOException("Archive intégrée trop volumineuse");out.write(bytes,0,n);}
        out.getFD().sync();
      }
      if(!source.verify(pending))throw new IOException("Empreinte de l’archive intégrée incorrecte");
      cancellation.check();File target=new File(folder,name);
      Files.move(pending.toPath(),target.toPath(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);return target;
    }finally{if(pending!=null)pending.delete();}
  }
  static int install(File archive,RadarDownloadSource source,File updates,File temporary,
      CanonicalRadarStore.Cancellation cancellation)throws IOException{
    return install(archive,source,updates,temporary,cancellation,null);
  }
  static int install(File archive,RadarDownloadSource source,File updates,File temporary,
      CanonicalRadarStore.Cancellation cancellation,BundledRadarCatalog.Source assets)throws IOException{
    cancellation.check();if(source==null||!source.verify(archive))throw new IOException("Archive différente de la source vérifiée");
    File sqlite=verifiedSeed(source.country,assets,temporary,cancellation);
    if(sqlite==null)sqlite=RadarSevenZipExtractor.extract(archive,source.country,temporary,cancellation);
    try{
      List<CanonicalRadar> records=SpeedAlertSqliteImporter.read(sqlite,source.country,
          "SpeedAlert_Databases europe-202606","2026-06-04","release_sha256_verifie",cancellation);
      // The upstream type "speed" does not distinguish fixed/mobile or provide a limit.
      // Preserve it as UNKNOWN; never fabricate a subtype, speed or direction.
      CanonicalRadarStore.install(updates,source.country,records,cancellation);return records.size();
    }finally{sqlite.delete();}
  }
  /** Exact SQLite payloads from the pinned FR/CH archives, included for Android
   * builds whose Java compiler cannot package a local 7z JAR. Never use a seed
   * for an unverified archive or an unknown country. */
  private static File verifiedSeed(String country,BundledRadarCatalog.Source assets,File temporary,
      CanonicalRadarStore.Cancellation cancellation)throws IOException{
    String name,expected;
    if("FR".equals(country)){name="fr-camera.sqlite";expected="005f06a40f6a22c849fbfe44bd5106b3d2b2f498a91aff0552789b788b87073c";}
    else if("CH".equals(country)){name="ch-camera.sqlite";expected="d72c64463a8bde8edf25ec60d9821c802af6b468840894cb7470b70263047a4f";}
    else return null;
    if(assets==null)return null;
    if(!temporary.isDirectory()&&!temporary.mkdirs())throw new IOException("Stockage temporaire inaccessible");
    File output=File.createTempFile(country.toLowerCase(Locale.ROOT)+"-seed-",".sqlite",temporary);
    boolean complete=false;
    try(InputStream in=assets.open("radar_sqlite/"+name);FileOutputStream out=new FileOutputStream(output)){
      MessageDigest digest=MessageDigest.getInstance("SHA-256");byte[] buffer=new byte[16384];int n;long size=0;
      while((n=in.read(buffer))!=-1){cancellation.check();size+=n;
        if(size>CanonicalRadarStore.MAX_BYTES)throw new IOException("Base radar intégrée trop volumineuse");
        digest.update(buffer,0,n);out.write(buffer,0,n);
      }
      out.getFD().sync();StringBuilder hash=new StringBuilder();for(byte b:digest.digest())hash.append(String.format(Locale.ROOT,"%02x",b&255));
      if(size<512||!expected.contentEquals(hash))throw new IOException("Base radar intégrée altérée");
      try(InputStream check=new FileInputStream(output)){if(!SpeedAlertSqliteImporter.sqliteHeader(check))throw new IOException("Base radar intégrée invalide");}
      complete=true;return output;
    }catch(java.security.NoSuchAlgorithmException impossible){throw new IOException("SHA-256 indisponible",impossible);}
    finally{if(!complete)output.delete();}
  }
  private RadarArchiveInstaller(){}
}
