package fr.roadguard.app;
/** One effects owner shared by the cockpit and GPS. */
public final class AudioStudioAccess {
 public static void start(android.content.Context c){MobileAudioStudio.start(c);}
 public static void show(android.content.Context c){android.content.Context current=c;for(int i=0;i<16;i++){if(current instanceof android.app.Activity){MobileAudioStudio.show((android.app.Activity)current);return;}if(!(current instanceof android.content.ContextWrapper))break;android.content.Context next=((android.content.ContextWrapper)current).getBaseContext();if(next==current)break;current=next;}}
 private AudioStudioAccess(){}
}
