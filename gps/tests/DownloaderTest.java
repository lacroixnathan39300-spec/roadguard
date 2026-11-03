package fr.roadguard.app;
import com.sun.net.httpserver.HttpServer;
import java.io.*;import java.net.*;import java.nio.file.*;import java.security.*;
public class DownloaderTest {
 static String sha(byte[] b)throws Exception{byte[] d=MessageDigest.getInstance("SHA-256").digest(b);StringBuilder s=new StringBuilder();for(byte x:d)s.append(String.format("%02x",x&255));return s.toString();}
 public static void main(String[] args)throws Exception{
  byte[] bytes=new byte[50000];for(int i=0;i<bytes.length;i++)bytes[i]=(byte)i;
  HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  server.createContext("/pack",e->{e.sendResponseHeaders(200,bytes.length);try(OutputStream o=e.getResponseBody()){o.write(bytes);}});
  server.createContext("/redirect",e->{e.getResponseHeaders().add("Location","/pack");e.sendResponseHeaders(302,-1);e.close();});server.start();
  File root=Files.createTempDirectory("rg-download-test").toFile();
  try{
   OfflineCellStore store=new OfflineCellStore(root);String base="http://127.0.0.1:"+server.getAddress().getPort();
   VerifiedMapDownloader.Pack pack=new VerifiedMapDownloader.Pack("5km:1:2",base+"/pack",sha(bytes),100000);
   if(!VerifiedMapDownloader.download(store,pack,true)||!store.hasVerified(pack.id))throw new AssertionError("download failed");
   if(VerifiedMapDownloader.download(store,pack,true))throw new AssertionError("duplicate download");
   if(!new OfflineCellStore(root).hasVerified(pack.id))throw new AssertionError("not persistent");
   try{VerifiedMapDownloader.download(store,new VerifiedMapDownloader.Pack("bad",base+"/pack",sha(new byte[]{1}),100000),true);throw new AssertionError("bad digest accepted");}catch(IOException expected){}
   if(store.count()!=1)throw new AssertionError("bad checksum registered");
   try{VerifiedMapDownloader.download(store,new VerifiedMapDownloader.Pack("small",base+"/pack",sha(bytes),10),true);throw new AssertionError("size accepted");}catch(IOException expected){}
   try{VerifiedMapDownloader.download(store,new VerifiedMapDownloader.Pack("redirect",base+"/redirect",sha(bytes),100000),true);throw new AssertionError("redirect accepted");}catch(IOException expected){}
   try{VerifiedMapDownloader.download(store,new VerifiedMapDownloader.Pack("http",base+"/pack",sha(bytes),100000));throw new AssertionError("http accepted");}catch(IOException expected){}
   System.out.println("PASS: HTTP loopback test, checksum, persistence, deduplication, size, redirects, HTTPS-only production");
  }finally{server.stop(0);}
 }
}
