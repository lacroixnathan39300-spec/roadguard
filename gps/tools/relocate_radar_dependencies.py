#!/usr/bin/env python3
"""Reproduce the bundled radar JAR; normal Android builds use the included result.

Optional tool: com.eed3si9n.jarjar:jarjar-assembly:1.14.1 from Maven Central.
Pass its downloaded path with --jarjar. The tool is not included in the APK.
"""
from pathlib import Path
import argparse,hashlib,json,subprocess,tempfile,zipfile
ROOT=Path(__file__).resolve().parents[1]
TOOL_SHA='270fe285e399e298fb08c5daf1486648203a5bd5e90eaba4a6466097be7dd50f'
NAMES=['commons-compress-1.28.0.jar','commons-io-2.20.0.jar','commons-lang3-3.18.0.jar','commons-codec-1.19.0.jar','xz-1.12.jar']
parser=argparse.ArgumentParser();parser.add_argument('--jarjar',required=True);args=parser.parse_args()
tool=Path(args.jarjar).resolve();assert hashlib.sha256(tool.read_bytes()).hexdigest()==TOOL_SHA,'Unexpected JarJar binary'
manifest=json.loads((ROOT/'app/radar-dependencies.json').read_text());entries={};inputs={}
for name in NAMES:
 path=ROOT/'app/libs'/name;raw=path.read_bytes();expected=manifest['libs/'+name]
 assert len(raw)==expected['bytes'] and hashlib.sha256(raw).hexdigest()==expected['sha256'],name
 inputs[name]=expected
 with zipfile.ZipFile(path) as archive:
  for entry in archive.infolist():
   if entry.is_dir() or entry.filename.startswith('META-INF/') or entry.filename=='module-info.class':continue
   content=archive.read(entry)
   if entry.filename in entries:assert entries[entry.filename]==content,entry.filename
   entries[entry.filename]=content
def write(target,contents):
 with zipfile.ZipFile(target,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=9) as archive:
  for name,content in sorted(contents.items()):
   entry=zipfile.ZipInfo(name,date_time=(2020,1,1,0,0,0));entry.compress_type=zipfile.ZIP_DEFLATED;entry.external_attr=0o644<<16
   archive.writestr(entry,content)
with tempfile.TemporaryDirectory(prefix='roadguard-relocate-') as temporary:
 temp=Path(temporary);write(temp/'input.jar',entries)
 subprocess.run(['java','-jar',str(tool),'process',str(ROOT/'tools/radar-relocation.rules'),str(temp/'input.jar'),str(temp/'output.jar')],check=True)
 with zipfile.ZipFile(temp/'output.jar') as archive:output={n:archive.read(n) for n in archive.namelist() if not n.endswith('/')}
 assert 'fr/roadguard/vendor/commons/compress/archivers/sevenz/SevenZFile.class' in output
 assert 'fr/roadguard/vendor/xz/LZMA2InputStream.class' in output
 assert not any(n.startswith(('org/apache/commons/','org/tukaani/xz/')) for n in output)
 output['META-INF/ROADGUARD-RELOCATION.txt']=('Commons Compress 1.28.0, IO 2.20.0, Lang3 3.18.0, Codec 1.19.0 and XZ 1.12.\n'
   'Package relocation only, using tools/radar-relocation.rules.\nOriginal JARs: app/libs. Licenses/notices: app/src/main/assets/vendor.\n').encode()
 for path in sorted((ROOT/'app/src/main/assets/vendor').glob('*')):
  if path.is_file() and any(part in path.name.lower() for part in ['commons','xz-1.12']):output['META-INF/roadguard-licenses/'+path.name]=path.read_bytes()
 target=ROOT/'app/libs/roadguard-radar-runtime-1.jar';write(target,output)
 classes=[b for n,b in output.items() if n.endswith('.class')]
 assert classes and all(int.from_bytes(b[6:8],'big')<=52 for b in classes),'Non Java 8 class'
 report={'tool':{'coordinate':'com.eed3si9n.jarjar:jarjar-assembly:1.14.1','sha256':TOOL_SHA,'url':'https://repo.maven.apache.org/maven2/com/eed3si9n/jarjar/jarjar-assembly/1.14.1/jarjar-assembly-1.14.1.jar'},'inputs':inputs,'rules':(ROOT/'tools/radar-relocation.rules').read_text(),'output':{'path':str(target.relative_to(ROOT)),'bytes':target.stat().st_size,'sha256':hashlib.sha256(target.read_bytes()).hexdigest(),'class_count':len(classes),'max_class_version':max(int.from_bytes(b[6:8],'big') for b in classes)}}
 (ROOT/'verification/radar-relocation-11.14.246.json').write_text(json.dumps(report,indent=2)+'\n')
 print(json.dumps(report['output'],indent=2))
