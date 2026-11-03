package fr.roadguard.app;

import android.graphics.Color;
import android.location.Location;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/** Native banner shared by ROADGUARD GPS and DRIVE. */
final class AverageSpeedZoneUi {
  private AverageSpeedZoneUi(){}
  static void loadAsync(MainActivity a){
    if(a.averageZoneMonitor!=null||a.averageZoneLoading)return;
    a.averageZoneLoading=true;
    new Thread(()->{try{load(a);}finally{a.averageZoneLoading=false;}},"roadguard-average-zones").start();
  }
  static void load(MainActivity a){
    try{
      ArrayList<AverageSpeedZoneMonitor.Zone> zones=new ArrayList<>();
      ArrayList<AverageSpeedZoneMonitor.Point> points=new ArrayList<>();
      for(String country:new String[]{"ch","fr","be","lu","de","it","es"}){
        zones.addAll(AverageSpeedZoneMonitor.parse(new InputStreamReader(
            a.getAssets().open("radars_embedded_"+country+".csv"),StandardCharsets.UTF_8)));
        // Tronçons français : début et fin ne sont pas reliés, ils sont reconnus en roulant.
        points.addAll(AverageSpeedZoneMonitor.parsePoints(new InputStreamReader(
            a.getAssets().open("radars_embedded_"+country+".csv"),StandardCharsets.UTF_8)));
      }
      a.averageZoneMonitor=new AverageSpeedZoneMonitor(zones,points);
    }catch(Exception error){
      android.util.Log.w("ROADGUARD","Zones de vitesse moyenne indisponibles",error);
      a.averageZoneMonitor=new AverageSpeedZoneMonitor(new ArrayList<>());
    }
  }
  static void attach(MainActivity a,int marginTopDp){
    TextView banner=a.label("",15);
    banner.setTextColor(Color.WHITE);banner.setGravity(Gravity.CENTER);
    banner.setPadding(a.pxDp(14),a.pxDp(8),a.pxDp(14),a.pxDp(8));
    banner.setBackground(a.premiumPanel(0xf4122431,0xf307111c,0xffa4ddf4,16));
    banner.setElevation(a.pxDp(10));banner.setVisibility(View.GONE);
    FrameLayout.LayoutParams params=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.CENTER_HORIZONTAL);
    params.setMargins(a.pxDp(12),a.pxDp(marginTopDp),a.pxDp(12),0);
    a.frame.addView(banner,params);a.averageZoneBadge=banner;
    banner.setOnClickListener(v->new android.app.AlertDialog.Builder(a)
        .setTitle("Vitesse moyenne").setMessage("La moyenne est une estimation GPS entre deux extrémités connues. "
            +"Zones complètes disponibles : "+a.averageZoneMonitor.zoneCount()
            +". Si la position GPS est imprécise, le calcul s'interrompt.")
        .setPositiveButton("OK",null).show());
  }
  static void update(MainActivity a,Location fix){
    if(a.averageZoneMonitor==null||fix==null)return;
    AverageSpeedZoneMonitor.Status status=a.averageZoneMonitor.update(
        fix.getLatitude(),fix.getLongitude(),a.lastSpeedKmh,
        fix.hasAccuracy()?fix.getAccuracy():1000,
        a.reliableBearing?a.lastBearing:Double.NaN,
        fix.getElapsedRealtimeNanos()/1000000L);
    if(status==null){if(a.averageZoneBadge!=null)a.averageZoneBadge.setVisibility(View.GONE);return;}
    if(a.averageZoneBadge!=null){
      a.averageZoneBadge.setText(status.text);
      a.averageZoneBadge.setTextColor(status.overLimit?0xffff6571:0xffe5f5ff);
      a.averageZoneBadge.setVisibility(View.VISIBLE);
      if(status.finished)a.averageZoneBadge.postDelayed(()->{
        if(a.averageZoneBadge!=null&&a.averageZoneBadge.getText().toString().equals(status.text))
          a.averageZoneBadge.setVisibility(View.GONE);
      },9000);
    }
    boolean announce=status.finished||status.active&&status.text.startsWith("Zone moyenne")
        ||status.overLimit&&!a.averageZoneWasOverLimit;
    a.averageZoneWasOverLimit=status.active&&status.overLimit;
    if(announce&&!status.text.equals(a.averageZoneLastAnnouncement)&&a.tts!=null&&a.voiceReady){
      a.averageZoneLastAnnouncement=status.text;
      a.alertSpeechTracker.speakOther(a.tts,status.text,GuidanceAudioPolicy.HAZARD);
    }
  }
}
