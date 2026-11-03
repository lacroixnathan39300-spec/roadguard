#!/usr/bin/env python3
"""Verify delivered APK signatures/manifests and included assets against this tree."""
from pathlib import Path
import argparse,hashlib,json,re,subprocess,zipfile
ROOT=Path(__file__).resolve().parents[1];VERSION='11.14.250'
p=argparse.ArgumentParser();p.add_argument('--build-tools',required=True);a=p.parse_args();sdk=Path(a.build_tools).resolve();records=[]
for kind in ['TELEPHONE']:
 apk=ROOT/'LIVRAISON'/f'ROADGUARD-{VERSION}-{kind}-debug.apk'
 def command(tool,*args):
  result=subprocess.run([str(sdk/tool),*map(str,args)],text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
  assert result.returncode==0,result.stdout
  return result.stdout
 signature=command('apksigner','verify','--verbose','--print-certs',apk)
 badging=command('aapt2','dump','badging',apk)
 for expected in ["name='fr.roadguard.app'","versionCode='450'","versionName='11.14.250'","minSdkVersion:'26'","targetSdkVersion:'36'"]:assert expected in badging,expected
 xml=command('aapt2','dump','xmltree','--file','AndroidManifest.xml',apk)
 assert ('RoadguardCarAppService' in xml)==(kind=='ANDROID-AUTO')
 paths=['map.html','map_maplibre.html','map_offline.html','reference_ui.html','tile_recovery.js','map_packs/appearance.js','map_packs/roadguard.css','roadguard_progress_11_14_248.json','radar_archives/fr-camera.sqlite.7z','radar_archives/ch-camera.sqlite.7z']
 with zipfile.ZipFile(apk) as archive:
  assert archive.testzip() is None
  for name in paths:assert archive.read('assets/'+name)==(ROOT/'app/src/main/assets'/name).read_bytes(),name
  dex=b''.join(archive.read(n) for n in archive.namelist() if re.fullmatch(r'classes\d*\.dex',n))
  assert b'Lfr/roadguard/vendor/commons/compress/archivers/sevenz/SevenZFile;' in dex
  assert b'Lfr/roadguard/vendor/xz/LZMA2InputStream;' in dex
  assert b'Lorg/apache/commons/compress/archivers/sevenz/SevenZFile;' not in dex
 digest=hashlib.sha256(apk.read_bytes()).hexdigest();apk.with_suffix('.apk.sha256').write_text(digest+'  '+apk.name+'\n')
 records.append({'variant':kind,'file':str(apk.relative_to(ROOT)),'bytes':apk.stat().st_size,'sha256':digest,'signature_exit':0,'signature':signature,'badging':badging,'car_manifest':kind=='ANDROID-AUTO','assets_verified':paths,'isolated_radar_classes_present':True})
 print(kind,apk.stat().st_size,'signature, manifest, assets and isolated radar classes OK')
certificates=[re.search(r'certificate SHA-256 digest: (\w+)',r['signature']).group(1) for r in records];assert len(set(certificates))==1
(ROOT/f'verification/apk-{VERSION}.json').write_text(json.dumps(records,indent=2)+'\n')
