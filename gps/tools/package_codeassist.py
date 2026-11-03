#!/usr/bin/env python3
import argparse,hashlib,json,re,zipfile
from pathlib import Path
from verify_preserved_data import verify
ROOT=Path(__file__).resolve().parents[1]
VERSION=re.search(r"versionName\s+'([^']+)'",(ROOT/'app/build.gradle').read_text()).group(1)
parser=argparse.ArgumentParser();parser.add_argument('destination');args=parser.parse_args()
assert not verify()['failures'],'Original data changed'
target=Path(args.destination).resolve();target.parent.mkdir(parents=True,exist_ok=True)
with zipfile.ZipFile(target.with_suffix('.pending'), 'w',compression=zipfile.ZIP_DEFLATED,compresslevel=6) as z:
 for path in sorted(ROOT.rglob('*')):
  if not path.is_file():continue
  rel=path.relative_to(ROOT)
  if any(part.startswith('.') for part in rel.parts) or rel.parts[0] in ['.gradle','build'] or rel.parts[:2]==('app','build') or any(p=='__pycache__' for p in rel.parts) or path.name=='local.properties':continue
  z.write(path,Path('ROADGUARD-'+VERSION)/rel)
target.with_suffix('.pending').replace(target)
report=verify(target)
assert not report['failures']
report.update({'zip':target.name,'zip_bytes':target.stat().st_size,'zip_sha256':hashlib.sha256(target.read_bytes()).hexdigest()})
print(json.dumps(report,indent=2,ensure_ascii=False))
