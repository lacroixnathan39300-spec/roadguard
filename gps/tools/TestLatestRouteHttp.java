package fr.roadguard.app;
import java.io.*;import java.net.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;
public final class TestLatestRouteHttp {
 static int checks;
 static final String URL="https://router.project-osrm.org/route/v1/driving/5,46;6,47?overview=full";
 static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
 static class Connection extends HttpURLConnection {
  byte[] bytes="{}".getBytes();int code=200;long length=-1;volatile boolean disconnected;boolean bodyRead;
  Connection()throws Exception{super(new URL(URL));}
  public void connect(){}public boolean usingProxy(){return false;}public void disconnect(){disconnected=true;}
  public String getContentType(){return "application/json";}public int getResponseCode()throws IOException{return code;}public long getContentLengthLong(){return length;}
  public InputStream getInputStream(){bodyRead=true;return new ByteArrayInputStream(bytes);}
 }
 static void fails(LatestRouteHttp client,int token,int limit)throws Exception{try{client.read(URL,token,limit);throw new AssertionError("read should fail");}catch(IOException expected){checks++;}}
 public static void main(String[] args)throws Exception{
  Connection good=new Connection();LatestRouteHttp client=new LatestRouteHttp(u->good);check(client.read(URL,0,2).equals("{}"),"exact response boundary");check(good.disconnected&&!good.getInstanceFollowRedirects()&&good.getConnectTimeout()==9000,"connection lifetime/configuration");
  Connection big=new Connection();big.length=10;client=new LatestRouteHttp(u->big);fails(client,0,2);check(big.disconnected&&!big.bodyRead,"declared size read before rejection");
  Connection stream=new Connection();stream.bytes=new byte[10];client=new LatestRouteHttp(u->stream);fails(client,0,2);check(stream.disconnected,"observed size did not close stream");
  Connection denied=new Connection();denied.code=302;client=new LatestRouteHttp(u->denied);fails(client,0,20);check(denied.disconnected&&!denied.bodyRead,"redirect followed");
  AtomicInteger opened=new AtomicInteger();client=new LatestRouteHttp(u->{opened.incrementAndGet();return good;});client.invalidate(1);fails(client,0,20);check(opened.get()==0,"cancelled request opened a connection");
  try{client.read("https://example.com/",1,20);throw new AssertionError("wrong search host");}catch(IOException expected){check(opened.get()==0,"wrong host opened");}
  for(String bad:new String[]{"http://router.project-osrm.org/route/v1/driving/5,46;6,47","https://router.project-osrm.org:444/route/v1/driving/x","https://user@router.project-osrm.org/route/v1/driving/x","https://router.project-osrm.org/other","https://router.project-osrm.org/route/v1/driving/%2e%2e","https://router.project-osrm.org/route/v1/driving/x#fragment"}){
    try{client.read(bad,1,20);throw new AssertionError("unsafe route endpoint");}catch(IOException expected){check(opened.get()==0,"unsafe endpoint opened");}
  }
  Connection wrongType=new Connection(){@Override public String getContentType(){return "text/html";}};fails(new LatestRouteHttp(u->wrongType),0,20);check(!wrongType.bodyRead&&wrongType.disconnected,"HTML accepted as routing JSON");
  Thread.currentThread().interrupt();try{fails(client,1,20);check(opened.get()==0,"interrupted request opened");}finally{Thread.interrupted();}
  ExecutorService worker=Executors.newSingleThreadExecutor();
  CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
  Connection blocked=new Connection(){@Override public int getResponseCode()throws IOException{entered.countDown();try{if(!release.await(5,TimeUnit.SECONDS))throw new IOException("fixture timeout");}catch(InterruptedException e){Thread.currentThread().interrupt();throw new InterruptedIOException();}return 200;}@Override public void disconnect(){super.disconnect();release.countDown();}};
  LatestRouteHttp active=new LatestRouteHttp(u->blocked);active.invalidate(5);
  try{Future<Boolean> cancelled=worker.submit(()->{try{active.read(URL,5,20);return false;}catch(IOException expected){return true;}});check(entered.await(2,TimeUnit.SECONDS),"headers fixture never entered");active.invalidate(6);check(cancelled.get(2,TimeUnit.SECONDS)&&blocked.disconnected,"invalidation did not stop pending header read");}finally{release.countDown();worker.shutdownNow();}
  Connection newer=new Connection();LatestRouteHttp next=new LatestRouteHttp(u->newer);next.invalidate(10);check(next.read(URL,10,20).equals("{}"),"replacement request cannot complete");
  System.out.println("PASS latest route HTTP: "+checks+" checks; injected HTTP boundary, no external request");
 }
}
