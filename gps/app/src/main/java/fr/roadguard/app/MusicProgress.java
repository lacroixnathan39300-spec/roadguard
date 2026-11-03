package fr.roadguard.app;
/** Local playback timing. Unknown positions never become invented progress. */
final class MusicProgress {
  static long position(long reported,long updatedAt,long now,float speed,boolean playing,long duration){
    if(reported<0)return -1;
    double value=reported;
    if(playing&&updatedAt>0&&now>=updatedAt&&Float.isFinite(speed)&&speed>=0&&speed<=8)value+=(now-updatedAt)*(double)speed;
    value=Math.max(0,Math.min(Long.MAX_VALUE,value));
    return duration>0?Math.min(duration,(long)value):(long)value;
  }
  static int progress(long position,long duration){return position<0||duration<=0?0:(int)Math.round(Math.min(1,position/(double)duration)*1000);}
  static String time(long millis){if(millis<0)return "--:--";long seconds=millis/1000;return String.format(java.util.Locale.ROOT,"%d:%02d",seconds/60,seconds%60);}
  private MusicProgress(){}
}
