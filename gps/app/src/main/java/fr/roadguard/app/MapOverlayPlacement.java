package fr.roadguard.app;

/** Small rectangle packing policy. All dimensions use the caller's coordinate system. */
final class MapOverlayPlacement {
  static final class Box {
    final int left,top,width,height;
    Box(int left,int top,int width,int height){this.left=left;this.top=top;this.width=Math.max(0,width);this.height=Math.max(0,height);}
    int right(){return left+width;}int bottom(){return top+height;}
    boolean overlaps(Box b,int gap){return b!=null&&width>0&&height>0&&b.width>0&&b.height>0&&left<b.right()+gap&&right()+gap>b.left&&top<b.bottom()+gap&&bottom()+gap>b.top;}
  }
  static Box floating(int width,int height,int sizeX,int sizeY,int gap,Box top,Box sheet,Box rail){
    return floating(width,height,sizeX,sizeY,gap,top,sheet,rail,false);
  }
  static Box floating(int width,int height,int sizeX,int sizeY,int gap,Box top,Box sheet,Box rail,boolean preferRight){
    if(width<=0||height<=0||sizeX<=0||sizeY<=0||sizeX+gap*2>width||sizeY+gap*2>height)return null;
    int[] xs={gap,top==null?gap:top.right()+gap,sheet==null?gap:sheet.left-sizeX-gap,rail==null?width-sizeX-gap:rail.left-sizeX-gap,width-sizeX-gap};
    if(preferRight){int first=xs[0];xs[0]=xs[xs.length-1];xs[xs.length-1]=first;}
    int[] ys={height-sizeY-gap,sheet==null?height-sizeY-gap:sheet.top-sizeY-gap,top==null?gap:top.bottom()+gap,rail==null?gap:rail.bottom()+gap,gap};
    for(int y:ys)for(int x:xs){
      Box candidate=new Box(x,y,sizeX,sizeY);
      if(x<gap||y<gap||candidate.right()>width-gap||candidate.bottom()>height-gap)continue;
      if(!candidate.overlaps(top,gap)&&!candidate.overlaps(sheet,gap)&&!candidate.overlaps(rail,gap))return candidate;
    }
    return null; // Caller uses an inline accessible value when no floating rectangle fits.
  }
}
