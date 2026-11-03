package fr.roadguard.app;
final class RadarProximityCadence {
 static long interval(double d){if(!Double.isFinite(d)||d<0||d>1000)return 0;return d<=50?100:d<=100?240:d<=200?420:d<=300?650:d<=500?950:d<=750?1300:1800;}
 static boolean continuous(double d){return interval(d)==100;}static int pulse(double d){return (int)Math.min(160,Math.max(90,interval(d)-80));}private RadarProximityCadence(){}
}
