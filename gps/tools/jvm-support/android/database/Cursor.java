package android.database;
import java.io.Closeable;
/** Test boundary only, mirroring the Android cursor methods used by the importer. */
public interface Cursor extends Closeable {
  int FIELD_TYPE_NULL=0,FIELD_TYPE_INTEGER=1,FIELD_TYPE_FLOAT=2,FIELD_TYPE_STRING=3,FIELD_TYPE_BLOB=4;
  boolean moveToFirst();boolean moveToNext();String getString(int column);long getLong(int column);double getDouble(int column);
  int getType(int column);boolean isNull(int column);int getColumnIndexOrThrow(String name);String getColumnName(int column);int getColumnCount();
  void close();
}
