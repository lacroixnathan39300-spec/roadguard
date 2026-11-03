package fr.roadguard.app;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/** Configure every GET before the first response read, which opens the connection. */
final class SecureHttp {
  interface ConnectionFactory { HttpURLConnection open(URL url) throws IOException; }
  static HttpURLConnection open(String address,int connectTimeout,int readTimeout,String userAgent)throws IOException {
    return open(address,connectTimeout,readTimeout,userAgent,url->(HttpURLConnection)url.openConnection());
  }
  static HttpURLConnection open(String address,int connectTimeout,int readTimeout,String userAgent,ConnectionFactory factory)throws IOException {
    URL url=new URL(address);
    if(!"https".equalsIgnoreCase(url.getProtocol())||url.getUserInfo()!=null)throw new IOException("HTTPS obligatoire");
    if(connectTimeout<=0||readTimeout<=0)throw new IOException("Délai de connexion invalide");
    HttpURLConnection connection=factory.open(url);
    try {
      connection.setConnectTimeout(connectTimeout);
      connection.setReadTimeout(readTimeout);
      connection.setInstanceFollowRedirects(false);
      connection.setRequestMethod("GET");
      connection.setRequestProperty("User-Agent",userAgent);
      int code=connection.getResponseCode();
      if(code!=200)throw new IOException("Serveur HTTP "+code);
      return connection;
    }catch(IOException|RuntimeException error){connection.disconnect();throw error;}
  }
  private SecureHttp(){}
}
