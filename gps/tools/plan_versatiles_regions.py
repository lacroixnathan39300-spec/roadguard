#!/usr/bin/env python3
"""Plan reproducible regional PMTiles extracts from VersaTiles; never downloads implicitly.

The official remote container is a *source*, not an Android pack endpoint.
Run generated commands on a preparation machine with VersaTiles installed.
Upload verified outputs separately and build a ROADGUARD HTTPS catalog.
"""
import argparse, json, math, pathlib, re, shlex
from urllib.parse import urlparse

DEFAULT_SOURCE='https://download.versatiles.org/osm.versatiles'

def validate_bbox(b):
    if len(b)!=4 or not all(isinstance(x,(int,float)) and math.isfinite(x) for x in b):
        raise ValueError('bbox must contain four finite numbers')
    w,s,e,n=b
    if not (-180<=w<e<=180 and -85<=s<n<=85):
        raise ValueError('bbox must be ordered, inside [-180,180] longitude and [-85,85] latitude; split antimeridian regions')
    return ','.join(f'{x:.6f}' for x in b)

def build(data, source, maxzoom):
    parsed=urlparse(source)
    if parsed.scheme!='https' or not parsed.hostname or parsed.username or parsed.password or parsed.query or parsed.fragment:
        raise ValueError('source must be a plain HTTPS URL')
    if not 0<=maxzoom<=14: raise ValueError('maxzoom outside supported OSM source range 0..14')
    if not isinstance(data,list) or not 1<=len(data)<=5000:raise ValueError('regions must be a nonempty list of <=5000 entries')
    result=[]; seen=set()
    for item in data:
        if not isinstance(item,dict):raise ValueError('region entry must be an object')
        name=item.get('id')
        if not isinstance(name,str) or not re.fullmatch('[a-z0-9][a-z0-9_-]{1,59}',name):raise ValueError('invalid region id')
        if name in seen:raise ValueError('duplicate region id: '+name)
        seen.add(name)
        bbox=validate_bbox(item.get('bbox'))
        output=f'{name}.pmtiles'
        argv=['versatiles','convert','--bbox-border','3','--bbox',bbox,'--min-zoom','0','--max-zoom',str(maxzoom),source,output]
        result.append({'id':name,'bbox':item['bbox'],'output':output,'command':shlex.join(argv),'status':'NOT_DOWNLOADED','source':source})
    return result

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('regions_json',help='JSON list of {id,bbox:[west,south,east,north]}')
    p.add_argument('--source',default=DEFAULT_SOURCE)
    p.add_argument('--maxzoom',type=int,default=14)
    p.add_argument('--out',default='versatiles-extraction-plan.json')
    args=p.parse_args()
    plan=build(json.loads(pathlib.Path(args.regions_json).read_text()),args.source,args.maxzoom)
    pathlib.Path(args.out).write_text(json.dumps({'schema':1,'source':args.source,'note':'Planning only. No packs downloaded, hosted, or Android-tested.','regions':plan},indent=2,ensure_ascii=False)+'\n')
    print('Planned',len(plan),'regions; no downloads performed. Output:',args.out)
if __name__=='__main__':main()
