package fr.roadguard.app;
public final class TestMapInteraction {
 static int checks;
 static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
 static void coordinate(String value,double a,double b){double[] p=DestinationCoordinates.parse(value);check(p!=null&&Math.abs(p[0]-a)<.000001&&Math.abs(p[1]-b)<.000001,"coordinate: "+value);}
 public static void main(String[] args){
  for(String value:new String[]{"46.75,5.9","46.75, 5.9","46.75 ; 5.9","46,75 ; 5,9","46,75 5,9"," 46.75  5.9 ","46,75\u00a05,9"})coordinate(value,46.75,5.9);
  coordinate("-46.75; -5.9",-46.75,-5.9);coordinate("+90; +180",90,180);coordinate(".5;-.2",.5,-.2);
  for(String value:new String[]{"91;0","0;181","NaN;5","Infinity;0","46,75,5,9","Champagnole","1e3;0","46.7, 5.9, 8","","46.7;5.9;1"})check(DestinationCoordinates.parse(value)==null,"invalid coordinate accepted: "+value);
  check(DestinationCoordinates.parse(null)==null,"null coordinates");
  check(NavigationIntentInput.coordinates("46.75,5.9 (Maison)")[1]==5.9,"geo intent label parsing");
  int cases=0,placed=0;
  for(int width:new int[]{320,393,600,800,1000})for(int height:new int[]{180,280,393,640,852})for(int topHeight:new int[]{44,120,200})for(int sheetHeight:new int[]{70,140}){
   boolean wide=width>=500&&width>height;int topWidth=wide?Math.min(360,(int)(width*.46)):width-20;
   MapOverlayPlacement.Box top=new MapOverlayPlacement.Box(10,10,topWidth,Math.min(topHeight,height-20-(wide?0:sheetHeight)));
   MapOverlayPlacement.Box panel=new MapOverlayPlacement.Box(wide?width-Math.min(380,width/2):0,height-sheetHeight,wide?Math.min(380,width/2):width,sheetHeight);
   MapOverlayPlacement.Box rail=MapOverlayPlacement.floating(width,height,54,60,8,top,panel,null,true);
   MapOverlayPlacement.Box speed=MapOverlayPlacement.floating(width,height,66,66,8,top,panel,rail);
   if(rail!=null){check(!rail.overlaps(top,8)&&!rail.overlaps(panel,8),"rail collision");}
   if(speed!=null){placed++;check(!speed.overlaps(top,8)&&!speed.overlaps(panel,8)&&!speed.overlaps(rail,8),"speed collision");check(speed.left>=8&&speed.top>=8&&speed.right()<=width-8&&speed.bottom()<=height-8,"speed outside map");}
   cases++;
  }
  check(placed>0,"all viewports lost floating speed");
  check(MapOverlayPlacement.floating(393,852,66,66,8,new MapOverlayPlacement.Box(10,10,373,140),new MapOverlayPlacement.Box(0,760,393,92),null)!=null,"standard portrait viewport lost speed");
  MapOverlayPlacement.Box full=new MapOverlayPlacement.Box(0,0,100,100);
  check(MapOverlayPlacement.floating(100,100,66,66,8,full,null,null)==null,"unplaceable speed should use inline fallback");
  check(MapOverlayPlacement.floating(0,100,66,66,8,null,null,null)==null,"zero viewport");
  System.out.println("PASS interaction: "+checks+" assertions, "+cases+" rectangle scenarios / "+placed+" floating placements, French coordinates; no Android rendering");
 }
}
