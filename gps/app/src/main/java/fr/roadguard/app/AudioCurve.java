package fr.roadguard.app;

/** Ten saved user bands mapped onto the actual equalizer bands. Values are millibels. */
final class AudioCurve {
  static final int[] HZ={31,63,125,250,500,1000,2000,4000,8000,16000};
  static final String[] PRESETS={"Personnalisé","Neutre","Voix","Rock","Jazz","Électronique"};
  static final int[][] CURVES={{0,0,0,0,0,0,0,0,0,0},{-300,-200,-100,0,100,250,300,200,0,-100},{250,200,0,-100,-150,0,150,200,250,200},{150,100,0,100,150,100,150,100,50,0},{300,250,100,0,-100,0,150,200,200,150}};
  static int nearest(double hz){int best=0;double distance=Double.POSITIVE_INFINITY;for(int i=0;i<HZ.length;i++){double d=Math.abs(Math.log(Math.max(1,hz)/HZ[i]));if(d<distance){best=i;distance=d;}}return best;}
  static int gain(int[] curve,double hz,boolean headroom,int min,int max){if(curve==null||curve.length!=10||min>max)return 0;int reserve=0;if(headroom)for(int x:curve)reserve=Math.max(reserve,Math.max(-1200,Math.min(1200,x)));return Math.max(min,Math.min(max,Math.max(-1200,Math.min(1200,curve[nearest(hz)]))-reserve));}
  private AudioCurve(){}
}
