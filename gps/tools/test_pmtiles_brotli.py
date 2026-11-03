"""Exercise PMTiles vector-tile Brotli decoding with a tiny local fixture."""
from pathlib import Path
import struct, subprocess, tempfile

ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'app/src/main/java/fr/roadguard/app/PmtilesReader.java'
JAR=ROOT/'app/libs/brotli-dec-0.1.2.jar'
def v(n):
 out=bytearray()
 while n>=128:out.append((n&127)|128);n>>=7
 out.append(n);return bytes(out)

with tempfile.TemporaryDirectory() as td:
 td=Path(td);plain=b'ROADGUARD BROTLI VECTOR TILE TEST';src=td/'tile.bin';src.write_bytes(plain)
 compressed=subprocess.check_output(['node','-e',"const z=require('zlib'),fs=require('fs');process.stdout.write(z.brotliCompressSync(fs.readFileSync(process.argv[1])));",str(src)])
 directory=b''.join((v(1),v(0),v(1),v(len(compressed)),v(1)))
 header=bytearray(127);header[:7]=b'PMTiles';header[7]=3
 for off,val in [(8,127),(16,len(directory)),(56,127+len(directory)),(64,len(compressed))]:struct.pack_into('<Q',header,off,val)
 header[97]=1;header[98]=3;header[99]=1;header[100]=0;header[101]=0
 archive=td/'brotli.pmtiles';archive.write_bytes(header+directory+compressed)
 runner=td/'Test.java';runner.write_text('''import fr.roadguard.app.PmtilesReader;import java.io.*;import java.util.*;
public class Test{public static void main(String[]a)throws Exception{PmtilesReader r=new PmtilesReader(new File(a[0]));byte[] got=r.vectorTile(0,0,0);byte[] expected="ROADGUARD BROTLI VECTOR TILE TEST".getBytes("UTF-8");if(!Arrays.equals(got,expected))throw new AssertionError("Brotli tile differs");System.out.println("PMTiles Brotli: tile decoded");}}''')
 subprocess.run(['java','com.sun.tools.javac.Main','-classpath',str(JAR),'-d',str(td),str(JAVA),str(runner)],check=True)
 subprocess.run(['java','-classpath',str(td)+':'+str(JAR),'Test',str(archive)],check=True)
