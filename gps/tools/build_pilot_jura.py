#!/usr/bin/env python3
"""Create and validate a Jura pilot PMTiles extract, then optionally publish a HTTPS catalog.
Requires official `pmtiles` CLI, authorized source archive, and a machine with network access.
No public OSM tile-server scraping. Example:
  python tools/build_pilot_jura.py --source https://build.protomaps.com/DATE.pmtiles --output-dir packs
  python tools/build_pilot_jura.py --source ... --base-url https://example.org/maps/ --output-dir packs
"""
import argparse, hashlib, json, pathlib, shutil, subprocess, sys, urllib.parse
p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--source',required=True,help='Authorized remote PMTiles v3 archive (HTTPS)')
p.add_argument('--output-dir',default='packs')
p.add_argument('--base-url',help='HTTPS directory where the resulting file will be uploaded; generates catalog.json')
p.add_argument('--maxzoom',type=int,default=14)
a=p.parse_args()
u=urllib.parse.urlparse(a.source)
if u.scheme!='https' or not u.netloc or not u.path.endswith('.pmtiles'):p.error('Source must be a HTTPS .pmtiles URL')
if a.maxzoom not in range(10,16):p.error('Zoom must be 10..15')
if not shutil.which('pmtiles'):p.error('Install official pmtiles CLI (protomaps/go-pmtiles)')
out=pathlib.Path(a.output_dir);out.mkdir(parents=True,exist_ok=True)
pack=out/'jura_pilot.pmtiles'
# Jura department bounding rectangle with modest buffer; NOT an administrative border.
cmd=['pmtiles','extract',a.source,str(pack),'--bbox=5.25,46.2,6.3,47.35',f'--maxzoom={a.maxzoom}']
print('Extracting Jura pilot...');subprocess.run(cmd,check=True)
subprocess.run(['pmtiles','verify',str(pack)],check=True)
size=pack.stat().st_size
if size>600*1024*1024:sys.exit('Pilot exceeds ROADGUARD 600 MiB limit; lower zoom or split area')
with pack.open('rb') as f:
 if f.read(8)!=b'PMTiles\x03':sys.exit('Invalid PMTiles v3 signature')
with pack.open('rb') as f: sha=hashlib.file_digest(f,'sha256').hexdigest()
print(f'Verified {pack}: {size} bytes, sha256={sha}')
if a.base_url:
 b=urllib.parse.urlparse(a.base_url)
 if b.scheme!='https' or not b.netloc or not a.base_url.endswith('/'):p.error('Base URL must be HTTPS and end in /')
 catalog={'schema':1,'packs':[{'id':'jura_pilot','name':'Jura (pilote)','country':'france','format':'pmtiles','url':urllib.parse.urljoin(a.base_url,pack.name),'size':size,'sha256':sha}]}
 dest=out/'catalog.json';dest.write_text(json.dumps(catalog,ensure_ascii=False,indent=2)+'\n')
 print(f'Created {dest}; upload both files to HTTPS host before configuring ROADGUARD.')
else:print('To activate one-click download, rerun with --base-url and upload pack + catalog to that HTTPS host.')
print('MapLibre vector rendering remains experimental; validate on Xiaomi before production.')
