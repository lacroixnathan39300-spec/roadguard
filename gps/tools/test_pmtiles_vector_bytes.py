"""Java integration: read a synthetic vector PMTiles tile, including gzip."""
from pathlib import Path
import tempfile, subprocess, struct, gzip
root=Path(__file__).resolve().parents[1]
java=root/'app/src/main/java/fr/roadguard/app/PmtilesReader.java'
def varint(n):
    out=bytearray()
    while n>127:out.append((n&127)|128);n>>=7
    out.append(n);return bytes(out)
with tempfile.TemporaryDirectory() as td:
    t=Path(td)
    # Minimal syntactically valid protobuf MVT with empty layer name, version 2, extent 4096.
    payload=b'\x1a\x09\x0a\x00\x28\x80\x20\x78\x02\x12\x00'
    for compressed in [False,True]:
        tile=gzip.compress(payload) if compressed else payload
        directory=b''.join(varint(x) for x in [1,0,1,len(tile),1])
        if compressed:directory=gzip.compress(directory)
        h=bytearray(127);h[:7]=b'PMTiles';h[7]=3
        for off,val in [(8,127),(16,len(directory)),(56,127+len(directory)),(64,len(tile))]:struct.pack_into('<Q',h,off,val)
        h[97]=2 if compressed else 1;h[98]=2 if compressed else 1;h[99]=1
        (t/('gzip.pmtiles' if compressed else 'plain.pmtiles')).write_bytes(h+directory+tile)
    (t/'Test.java').write_text('''import fr.roadguard.app.PmtilesReader;import java.io.*;import java.util.*;
public class Test {public static void main(String[] a)throws Exception {
for(String p:a){PmtilesReader r=new PmtilesReader(new File(p));
if(!r.isVector()||r.isRaster())throw new AssertionError("type");
if(!"application/vnd.mapbox-vector-tile".equals(r.vectorMimeType()))throw new AssertionError("mime");
byte[] tile=r.vectorTile(0,0,0);if(tile==null||tile.length!=11||tile[0]!=26)throw new AssertionError("data");
if(r.vectorTile(1,0,0)!=null)throw new AssertionError("missing");
try{r.rasterTile(0,0,0);throw new AssertionError("raster accepted");}catch(IOException expected){}
}System.out.println("Vector raw tile: 10 assertions passed");}}''')
    subprocess.run(['java','com.sun.tools.javac.Main','-d',str(t),str(java),str(t/'Test.java')],check=True)
    subprocess.run(['java','-cp',str(t),'Test',str(t/'plain.pmtiles'),str(t/'gzip.pmtiles')],check=True)
