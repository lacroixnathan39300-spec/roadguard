"""Actual Java download registry + mosaic + PMTiles bytes; no Android renderer."""
from pathlib import Path
import tempfile,subprocess,struct,hashlib
root=Path(__file__).resolve().parents[1]
with tempfile.TemporaryDirectory() as d:
 t=Path(d);legacy=t/'pmtiles';sectors=t/'offline-cells';legacy.mkdir();sectors.mkdir()
 payload=b'\x1a\x00';directory=bytes([1,0,1,len(payload),1]);h=bytearray(127);h[:7]=b'PMTiles';h[7]=3
 for off,val in [(8,127),(16,len(directory)),(56,127+len(directory)),(64,len(payload))]:struct.pack_into('<Q',h,off,val)
 h[97]=h[98]=h[99]=1;h[101]=14
 for off,val in [(102,-100000000),(106,400000000),(110,100000000),(114,500000000)]:struct.pack_into('<i',h,off,val)
 data=h+directory+payload
 (legacy/'local.pmtiles').write_bytes(data);(sectors/'downloaded.pmtiles').write_bytes(data);(sectors/'orphan.pmtiles').write_bytes(data);(sectors/'unfinished.pmtiles.part').write_bytes(data)
 (sectors/'completed-sectors.tsv').write_text('downloaded\tdownloaded.pmtiles\t'+str(len(data))+'\t'+hashlib.sha256(data).hexdigest()+'\n')
 (t/'Probe.java').write_text('''package fr.roadguard.app;
import java.io.*;import java.util.*;
class Probe { public static void main(String[] args)throws Exception {
File legacy=new File(args[0]),sectors=new File(args[1]);
List<InstalledPmtilesMosaic.Region> all=InstalledPmtilesMosaic.installed(legacy,sectors);
if(all.size()!=2)throw new AssertionError("completed and manual only: "+all.size());
for(InstalledPmtilesMosaic.Region r:all)if(new PmtilesReader(r.file).vectorTile(0,0,0).length!=2)throw new AssertionError("payload");
OfflineCellStore store=new OfflineCellStore(sectors);if(store.verifiedFile("downloaded")==null)throw new AssertionError("checksum");
if(LocalPmtilesResolver.resolve("downloaded",store,legacy)==null)throw new AssertionError("download resolution");
if(LocalPmtilesResolver.resolve("../downloaded",store,legacy)!=null)throw new AssertionError("path escape");
try(FileOutputStream out=new FileOutputStream(new File(sectors,"downloaded.pmtiles"),true)){out.write(1);}
if(InstalledPmtilesMosaic.installed(legacy,sectors).size()!=1)throw new AssertionError("truncated/replaced file retained");
System.out.println("PASS registered sector + manual mosaic, actual vector payload, checksum, resolver, path guard and changed length; orphan/partial excluded.");
}}''')
 names=['InstalledPmtilesMosaic','OfflineCellStore','LocalPmtilesResolver','PmtilesReader','NearbyOfflinePlanner']
 sources=[root/'app/src/main/java/fr/roadguard/app'/(n+'.java') for n in names]
 subprocess.run(['java','com.sun.tools.javac.Main','--release','8','-d',str(t),*map(str,sources),str(t/'Probe.java')],check=True)
 subprocess.run(['java','-cp',str(t),'fr.roadguard.app.Probe',str(legacy),str(sectors)],check=True)
