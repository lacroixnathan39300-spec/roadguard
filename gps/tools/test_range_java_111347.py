from pathlib import Path
import subprocess,tempfile
s=(Path(__file__).resolve().parents[1]/'app/src/main/java/fr/roadguard/app/MainActivity.java').read_text()
a=s.index('  static boolean validPmtilesRange(');b=s.index('  void downloadPmtilesPack(',a)
method=s[a:b]
code='public class RangeTest {\n'+method+'''  public static void main(String[] args){
    String[] good={"bytes 0-9/100", "bytes 25-99/100"};
    if(!validPmtilesRange(good[0],0,100)||!validPmtilesRange(good[1],25,100))throw new AssertionError("valid range rejected");
    String[] bad={"bytes 24-99/100","bytes 25-100/100","bytes 25-99/101","bytes */100","bytes 25-99/*","bytes 25-24/100","bytes 25-99/100junk","bytes 25-99/0","bytes 25-99/9999999999999999999999999"};
    for(String v:bad)if(validPmtilesRange(v,25,100))throw new AssertionError("invalid range accepted: "+v);
    if(validPmtilesRange(null,25,100)||validPmtilesRange("bytes 0-9/100",100,100))throw new AssertionError("invalid state accepted");
    System.out.println("PASS 13 HTTP Range cases (real Java method)");
  }
}\n'''
with tempfile.TemporaryDirectory() as d:
 p=Path(d)/'RangeTest.java';p.write_text(code)
 subprocess.run(['javac',str(p)],check=True)
 subprocess.run(['java','-cp',d,'RangeTest'],check=True)
