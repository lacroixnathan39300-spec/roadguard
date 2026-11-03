#!/usr/bin/env python3
"""Check every original CSV/ASC/JSON byte-for-byte, in this project or its ZIP."""
import argparse,hashlib,json,zipfile
from pathlib import Path,PurePosixPath

ROOT=Path(__file__).resolve().parents[1]
def verify(archive=None):
    expected=json.loads((ROOT/'verification/original-data-11.14.240.json').read_text())
    failures=[]
    if archive:
        with zipfile.ZipFile(archive) as z:
            if z.testzip():raise ValueError('CRC ZIP incorrect')
            names=z.namelist()
            for name in names:
                p=PurePosixPath(name)
                if p.is_absolute() or '..' in p.parts:raise ValueError('Chemin ZIP incorrect')
            roots=[n[:-len('settings.gradle')] for n in names if n.endswith('/settings.gradle')]
            if len(roots)!=1:raise ValueError('Racine Gradle ambiguë')
            for name,digest in expected.items():
                try:actual=hashlib.sha256(z.read(roots[0]+name)).hexdigest()
                except KeyError:actual='ABSENT'
                if actual!=digest:failures.append(name)
    else:
        for name,digest in expected.items():
            path=ROOT/name
            if not path.is_file() or hashlib.sha256(path.read_bytes()).hexdigest()!=digest:failures.append(name)
    return {'reference':'11.14.240','checked_original_files':len(expected),'unchanged':len(expected)-len(failures),'failures':failures}

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--zip');args=parser.parse_args()
    result=verify(args.zip);print(json.dumps(result,indent=2,ensure_ascii=False));raise SystemExit(bool(result['failures']))
