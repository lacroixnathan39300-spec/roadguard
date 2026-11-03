package fr.roadguard.app;

import java.io.*;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Locale;

/** Pinned public release, HTTP/size/SHA-256 verified on 2026-10-20.
 * Databases: hvdwolf/SpeedAlert_Databases, CC BY 4.0. Receiving an archive is not activation. */
final class RadarDownloadSource {
  final String country,url,sha256;final long bytes;
  RadarDownloadSource(String country,long bytes,String sha256){this.country=country;this.bytes=bytes;this.sha256=sha256;url="https://github.com/hvdwolf/SpeedAlert_Databases/releases/download/europe-202606/"+country.toLowerCase(Locale.ROOT)+"-camera.sqlite.7z";}
  private static final RadarDownloadSource[] VERIFIED={
    new RadarDownloadSource("BE",39129L,"a36efc1159b3260ae1e4494c73eea1c509cbaf9c7c1ea2cce095017445b8b72c"),
    new RadarDownloadSource("CH",16391L,"1ad92d13d8e7282db196c45f53b179af0f7578806c529e8029e27650385e5098"),
    new RadarDownloadSource("DE",134412L,"feab87c86ab121590f10e336abb379e3dc530f5a94c063e2a52a00fd63f2dc85"),
    new RadarDownloadSource("ES",63186L,"1f49ac08de0ed1db2af73618434768b07c7bdfa04103bc249f47d93bff44e197"),
    new RadarDownloadSource("FR",101736L,"5d38478a92d488668818232495bbd1ae690952ced5e409e2e69cbd3bfd6d63d1"),
    new RadarDownloadSource("IT",122237L,"59ae552cdfc242d27fe61c6b7fb47b4f97fdc10462ae4e56347a542d555b85a1"),
    new RadarDownloadSource("LU",1729L,"3c91bdd2db41c3ff6d58f66474917a613d2584189127290220ddf022cb6af109"),
  };
  static RadarDownloadSource find(String country){for(RadarDownloadSource source:VERIFIED)if(source.country.equals(country))return source;return null;}
  static boolean allowed(URL url){String host=url.getHost().toLowerCase(Locale.ROOT);return "https".equalsIgnoreCase(url.getProtocol())&&url.getUserInfo()==null&&url.getRef()==null&&(url.getPort()==-1||url.getPort()==443)&&("github.com".equals(host)||host.endsWith(".githubusercontent.com"));}
  boolean verify(File file)throws IOException{
    if(file.length()!=bytes)return false;
    try{MessageDigest digest=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(file)){byte[] buffer=new byte[16384];int n;while((n=in.read(buffer))!=-1)digest.update(buffer,0,n);}StringBuilder actual=new StringBuilder();for(byte b:digest.digest())actual.append(String.format(Locale.ROOT,"%02x",b&255));return sha256.contentEquals(actual);}
    catch(java.security.NoSuchAlgorithmException unavailable){throw new IOException("SHA-256 absent",unavailable);}
  }
}
