package fr.roadguard.app;
public final class TestMusicProgress {
 static int checks;static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
 public static void main(String[] args){
  check(MusicProgress.position(1000,2000,5000,1,true,10000)==4000,"playing clock");check(MusicProgress.position(1000,2000,5000,1,false,10000)==1000,"paused or buffering advanced");
  check(MusicProgress.position(-1,2000,5000,1,true,10000)==-1,"unknown position invented");check(MusicProgress.position(1000,0,5000,1,true,10000)==1000,"absent update clock invented progress");check(MusicProgress.position(1000,6000,5000,1,true,10000)==1000,"future update clock");
  check(MusicProgress.position(9000,2000,5000,1,true,10000)==10000,"progress exceeded track");check(MusicProgress.position(1000,2000,5000,2,true,10000)==7000,"playback speed ignored");
  for(float speed:new float[]{Float.NaN,Float.POSITIVE_INFINITY,-1,9})check(MusicProgress.position(1000,2000,5000,speed,true,10000)==1000,"invalid speed invented progress");
  check(MusicProgress.position(1000,2000,5000,1,true,0)==4000,"live track should not acquire an invented duration");check(MusicProgress.progress(-1,1000)==0&&MusicProgress.progress(1000,0)==0,"unknown seek fraction");check(MusicProgress.progress(500,1000)==500&&MusicProgress.progress(2000,1000)==1000,"seek fraction bounds");
  check(MusicProgress.time(-1).equals("--:--")&&MusicProgress.time(0).equals("0:00")&&MusicProgress.time(61000).equals("1:01")&&MusicProgress.time(3601000).equals("60:01"),"time labels");
  System.out.println("PASS music progress: "+checks+" checks, actual timing policy; no Spotify session or sound");
 }
}
