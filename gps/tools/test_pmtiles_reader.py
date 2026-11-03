import pathlib, subprocess, tempfile, struct, gzip
root=pathlib.Path(__file__).resolve().parents[1]
java=root/'app/src/main/java/fr/roadguard/app/PmtilesReader.java'
def v(n):
 out=bytearray()
 while n>=128:out.append((n&127)|128);n>>=7
 out.append(n);return bytes(out)
# One PNG tile at z0/0/0, PMTiles v3 directory (count,id delta,run,length,offset+1)
png=b"\x89PNG\r\n\x1a\n"+b"test-tile-data"
directory=b''.join([v(1),v(0),v(1),v(len(png)),v(1)])
header=bytearray(127);header[:7]=b'PMTiles';header[7]=3
for off,val in [(8,127),(16,len(directory)),(56,127+len(directory)),(64,len(png))]:struct.pack_into('<Q',header,off,val)
header[97]=1;header[98]=1;header[99]=2;header[100]=0;header[101]=0
with tempfile.TemporaryDirectory() as d:
 d=pathlib.Path(d);f=d/'test.pmtiles';f.write_bytes(header+directory+png)
 runner=d/'Test.java';runner.write_text('import fr.roadguard.app.PmtilesReader; import java.io.*;\npublic class Test {public static void main(String[] a)throws Exception {\n PmtilesReader r=new PmtilesReader(new File(a[0]));\n if(!r.isRaster()||!"image/png".equals(r.mimeType()))throw new AssertionError("mime");\n byte[] t=r.rasterTile(0,0,0);if(t==null||t.length!=22)throw new AssertionError("tile");\n if(r.rasterTile(1,0,0)!=null)throw new AssertionError("missing tile");\n System.out.println("PMTiles reader: 3 checks passed");\n}}')
 subprocess.run(['java','com.sun.tools.javac.Main','-d',str(d),str(java),str(runner)],check=True)
 subprocess.run(['java','-cp',str(d),'Test',str(f)],check=True)
