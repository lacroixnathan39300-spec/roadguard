package android.content;
import java.io.File;
import android.content.res.AssetManager;
/** Only the file/preferences boundary needed by the catalog's JVM unit tests. */
public abstract class Context {
  public static final int MODE_PRIVATE=0;
  public abstract File getFilesDir();public abstract AssetManager getAssets();
  public abstract SharedPreferences getSharedPreferences(String name,int mode);
}
