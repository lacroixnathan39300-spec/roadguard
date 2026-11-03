package fr.roadguard.app;
import java.io.*;
import java.util.*;

/** Shared bounded local-first tile store. Only completed map downloads are read. */
public final class DownloadedMapStore {
  public static final byte[] TRANSPARENT=java.util.Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAAC0lEQVR4nGNgAAIAAAUAAXpeqz8AAAAASUVORK5CYII=");
  private final File files;private final VisibleTileCache rasterCache;
  private long stamp=Long.MIN_VALUE;private List<InstalledPmtilesMosaic.Region> regions=Collections.emptyList();
  private final LinkedHashMap<String,PmtilesReader> readers=new LinkedHashMap<String,PmtilesReader>(8,.75f,true){protected boolean removeEldestEntry(Map.Entry<String,PmtilesReader> e){return size()>8;}};
  private final LinkedHashMap<String,byte[]> tiles=new LinkedHashMap<>(32,.75f,true);private long tileBytes;
  private final Set<String> absent=new LinkedHashSet<>();
  public DownloadedMapStore(File files,File cache){this(files,new VisibleTileCache(new File(cache,"visible-tiles")));}
  DownloadedMapStore(File files,VisibleTileCache cache){this.files=files;rasterCache=cache;}
  private synchronized void refresh(){File legacy=new File(files,"pmtiles"),verified=new File(files,"offline-cells");long s=31*legacy.lastModified()+verified.lastModified()+new File(verified,"completed-sectors.tsv").lastModified();if(stamp!=s){regions=InstalledPmtilesMosaic.installed(legacy,verified);stamp=s;readers.clear();tiles.clear();absent.clear();tileBytes=0;}}
  public synchronized long revision(){refresh();return stamp;}
  public synchronized boolean hasVectors(){refresh();return !regions.isEmpty();}
  public synchronized int maxZoom(){refresh();int z=22;for(InstalledPmtilesMosaic.Region r:regions)z=Math.min(z,r.maxZoom);return regions.isEmpty()?14:z;}
  public synchronized byte[] vector(int z,int x,int y){
    refresh();if(z<0||z>22||x<0||y<0||x>=(1<<z)||y>=(1<<z))return null;String key=z+"/"+x+"/"+y;
    byte[] cached=tiles.get(key);if(cached!=null)return cached;if(absent.contains(key))return null;
    for(InstalledPmtilesMosaic.Region r:InstalledPmtilesMosaic.candidates(regions,z,x,y))try{
      String id=r.file.getAbsolutePath()+":"+r.file.length()+":"+r.file.lastModified();PmtilesReader reader=readers.get(id);if(reader==null){reader=new PmtilesReader(r.file);readers.put(id,reader);}byte[] data=reader.vectorTile(z,x,y);
      if(data!=null&&data.length>0){if(data.length<=8*1024*1024){tiles.put(key,data);tileBytes+=data.length;while(tileBytes>8L*1024*1024||tiles.size()>32){Iterator<Map.Entry<String,byte[]>> i=tiles.entrySet().iterator();Map.Entry<String,byte[]> e=i.next();tileBytes-=e.getValue().length;i.remove();}}return data;}
    }catch(IOException unavailable){}
    absent.add(key);if(absent.size()>256)absent.remove(absent.iterator().next());return null;
  }
  public boolean covers(int z,int x,int y){if(z<6||!hasVectors())return false;int max=maxZoom();if(z>max){x>>=z-max;y>>=z-max;z=max;}return vector(z,x,y)!=null;}
  public static final class Raster {public final String mime;public final InputStream body;Raster(String m,InputStream b){mime=m;body=b;}}
  public Raster raster(int z,int x,int y,boolean online,boolean vectorsVisible)throws IOException{
    if(z<0||z>19||x<0||y<0||x>=(1<<z)||y>=(1<<z))return null;
    if(vectorsVisible&&covers(z,x,y))return new Raster("image/png",new ByteArrayInputStream(TRANSPARENT));
    RasterTileSource.Tile t=new RasterTileSource(rasterCache,new File(files,"tiles")).get("https://tile.openstreetmap.org/"+z+"/"+x+"/"+y+".png",online);return t==null?null:new Raster(t.mime,t.body);
  }
  /** Warm a small local neighbourhood only: never downloads extra network tiles. */
  public void warm(double lat,double lon){if(!Double.isFinite(lat)||!Double.isFinite(lon)||lat< -85||lat>85||lon< -180||lon>180||!hasVectors())return;int z=Math.min(14,maxZoom());for(int level:new int[]{Math.max(0,z-2),z}){int n=1<<level,cx=(int)Math.floor((lon+180)/360*n);double r=Math.toRadians(lat);int cy=(int)Math.floor((1-Math.log(Math.tan(r)+1/Math.cos(r))/Math.PI)/2*n);for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++)if(dx!=0||dy!=0)vector(level,Math.floorMod(cx+dx,n),cy+dy);vector(level,Math.floorMod(cx,n),cy);}}
}
