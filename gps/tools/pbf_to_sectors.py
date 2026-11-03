#!/usr/bin/env python3
"""Extrait directement des secteurs depuis un PBF Geofabrik (sans GeoJSON intermédiaire).
Commande: python tools/pbf_to_sectors.py franche-comte-latest.osm.pbf --bbox 5.84,46.65,6.02,46.83 --output secteurs
Nécessite: pip install osmium. Refuse tout dépassement plutôt que perdre des routes.
"""
import argparse,collections,json,math,pathlib
try: import osmium
except ImportError: osmium=None
from split_osm_sectors import split
from install_osm_sectors import validate

HIGHWAYS={'motorway','trunk','primary','secondary','tertiary','unclassified','residential','living_street','service','motorway_link','trunk_link','primary_link','secondary_link','tertiary_link'}
def clip_segment(a,b,bbox):
    """Liang-Barsky: limite exactement un segment a la bbox (lon/lat)."""
    west,south,east,north=bbox
    x0,y0=a; x1,y1=b
    dx=x1-x0;dy=y1-y0
    u0,u1=0.,1.
    for p,q in ((-dx,x0-west),(dx,east-x0),(-dy,y0-south),(dy,north-y0)):
        if p==0:
            if q<0:return None
        else:
            t=q/p
            if p<0:u0=max(u0,t)
            else:u1=min(u1,t)
            if u0>u1:return None
    return [[x0+u0*dx,y0+u0*dy],[x0+u1*dx,y0+u1*dy]]

def clip_polyline(points,bbox):
    """Retourne des lignes independantes, sans relier les discontinuites."""
    result=[];current=[]
    for a,b in zip(points,points[1:]):
        pair=clip_segment(a,b,bbox)
        if pair is None or pair[0]==pair[1]:
            if len(current)>1:result.append(current)
            current=[];continue
        if current and current[-1]==pair[0]:current.append(pair[1])
        else:
            if len(current)>1:result.append(current)
            current=pair
    if len(current)>1:result.append(current)
    return result

def extract(pbf,bbox):
    if osmium is None: raise RuntimeError('Installer osmium : python -m pip install osmium')
    if len(bbox)!=4 or not all(map(math.isfinite,bbox)) or not(-180<=bbox[0]<bbox[2]<=180 and -90<=bbox[1]<bbox[3]<=90):
        raise ValueError('BBox invalide')
    class Handler(osmium.SimpleHandler):
        def __init__(self):super().__init__();self.features=[]
        def way(self,way):
            tags=dict(way.tags)
            if tags.get('highway') not in HIGHWAYS or tags.get('area')=='yes':return
            segment=[]
            def flush():
                for clipped in clip_polyline(segment,bbox):
                    self.features.append({'type':'Feature','properties':{'highway':tags['highway'],'name':tags.get('name','')},'geometry':{'type':'LineString','coordinates':clipped}})
            for node in way.nodes:
                if node.location.valid():
                    x,y=node.lon,node.lat
                    if math.isfinite(x) and math.isfinite(y):
                        segment.append([x,y]);continue
                flush();segment=[]
            flush()
    h=Handler();h.apply_file(str(pbf),locations=True)
    return {'type':'FeatureCollection','features':h.features}

def generate(data,folder,cell=0.05):
    if not data['features']:raise ValueError('Aucune route dans la zone')
    sectors=split(data,max_points=3900,max_bytes=340000,cell=cell)
    if not sectors:raise ValueError('Aucun secteur')
    folder.mkdir(parents=True,exist_ok=True)
    names=[]
    for name,geo in sectors.items():
        (folder/name).write_text(json.dumps(geo,ensure_ascii=False,separators=(',',':')),encoding='utf-8')
        names.append(name)
    manifest={'format':'roadguard-osm-sectors-v1','source':'Geofabrik / OpenStreetMap','cell':cell,'attribution':'© OpenStreetMap contributors (ODbL 1.0)','sectors':names}
    (folder/'manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2),encoding='utf-8')
    validate(folder)
    return len(names),len(data['features'])
def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('pbf',type=pathlib.Path);p.add_argument('--bbox',required=True)
    p.add_argument('--output',type=pathlib.Path,default=pathlib.Path('secteurs-jura'))
    p.add_argument('--cell',type=float,default=.05)
    a=p.parse_args()
    if not a.pbf.is_file():p.error('Fichier PBF absent')
    try:
        bbox=tuple(float(v) for v in a.bbox.split(','))
        if len(bbox)!=4:raise ValueError('BBox: ouest,sud,est,nord')
        count,roads=generate(extract(a.pbf,bbox),a.output,a.cell)
        print(f'{roads} segments OSM, {count} secteurs valides')
    except (ValueError,RuntimeError,OSError) as exc:p.error(str(exc))
if __name__=='__main__':main()
