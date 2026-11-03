package fr.roadguard.app;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

/** One active HTTP read; invalidation closes even a connection waiting for response headers. */
final class LatestRouteHttp {
  interface Factory { HttpURLConnection open(URL url)throws IOException; }
  private final Factory factory;
  private int generation;
  private HttpURLConnection active;
  LatestRouteHttp(){this(url->(HttpURLConnection)url.openConnection());}
  LatestRouteHttp(Factory factory){this.factory=factory;}
  void invalidate(int token){
    HttpURLConnection previous;
    synchronized(this){generation=token;previous=active;active=null;}
    if(previous!=null)try{previous.disconnect();}catch(RuntimeException ignored){}
  }
  private synchronized void check(int token,HttpURLConnection connection)throws InterruptedIOException{
    if(Thread.currentThread().isInterrupted()||token!=generation||connection!=null&&active!=connection)throw new InterruptedIOException("Calcul remplacé");
  }
  String read(String address,int token,int limit)throws IOException{
    check(token,null);
    URL url=new URL(address);
    if(!RouteRequestPolicy.isSafeRouteRequest(address)||limit<=0||limit>1500000)throw new IOException("Adresse de calcul invalide");
    HttpURLConnection connection=factory.open(url);
    synchronized(this){
      if(token!=generation||Thread.currentThread().isInterrupted()){connection.disconnect();throw new InterruptedIOException("Calcul remplacé");}
      if(active!=null){connection.disconnect();throw new IOException("Calcul déjà actif");}
      active=connection;
    }
    try{
      connection.setConnectTimeout(9000);connection.setReadTimeout(12000);connection.setInstanceFollowRedirects(false);
      connection.setRequestProperty("Accept","application/json");connection.setRequestProperty("User-Agent","ROADGUARD route");
      check(token,connection);int status=connection.getResponseCode();check(token,connection);
      if(!RouteRequestPolicy.isSafeResponse(status,connection.getContentType(),connection.getContentLengthLong()))throw new IOException("Réponse de calcul invalide (HTTP "+status+")");
      if(connection.getContentLengthLong()>limit)throw new IOException("Réponse de calcul trop volumineuse");
      try(InputStream in=connection.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
        byte[] buffer=new byte[4096];int n;
        while(true){check(token,connection);n=in.read(buffer);check(token,connection);if(n<0)break;if(out.size()+n>limit)throw new IOException("Réponse de calcul trop volumineuse");out.write(buffer,0,n);}
        return new String(out.toByteArray(),StandardCharsets.UTF_8);
      }
    }finally{
      synchronized(this){if(active==connection)active=null;}
      connection.disconnect();
    }
  }
}
