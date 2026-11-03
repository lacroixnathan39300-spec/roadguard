from pathlib import Path
import subprocess,tempfile
root=Path(__file__).resolve().parents[1]
src=root/'app/src/main/java/fr/roadguard/app'
with tempfile.TemporaryDirectory() as d:
 p=Path(d)
 (p/'TestRecovery.java').write_text('''import fr.roadguard.app.*;import java.io.*;import java.nio.file.*;import java.security.*;
public class TestRecovery {
 static String hash(byte[] b)throws Exception{byte[] h=MessageDigest.getInstance("SHA-256").digest(b);StringBuilder s=new StringBuilder();for(byte v:h)s.append(String.format("%02x",v&255));return s.toString();}
 public static void main(String[]a)throws Exception{
  File dir=new File(a[0]);dir.mkdirs();byte[] bytes="map-data".getBytes("UTF-8");Files.write(new File(dir,"tile.pmtiles").toPath(),bytes);
  OfflineCellStore s=new OfflineCellStore(dir);s.register("5km:1:2","tile.pmtiles",hash(bytes));
  File primary=new File(dir,"completed-sectors.tsv"),backup=new File(dir,"completed-sectors.tsv.bak");
  if(!primary.renameTo(backup))throw new RuntimeException("backup simulation failed");
  OfflineCellStore recovered=new OfflineCellStore(dir);
  if(!recovered.hasVerified("5km:1:2")||!primary.isFile())throw new RuntimeException("crash recovery failed");
  Files.write(new File(dir,"tile.pmtiles").toPath(),"corrupt".getBytes("UTF-8"));
  if(recovered.hasVerified("5km:1:2"))throw new RuntimeException("corruption accepted");
  System.out.println("PASS: recovery, checksum and index persistence");
 }
}''')
 subprocess.run(['javac','-d',d,str(src/'NearbyOfflinePlanner.java'),str(src/'OfflineCellStore.java'),str(p/'TestRecovery.java')],check=True)
 subprocess.run(['java','-cp',d,'TestRecovery',str(p/'data')],check=True)
