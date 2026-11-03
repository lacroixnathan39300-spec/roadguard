package android.content;
import java.util.Map;
/** Minimal JVM test contract, not an Android runtime or an application dependency. */
public interface SharedPreferences {
  Map<String,?> getAll();long getLong(String key,long fallback);int getInt(String key,int fallback);
  boolean getBoolean(String key,boolean fallback);String getString(String key,String fallback);Editor edit();
  interface Editor {Editor putLong(String key,long value);Editor putInt(String key,int value);Editor putBoolean(String key,boolean value);Editor putString(String key,String value);Editor clear();boolean commit();void apply();}
}
