"""Integration tests: real Java PMTiles reader with synthetic v3 files.
Uses valid directory encodings and genuine PNG bytes; no Android emulator required.
"""
from pathlib import Path
import tempfile, subprocess, struct, gzip, base64
ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'app/src/main/java/fr/roadguard/app/PmtilesReader.java'
PNG=base64.b64decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/9XcAAAAASUVORK5CYII=")
def v(n):
    b=bytearray()
    while n>127:b.append((n&127)|128);n>>=7
    b.append(n);return bytes(b)
def directory(entries):
    # entries (tile id, run, length, offset); PMTiles v3 column-oriented directory
    data=v(len(entries));prev=0
    for i,run,length,offset in entries:
        data+=v(i-prev);prev=i
    for i,run,length,offset in entries:data+=v(run)
    for i,run,length,offset in entries:data+=v(length)
    prev_end=0
    for i,run,length,offset in entries:
        data+=v(0 if offset==prev_end else offset+1)
        prev_end=offset+length
    return data
def make(path,*,compressed=False,leaf=False,invalid=False):
    tile=gzip.compress(PNG) if compressed else PNG
    inner=directory([(0,1,len(tile),0)])
    inner=gzip.compress(inner) if compressed else inner
    root=directory([(0,0,len(inner),0)]) if leaf else inner
    if compressed and leaf:root=gzip.compress(root)
    leafdata=inner if leaf else b''
    head=bytearray(127);head[:7]=b'PMTiles';head[7]=3
    ro=127;lo=ro+len(root);to=lo+len(leafdata)
    for off,val in [(8,ro),(16,len(root)),(40,lo),(48,len(leafdata)),(56,to),(64,len(tile))]:
        struct.pack_into('<Q',head,off,val)
    head[97]=2 if compressed else 1;head[98]=2 if compressed else 1;head[99]=2
    if invalid:struct.pack_into('<Q',head,64,9999999)
    path.write_bytes(head+root+leafdata+tile)
with tempfile.TemporaryDirectory() as td:
    td=Path(td)
    for name,kw in [('plain',{}),('gzip',{'compressed':True}),('leaf',{'leaf':True}),('leafgzip',{'leaf':True,'compressed':True}),('invalid',{'invalid':True})]:
        make(td/(name+'.pmtiles'),**kw)
    (td/'Test.java').write_text("""
import fr.roadguard.app.PmtilesReader;import java.io.*;import java.util.*;
public class Test{
 public static void main(String[] a)throws Exception{
  int ok=0;
  for(int i=0;i<4;i++){
   System.out.println("testing "+i); PmtilesReader r=new PmtilesReader(new File(a[i]));
   if(!r.isRaster()||!"image/png".equals(r.mimeType()))throw new AssertionError("type "+i);
   byte[] png=r.rasterTile(0,0,0);
   if(png==null||png.length<20||png[0]!=(byte)0x89||png[1]!='P')throw new AssertionError("tile "+i);
   if(r.rasterTile(1,0,0)!=null)throw new AssertionError("unexpected tile "+i);
   ok+=3;
  }
  try{new PmtilesReader(new File(a[4]));throw new AssertionError("bad range accepted");}
  catch(IOException expected){ok++;}
  System.out.println("PMTiles extended: "+ok+" checks passed");
 }
}""")
    subprocess.run(['java','com.sun.tools.javac.Main','-d',str(td),str(JAVA),str(td/'Test.java')],check=True)
    subprocess.run(['java','-cp',str(td),'Test',*[str(td/(n+'.pmtiles')) for n in ['plain','gzip','leaf','leafgzip','invalid']]],check=True)
