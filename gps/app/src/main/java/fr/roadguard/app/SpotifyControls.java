package fr.roadguard.app;

import android.app.*;
import android.content.*;
import android.media.MediaMetadata;
import android.media.session.*;
import android.media.session.MediaController;
import android.os.*;
import android.provider.Settings;
import android.widget.*;
import java.util.*;

/** Local Spotify controls. No Spotify password, API token or playback server is used. */
final class SpotifyControls {
  static final String PACKAGE="com.spotify.music";
  static ComponentName listener(Context c){return new ComponentName(c,SpotifySessionListener.class);}
  static boolean authorized(Context c){try{if(Build.VERSION.SDK_INT>=27){NotificationManager manager=c.getSystemService(NotificationManager.class);return manager!=null&&manager.isNotificationListenerAccessGranted(listener(c));}String value=Settings.Secure.getString(c.getContentResolver(),"enabled_notification_listeners");if(value!=null)for(String item:value.split(":"))if(listener(c).equals(ComponentName.unflattenFromString(item)))return true;}catch(RuntimeException unavailable){}return false;}
  static MediaController select(List<MediaController> controllers){MediaController first=null;if(controllers!=null)for(MediaController c:controllers)if(c!=null&&PACKAGE.equals(c.getPackageName())){if(playing(c.getPlaybackState()))return c;if(first==null)first=c;}return first;}
  static MediaController current(Context c){if(!authorized(c))return null;try{MediaSessionManager manager=c.getSystemService(MediaSessionManager.class);return manager==null?null:select(manager.getActiveSessions(listener(c)));}catch(RuntimeException unavailable){return null;}}
  static boolean supported(MediaController c,String action){return c!=null&&PACKAGE.equals(c.getPackageName())&&supportedPlayer(c,action);}
  static boolean supportedPlayer(MediaController c,String action){if(c==null)return false;PlaybackState state=c.getPlaybackState();if(state==null)return false;long allowed=state.getActions();switch(action){case "seek":return (allowed&PlaybackState.ACTION_SEEK_TO)!=0;case "previous":return (allowed&PlaybackState.ACTION_SKIP_TO_PREVIOUS)!=0;case "next":return (allowed&PlaybackState.ACTION_SKIP_TO_NEXT)!=0;case "toggle":return (allowed&(PlaybackState.ACTION_PLAY_PAUSE|(playing(state)?PlaybackState.ACTION_PAUSE:PlaybackState.ACTION_PLAY)))!=0;default:return false;}}
  static boolean playing(PlaybackState s){return s!=null&&(s.getState()==PlaybackState.STATE_PLAYING||s.getState()==PlaybackState.STATE_BUFFERING||s.getState()==PlaybackState.STATE_CONNECTING);}
  static boolean send(MediaController c,String action){return supported(c,action)&&sendPlayer(c,action);}
  static boolean sendPlayer(MediaController c,String action){if(!supportedPlayer(c,action))return false;try{MediaController.TransportControls t=c.getTransportControls();switch(action){case "previous":t.skipToPrevious();break;case "next":t.skipToNext();break;case "toggle":if(playing(c.getPlaybackState()))t.pause();else t.play();break;default:return false;}return true;}catch(RuntimeException unavailable){return false;}}
  static Intent launchIntent(){return new Intent(Intent.ACTION_VIEW,android.net.Uri.parse("spotify:")).setPackage(PACKAGE);}
  static void open(Activity a){try{a.startActivity(launchIntent());}catch(ActivityNotFoundException missing){try{a.startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://open.spotify.com/")));}catch(ActivityNotFoundException none){Toast.makeText(a,"Installez Spotify pour écouter votre musique",Toast.LENGTH_LONG).show();}}}
  static void authorize(Activity a){try{Intent intent=new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);if(Build.VERSION.SDK_INT>=30)intent=new Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,listener(a).flattenToString());a.startActivity(intent);}catch(ActivityNotFoundException unavailable){a.startActivity(new Intent(Settings.ACTION_SETTINGS));}}
  static void close(MainActivity a){AlertDialog dialog=a.spotifyDialog;a.spotifyDialog=null;if(dialog!=null)dialog.dismiss();}
  private static TextView text(MainActivity a,String value,int size,int color){TextView view=new TextView(a);view.setText(value);view.setTextSize(size);view.setTextColor(color);return view;}
  private static TextView action(MainActivity a,String title,boolean primary){
    TextView button=text(a,title,14,primary?0xff081611:0xfff4f7fa);button.setGravity(android.view.Gravity.CENTER);button.setMinHeight(a.pxDp(48));button.setPadding(a.pxDp(14),a.pxDp(10),a.pxDp(14),a.pxDp(10));button.setTypeface(null,android.graphics.Typeface.BOLD);button.setBackground(a.bg(primary?0xff1ed760:0xff27313e,16));button.setClickable(true);button.setFocusable(true);return button;
  }
  private static ImageButton transport(MainActivity a,String glyph,String description,boolean primary){
    ImageButton button=new ImageButton(a);button.setImageDrawable(new MusicGlyph(glyph,primary?0xff07150c:0xfff4f7fa));button.setContentDescription(description);button.setPadding(a.pxDp(14),a.pxDp(14),a.pxDp(14),a.pxDp(14));button.setBackground(a.bg(primary?0xff1ed760:0xff26303d,32));return button;
  }
  static void show(MainActivity a){
    if(a.isFinishing()||a.isDestroyed()||a.spotifyDialog!=null&&a.spotifyDialog.isShowing())return;
    final int green=0xff1ed760,white=0xfff4f7fa,muted=0xffaeb9c7;
    LinearLayout box=new LinearLayout(a);box.setOrientation(LinearLayout.VERTICAL);int pad=a.pxDp(20);box.setPadding(pad,pad,pad,pad);box.setBackground(a.bg(0xff141c27,24));
    LinearLayout header=new LinearLayout(a);header.setGravity(android.view.Gravity.CENTER_VERTICAL);TextView brand=text(a,"MUSIQUE",13,green);brand.setTypeface(null,android.graphics.Typeface.BOLD);header.addView(brand,new LinearLayout.LayoutParams(0,-2,1));TextView local=text(a,"LECTEUR LOCAL",10,muted);header.addView(local);box.addView(header);
    Button mini=new Button(a);mini.setText(a.getPreferences(0).getBoolean("music_map_bubble",false)?"Masquer le mini lecteur sur la carte":"Afficher le mini lecteur sur la carte");box.addView(mini);mini.setOnClickListener(v->{a.getPreferences(0).edit().putBoolean("music_map_bubble",!a.getPreferences(0).getBoolean("music_map_bubble",false)).apply();close(a);a.selected="Carte";a.render();});
    Button source=new Button(a);source.setText("Lecteur : "+MusicSources.label(a));box.addView(source);source.setOnClickListener(v->MusicSources.choose(a));
    Button studio=new Button(a);studio.setText("Studio audio · égaliseur");box.addView(studio);studio.setOnClickListener(v->MobileAudioStudio.show(a));
    LinearLayout track=new LinearLayout(a);track.setGravity(android.view.Gravity.CENTER_VERTICAL);LinearLayout.LayoutParams trackLp=new LinearLayout.LayoutParams(-1,-2);trackLp.topMargin=a.pxDp(20);box.addView(track,trackLp);
    ImageView cover=new ImageView(a);cover.setImageDrawable(new MusicGlyph("music",green));cover.setBackground(a.bg(0xff233240,16));cover.setScaleType(ImageView.ScaleType.CENTER_INSIDE);cover.setPadding(a.pxDp(8),a.pxDp(8),a.pxDp(8),a.pxDp(8));cover.setImportantForAccessibility(android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO);track.addView(cover,new LinearLayout.LayoutParams(a.pxDp(80),a.pxDp(80)));
    LinearLayout words=new LinearLayout(a);words.setOrientation(LinearLayout.VERTICAL);LinearLayout.LayoutParams wordsLp=new LinearLayout.LayoutParams(0,-2,1);wordsLp.leftMargin=a.pxDp(16);track.addView(words,wordsLp);
    TextView title=text(a,"Ta musique, sur la route",20,white);title.setTypeface(null,android.graphics.Typeface.BOLD);title.setMaxLines(2);title.setEllipsize(android.text.TextUtils.TruncateAt.END);words.addView(title);
    TextView artist=text(a,"",14,muted);artist.setMaxLines(2);artist.setEllipsize(android.text.TextUtils.TruncateAt.END);words.addView(artist);
    TextView status=text(a,"",12,muted);status.setPadding(0,a.pxDp(16),0,a.pxDp(8));box.addView(status);
    SeekBar seek=new SeekBar(a);seek.setMax(1000);seek.setProgressTintList(android.content.res.ColorStateList.valueOf(green));seek.setThumbTintList(android.content.res.ColorStateList.valueOf(green));seek.setContentDescription("Position dans le morceau");box.addView(seek,new LinearLayout.LayoutParams(-1,a.pxDp(44)));
    LinearLayout times=new LinearLayout(a);TextView elapsed=text(a,"--:--",12,muted),duration=text(a,"--:--",12,muted);times.addView(elapsed,new LinearLayout.LayoutParams(0,-2,1));times.addView(duration);box.addView(times);
    LinearLayout controls=new LinearLayout(a);controls.setGravity(android.view.Gravity.CENTER);LinearLayout.LayoutParams controlsLp=new LinearLayout.LayoutParams(-1,-2);controlsLp.topMargin=a.pxDp(14);controlsLp.bottomMargin=a.pxDp(18);box.addView(controls,controlsLp);
    ImageButton previous=transport(a,"previous","Titre précédent",false),toggle=transport(a,"play","Lecture",true),next=transport(a,"next","Titre suivant",false);
    controls.addView(previous,new LinearLayout.LayoutParams(a.pxDp(56),a.pxDp(56)));LinearLayout.LayoutParams toggleLp=new LinearLayout.LayoutParams(a.pxDp(72),a.pxDp(72));toggleLp.leftMargin=toggleLp.rightMargin=a.pxDp(20);controls.addView(toggle,toggleLp);controls.addView(next,new LinearLayout.LayoutParams(a.pxDp(56),a.pxDp(56)));
    TextView access=action(a,"Activer les commandes",true);box.addView(access,new LinearLayout.LayoutParams(-1,-2));access.setOnClickListener(v->new AlertDialog.Builder(a).setTitle("Commandes musicales").setMessage("Android demande l’accès aux notifications pour autoriser les commandes musicales. ROADGUARD utilise les sessions des lecteurs pour afficher le titre, la pochette et les commandes du lecteur choisi. Le contenu des notifications n’est ni inspecté, ni enregistré, ni envoyé.").setNegativeButton("Annuler",null).setPositiveButton("Réglages Android",(d,w)->authorize(a)).show());
    TextView launch=action(a,"Ouvrir le lecteur  ↗",false);LinearLayout.LayoutParams launchLp=new LinearLayout.LayoutParams(-1,-2);launchLp.topMargin=a.pxDp(10);box.addView(launch,launchLp);launch.setOnClickListener(v->MusicSources.open(a));
    TextView note=text(a,"Les commandes disponibles dépendent du lecteur choisi. Le son peut être envoyé en Bluetooth avec les réglages Android.",12,muted);note.setPadding(0,a.pxDp(12),0,0);box.addView(note);
    ScrollView scroll=new ScrollView(a){@Override protected void onMeasure(int width,int height){super.onMeasure(width,android.view.View.MeasureSpec.makeMeasureSpec((int)(a.getResources().getDisplayMetrics().heightPixels*.70f),android.view.View.MeasureSpec.AT_MOST));}};scroll.setFillViewport(false);scroll.addView(box);
    AlertDialog dialog=new AlertDialog.Builder(a).setView(scroll).setPositiveButton("Fermer",null).create();a.spotifyDialog=dialog;
    Handler handler=new Handler(Looper.getMainLooper());boolean[] dragging={false};MediaSession.Token[] seekingSession={null};String[] seekingTitle={""};long[] seekingDuration={0};android.graphics.Bitmap[] artwork={null};String[] lastGlyph={""};
    previous.setOnClickListener(v->{if(!sendPlayer(MusicSources.current(a),"previous"))Toast.makeText(a,"Commande indisponible dans ce lecteur",Toast.LENGTH_SHORT).show();});
    next.setOnClickListener(v->{if(!sendPlayer(MusicSources.current(a),"next"))Toast.makeText(a,"Commande indisponible dans ce lecteur",Toast.LENGTH_SHORT).show();});
    toggle.setOnClickListener(v->{if(!sendPlayer(MusicSources.current(a),"toggle"))Toast.makeText(a,"Démarre une lecture dans le lecteur choisi",Toast.LENGTH_SHORT).show();});
    seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
      public void onStartTrackingTouch(SeekBar bar){dragging[0]=true;MediaController c=MusicSources.current(a);MediaMetadata m=c==null?null:c.getMetadata();seekingSession[0]=c==null?null:c.getSessionToken();seekingTitle[0]=m==null?"":String.valueOf(m.getText(MediaMetadata.METADATA_KEY_TITLE));seekingDuration[0]=m==null?0:m.getLong(MediaMetadata.METADATA_KEY_DURATION);}
      public void onProgressChanged(SeekBar bar,int progress,boolean user){MediaController c=MusicSources.current(a);MediaMetadata m=c==null?null:c.getMetadata();long length=m==null?0:m.getLong(MediaMetadata.METADATA_KEY_DURATION);if(user&&length>0)elapsed.setText(MusicProgress.time(Math.round(length*(progress/1000d))));}
      public void onStopTrackingTouch(SeekBar bar){dragging[0]=false;MediaController c=MusicSources.current(a);MediaMetadata m=c==null?null:c.getMetadata();long length=m==null?0:m.getLong(MediaMetadata.METADATA_KEY_DURATION);if(c==null||seekingSession[0]==null||!seekingSession[0].equals(c.getSessionToken())||m==null||!seekingTitle[0].equals(String.valueOf(m.getText(MediaMetadata.METADATA_KEY_TITLE)))||length!=seekingDuration[0])return;if(length>0&&supportedPlayer(c,"seek"))try{c.getTransportControls().seekTo(Math.round(length*(bar.getProgress()/1000d)));}catch(RuntimeException unavailable){Toast.makeText(a,"Déplacement indisponible dans ce lecteur",Toast.LENGTH_SHORT).show();}}
    });
    Runnable refresh=new Runnable(){public void run(){
      if(!dialog.isShowing())return;if(a.isDestroyed()||a.isFinishing()){dialog.dismiss();return;}
      if(!a.activityResumed){handler.postDelayed(this,1000);return;}
      MediaController c=MusicSources.current(a);MediaMetadata m=c==null?null:c.getMetadata();PlaybackState playback=c==null?null:c.getPlaybackState();boolean allowed=authorized(a),active=playing(playback);
      CharSequence trackName=m==null?null:m.getText(MediaMetadata.METADATA_KEY_TITLE),performer=m==null?null:m.getText(MediaMetadata.METADATA_KEY_ARTIST);
      title.setText(trackName==null||trackName.length()==0?"Ta musique, sur la route":trackName);artist.setText(performer==null?"":performer);
      status.setText(!allowed?"Autorise les commandes locales ci-dessous":c==null?"Démarre une lecture dans le lecteur choisi":playback!=null&&(playback.getState()==PlaybackState.STATE_BUFFERING||playback.getState()==PlaybackState.STATE_CONNECTING)?"Chargement dans le lecteur…":active?"● Lecture en cours":"● Lecture en pause");status.setTextColor(allowed&&c!=null?green:muted);access.setVisibility(allowed?android.view.View.GONE:android.view.View.VISIBLE);
      for(ImageButton b:new ImageButton[]{previous,toggle,next}){String command=b==previous?"previous":b==next?"next":"toggle";boolean enabled=supportedPlayer(c,command);b.setEnabled(enabled);b.setAlpha(enabled?1f:.35f);}
      String glyph=active?"pause":"play";if(!glyph.equals(lastGlyph[0])){toggle.setImageDrawable(new MusicGlyph(glyph,0xff07150c));lastGlyph[0]=glyph;}toggle.setContentDescription(active?"Pause":"Lecture");
      android.graphics.Bitmap bitmap=m==null?null:m.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);if(bitmap==null&&m!=null)bitmap=m.getBitmap(MediaMetadata.METADATA_KEY_ART);if(bitmap==null&&m!=null)bitmap=m.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON);
      if(bitmap!=null&&bitmap.isRecycled())bitmap=null;
      if(bitmap!=artwork[0]){artwork[0]=bitmap;if(bitmap==null){cover.setImageDrawable(new MusicGlyph("music",green));cover.setScaleType(ImageView.ScaleType.CENTER_INSIDE);}else{cover.setImageBitmap(bitmap);cover.setScaleType(ImageView.ScaleType.CENTER_CROP);}}
      long length=m==null?0:m.getLong(MediaMetadata.METADATA_KEY_DURATION),position=playback==null?-1:MusicProgress.position(playback.getPosition(),playback.getLastPositionUpdateTime(),SystemClock.elapsedRealtime(),playback.getPlaybackSpeed(),playback.getState()==PlaybackState.STATE_PLAYING,length);
      duration.setText(MusicProgress.time(length>0?length:-1));seek.setEnabled(length>0&&position>=0&&supportedPlayer(c,"seek"));if(!dragging[0]){elapsed.setText(MusicProgress.time(position));seek.setProgress(MusicProgress.progress(position,length));}
      handler.postDelayed(this,1000);
    }};
    dialog.setOnDismissListener(d->{handler.removeCallbacksAndMessages(null);cover.setImageDrawable(null);artwork[0]=null;if(a.spotifyDialog==dialog)a.spotifyDialog=null;});dialog.show();
    if(dialog.getWindow()!=null){dialog.getWindow().setBackgroundDrawable(a.bg(0xff141c27,24));dialog.getWindow().setLayout(Math.min(a.getResources().getDisplayMetrics().widthPixels-a.pxDp(24),a.pxDp(560)),-2);dialog.getWindow().setDimAmount(.65f);}dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(green);handler.post(refresh);
  }
  private SpotifyControls(){}
}
