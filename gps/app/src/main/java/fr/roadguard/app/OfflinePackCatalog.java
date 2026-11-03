package fr.roadguard.app;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Strict, bounded, UTF-8 offline pack catalog. Only trusted, checksum-pinned HTTPS archives. */
public final class OfflinePackCatalog {
  private OfflinePackCatalog() {}
  public static List<OfflinePackRegionSelector.Region> read(InputStream source) throws IOException {
    if(source==null)throw new IOException("Missing catalog");
    ArrayList<OfflinePackRegionSelector.Region> regions=new ArrayList<>();
    HashSet<String> ids=new HashSet<>();
    try(BufferedReader reader=new BufferedReader(new InputStreamReader(new BoundedInputStream(source,1048576),StandardCharsets.UTF_8))){
      String line;int lineNo=0;
      while((line=reader.readLine())!=null){
        lineNo++;
        if(line.isEmpty()||line.startsWith("#"))continue;
        if(regions.size()>=10000)throw new IOException("Catalog exceeds region limit");
        String[] cols=line.split("\\t",-1);
        if(cols.length!=9)throw new IOException("Invalid catalog row "+lineNo);
        try {
          String id=cols[0],url=cols[1],sha=cols[2];
          if(!id.matches("[A-Za-z0-9_.:-]{1,120}")||id.equals(".")||id.equals("..")||!ids.add(id))throw new IllegalArgumentException("Duplicate/invalid ID");
          URI uri=new URI(url);
          if(!"https".equalsIgnoreCase(uri.getScheme())||uri.getHost()==null||uri.getUserInfo()!=null||uri.getFragment()!=null||url.indexOf(' ')>=0)throw new IllegalArgumentException("HTTPS URL required");
          if(!sha.matches("[a-fA-F0-9]{64}"))throw new IllegalArgumentException("Invalid SHA256");
          long max=Long.parseLong(cols[3]);
          if(max<=0||max>10_000_000_000L)throw new IllegalArgumentException("Invalid size limit");
          double south=Double.parseDouble(cols[4]),west=Double.parseDouble(cols[5]),north=Double.parseDouble(cols[6]),east=Double.parseDouble(cols[7]);
          if(!"pmtiles".equals(cols[8]))throw new IllegalArgumentException("Unsupported format");
          regions.add(new OfflinePackRegionSelector.Region(new VerifiedMapDownloader.Pack(id,url,sha.toLowerCase(Locale.ROOT),max),south,west,north,east));
        }catch(Exception e){throw new IOException("Invalid catalog row "+lineNo+": "+e.getMessage(),e);}
      }
    }
    return Collections.unmodifiableList(regions);
  }
  private static final class BoundedInputStream extends FilterInputStream {
    private long remaining;
    BoundedInputStream(InputStream in,long max){super(in);remaining=max;}
    @Override public int read()throws IOException{int b=super.read();if(b>=0&&--remaining<0)throw new IOException("Catalog too large");return b;}
    @Override public int read(byte[] b,int off,int len)throws IOException{int n=super.read(b,off,len);if(n>0&&(remaining-=n)<0)throw new IOException("Catalog too large");return n;}
  }
}
