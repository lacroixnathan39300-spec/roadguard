package fr.roadguard.app;
/** Geometry shared by overlay creation, alert expansion, dragging and rotation. */
final class BubblePlacement {
  static final class Box {
    final int x,y,width,height;
    Box(int x,int y,int width,int height){this.x=x;this.y=y;this.width=width;this.height=height;}
    boolean overlaps(Box other,int gap){return other!=null&&x<other.x+other.width+gap&&x+width+gap>other.x&&y<other.y+other.height+gap&&y+height+gap>other.y;}
  }
  static Box fit(int left,int top,int right,int bottom,int width,int height,int x,int y){
    int w=Math.max(1,Math.min(Math.max(1,right-left),width)),h=Math.max(1,Math.min(Math.max(1,bottom-top),height));
    return new Box(Math.max(left,Math.min(right-w,x)),Math.max(top,Math.min(bottom-h,y)),w,h);
  }
  static Box action(int left,int top,int right,int bottom,Box bubble,int size,int gap){
    if(right-left<size||bottom-top<size)return null;
    int centerX=bubble.x+(bubble.width-size)/2,centerY=bubble.y+(bubble.height-size)/2;
    int[][] positions={{centerX,bubble.y+bubble.height+gap},{centerX,bubble.y-size-gap},{bubble.x+bubble.width+gap,centerY},{bubble.x-size-gap,centerY}};
    for(int[] pos:positions){Box box=fit(left,top,right,bottom,size,size,pos[0],pos[1]);if(!box.overlaps(bubble,gap))return box;}
    return null;
  }
  static int ratio(int position,int min,int max,int size){return Math.max(0,Math.min(10000,Math.round(10000f*(position-min)/Math.max(1,max-min-size))));}
  static int coordinate(int ratio,int min,int max,int size){return min+Math.round(Math.max(0,max-min-size)*Math.max(0,Math.min(10000,ratio))/10000f);}
  static boolean show(boolean armed,boolean appVisible,boolean interactive,boolean locked,boolean permission){return armed&&!appVisible&&interactive&&!locked&&permission;}
  private BubblePlacement(){}
}
