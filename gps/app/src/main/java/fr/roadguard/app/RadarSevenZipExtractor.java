package fr.roadguard.app;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.zip.CRC32;

/** Extracts exactly one expected SQLite payload to a unique, caller-owned temp file. */
final class RadarSevenZipExtractor {
  static final int MAX_MEMORY_KIB=32768;

  // CodeAssist may compile Java without indexing local JARs from Gradle.
  // Resolve the bundled relocated runtime only when an archive is imported.
  private static final class SevenZ implements Closeable {
    private final Object reader;
    private SevenZ(Object reader){this.reader=reader;}
    static SevenZ open(File archive)throws IOException{
      try{
        Class<?> type=Class.forName("fr.roadguard.vendor.commons.compress.archivers.sevenz.SevenZFile");
        Object builder=invoke(type,"builder");
        invoke(builder,"setFile",archive,File.class);
        invoke(builder,"setMaxMemoryLimitKiB",MAX_MEMORY_KIB,int.class);
        invoke(builder,"setTryToRecoverBrokenArchives",false,boolean.class);
        invoke(builder,"setUseDefaultNameForUnnamedEntries",false,boolean.class);
        return new SevenZ(invoke(builder,"get"));
      }catch(ClassNotFoundException|LinkageError missing){throw new IOException("Bibliothèque radar 7z absente de l’application",missing);}
    }
    Object next()throws IOException{return invoke(reader,"getNextEntry");}
    int read(byte[] buffer)throws IOException{return (Integer)invoke(reader,"read",buffer,byte[].class);}
    @Override public void close()throws IOException{invoke(reader,"close");}
  }
  private static Object invoke(Object receiver,String name)throws IOException{
    return invoke(receiver,name,new Object[0],new Class<?>[0]);
  }
  private static Object invoke(Object receiver,String name,Object value,Class<?> parameter)throws IOException{
    return invoke(receiver,name,new Object[]{value},new Class<?>[]{parameter});
  }
  private static Object invoke(Object receiver,String name,Object[] args,Class<?>[] parameters)throws IOException{
    try{
      Class<?> type=receiver instanceof Class?(Class<?>)receiver:receiver.getClass();
      Method method=type.getMethod(name,parameters);
      return method.invoke(receiver instanceof Class?null:receiver,args);
    }catch(InvocationTargetException error){
      Throwable cause=error.getCause();
      if(cause instanceof IOException)throw (IOException)cause;
      if(cause instanceof Error)throw (Error)cause;
      throw new IOException("Erreur du lecteur radar 7z : "+name,cause);
    }catch(ReflectiveOperationException error){throw new IOException("API du lecteur radar 7z indisponible : "+name,error);}
  }
  static File extract(File archive,String country,File temporary,CanonicalRadarStore.Cancellation cancellation)throws IOException{
    if(country==null||!country.matches("[A-Z]{2}"))throw new IOException("Pays d’archive invalide");
    cancellation.check();RadarArchivePolicy.verify(archive);
    if(!temporary.isDirectory()&&!temporary.mkdirs())throw new IOException("Stockage temporaire inaccessible");
    File output=File.createTempFile(country.toLowerCase(Locale.ROOT)+"-camera-",".sqlite",temporary);boolean complete=false;
    String expected=country.toLowerCase(Locale.ROOT)+"-camera.sqlite";
    try(SevenZ seven=SevenZ.open(archive)){
      cancellation.check();Object entry=seven.next();
      if(entry==null||!expected.equals(invoke(entry,"getName"))||(Boolean)invoke(entry,"isDirectory")||(Boolean)invoke(entry,"isAntiItem")
          ||!(Boolean)invoke(entry,"hasStream")||(Long)invoke(entry,"getSize")<512||(Long)invoke(entry,"getSize")>CanonicalRadarStore.MAX_BYTES
          ||!(Boolean)invoke(entry,"getHasCrc"))
        throw new IOException("L’archive doit contenir uniquement "+expected+" (32 Mo maximum)");
      int methods=0;
      for(Object method:(Iterable<?>)invoke(entry,"getContentMethods")){
        methods++;String codec=((Enum<?>)invoke(method,"getMethod")).name();
        if(!"COPY".equals(codec)&&!"LZMA".equals(codec)&&!"LZMA2".equals(codec))
          throw new IOException("Compression 7z non autorisée : "+codec);
      }
      if(methods<1||methods>2)throw new IOException("Chaîne de compression 7z invalide");
      long size=(Long)invoke(entry,"getSize");long expectedCrc=(Long)invoke(entry,"getCrcValue");
      long total=0;CRC32 crc=new CRC32();
      try(FileOutputStream writer=new FileOutputStream(output)){
        byte[] buffer=new byte[16384];int n;
        while((n=seven.read(buffer))!=-1){
          cancellation.check();if(n>size-total||n>CanonicalRadarStore.MAX_BYTES-total)throw new IOException("Taille SQLite décompressée dépassée");
          total+=n;crc.update(buffer,0,n);writer.write(buffer,0,n);
        }
        if(total!=size||crc.getValue()!=expectedCrc)throw new IOException("Taille ou CRC SQLite incorrect");
        writer.getFD().sync();
      }
      cancellation.check();if(seven.next()!=null)throw new IOException("Plusieurs entrées 7z non autorisées");
      try(InputStream input=new FileInputStream(output)){if(!SpeedAlertSqliteImporter.sqliteHeader(input))throw new IOException("Signature SQLite extraite invalide");}
      complete=true;return output;
    }finally{if(!complete)output.delete();}
  }
  private RadarSevenZipExtractor(){}
}
