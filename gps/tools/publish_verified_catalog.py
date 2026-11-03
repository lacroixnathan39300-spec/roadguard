#!/usr/bin/env python3
"""Generate ROADGUARD catalog TSV from *verified local* PMTiles, never publish automatically.

Upload packs to the same HTTPS path first; the generated TSV alone cannot host them.
"""
import argparse, hashlib, json, pathlib, re
from urllib.parse import urlparse, quote

MAX_PACK_BYTES=10_000_000_000

def build_catalog(results, pack_dir, base_url):
    p=urlparse(base_url)
    if p.scheme!='https' or not p.netloc or p.username or p.password or p.query or p.fragment or not base_url.endswith('/'):
        raise ValueError('base URL must be HTTPS directory ending in /')
    entries=results.get('results')
    if not isinstance(entries,list): raise ValueError('missing results')
    lines=['# ROADGUARD verified PMTiles catalog v1; generated from local verified files']
    seen=set()
    for entry in entries:
        if entry.get('status') not in ('EXTRACTED_UNHOSTED','EXISTING_INSPECTED_UNHOSTED'):
            continue
        id=entry['id']
        if not re.fullmatch(r'[a-z0-9][a-z0-9_-]{1,59}',id) or id in seen: raise ValueError('invalid or duplicate ID')
        seen.add(id)
        file=pathlib.Path(pack_dir)/(id+'.pmtiles')
        if not file.is_file() or file.is_symlink(): raise ValueError('missing pack: '+str(file))
        size=file.stat().st_size
        if size<127 or size>MAX_PACK_BYTES: raise ValueError('invalid size: '+id)
        h=hashlib.sha256()
        with file.open('rb') as f:
            if f.read(7)!=b'PMTiles' or f.read(1)!=b'\x03': raise ValueError('invalid PMTiles v3: '+id)
            f.seek(0)
            for block in iter(lambda:f.read(1024*1024),b''):h.update(block)
        sha=h.hexdigest()
        if sha!=entry.get('sha256') or size!=entry.get('bytes'): raise ValueError('mismatched pack: '+id)
        bbox=entry.get('bbox')
        if not isinstance(bbox,list) or len(bbox)!=4: raise ValueError('missing bbox: '+id)
        west,south,east,north=map(float,bbox)
        if not (-180<=west<east<=180 and -85<=south<north<=85): raise ValueError('invalid bbox: '+id)
        url=base_url+quote(id+'.pmtiles')
        lines.append('\t'.join([id,url,sha,str(size),str(south),str(west),str(north),str(east),'pmtiles']))
    if len(lines)==1: raise ValueError('no verified pack to publish')
    return '\n'.join(lines)+'\n'

if __name__=='__main__':
    a=argparse.ArgumentParser(description=__doc__)
    a.add_argument('extraction_results');a.add_argument('--pack-dir',required=True);a.add_argument('--base-url',required=True);a.add_argument('--out',required=True)
    args=a.parse_args()
    catalog=build_catalog(json.loads(pathlib.Path(args.extraction_results).read_text()),args.pack_dir,args.base_url)
    pathlib.Path(args.out).write_text(catalog,encoding='utf8')
    print('Catalog written; upload packs and verify HTTPS availability before use:',args.out)
