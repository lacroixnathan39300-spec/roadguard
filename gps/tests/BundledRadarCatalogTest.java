package fr.roadguard.app;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Executes the real APK CSV parser against both shipped countries. */
public final class BundledRadarCatalogTest {
  static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
  static String text(File file)throws Exception{
    ByteArrayOutputStream out=new ByteArrayOutputStream();
    try(InputStream in=new FileInputStream(file)){byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);}
    return new String(out.toByteArray(),StandardCharsets.UTF_8);
  }
  static List<BundledRadarCatalog.Point> parse(String data,String country)throws Exception{
    return BundledRadarCatalog.read(new BufferedReader(new StringReader(data)),country);
  }
  static void refused(String data,String country)throws Exception{
    try{parse(data,country);throw new AssertionError("Invalid inventory accepted");}catch(IOException expected){}
  }
  public static void main(String[] args)throws Exception{
    File assets=new File(args[0]);String fr=text(new File(assets,BundledRadarCatalog.FR_ASSET));
    String ch=text(new File(assets,BundledRadarCatalog.CH_ASSET));
    List<BundledRadarCatalog.Point> france=parse(fr,"FR"),swiss=parse(ch,"CH");
    int staticCount=0,historicalCount=0;boolean hasCrossing=false,hasRedlight=false,hasSection=false,hasWorks=false;
    for(BundledRadarCatalog.Point p:france){
      if(p.historical)historicalCount++;else staticCount++;
      if(p.category.equals("crossing"))hasCrossing=true;
      if(p.category.equals("redlight"))hasRedlight=true;
      if(p.category.equals("average_start"))hasSection=true;
      if(p.category.equals("works")){hasWorks=true;require(p.historical,"Works presence misrepresented");}
      require(DangerTypePolicy.isRadar(p.type),"Unclassified radar");
    }
    require(france.size()>23000&&swiss.size()>1500,"Incomplete bundled data");
    require(staticCount>6000&&historicalCount>16000,"Historical/static separation lost");
    require(hasCrossing&&hasRedlight&&hasSection&&hasWorks,"Detailed categories lost");
    require(DangerTypePolicy.matchesFilter("Zone de contrôle possible · présence non confirmée","danger"),"Temporary category filter lost");
    // Each individual file remains country-validated before deliberate FR/CH combination.
    refused(ch,"FR");refused(fr,"CH");refused(fr,"BE");
    String[] lines=fr.split("\n",3);String one=lines[0]+"\n"+lines[1]+"\n";
    refused(one+lines[1]+"\n","FR");
    String[] row=lines[1].split(";",-1);row[2]="NaN";
    refused(lines[0]+"\n"+String.join(";",row)+"\n","FR");
    row=lines[1].split(";",-1);row[4]="CH";
    refused(lines[0]+"\n"+String.join(";",row)+"\n","FR");
    List<BundledRadarCatalog.Point> cached=BundledRadarCatalog.france(name->new FileInputStream(new File(assets,name)));
    require(cached.size()==france.size(),"Cached inventory differs");
    require(cached==BundledRadarCatalog.france(name->{throw new IOException("Unexpected reopen");}),"Process cache not reused");
    try{cached.clear();throw new AssertionError("Shared inventory mutable");}catch(UnsupportedOperationException expected){}
    BundledRadarCatalog.Source source=name->new FileInputStream(new File(assets,name));
    List<BundledRadarCatalog.Point> chCached=BundledRadarCatalog.switzerland(source);
    require(chCached.size()==swiss.size(),"Swiss cache is not loaded");
    require(chCached==BundledRadarCatalog.switzerland(name->{throw new IOException("Unexpected Swiss reopen");}),"Swiss cache not reused");
    List<BundledRadarCatalog.Point> defaults=BundledRadarCatalog.active(source,false,true);
    require(defaults.size()==9102,"Default combined inventory count differs");
    int frActive=0,chActive=0,chMobile=0;boolean chRed=false,chTunnel=false,chVariable=false,chAverage=false;
    double[] lat=new double[defaults.size()],lon=new double[defaults.size()];int firstSwiss=-1;
    for(int i=0;i<defaults.size();i++){
      BundledRadarCatalog.Point point=defaults.get(i);lat[i]=point.lat;lon[i]=point.lon;
      if("CH".equals(point.country)){
        chActive++;if(firstSwiss<0)firstSwiss=i;
        if(point.historical){chMobile++;require(point.type.contains("non confirmée"),"Mobile presence misrepresented");}
        if(point.category.equals("redlight"))chRed=true;
        if(point.category.equals("tunnel"))chTunnel=true;
        if(point.category.equals("variable"))chVariable=true;
        if(point.category.equals("average"))chAverage=true;
      }else {frActive++;require(!point.historical,"Swiss preference enabled French historical points");}
    }
    require(frActive==7325&&chActive==1777&&chMobile==194,"Per-country default selection differs");
    require(chRed&&chTunnel&&chVariable&&chAverage,"Swiss types missing from active data");
    require(BundledRadarCatalog.active(source,false,false).size()==8908,"Swiss mobile switch not applied");
    require(BundledRadarCatalog.active(source,true,false).size()==26012,"French preference changed Swiss mobile switch");
    require(BundledRadarCatalog.active(source,true,true).size()==26206,"Full combined inventory incomplete");
    DangerSpatialIndex index=new DangerSpatialIndex(lat,lon);
    require(index.query(lat[firstSwiss],lon[firstSwiss],100).contains(firstSwiss),"Swiss point not reachable by the map/alert spatial query");
    System.out.println("Bundled parser/selection OK: default FR="+frActive+", CH="+chActive+" ("+chMobile+" unconfirmed mobile), total="+defaults.size()+"; country validation, independent switches, Swiss categories and spatial query OK");
  }
}
