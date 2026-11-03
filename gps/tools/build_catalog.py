#!/usr/bin/env python3
"""Create ROADGUARD HTTPS catalog from existing legally sourced XYZ ZIP files."""
import argparse, hashlib, json, pathlib, re, zipfile
p=argparse.ArgumentParser();p.add_argument('--base-url',required=True);p.add_argument('--out',default='catalog.json');p.add_argument('packs',nargs='+');a=p.parse_args()
if not a.base_url.startswith('https://') or not a.base_url.endswith('/'):p.error('HTTPS base URL ending with / required')
items=[]
for pathstr in a.packs:
 path=pathlib.Path(pathstr);ident=path.stem.lower().replace('.','-')
 if not re.fullmatch('[a-z0-9_-]{2,50}',ident):p.error('Invalid ID')
 if not zipfile.is_zipfile(path):p.error('Not a ZIP')
 with zipfile.ZipFile(path) as z:
  names=[i.filename.removeprefix('tiles/') for i in z.infolist() if not i.is_dir()]
  if not names or any(not re.fullmatch(r'[0-9]{1,2}/[0-9]{1,9}/[0-9]{1,9}\.(png|jpg|jpeg)',n) for n in names):p.error('Invalid XYZ ZIP')
 with open(path,'rb') as f:sha=hashlib.file_digest(f,'sha256').hexdigest()
 items.append(dict(id=ident,name=path.stem,url=a.base_url+path.name,size=path.stat().st_size,sha256=sha))
pathlib.Path(a.out).write_text(json.dumps(dict(schema=1,packs=items),indent=2)+'\n')
print('Catalog created:',a.out)
