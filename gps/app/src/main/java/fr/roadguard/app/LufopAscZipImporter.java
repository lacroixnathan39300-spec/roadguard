package fr.roadguard.app;

import java.io.*;
import java.util.Locale;
import java.util.zip.*;
import java.util.regex.*;

/** Converts authorized locally supplied Lufop ASC archives into a separate overlay.
 *  Does not modify any packaged radar database. */
final class LufopAscZipImporter {
  interface Progress { void onPoints(int points); }
  private static final Pattern ROW=Pattern.compile("^\\s*([-+]?\\d+(?:\\.\\d+)?)\\s*,\\s*([-+]?\\d+(?:\\.\\d+)?)\\s*,\\s*\"(.*)\"\\s*$");
  private static final Pattern SPEED=Pattern.compile("(?:^|[^0-9])([1-9][0-9]{1,2})(?:$|[^0-9])");
  private static final long MAX_ASC_ENTRY_BYTES=4_000_000L;
  private LufopAscZipImporter(){}
  /** Reject absolute paths, parent traversal and control characters even though entries are not extracted. */
  static boolean safeEntryName(String name){
    if(name==null||name.length()>240||name.startsWith("/")||name.startsWith("\\")||name.indexOf(':')>=0)return false;
    String normalized=name.replace('\\','/');
    for(String part:normalized.split("/",-1))if(part.equals("..")||part.equals("."))return false;
    for(int i=0;i<name.length();i++)if(name.charAt(i)<32)return false;
    return true;
  }
  static boolean isAscArchive(InputStream input) throws IOException {
    try(ZipInputStream zip=new ZipInputStream(new BufferedInputStream(input))){
      ZipEntry entry;int scanned=0;
      while((entry=zip.getNextEntry())!=null && ++scanned<=1200){
        if(!entry.isDirectory() && safeEntryName(entry.getName()) && entry.getName().toLowerCase(Locale.ROOT).endsWith(".asc"))return true;
      }
    }catch(ZipException notZip){return false;}
    return false;
  }
  static int convert(InputStream input,File output,Progress progress) throws IOException {
    int count=0,entries=0;long totalBytes=0;
    try(ZipInputStream zip=new ZipInputStream(new BufferedInputStream(input));
        BufferedWriter out=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output),"UTF-8"))){
      out.write("country;type;lat;lon;speed_kmh;id;label");out.newLine();
      ZipEntry entry;
      while((entry=zip.getNextEntry())!=null){
        if(++entries>2000)throw new IOException("Archive trop complexe");
        if(entry.isDirectory())continue;
        if(!safeEntryName(entry.getName()))throw new IOException("Nom de fichier ZIP non autorisé");
        String filename=entry.getName().replace('\\','/');
        filename=filename.substring(filename.lastIndexOf('/')+1);
        if(!filename.toLowerCase(Locale.ROOT).endsWith(".asc")||filename.length()<7)continue;
        String cc=filename.substring(0,2).toUpperCase(Locale.ROOT);
        if(!cc.matches("[A-Z]{2}"))continue;
        String type=filename.toLowerCase(Locale.ROOT).contains("feurouge")?"feu_rouge":filename.toLowerCase(Locale.ROOT).contains("mobile")?"mobile":filename.toLowerCase(Locale.ROOT).contains("fixe")?"radar_fixe":"zone_danger";
        Matcher speedMatch=SPEED.matcher(filename.substring(2));int speed=0;
        while(speedMatch.find()){int v=Integer.parseInt(speedMatch.group(1));if(v>=20&&v<=150)speed=v;}
        // Read each ZIP entry independently: a BufferedReader directly on ZipInputStream
        // can prefetch bytes belonging to the next entry and break subsequent reads.
        ByteArrayOutputStream entryData=new ByteArrayOutputStream();
        byte[] chunk=new byte[8192];int n;long entryBytes=0;
        while((n=zip.read(chunk))!=-1){
          entryBytes+=n;totalBytes+=n;
          if(entryBytes>MAX_ASC_ENTRY_BYTES)throw new IOException("Fichier ASC individuel trop volumineux");
          if(totalBytes>35000000L)throw new IOException("Archive ASC trop volumineuse");
          entryData.write(chunk,0,n);
        }
        BufferedReader reader=new BufferedReader(new InputStreamReader(new ByteArrayInputStream(entryData.toByteArray()),"UTF-8"));
        String line;int row=0;
        while((line=reader.readLine())!=null){
          Matcher match=ROW.matcher(line);if(!match.matches())continue;
          double lon,lat;
          try{lon=Double.parseDouble(match.group(1));lat=Double.parseDouble(match.group(2));}catch(NumberFormatException ex){continue;}
          if(!Double.isFinite(lat)||!Double.isFinite(lon)||Math.abs(lat)>90||Math.abs(lon)>180)continue;
          String label=match.group(3).replace(';',' ').replace('\r',' ').replace('\n',' ');
          String id=cc+"-"+entries+"-"+(++row);
          out.write(cc+";"+type+";"+lat+";"+lon+";"+speed+";"+id+";"+label);out.newLine();
          if(++count>200000)throw new IOException("Trop de points radar");
          if(count%5000==0 && progress!=null)progress.onPoints(count);
        }
        zip.closeEntry();
      }
    }catch(IOException|RuntimeException e){output.delete();throw e;}
    if(count==0){output.delete();throw new IOException("Aucun point ASC valide trouvé");}
    return count;
  }
}
