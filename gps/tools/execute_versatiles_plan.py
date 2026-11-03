#!/usr/bin/env python3
"""Execute previously validated VersaTiles extraction plans on a preparation machine.

No implicit network: --execute is mandatory. Outputs are NOT Android-ready until
hosted over HTTPS, entered in the catalog and rendered with a compatible style.
"""
import argparse, hashlib, json, pathlib, re, subprocess, sys
from plan_versatiles_regions import build

MAGIC = b'PMTiles'

def inspect_archive(path):
    """Lightweight file sanity check and full SHA-256 (not tile-level validation)."""
    path=pathlib.Path(path)
    with path.open('rb') as f:
        if f.read(7)!=MAGIC: raise ValueError('not a PMTiles archive')
    size=path.stat().st_size
    if size<127: raise ValueError('PMTiles archive is too small')
    h=hashlib.sha256()
    with path.open('rb') as f:
        for block in iter(lambda:f.read(1024*1024),b''): h.update(block)
    return {'bytes':size,'sha256':h.hexdigest()}


def prepare_jobs(plan):
    if not isinstance(plan,dict): raise ValueError('plan must be object')
    jobs=plan.get('regions',plan.get('jobs'))
    if not isinstance(jobs,list): raise ValueError('missing regions/jobs')
    source=plan.get('source') or (jobs[0].get('source') if jobs else None)
    if not isinstance(source,str): raise ValueError('missing source')
    maxzooms=set()
    for j in jobs:
        if not isinstance(j,dict):raise ValueError('bad job')
        m=re.search(r'--max-zoom\s+(\d+)',j.get('command',''))
        if not m: raise ValueError('missing maxzoom in plan')
        maxzooms.add(int(m.group(1)))
    if len(maxzooms)!=1: raise ValueError('inconsistent maxzooms')
    validated=build([{'id':j['id'],'bbox':j['bbox']} for j in jobs],source,maxzooms.pop())
    return validated

def execute(plan, output_dir, binary='versatiles', run=False):
    jobs=prepare_jobs(plan)
    root=pathlib.Path(output_dir); results=[]
    if run:root.mkdir(parents=True,exist_ok=True)
    for j in jobs:
        dest=root/j['output']
        if not run:
            results.append({'id':j['id'],'status':'DRY_RUN','destination':str(dest)})
            continue
        if dest.exists():
            try:
                meta=inspect_archive(dest)
                results.append({'id':j['id'],'status':'EXISTING_INSPECTED_UNHOSTED','destination':str(dest),**meta,'bbox':j['bbox'],'source':j['source']})
            except (OSError,ValueError) as exc:
                results.append({'id':j['id'],'status':'INVALID_EXISTING_NEEDS_MANUAL_REPAIR','destination':str(dest),'error':str(exc)})
            continue
        partial=dest.with_name(dest.stem + '.incomplete.pmtiles')
        if partial.exists():partial.unlink()  # no false resume claim; keep .pmtiles suffix for VersaTiles format detection
        args=[binary,'convert','--bbox-border','3','--bbox',','.join(f'{v:.6f}' for v in j['bbox']), '--min-zoom','0','--max-zoom',j['command'].split('--max-zoom ')[1].split()[0],j['source'],str(partial)]
        try:
            subprocess.run(args,check=True)
            meta=inspect_archive(partial)
            partial.replace(dest)
            results.append({'id':j['id'],'status':'EXTRACTED_UNHOSTED','destination':str(dest),**meta,'bbox':j['bbox'],'source':j['source']})
        except (OSError,ValueError,subprocess.CalledProcessError) as exc:
            partial.unlink(missing_ok=True)
            results.append({'id':j['id'],'status':'FAILED','error':str(exc)})
    return results

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('plan_json');p.add_argument('--out-dir',default='prepared-regions')
    p.add_argument('--binary',default='versatiles');p.add_argument('--execute',action='store_true')
    args=p.parse_args()
    jobs=execute(json.loads(pathlib.Path(args.plan_json).read_text()),args.out_dir,args.binary,args.execute)
    if args.execute:
        pathlib.Path(args.out_dir,'extraction-results.json').write_text(json.dumps({'schema':1,'results':jobs},indent=2)+'\n')
    print(json.dumps(jobs,indent=2))
    sys.exit(1 if any(j['status']=='FAILED' for j in jobs) else 0)
