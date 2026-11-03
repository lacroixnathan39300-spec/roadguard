import fr.roadguard.app.OfflineVectorZoom;
import java.io.*;
import java.nio.file.*;
public class OfflineVectorZoomTest {
  public static void main(String[] args)throws Exception {
    File real=new File(args[0]);
    int z=OfflineVectorZoom.read(real);
    if(z<0||z>22)throw new AssertionError("Invalid real archive max zoom");
    File temp=Files.createTempFile("rg-bad-zoom",".pmtiles").toFile();
    try(FileOutputStream out=new FileOutputStream(temp)){out.write(new byte[127]);}
    try{OfflineVectorZoom.read(temp);throw new AssertionError("Invalid archive accepted");}catch(IOException expected){}
    temp.delete();
    System.out.println("PASS: real PMTiles max zoom="+z+", invalid archive rejected");
  }
}
