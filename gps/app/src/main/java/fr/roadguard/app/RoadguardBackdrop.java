package fr.roadguard.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import java.io.InputStream;

/** Shared, center-cropped supplied scene. One decoded bitmap for every panel. */
final class RoadguardBackdrop extends Drawable {
  private static Bitmap scene;
  private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
  private final int scrim;
  private final boolean illustrated;
  RoadguardBackdrop(Context context,int scrim){
    this.scrim=scrim;this.illustrated=context.getSharedPreferences("roadguard_community",0).getBoolean("menu_photo",true);
    if(scene==null){
      try(InputStream input=context.getAssets().open("roadguard_home_scene.png")){
        BitmapFactory.Options options=new BitmapFactory.Options();options.inScaled=false;
        scene=BitmapFactory.decodeStream(input,null,options);
      }catch(java.io.IOException ignored){/* Keep a readable dark fallback. */}
    }
  }
  @Override public void draw(Canvas canvas){
    Rect bounds=getBounds();if(bounds.isEmpty())return;
    canvas.drawColor(0xff060b12);
    if(scene!=null&&illustrated){
      float scale=Math.max((float)bounds.width()/scene.getWidth(),(float)bounds.height()/scene.getHeight());
      float width=scene.getWidth()*scale,height=scene.getHeight()*scale;
      float left=bounds.left+(bounds.width()-width)*.70f,top=bounds.top+(bounds.height()-height)*.50f;
      canvas.drawBitmap(scene,null,new RectF(left,top,left+width,top+height),paint);
    }
    canvas.drawColor(scrim);
  }
  @Override public void setAlpha(int alpha){paint.setAlpha(alpha);invalidateSelf();}
  @Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);invalidateSelf();}
  @Override public int getOpacity(){return PixelFormat.OPAQUE;}
}
