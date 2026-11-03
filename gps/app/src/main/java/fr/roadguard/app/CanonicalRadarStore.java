package fr.roadguard.app;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** One self-contained CSV, immutable assets, two atomic backup generations. */
final class CanonicalRadarStore {
  static final long MAX_BYTES=32_000_000L;
  static final int MAX_RECORDS=100000;
  interface Cancellation {void check()throws IOException;}
  static final Cancellation NONE=()->{};
  private CanonicalRadarStore(){}
  static File file(File updates,String country){
    if(country==null||!country.matches("[A-Z]{2}"))throw new IllegalArgumentException("Pays invalide");
    return new File(new File(updates,"canonical"),country.toLowerCase(Locale.ROOT)+".csv");
  }
  static final class Snapshot {
    final List<CanonicalRadar> records;final boolean backup;final String warning;
    Snapshot(List<CanonicalRadar> records,boolean backup,String warning){this.records=records;this.backup=backup;this.warning=warning;}
  }
  static Snapshot load(File updates,String country)throws IOException{
    File target=file(updates,country);String error="";
    File[] candidates={target,new File(target+".backup"),new File(target+".backup.previous")};
    for(int i=0;i<candidates.length;i++){
      if(!candidates[i].isFile())continue;
      try{return new Snapshot(read(candidates[i],country),i>0,error);}
      catch(IOException invalid){error="Surcouche invalide : "+candidates[i].getName();}
    }
    return new Snapshot(Collections.emptyList(),false,error);
  }
  static List<CanonicalRadar> read(File file,String country)throws IOException{
    if(!file.isFile()||file.length()<1||file.length()>MAX_BYTES)throw new IOException("Taille de surcouche invalide");
    ArrayList<CanonicalRadar> records=new ArrayList<>();HashSet<String> ids=new HashSet<>();
    try(BufferedReader reader=new BufferedReader(new InputStreamReader(new FileInputStream(file),StandardCharsets.UTF_8))){
      if(!CanonicalRadar.HEADER.equals(line(reader)))throw new IOException("En-tête canonique invalide");String row;
      while((row=line(reader))!=null){
        if(records.size()>=MAX_RECORDS)throw new IOException("Trop de radars importés");
        CanonicalRadar record=CanonicalRadar.decode(row);
        if(!country.equals(record.country)||!ids.add(record.id))throw new IOException("Pays ou identifiant radar invalide");records.add(record);
      }
    }
    if(records.isEmpty())throw new IOException("Surcouche radar vide");return Collections.unmodifiableList(records);
  }
  private static String line(Reader reader)throws IOException{
    StringBuilder out=new StringBuilder();int c;
    while((c=reader.read())!=-1){if(c=='\n')return out.toString();if(c=='\r')continue;if(out.length()>=65536)throw new IOException("Ligne trop volumineuse");out.append((char)c);}
    return out.length()==0?null:out.toString();
  }
  static synchronized File install(File updates,String country,List<CanonicalRadar> records,Cancellation cancellation)throws IOException{
    if(records==null||records.isEmpty()||records.size()>MAX_RECORDS)throw new IOException("Surcouche vide ou trop volumineuse");
    File target=file(updates,country),folder=target.getParentFile();
    if(!folder.isDirectory()&&!folder.mkdirs())throw new IOException("Stockage des surcouches inaccessible");
    File pending=File.createTempFile(country.toLowerCase(Locale.ROOT)+"-", ".pending",folder);
    File backupTemp=null,olderTemp=null;File backup=new File(target+".backup"),older=new File(target+".backup.previous");
    try{
      try(FileOutputStream output=new FileOutputStream(pending);Writer writer=new OutputStreamWriter(output,StandardCharsets.UTF_8)){
        writer.write(CanonicalRadar.HEADER);writer.write('\n');long chars=CanonicalRadar.HEADER.length()+1;
        for(CanonicalRadar record:records){cancellation.check();if(!country.equals(record.country))throw new IOException("Pays de surcouche incorrect");String row=record.encode();chars+=row.length()+1;if(chars>MAX_BYTES)throw new IOException("Surcouche trop volumineuse");writer.write(row);writer.write('\n');}
        writer.flush();output.getFD().sync();
      }
      read(pending,country);cancellation.check();
      boolean validCurrent=valid(target,country),validBackup=valid(backup,country);
      if(validCurrent){
        if(validBackup){olderTemp=copy(backup,folder,cancellation);replace(olderTemp,older);olderTemp=null;}
        backupTemp=copy(target,folder,cancellation);replace(backupTemp,backup);backupTemp=null;
      }
      cancellation.check();replace(pending,target);return target;
    }finally{
      pending.delete();if(backupTemp!=null)backupTemp.delete();if(olderTemp!=null)olderTemp.delete();
    }
  }
  private static boolean valid(File file,String country){try{read(file,country);return true;}catch(IOException invalid){return false;}}
  private static File copy(File source,File folder,Cancellation cancellation)throws IOException{
    File temp=File.createTempFile("backup-",".pending",folder);boolean complete=false;
    try(InputStream input=new FileInputStream(source);FileOutputStream output=new FileOutputStream(temp)){
      byte[] buffer=new byte[8192];int n;long total=0;
      while((n=input.read(buffer))!=-1){cancellation.check();if((total+=n)>MAX_BYTES)throw new IOException("Sauvegarde trop volumineuse");output.write(buffer,0,n);}
      output.getFD().sync();complete=true;return temp;
    }finally{if(!complete)temp.delete();}
  }
  private static void replace(File source,File target)throws IOException{
    try{Files.move(source.toPath(),target.toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
    catch(AtomicMoveNotSupportedException unsupported){throw new IOException("Remplacement atomique indisponible ; surcouche précédente conservée",unsupported);}
  }
}
