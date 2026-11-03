import fr.roadguard.app.*;
import java.io.*;
import java.nio.file.*;
import java.security.*;

public class LocalPmtilesResolverTest {
  static String sha(File f) throws Exception {
    MessageDigest md=MessageDigest.getInstance("SHA-256");
    try(InputStream in=new FileInputStream(f)){byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)md.update(b,0,n);}
    StringBuilder s=new StringBuilder();for(byte b:md.digest())s.append(String.format("%02x",b&255));return s.toString();
  }
  public static void main(String[] args) throws Exception {
    File source=new File(args[0]);File root=Files.createTempDirectory("rg-resolver").toFile();
    File legacy=new File(root,"pmtiles");if(!legacy.mkdirs())throw new AssertionError();
    OfflineCellStore store=new OfflineCellStore(new File(root,"offline-cells"));
    File old=new File(legacy,"firenze.pmtiles");Files.write(old.toPath(),new byte[]{1,2,3});
    if(!old.equals(LocalPmtilesResolver.resolve("firenze",store,legacy)))throw new AssertionError("legacy fallback");
    store.importVerified("firenze",new FileInputStream(source),sha(source),source.length()+1);
    File selected=LocalPmtilesResolver.resolve("firenze",store,legacy);
    if(selected==null||selected.equals(old)||!selected.getParentFile().getName().equals("offline-cells"))throw new AssertionError("verified must win");
    if(LocalPmtilesResolver.resolve("../firenze",store,legacy)!=null)throw new AssertionError("unsafe id");
    if(LocalPmtilesResolver.resolve("unknown",store,legacy)!=null)throw new AssertionError("missing id");
    // Corrupt the verified copy: never serve it; legacy fallback remains explicit.
    try(FileOutputStream out=new FileOutputStream(selected,true)){out.write(0);}
    if(!old.equals(LocalPmtilesResolver.resolve("firenze",store,legacy)))throw new AssertionError("corrupt verification fallback");
    System.out.println("PASS: verified pack priority, legacy fallback, invalid ID, corruption");
  }
}
