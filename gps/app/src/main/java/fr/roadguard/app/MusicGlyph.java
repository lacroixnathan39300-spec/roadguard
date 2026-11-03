package fr.roadguard.app;
import android.graphics.*;
import android.graphics.drawable.Drawable;
/** Small transport icons drawn locally, independent of font glyph availability. */
final class MusicGlyph extends Drawable {
  private final String kind;private final Paint ink=new Paint(Paint.ANTI_ALIAS_FLAG);
  MusicGlyph(String kind){this(kind,Color.WHITE);}
  MusicGlyph(String kind,int color){this.kind=kind;ink.setColor(color);}
  public void draw(Canvas canvas){Rect b=getBounds();int save=canvas.save();canvas.translate(b.exactCenterX(),b.exactCenterY());float size=Math.min(b.width(),b.height())/48f;canvas.scale(size,size);canvas.translate(-24,-24);
    if("pause".equals(kind)){canvas.drawRect(14,10,21,38,ink);canvas.drawRect(27,10,34,38,ink);}
    else if("play".equals(kind)){Path p=new Path();p.moveTo(17,10);p.lineTo(38,24);p.lineTo(17,38);p.close();canvas.drawPath(p,ink);}
    else if("previous".equals(kind)||"next".equals(kind)){if("next".equals(kind))canvas.scale(-1,1,24,24);Path p=new Path();p.moveTo(33,11);p.lineTo(16,24);p.lineTo(33,37);p.close();canvas.drawPath(p,ink);canvas.drawRect(12,11,16,37,ink);}
    else{ink.setStyle(Paint.Style.STROKE);ink.setStrokeWidth(3);canvas.drawCircle(24,24,18,ink);Path p=new Path();p.moveTo(22,31);p.lineTo(22,14);p.lineTo(34,12);p.lineTo(34,28);canvas.drawPath(p,ink);ink.setStyle(Paint.Style.FILL);canvas.drawOval(14,28,23,35,ink);canvas.drawOval(26,25,35,32,ink);}
    canvas.restoreToCount(save);
  }
  public void setAlpha(int alpha){ink.setAlpha(alpha);invalidateSelf();}public void setColorFilter(ColorFilter filter){ink.setColorFilter(filter);invalidateSelf();}public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
