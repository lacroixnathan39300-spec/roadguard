package fr.roadguard.app;
import android.app.*;
import android.os.*;
import java.util.*;
/** Distinguishes any ROADGUARD page (bubble visibility) from an active GPS Activity (audio owner). */
final class RoadguardVisibility {
  private static boolean installed;
  private static final Handler MAIN=new Handler(Looper.getMainLooper());
  private static final Set<Activity> resumed=Collections.newSetFromMap(new IdentityHashMap<>());
  private static final AppVisibilityPolicy APP=new AppVisibilityPolicy((action,delay)->{MAIN.postDelayed(action,delay);return ()->MAIN.removeCallbacks(action);},BackgroundAlertService::setAppVisible);
  static synchronized void install(Application app){
    if(installed)return;installed=true;
    app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks(){
      public void onActivityCreated(Activity a,Bundle b){}
      public void onActivityStarted(Activity a){APP.started(a);}
      public void onActivityResumed(Activity a){if(a instanceof MainActivity)resumed(a);}
      public void onActivityPaused(Activity a){if(a instanceof MainActivity)paused(a);}
      public void onActivityStopped(Activity a){APP.stopped(a);}
      public void onActivitySaveInstanceState(Activity a,Bundle b){}
      public void onActivityDestroyed(Activity a){APP.stopped(a);paused(a);}
    });
  }
  static void resumed(Activity owner){resumed.add(owner);BackgroundAlertService.setRoadguardVisible(!resumed.isEmpty());}
  static void paused(Activity owner){resumed.remove(owner);BackgroundAlertService.setRoadguardVisible(!resumed.isEmpty());}
  private RoadguardVisibility(){}
}
