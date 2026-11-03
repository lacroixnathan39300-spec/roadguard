#!/usr/bin/env python3
"""Generate a geographic *planning* index; never invent downloadable pack URLs.
Inputs: GeoJSON FeatureCollection of country/admin boundaries (Polygon/MultiPolygon),
with ISO country code in properties. Output is JSON; no remote tile requests.
"""
import argparse, json, math, pathlib

def points(coords):
    if isinstance(coords,list):
        if len(coords)>=2 and all(isinstance(v,(float,int)) for v in coords[:2]):
            yield coords[:2]
        else:
            for item in coords: yield from points(item)

def build(data, key):
    if data.get('type')!='FeatureCollection': raise ValueError('FeatureCollection required')
    out=[]; ids=set()
    for f in data.get('features',[]):
        g=f.get('geometry') or {}
        if g.get('type') not in ('Polygon','MultiPolygon'): continue
        prop=f.get('properties') or {}; code=str(prop.get(key,'')).strip().upper()
        if not code or not code.replace('-','').isalnum(): raise ValueError('Missing/invalid country code')
        xy=list(points(g.get('coordinates',[])))
        if not xy: continue
        if any(not(-180<=x<=180 and -90<=y<=90) or not(math.isfinite(x) and math.isfinite(y)) for x,y in xy): raise ValueError('Invalid coordinates')
        # A single bbox crossing the antimeridian would cover most of the planet;
        # flag these for polygon-aware server processing rather than a bad download.
        west,east=min(x for x,y in xy),max(x for x,y in xy)
        south,north=min(y for x,y in xy),max(y for x,y in xy)
        name=str(prop.get('name',code))
        ident=f'{code}:{name}'
        if ident in ids: raise ValueError('Duplicate region')
        ids.add(ident)
        out.append(dict(id=ident,country=code,name=name,bbox=[west,south,east,north],requires_polygon_clip=east-west>180,pack_url=None,sha256=None,status='planning_only'))
    return {'schema':'roadguard-region-index-v1','source':'GeoJSON supplied by operator; verify license and attribution','download_source':'https://download.versatiles.org/osm.versatiles','note':'This index does not contain hosted packs or a verified pack catalog. Generate, host and checksum PMTiles separately.','regions':sorted(out,key=lambda r:r['id'])}

def main():
    p=argparse.ArgumentParser();p.add_argument('geojson');p.add_argument('output');p.add_argument('--country-field',default='ISO_A2');a=p.parse_args()
    obj=build(json.loads(pathlib.Path(a.geojson).read_text(encoding='utf8')),a.country_field)
    pathlib.Path(a.output).write_text(json.dumps(obj,indent=2,ensure_ascii=False)+'\n',encoding='utf8')
    print(len(obj['regions']),'planning regions')
if __name__=='__main__':main()
