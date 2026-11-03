package fr.roadguard.app;
import android.media.session.MediaController;

import android.app.*;import android.content.*;import android.media.session.*;import android.widget.*;import java.util.*;

/** Explicit local player selection; a pinned player never controls another application. */
final class MusicSources {
  static final String AUTO="*";
  static String selected(Context c){return c.getSharedPreferences("MainActivity",0).getString("music_source",SpotifyControls.PACKAGE);}
  static MediaController select(List<MediaController> list,String source){if(source==null)source=SpotifyControls.PACKAGE;MediaController first=null;if(list!=null)for(MediaController c:list)if(c!=null&&(AUTO.equals(source)||source.equals(c.getPackageName()))){if(SpotifyControls.playing(c.getPlaybackState()))return c;if(first==null)first=c;}return first;}
  static List<MediaController> sessions(Context c){if(!SpotifyControls.authorized(c))return Collections.emptyList();try{MediaSessionManager m=c.getSystemService(MediaSessionManager.class);return m==null?Collections.emptyList():m.getActiveSessions(SpotifyControls.listener(c));}catch(RuntimeException unavailable){return Collections.emptyList();}}
  static MediaController current(Context c){try{return select(sessions(c),selected(c));}catch(RuntimeException unavailable){return null;}}
  static String label(Context c,String pkg){if(AUTO.equals(pkg))return "Lecteur actif · automatique";try{return c.getPackageManager().getApplicationLabel(c.getPackageManager().getApplicationInfo(pkg,0)).toString();}catch(Exception missing){return SpotifyControls.PACKAGE.equals(pkg)?"Spotify":pkg;}}
  static String label(Context c){return label(c,selected(c));}
  static void close(MainActivity a){AlertDialog d=a.musicSourceDialog;a.musicSourceDialog=null;if(d!=null)d.dismiss();}
  static void choose(MainActivity a){if(a.isFinishing()||a.isDestroyed()||a.musicSourceDialog!=null&&a.musicSourceDialog.isShowing())return;LinkedHashSet<String> packages=new LinkedHashSet<>();packages.add(AUTO);packages.add(SpotifyControls.PACKAGE);for(MediaController c:sessions(a))packages.add(c.getPackageName());
    Intent query=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MUSIC);for(android.content.pm.ResolveInfo r:a.getPackageManager().queryIntentActivities(query,0))if(r.activityInfo!=null)packages.add(r.activityInfo.packageName);
    for(String pkg:new String[]{"com.google.android.apps.youtube.music","deezer.android.app","org.videolan.vlc"})if(a.getPackageManager().getLaunchIntentForPackage(pkg)!=null)packages.add(pkg);
    packages.add(selected(a));String[] values=packages.toArray(new String[0]),labels=new String[values.length];for(int i=0;i<values.length;i++)labels[i]=label(a,values[i]);int index=Arrays.asList(values).indexOf(selected(a));
    AlertDialog dialog=new AlertDialog.Builder(a).setTitle("Choisir le lecteur musical").setSingleChoiceItems(labels,index,(d,which)->{a.getPreferences(0).edit().putString("music_source",values[which]).apply();d.dismiss();SpotifyControls.close(a);SpotifyControls.show(a);}).setNegativeButton("Annuler",null).create();a.musicSourceDialog=dialog;dialog.setOnDismissListener(d->{if(a.musicSourceDialog==dialog)a.musicSourceDialog=null;});dialog.show();
  }
  static void open(MainActivity a){String pkg=selected(a);if(AUTO.equals(pkg)){MediaController c=current(a);if(c!=null)pkg=c.getPackageName();else{choose(a);return;}}if(SpotifyControls.PACKAGE.equals(pkg)){SpotifyControls.open(a);return;}try{Intent i=a.getPackageManager().getLaunchIntentForPackage(pkg);if(i!=null){a.startActivity(i);return;}}catch(RuntimeException unavailable){}Toast.makeText(a,"Ouvre le lecteur musical installé sur ton téléphone",Toast.LENGTH_LONG).show();}
  private MusicSources(){}
}
