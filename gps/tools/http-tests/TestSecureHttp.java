package fr.roadguard.app;

import java.io.*;
import java.net.*;
import java.util.*;

/** An Android-style strict connection rejects headers changed after response access. */
public final class TestSecureHttp {
  static final class StrictConnection extends HttpURLConnection {
    boolean closed,failResponse,checked;int result=200,afterConnectWrites;
    Map<String,String> headers=new HashMap<>();
    StrictConnection()throws Exception{super(new URL("https://example.test/radars.geojson"));}
    @Override public void setRequestProperty(String key,String value){if(connected){afterConnectWrites++;throw new IllegalStateException("Cannot set request property after connection is made");}headers.put(key,value);}
    @Override public int getResponseCode()throws IOException{checked=true;connected=true;if(failResponse)throw new IOException("TLS unavailable");return result;}
    @Override public InputStream getInputStream(){if(!connected)throw new AssertionError("Response was not checked");return new ByteArrayInputStream("radar-data".getBytes());}
    @Override public void connect(){connected=true;}
    @Override public void disconnect(){closed=true;}
    @Override public boolean usingProxy(){return false;}
  }
  static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
  public static void main(String[] args)throws Exception{
    StrictConnection legacy=new StrictConnection();legacy.getResponseCode();boolean reproduced=false;try{legacy.setRequestProperty("User-Agent","ROADGUARD");}catch(IllegalStateException expected){reproduced=true;}check(reproduced,"Original Android failure reproduced");
    StrictConnection success=new StrictConnection();HttpURLConnection result=SecureHttp.open("https://example.test/radars.geojson",20000,60000,"ROADGUARD Android radar download",u->success);
    check(result==success&&success.checked&&!success.closed,"Successful checked connection remains readable");check(success.afterConnectWrites==0,"No late header write");check(success.getConnectTimeout()==20000&&success.getReadTimeout()==60000,"MapAtlas timeouts installed before request");check(!success.getInstanceFollowRedirects(),"Redirects remain disabled");check("GET".equals(success.getRequestMethod()),"GET configured");check("ROADGUARD Android radar download".equals(success.headers.get("User-Agent")),"User agent configured before connecting");check(success.getInputStream().read()=='r',"Data can be read");result.disconnect();check(success.closed,"Caller can close successful request");
    for(int code:new int[]{301,403,404,429,500}){StrictConnection failed=new StrictConnection();failed.result=code;try{SecureHttp.open("https://example.test/file",15000,30000,"ROADGUARD",u->failed);throw new AssertionError("HTTP "+code+" accepted");}catch(IOException expected){check(expected.getMessage().equals("Serveur HTTP "+code),"Stable status error");}check(failed.closed,"HTTP error releases connection");}
    StrictConnection transport=new StrictConnection();transport.failResponse=true;try{SecureHttp.open("https://example.test/file",15000,30000,"ROADGUARD",u->transport);throw new AssertionError("Transport failure accepted");}catch(IOException expected){}check(transport.closed,"Transport error releases connection");
    final int[] opened={0};for(String address:new String[]{"http://example.test/file","https://user:secret@example.test/file"}){try{SecureHttp.open(address,15000,30000,"ROADGUARD",u->{opened[0]++;return success;});throw new AssertionError("Unsafe address accepted");}catch(IOException expected){}}check(opened[0]==0,"Unsafe address rejected before opening");
    System.out.println("PASS secure HTTP: original late-header failure reproduced; corrected MapAtlas request readable; timeout/header order, HTTP errors, TLS failure, close and HTTPS guards verified.");
  }
}
