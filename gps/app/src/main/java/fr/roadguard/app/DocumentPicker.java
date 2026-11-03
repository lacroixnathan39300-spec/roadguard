package fr.roadguard.app;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

/** Keep imports usable on head units whose firmware omits DocumentsUI. */
final class DocumentPicker {
    static boolean open(Activity activity,Intent intent,int requestCode) {
        if(activity.isFinishing()||activity.isDestroyed())return false;
        try { activity.startActivityForResult(intent,requestCode); return true; }
        catch(ActivityNotFoundException|SecurityException unavailable) {
            if(Intent.ACTION_OPEN_DOCUMENT.equals(intent.getAction())) {
                Intent fallback=new Intent(intent).setAction(Intent.ACTION_GET_CONTENT);
                fallback.removeFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                fallback.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                try { activity.startActivityForResult(fallback,requestCode); return true; }
                catch(ActivityNotFoundException|SecurityException missing) {
                    Log.w("ROADGUARD","Document picker unavailable",missing);
                }
            } else Log.w("ROADGUARD","Document creation unavailable",unavailable);
            Toast.makeText(activity,"Sélecteur de fichiers indisponible. Active un gestionnaire de documents Android compatible.",Toast.LENGTH_LONG).show();
            return false;
        }
    }
    private DocumentPicker(){}
}
