package fr.roadguard.app;
import java.io.*;
import java.nio.file.*;
public class TestRadarDownloadSources {
  public static void main(String[] args)throws Exception{
    for(String cc:new String[]{"FR","CH","DE","BE","ES","IT","LU"}){
      RadarDownloadSource source=RadarDownloadSource.find(cc);
      if(source==null||!source.verify(new File(args[0],cc.toLowerCase(java.util.Locale.ROOT)+"-camera.sqlite.7z")))throw new AssertionError(cc);
    }
    File bad=File.createTempFile("roadguard-corrupt-",".7z");try{
      byte[] bytes=Files.readAllBytes(new File(args[0],"lu-camera.sqlite.7z").toPath());bytes[bytes.length-1]^=1;Files.write(bad.toPath(),bytes);
      if(RadarDownloadSource.find("LU").verify(bad))throw new AssertionError("Same-size corruption accepted");
    }finally{bad.delete();}
    if(RadarDownloadSource.find("AL")!=null||RadarDownloadSource.allowed(new java.net.URL("https://example.com/a")))throw new AssertionError("Unverified source offered");
    System.out.println("PASS pinned sources: seven real downloaded archives, same-size corruption rejected, unverified country and host blocked");
  }
}
