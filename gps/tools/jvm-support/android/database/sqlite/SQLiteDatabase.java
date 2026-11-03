package android.database.sqlite;

import android.database.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** JVM adapter to real Python SQLite in mode=ro. NOT an Android SQLite implementation. */
public final class SQLiteDatabase implements Closeable {
  public interface CursorFactory {}
  public static final int OPEN_READONLY=1,NO_LOCALIZED_COLLATORS=16;
  private final File file;private final boolean readOnly;
  private SQLiteDatabase(File file,boolean readOnly){this.file=file;this.readOnly=readOnly;}
  public static SQLiteDatabase openDatabase(String path,CursorFactory factory,int flags,DatabaseErrorHandler handler){
    if(!new File(path).isFile())throw new SQLiteException("missing file");return new SQLiteDatabase(new File(path),(flags&OPEN_READONLY)!=0);
  }
  public boolean isReadOnly(){return readOnly;}
  public Cursor rawQuery(String sql,String[] args){
    if(!readOnly)throw new SQLiteException("test adapter only permits read-only queries");
    List<String> command=new ArrayList<>(Arrays.asList("python3",System.getProperty("roadguard.sqlite.probe"),file.getAbsolutePath(),sql));
    if(args!=null)command.addAll(Arrays.asList(args));
    try{
      Process process=new ProcessBuilder(command).redirectErrorStream(true).start();List<String> lines=new ArrayList<>();
      try(BufferedReader reader=new BufferedReader(new InputStreamReader(process.getInputStream(),StandardCharsets.UTF_8))){String line;while((line=reader.readLine())!=null)lines.add(line);}
      if(process.waitFor()!=0||lines.isEmpty()||!lines.get(0).startsWith("C\t"))throw new SQLiteException(String.join("\n",lines));
      return new ProbeCursor(lines);
    }catch(IOException error){throw new SQLiteException(error.toString());}catch(InterruptedException error){Thread.currentThread().interrupt();throw new SQLiteException(error.toString());}
  }
  public void close(){}
  static String decode(String value){return new String(Base64.getDecoder().decode(value),StandardCharsets.UTF_8);}
  static final class ProbeCursor implements Cursor {
    final String[] columns;final List<String[]> rows=new ArrayList<>();int position=-1;
    ProbeCursor(List<String> lines){String[] names=lines.get(0).split("\t",-1);columns=new String[names.length-1];for(int i=1;i<names.length;i++)columns[i-1]=decode(names[i]);for(int i=1;i<lines.size();i++){if(!lines.get(i).startsWith("R\t"))throw new SQLiteException("bad probe row");rows.add(lines.get(i).substring(2).split("\t",-1));}}
    public boolean moveToFirst(){position=0;return !rows.isEmpty();}
    public boolean moveToNext(){position++;return position<rows.size();}
    String cell(int column){return rows.get(position)[column];}
    public String getString(int column){String value=cell(column);return value.charAt(0)=='N'?null:decode(value.substring(2));}
    public long getLong(int column){return Long.parseLong(getString(column));}
    public double getDouble(int column){return Double.parseDouble(getString(column));}
    public int getType(int column){switch(cell(column).charAt(0)){case 'N':return FIELD_TYPE_NULL;case 'I':return FIELD_TYPE_INTEGER;case 'F':return FIELD_TYPE_FLOAT;case 'S':return FIELD_TYPE_STRING;default:return FIELD_TYPE_BLOB;}}
    public boolean isNull(int column){return getType(column)==FIELD_TYPE_NULL;}
    public int getColumnIndexOrThrow(String name){for(int i=0;i<columns.length;i++)if(columns[i].equals(name))return i;throw new IllegalArgumentException(name);}
    public String getColumnName(int column){return columns[column];}public int getColumnCount(){return columns.length;}public void close(){}
  }
}
