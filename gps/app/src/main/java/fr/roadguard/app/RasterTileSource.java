package fr.roadguard.app;
import java.io.*;
import java.net.URI;
/** Same raster address online and offline: cached bytes first, imported XYZ as a local fallback. */
final class RasterTileSource {
  static final class Tile {final String mime;final InputStream body;Tile(String mime,InputStream body){this.mime=mime;this.body=body;}}
  private final VisibleTileCache cache;private final File imports;
  RasterTileSource(VisibleTileCache cache,File imports){this.cache=cache;this.imports=imports;}
  Tile get(String address,boolean online)throws IOException{
    String url=VisibleTileCache.canonical(address);if(url==null)return null;
    if(imports!=null){
    String path=URI.create(url).getPath();path=path.substring(1,path.length()-4);
    for(String extension:new String[]{"png","jpg","jpeg"}){
      File file=new File(imports,path+"."+extension);
      if(!file.isFile()||file.length()<8||file.length()>2L*1024*1024||!file.getCanonicalPath().startsWith(imports.getCanonicalPath()+File.separator))continue;
      BufferedInputStream in=new BufferedInputStream(new FileInputStream(file));
      try{in.mark(8);byte[] magic=new byte[8];int n=in.read(magic);in.reset();
        boolean png=n==8&&(magic[0]&255)==137&&magic[1]==80&&magic[2]==78&&magic[3]==71&&magic[4]==13&&magic[5]==10&&magic[6]==26&&magic[7]==10;
        boolean jpeg=n>=3&&(magic[0]&255)==255&&(magic[1]&255)==216&&(magic[2]&255)==255;
        if("png".equals(extension)?png:jpeg)return new Tile(png?"image/png":"image/jpeg",in);
      }catch(IOException invalid){in.close();continue;}in.close();
    }
    }
    if(cache!=null){VisibleTileCache.Tile stored=cache.get(url,online);if(stored!=null)return new Tile("image/png",new ByteArrayInputStream(stored.bytes));}
    return null;
  }
}
