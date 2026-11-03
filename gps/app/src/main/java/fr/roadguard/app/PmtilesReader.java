package fr.roadguard.app;

import java.io.*;
import java.util.zip.GZIPInputStream;

/** PMTiles v3 local random-access reader. Raster PNG/JPEG/WEBP only; no vector renderer. */
public final class PmtilesReader {
  private final File file;
  // Avoid reopening/decompressing the root directory for every WebView tile request.
  private final byte[] rootDirectory;
  private final java.util.LinkedHashMap<String,byte[]> leafCache =
      new java.util.LinkedHashMap<String,byte[]>(16,0.75f,true){
        @Override protected boolean removeEldestEntry(java.util.Map.Entry<String,byte[]> e){
          return size()>8;
        }
      };
  private final long rootOffset, rootLength, leafOffset, leafLength, dataOffset, dataLength;
  private final int internalCompression, tileCompression, tileType;
  private static final int MAX_DIRECTORY_BYTES = 16 * 1024 * 1024;
  private static final int MAX_TILE_BYTES = 8 * 1024 * 1024;
  public PmtilesReader(File file) throws IOException {
    this.file=file;
    try(RandomAccessFile raf=new RandomAccessFile(file,"r")){
      if(raf.length()<127)throw new IOException("PMTiles header too short");
      byte[] h=new byte[127];raf.readFully(h);
      if(!"PMTiles".equals(new String(h,0,7,"US-ASCII"))||(h[7]&255)!=3)throw new IOException("Not PMTiles v3");
      rootOffset=le64(h,8);rootLength=le64(h,16);leafOffset=le64(h,40);leafLength=le64(h,48);
      dataOffset=le64(h,56);dataLength=le64(h,64);
      internalCompression=h[97]&255;tileCompression=h[98]&255;tileType=h[99]&255;
      if(!range(rootOffset,rootLength,raf.length())||!range(dataOffset,dataLength,raf.length())||
         (leafLength!=0&&!range(leafOffset,leafLength,raf.length())))throw new IOException("Invalid PMTiles ranges");
      if(internalCompression<1||internalCompression>3)throw new IOException("Unsupported directory compression");
      if(tileCompression<1||tileCompression>3)throw new IOException("Unsupported tile compression");
      rootDirectory=inflate(read(raf,rootOffset,rootLength,MAX_DIRECTORY_BYTES),internalCompression,MAX_DIRECTORY_BYTES);
    }
  }
  public int tileType(){return tileType;}
  public boolean isVector(){return tileType==1;}
  public String vectorMimeType(){return isVector()?"application/vnd.mapbox-vector-tile":null;}
  /** Returns decompressed Mapbox Vector Tile protobuf bytes; does NOT render them. */
  public byte[] vectorTile(int z,int x,int y)throws IOException{
    if(!isVector())throw new IOException("Not a vector PMTiles archive");
    return tileBytes(z,x,y);
  }
  public boolean isRaster(){return tileType==2||tileType==3||tileType==4;}
  public String mimeType(){return tileType==2?"image/png":tileType==3?"image/jpeg":tileType==4?"image/webp":null;}
  private static long le64(byte[] b,int off)throws IOException{long n=0;for(int i=7;i>=0;i--)n=(n<<8)|(b[off+i]&255L);if(n<0)throw new IOException("Overflow");return n;}
  private static boolean range(long o,long n,long total){return o>=0&&n>=0&&o<=total&&n<=total-o;}
  private static byte[] read(RandomAccessFile raf,long o,long n,int max)throws IOException{
    if(n>max||!range(o,n,raf.length()))throw new IOException("Range out of bounds");
    byte[] b=new byte[(int)n];raf.seek(o);raf.readFully(b);return b;
  }
  private static byte[] inflate(byte[] raw,int codec,int max)throws IOException{
    if(codec==1)return raw;
    ByteArrayOutputStream out=new ByteArrayOutputStream();
    InputStream decoded;
    if(codec==2)decoded=new GZIPInputStream(new ByteArrayInputStream(raw));
    else if(codec==3){
      try{decoded=(InputStream)Class.forName("org.brotli.dec.BrotliInputStream").getConstructor(InputStream.class).newInstance(new ByteArrayInputStream(raw));}
      catch(Exception e){throw new IOException("Brotli decoder unavailable",e);}
    }else throw new IOException("Unsupported compression codec");
    try(InputStream in=decoded){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()>max-n)throw new IOException("Decompression limit");out.write(b,0,n);}}
    return out.toByteArray();
  }
  private static long varint(InputStream in)throws IOException{
    long n=0;for(int shift=0;shift<64;shift+=7){int b=in.read();if(b<0)throw new EOFException("Directory truncated");
      if(shift==63&&(b&0xfe)!=0)throw new IOException("Varint overflow");n|=(long)(b&127)<<shift;
      if((b&128)==0){if(n<0)throw new IOException("Varint overflow");return n;}}
    throw new IOException("Varint too long");
  }
  private static long tileId(int z,int x,int y)throws IOException{
    if(z<0||z>26||x<0||y<0||x>=(1<<z)||y>=(1<<z))throw new IOException("Tile outside zoom");
    long offset=((1L<<(2*z))-1)/3;
    int xx=x,yy=y;long d=0;for(int s=(1<<z)/2;s>0;s/=2){int rx=(xx&s)!=0?1:0,ry=(yy&s)!=0?1:0;
      d+=(long)s*s*((3*rx)^ry);if(ry==0){if(rx==1){xx=(1<<z)-1-xx;yy=(1<<z)-1-yy;}int tmp=xx;xx=yy;yy=tmp;}}
    return offset+d;
  }
  private static final class Entry{long id,offset,length,run;}
  private static Entry find(byte[] bytes,long wanted)throws IOException{
    ByteArrayInputStream in=new ByteArrayInputStream(bytes);long count=varint(in);
    if(count>1000000)throw new IOException("Too many directory entries");int size=(int)count;
    Entry[] es=new Entry[size];long id=0;
    for(int i=0;i<size;i++){long delta=varint(in);if(delta>Long.MAX_VALUE-id)throw new IOException("Tile id overflow");id+=delta;if(id<0)throw new IOException("Tile id overflow");es[i]=new Entry();es[i].id=id;}
    for(Entry e:es)e.run=varint(in);
    for(Entry e:es)e.length=varint(in);
    long prevOffset=0,prevLength=0;
    for(Entry e:es){long raw=varint(in);if(raw==0&&prevOffset>Long.MAX_VALUE-prevLength)throw new IOException("Directory offset overflow");e.offset=raw==0?prevOffset+prevLength:raw-1;prevOffset=e.offset;prevLength=e.length;}
    int lo=0,hi=size-1,idx=-1;while(lo<=hi){int m=(lo+hi)>>>1;if(es[m].id<=wanted){idx=m;lo=m+1;}else hi=m-1;}
    if(idx<0)return null;Entry e=es[idx];
    if(e.run==0)return e; // leaf directory pointer, only when this is preceding entry
    return wanted-e.id<e.run?e:null;
  }
  /** Returns null for missing tiles. Returns raw PNG/JPEG/WEBP bytes for raster archives. */
  public byte[] rasterTile(int z,int x,int y)throws IOException{
    if(!isRaster())throw new IOException("Vector PMTiles requires a vector renderer");
    return tileBytes(z,x,y);
  }
  private byte[] tileBytes(int z,int x,int y)throws IOException{
    long wanted=tileId(z,x,y);
    try(RandomAccessFile raf=new RandomAccessFile(file,"r")){
      Entry e=find(rootDirectory,wanted);if(e==null)return null;
      if(e.run==0){if(!range(e.offset,e.length,leafLength))throw new IOException("Leaf pointer invalid");
        String key=e.offset+":"+e.length;
        byte[] leaf;
        synchronized(leafCache){leaf=leafCache.get(key);}
        if(leaf==null){
          leaf=inflate(read(raf,leafOffset+e.offset,e.length,MAX_DIRECTORY_BYTES),internalCompression,MAX_DIRECTORY_BYTES);
          synchronized(leafCache){leafCache.put(key,leaf);}
        }
        e=find(leaf,wanted);if(e==null||e.run==0)return null;
      }
      if(!range(e.offset,e.length,dataLength))throw new IOException("Tile pointer invalid");
      return inflate(read(raf,dataOffset+e.offset,e.length,MAX_TILE_BYTES),tileCompression,MAX_TILE_BYTES);
    }
  }
}
