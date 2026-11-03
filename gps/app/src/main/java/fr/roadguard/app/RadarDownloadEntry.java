package fr.roadguard.app;
import android.content.*;import android.widget.Toast;
/** A packaging error must not close the map or erase installed radar data. */
final class RadarDownloadEntry {
 static boolean open(MainActivity a){
  try{a.startActivity(new Intent(a,RadarDownloadActivity.class));return true;}
  catch(ActivityNotFoundException unavailable){android.util.Log.e("ROADGUARD_RADARS","Écran de téléchargement absent de cette installation",unavailable);Toast.makeText(a,"Écran radar indisponible : installe la dernière version ROADGUARD sans désinstaller l’application.",Toast.LENGTH_LONG).show();return false;}
 }
 private RadarDownloadEntry(){}
}
