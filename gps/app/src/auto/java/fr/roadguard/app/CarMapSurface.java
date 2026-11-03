package fr.roadguard.app;
import android.graphics.*;
import android.view.Surface;
import android.util.LruCache;
import androidx.car.app.*;
import java.io.File;
import java.util.*;
import java.util.concurrent.*;

/** Draws only visible tiles to the car host's map surface. UI overlays are owned by the host. */
final class CarMapSurface implements SurfaceCallback {
  final RoadguardCarScreen screen;final VisibleTileCache cache;
  final ExecutorService io=Executors.newFixedThreadPool(2);
  final LruCache<String,Bitmap> images=new LruCache<>(64);final Set<String> pending=new HashSet<>();
  Surface surface;int width,height,dpi=160,zoom=15;Rect stable=new Rect();boolean closed=false;
  CarMapSurface(RoadguardCarScreen s){screen=s;cache=new VisibleTileCache(new File(s.getCarContext().getCacheDir(),"visible-tiles"));}
  @Override public void onSurfaceAvailable(SurfaceContainer c){Surface next=c.getSurface();if(closed){release(next);return;}if(surface!=next)release(surface);surface=next;width=c.getWidth();height=c.getHeight();dpi=c.getDpi();draw();}
  @Override public void onStableAreaChanged(Rect r){stable=new Rect(r);draw();}
  @Override public void onVisibleAreaChanged(Rect r){if(stable.isEmpty())stable=new Rect(r);draw();}
  @Override public void onSurfaceDestroyed(SurfaceContainer c){Surface old=c.getSurface();release(surface);if(old!=surface)release(old);surface=null;}
  void release(Surface value){if(value!=null)try{value.release();}catch(RuntimeException ignored){}}
  void zoom(int change){zoom=Math.max(12,Math.min(18,zoom+change));draw();}
  void draw(){
    if(closed||surface==null||!surface.isValid()||width<=0||height<=0)return;
    Canvas canvas=null;try{canvas=surface.lockCanvas(null);canvas.drawColor(Color.rgb(20,28,36));Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setTypeface(Typeface.DEFAULT_BOLD);
      if(screen.fix==null){p.setColor(Color.WHITE);p.setTextSize(24);canvas.drawText("GPS en attente",Math.max(18,width/3f),height*.65f,p);return;}
      double tile=256*Math.max(1,Math.min(2,dpi/160.0)),world=tile*(1<<zoom);
      double cx=CarProjection.x(screen.fix.getLongitude(),world),cy=CarProjection.y(screen.fix.getLatitude(),world);
      double sx=stable.isEmpty()?width*.5:stable.exactCenterX(),sy=stable.isEmpty()?height*.62:stable.exactCenterY();
      int left=(int)Math.floor((cx-sx)/tile),right=(int)Math.floor((cx+width-sx)/tile),top=(int)Math.floor((cy-sy)/tile),bottom=(int)Math.floor((cy+height-sy)/tile),n=1<<zoom;
      int drawn=0;for(int ty=top;ty<=bottom;ty++)for(int tx=left;tx<=right;tx++){if(ty<0||ty>=n||drawn++>=64)continue;int xx=(tx%n+n)%n;String url="https://tile.openstreetmap.org/"+zoom+"/"+xx+"/"+ty+".png";Bitmap image=images.get(url);RectF box=new RectF((float)(sx+tx*tile-cx),(float)(sy+ty*tile-cy),(float)(sx+(tx+1)*tile-cx),(float)(sy+(ty+1)*tile-cy));
        if(image!=null){p.setColorFilter(screen.prefs.getBoolean("night_map",true)?nightFilter():null);canvas.drawBitmap(image,null,box,p);p.setColorFilter(null);}else request(url);
      }
      CarRouteData data=screen.data;if(data!=null){Path path=new Path();boolean first=true;for(double[] point:data.route){float x=(float)(sx+CarProjection.deltaX(CarProjection.x(point[1],world),cx,world)),y=(float)(sy+CarProjection.y(point[0],world)-cy);if(first){path.moveTo(x,y);first=false;}else path.lineTo(x,y);}
        p.setStyle(Paint.Style.STROKE);p.setStrokeJoin(Paint.Join.ROUND);p.setStrokeCap(Paint.Cap.ROUND);p.setColor(Color.WHITE);p.setStrokeWidth(10);canvas.drawPath(path,p);p.setColor(Color.parseColor(screen.appearance("route_color","#329dff")));p.setStrokeWidth(6);canvas.drawPath(path,p);p.setStyle(Paint.Style.FILL);
        int count=0;for(int index:data.radarIndex.query(screen.fix.getLatitude(),screen.fix.getLongitude(),15000)){CarRouteData.Radar r=data.radars.get(index);if(r.expires>0&&r.expires<=System.currentTimeMillis())continue;float x=(float)(sx+CarProjection.deltaX(CarProjection.x(r.lon,world),cx,world)),y=(float)(sy+CarProjection.y(r.lat,world)-cy);if(x<0||x>width||y<0||y>height||count++>=120)continue;p.setColor(Color.WHITE);canvas.drawCircle(x,y,8,p);p.setColor(RadarSourcePolicy.historical(r.type)?0xffffb04f:0xffef394e);canvas.drawCircle(x,y,5,p);}
      }
      p.setColor(Color.parseColor(screen.appearance("accent","#ff304f")));String cursor=screen.appearance("cursor","arrow");
      canvas.save();canvas.translate((float)sx,(float)sy);canvas.rotate((float)(Double.isFinite(screen.motion.course())?screen.motion.course():0));
      if("dot".equals(cursor)){p.setColor(Color.WHITE);canvas.drawCircle(0,0,12,p);p.setColor(Color.parseColor(screen.appearance("accent","#ff304f")));canvas.drawCircle(0,0,9,p);}else{
        Path arrow=new Path();if("car".equals(cursor)){arrow.addRoundRect(-10,-18,10,18,5,5,Path.Direction.CW);}else{arrow.moveTo(0,-21);arrow.lineTo(16,16);arrow.lineTo(0,9);arrow.lineTo(-16,16);arrow.close();}
        p.setColor(Color.WHITE);p.setStrokeWidth(5);p.setStyle(Paint.Style.STROKE);canvas.drawPath(arrow,p);p.setStyle(Paint.Style.FILL);p.setColor(Color.parseColor(screen.appearance("accent","#ff304f")));canvas.drawPath(arrow,p);
      }canvas.restore();
      p.setColor(0xdd101820);canvas.drawRect(0,height-27,width,height,p);p.setColor(Color.WHITE);p.setTextSize(15);canvas.drawText("© OpenStreetMap contributors",12,height-8,p);
      double raw=screen.motion.speed(android.os.SystemClock.elapsedRealtime()),speed=GpsMotion.displaySpeed(raw,screen.prefs.getInt("speed_offset_kmh",0));
      if(screen.radarBlueActive){p.setColor(0xff39aaff);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(10);canvas.drawRoundRect(6,6,width-6,height-6,18,18,p);p.setStyle(Paint.Style.FILL);}
      if(!stable.isEmpty()){p.setColor(0xe0101820);canvas.drawRoundRect(stable.left+10,stable.bottom-57,stable.left+155,stable.bottom-10,12,12,p);p.setColor(Color.WHITE);p.setTextSize(23);canvas.drawText(Double.isFinite(speed)?String.format(Locale.FRANCE,"%.0f km/h",speed):"GPS --",stable.left+22,stable.bottom-25,p);}
    }catch(RuntimeException ignored){}finally{if(canvas!=null)try{surface.unlockCanvasAndPost(canvas);}catch(Exception ignored){}}
  }
  ColorMatrixColorFilter nightFilter(){ColorMatrix m=new ColorMatrix(new float[]{-.75f,0,0,0,220,0,-.75f,0,0,225,0,0,-.65f,0,230,0,0,0,1,0});return new ColorMatrixColorFilter(m);}
  void request(String url){if(closed||surface==null||pending.size()>=32||!pending.add(url))return;
    io.execute(()->{Bitmap bitmap=null;try{VisibleTileCache.Tile t=cache.get(url,true);if(t!=null)bitmap=BitmapFactory.decodeByteArray(t.bytes,0,t.bytes.length);}catch(Exception ignored){}final Bitmap ready=bitmap;screen.main.post(()->{pending.remove(url);if(closed)return;if(ready!=null){images.put(url,ready);draw();}});});
  }
  void close(){if(closed)return;closed=true;release(surface);surface=null;io.shutdownNow();images.evictAll();pending.clear();}
}
