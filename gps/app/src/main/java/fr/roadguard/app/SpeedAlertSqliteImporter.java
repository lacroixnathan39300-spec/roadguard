package fr.roadguard.app;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;

/** Reads the observed cameras(id,lat,lon,type) schema, never a source database in write mode. */
final class SpeedAlertSqliteImporter {
  private SpeedAlertSqliteImporter(){}
  static boolean sqliteHeader(InputStream input)throws IOException{
    byte[] expected={'S','Q','L','i','t','e',' ','f','o','r','m','a','t',' ','3',0},actual=new byte[16];
    int read=0,n;while(read<16&&(n=input.read(actual,read,16-read))!=-1)read+=n;
    return read==16&&Arrays.equals(expected,actual);
  }
  static List<CanonicalRadar> readLocal(File file,String country,CanonicalRadarStore.Cancellation cancellation)throws IOException{
    return read(file,country,"Import SQLite local", "", "import_local",cancellation);
  }
  static List<CanonicalRadar> read(File file,String country,String source,String sourceDate,String reliability,CanonicalRadarStore.Cancellation cancellation)throws IOException{
    if(!file.isFile()||file.length()<512||file.length()>CanonicalRadarStore.MAX_BYTES)throw new IOException("Base SQLite vide ou trop volumineuse");
    try(InputStream input=new FileInputStream(file)){if(!sqliteHeader(input))throw new IOException("Signature SQLite invalide");}
    cancellation.check();String digest=sha256(file);ArrayList<CanonicalRadar> result=new ArrayList<>();HashSet<Long> ids=new HashSet<>();
    // An explicit error handler avoids the default corruption handler deleting the input file.
    try(SQLiteDatabase database=SQLiteDatabase.openDatabase(file.getAbsolutePath(),null,SQLiteDatabase.OPEN_READONLY|SQLiteDatabase.NO_LOCALIZED_COLLATORS,db->{})){
      if(!database.isReadOnly())throw new IOException("La base source doit rester en lecture seule");
      try(Cursor check=database.rawQuery("PRAGMA quick_check(1)",null)){
        if(!check.moveToFirst()||!"ok".equalsIgnoreCase(check.getString(0)))throw new IOException("Contrôle SQLite incorrect");
      }
      try(Cursor table=database.rawQuery("SELECT type, sql FROM sqlite_master WHERE name = ?",new String[]{"cameras"})){
        if(!table.moveToFirst()||!"table".equals(table.getString(0))||table.getString(1)==null
            ||!table.getString(1).trim().toUpperCase(Locale.ROOT).startsWith("CREATE TABLE")||table.moveToNext())
          throw new IOException("Table cameras absente, virtuelle ou ambiguë");
      }
      HashSet<String> columns=new HashSet<>();
      try(Cursor schema=database.rawQuery("PRAGMA table_info(cameras)",null)){
        while(schema.moveToNext()){if(columns.size()>=32)throw new IOException("Schéma SQLite trop complexe");columns.add(schema.getString(1));}
      }
      if(!columns.containsAll(Arrays.asList("id","lat","lon","type")))throw new IOException("Schéma cameras non compatible");
      long expected;
      try(Cursor count=database.rawQuery("SELECT COUNT(*) FROM cameras",null)){
        if(!count.moveToFirst())throw new IOException("Comptage SQLite impossible");expected=count.getLong(0);
        if(expected<1||expected>CanonicalRadarStore.MAX_RECORDS)throw new IOException("Base cameras vide ou trop volumineuse");
      }
      try(Cursor rows=database.rawQuery("SELECT * FROM cameras ORDER BY id LIMIT 100001",null)){
        int idColumn=rows.getColumnIndexOrThrow("id"),latColumn=rows.getColumnIndexOrThrow("lat"),lonColumn=rows.getColumnIndexOrThrow("lon"),typeColumn=rows.getColumnIndexOrThrow("type");
        while(rows.moveToNext()){
          cancellation.check();if(result.size()>=CanonicalRadarStore.MAX_RECORDS)throw new IOException("Trop de points SQLite");
          if(rows.getType(idColumn)!=Cursor.FIELD_TYPE_INTEGER||rows.isNull(latColumn)||rows.isNull(lonColumn)||rows.isNull(typeColumn))throw new IOException("Point SQLite incomplet");
          if(!numeric(rows.getType(latColumn))||!numeric(rows.getType(lonColumn))||rows.getType(typeColumn)!=Cursor.FIELD_TYPE_STRING)throw new IOException("Types SQLite invalides");
          long id=rows.getLong(idColumn);if(id<0||!ids.add(id))throw new IOException("Identifiant SQLite invalide ou dupliqué");
          String raw=rows.getString(typeColumn);if(raw==null||raw.length()>2048)throw new IOException("Type radar trop volumineux");
          TreeMap<String,String> metadata=new TreeMap<>();metadata.put("raw_type",raw);metadata.put("sqlite_sha256",digest);metadata.put("speed_state","not_provided");metadata.put("direction_state","not_provided");
          for(int column=0;column<rows.getColumnCount();column++){
            String name=rows.getColumnName(column);if(columns.contains(name)&&!Arrays.asList("id","lat","lon","type").contains(name)){
              if(rows.getType(column)==Cursor.FIELD_TYPE_BLOB)throw new IOException("Métadonnée binaire SQLite non prise en charge");
              metadata.put("sqlite."+name,rows.isNull(column)?"<NULL>":rows.getString(column));
            }
          }
          result.add(new CanonicalRadar(country,Long.toString(id),rows.getDouble(latColumn),rows.getDouble(lonColumn),CanonicalRadar.kind(raw),0,null,source,sourceDate,reliability,metadata));
        }
      }
      if(result.size()!=expected)throw new IOException("Base SQLite modifiée pendant sa lecture");
    }catch(android.database.sqlite.SQLiteException error){throw new IOException("Lecture SQLite refusée : "+error.getMessage(),error);}
    if(result.isEmpty())throw new IOException("Base cameras vide");return Collections.unmodifiableList(result);
  }
  private static boolean numeric(int type){return type==Cursor.FIELD_TYPE_INTEGER||type==Cursor.FIELD_TYPE_FLOAT;}
  private static String sha256(File file)throws IOException{
    try{MessageDigest digest=MessageDigest.getInstance("SHA-256");try(InputStream input=new FileInputStream(file)){byte[] buffer=new byte[8192];int n;while((n=input.read(buffer))!=-1)digest.update(buffer,0,n);}StringBuilder value=new StringBuilder();for(byte b:digest.digest())value.append(String.format(Locale.ROOT,"%02x",b&255));return value.toString();}
    catch(java.security.NoSuchAlgorithmException error){throw new IOException(error);}
  }
}
