package fr.roadguard.app;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Canonical supplemental record. Unknown speed/direction are kept unknown. */
final class CanonicalRadar {
  static final String HEADER="id;country;lat;lon;kind;speed_kmh;direction_deg;source_id;source;source_date;reliability;metadata_b64";
  enum Kind { FIXED, RED_LIGHT, AVERAGE, MOBILE, CROSSING, WORKS, UNKNOWN }
  final String id,country,sourceId,source,sourceDate,reliability;
  final double lat,lon;
  final int speedKmh;
  final Double direction;
  final Kind kind;
  final Map<String,String> metadata;
  CanonicalRadar(String country,String sourceId,double lat,double lon,Kind kind,int speedKmh,Double direction,
      String source,String sourceDate,String reliability,Map<String,String> metadata)throws IOException{
    if(country==null||!country.matches("[A-Z]{2}")||!AlertMilestones.validCoordinates(lat,lon)||kind==null
        ||speedKmh!=0&&(speedKmh<5||speedKmh>300)||direction!=null&&(!Double.isFinite(direction)||direction<0||direction>=360))
      throw new IOException("Point canonique invalide");
    field(sourceId,128);field(source,100);field(sourceDate,48);field(reliability,48);
    if(sourceId.isEmpty()||source.isEmpty()||reliability.isEmpty()||metadata==null||metadata.size()>32)
      throw new IOException("Provenance radar absente ou trop volumineuse");
    TreeMap<String,String> copy=new TreeMap<>();int chars=0;
    for(Map.Entry<String,String> entry:metadata.entrySet()){
      String key=entry.getKey(),value=entry.getValue();
      if(key==null||value==null||key.isEmpty()||key.length()>128||value.length()>2048||(chars+=key.length()+value.length())>8192)
        throw new IOException("Métadonnées radar trop volumineuses");
      copy.put(key,value);
    }
    this.country=country;this.sourceId=sourceId;this.lat=lat;this.lon=lon;this.kind=kind;this.speedKmh=speedKmh;this.direction=direction;
    this.source=source;this.sourceDate=sourceDate;this.reliability=reliability;this.metadata=Collections.unmodifiableMap(copy);
    id=identity(country+"\t"+sourceId+"\t"+Double.toString(lat)+"\t"+Double.toString(lon)+"\t"+kind.name());
  }
  private static void field(String value,int max)throws IOException{
    if(value==null||value.length()>max||value.indexOf(';')>=0||value.indexOf('\n')>=0||value.indexOf('\r')>=0||value.indexOf(0)>=0)
      throw new IOException("Champ radar invalide");
  }
  private static String identity(String text)throws IOException{
    try{byte[] hash=MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));StringBuilder result=new StringBuilder("sa-");for(int i=0;i<16;i++)result.append(String.format(Locale.ROOT,"%02x",hash[i]&255));return result.toString();}
    catch(java.security.NoSuchAlgorithmException error){throw new IOException(error);}
  }
  static Kind kind(String raw){
    String name=raw==null?"":raw.trim().toLowerCase(Locale.ROOT).replace('-','_').replace(' ','_');
    switch(name){
      case "speed_camera":case "fixed":case "fixed_camera":case "radar_fixe":return Kind.FIXED;
      case "red_light":case "redlight":case "red_light_camera":case "radar_feu_rouge":return Kind.RED_LIGHT;
      case "average_speed":case "average":case "section":return Kind.AVERAGE;
      case "mobile":case "mobile_camera":return Kind.MOBILE;
      case "level_crossing":case "crossing":return Kind.CROSSING;
      case "construction":case "works":return Kind.WORKS;
      default:return Kind.UNKNOWN;
    }
  }
  String label(){
    switch(kind){case FIXED:return "Radar fixe";case RED_LIGHT:return "Radar feu rouge";case AVERAGE:return "Radar vitesse moyenne";
      case MOBILE:return "Radar mobile";case CROSSING:return "Radar passage à niveau";case WORKS:return "Radar chantier";
      default:return "Radar · type non renseigné";}
  }
  BundledRadarCatalog.Point point(){return new BundledRadarCatalog.Point(id,label(),lat,lon,kind.name(),kind==Kind.MOBILE||kind==Kind.WORKS,source,country,speedKmh,direction,reliability);}
  String encode()throws IOException{
    ByteArrayOutputStream buffer=new ByteArrayOutputStream();
    try(DataOutputStream data=new DataOutputStream(buffer)){data.writeInt(metadata.size());for(Map.Entry<String,String> entry:metadata.entrySet()){data.writeUTF(entry.getKey());data.writeUTF(entry.getValue());}}
    return id+";"+country+";"+lat+";"+lon+";"+kind.name()+";"+speedKmh+";"+(direction==null?"":direction)+";"+sourceId+";"+source+";"+sourceDate+";"+reliability+";"+Base64.getUrlEncoder().withoutPadding().encodeToString(buffer.toByteArray());
  }
  static CanonicalRadar decode(String line)throws IOException{
    if(line.length()>65536)throw new IOException("Ligne canonique trop longue");
    String[] c=line.split(";",-1);if(c.length!=12||c[11].length()>40000)throw new IOException("Ligne canonique invalide");
    try{
      Map<String,String> metadata=new TreeMap<>();
      try(DataInputStream data=new DataInputStream(new ByteArrayInputStream(Base64.getUrlDecoder().decode(c[11])))){
        int count=data.readInt();if(count<0||count>32)throw new IOException("Trop de métadonnées");
        for(int i=0;i<count;i++){String key=data.readUTF(),value=data.readUTF();if(metadata.put(key,value)!=null)throw new IOException("Métadonnée dupliquée");}
        if(data.read()!=-1)throw new IOException("Métadonnées supplémentaires non reconnues");
      }
      CanonicalRadar radar=new CanonicalRadar(c[1],c[7],Double.parseDouble(c[2]),Double.parseDouble(c[3]),Kind.valueOf(c[4]),Integer.parseInt(c[5]),c[6].isEmpty()?null:Double.valueOf(c[6]),c[8],c[9],c[10],metadata);
      if(!radar.id.equals(c[0]))throw new IOException("Identifiant canonique incohérent");return radar;
    }catch(IllegalArgumentException invalid){throw new IOException("Valeur canonique invalide",invalid);}
  }
}
