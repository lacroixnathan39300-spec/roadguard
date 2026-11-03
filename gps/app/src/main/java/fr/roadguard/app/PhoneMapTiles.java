package fr.roadguard.app;
import android.content.*;import java.io.*;import java.security.MessageDigest;import java.util.*;import org.json.JSONObject;
/** Only installed packs: no HTTP download or export of the provider HTTP cache. */
final class PhoneMapTiles {
 static final int MAX_BYTES=256*1024;final Context context;final SharedPreferences prefs;final DownloadedMapStore store;final File raster;final String identity;
 PhoneMapTiles(Context c){context=c;prefs=c.getSharedPreferences("MainActivity",0);store=new DownloadedMapStore(c.getFilesDir(),c.getCacheDir());raster=new File(c.getFilesDir(),"tiles");String id=prefs.getString("phone_map_identity","");if(!id.matches("[a-f0-9-]{36}")){id=UUID.randomUUID().toString();prefs.edit().putString("phone_map_identity",id).apply();}identity=id;}
 boolean allowed(){return prefs.getBoolean("phone_guidance_share",false)&&prefs.getBoolean("phone_map_share",false)&&context.getSharedPreferences("roadguard_community",0).getBoolean("approved",false);}
 String catalog(){try{store.hasVectors();String s=identity+":"+store.revision()+":"+raster.lastModified()+":"+prefs.getLong("phone_map_epoch",0);StringBuilder b=new StringBuilder();for(byte v:MessageDigest.getInstance("SHA-256").digest(s.getBytes("UTF-8")))b.append(String.format(Locale.ROOT,"%02x",v&255));return b.toString();}catch(Exception e){throw new IllegalStateException(e);}}
 String info(){try{boolean on=allowed();return new JSONObject().put("enabled",on).put("catalog",on?catalog():"").put("vectorMaxZoom",on&&store.hasVectors()?store.maxZoom():0).put("raster",on&&raster.isDirectory()).toString();}catch(Exception e){return "{}";}}
 static boolean coordinate(String kind,int z,int x,int y){return ("raster".equals(kind)||"vector".equals(kind))&&z>=0&&z<=("raster".equals(kind)?19:22)&&x>=0&&y>=0&&x<(1<<z)&&y<(1<<z);}
 byte[] tile(String expected,String kind,int z,int x,int y){if(!allowed()||expected==null||!expected.matches("[a-f0-9]{64}")||!coordinate(kind,z,x,y)||!expected.equals(catalog()))return null;
  try{byte[] data;if("vector".equals(kind))data=store.vector(z,x,y);else {RasterTileSource.Tile t=new RasterTileSource(null,raster).get("https://tile.openstreetmap.org/"+z+"/"+x+"/"+y+".png",false);if(t==null)return null;try(InputStream in=t.body;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>MAX_BYTES)return null;out.write(b,0,n);}data=out.toByteArray();}}
   return data!=null&&data.length>0&&data.length<=MAX_BYTES&&allowed()&&expected.equals(catalog())?data:null;
  }catch(Exception unavailable){return null;}
 }
}
