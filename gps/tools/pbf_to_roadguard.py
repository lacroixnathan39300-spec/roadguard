#!/usr/bin/env python3
"""Conversion OSM .pbf vers GeoJSON ROADGUARD (outil PC, pas sur Android).
Dependance optionnelle: python -m pip install osmium
Exemple: python tools/pbf_to_roadguard.py franche-comte-latest.osm.pbf --bbox 5.75,46.72,6.15,47.05
Le resultat est limite a 3900 points et 350 Ko; choisir une petite zone.
"""
import argparse,json,math,pathlib,sys
try:
    import osmium
except ImportError:
    osmium=None
HIGHWAYS={'motorway','trunk','primary','secondary','tertiary','unclassified','residential','living_street','service','motorway_link','trunk_link','primary_link','secondary_link','tertiary_link'}

def convert(source,output,bbox,max_points=3900,max_bytes=350000):
    if osmium is None:raise RuntimeError('Installer la dependance: python -m pip install osmium')
    west,south,east,north=bbox
    if not all(map(math.isfinite,bbox)) or not (-180<=west<east<=180 and -90<=south<north<=90):raise ValueError('bbox invalide')
    if not 2<=max_points<=4000:raise ValueError('max_points hors limites')
    class Roads(osmium.SimpleHandler):
        def __init__(self):super().__init__();self.features=[];self.points=0;self.skipped=0
        def way(self,w):
            tags=dict(w.tags)
            if tags.get('highway') not in HIGHWAYS or tags.get('area')=='yes':return
            # Les noeuds sans position interrompent le segment (pas de raccord artificiel).
            segment=[]
            def finish():
                if len(segment)<2:return
                if not any(west<=x<=east and south<=y<=north for x,y in segment):return
                if self.points+len(segment)>max_points:self.skipped+=1;return
                self.features.append({'type':'Feature','properties':{'highway':tags['highway'],'name':tags.get('name','')},'geometry':{'type':'LineString','coordinates':segment[:]}})
                self.points+=len(segment)
            for n in w.nodes:
                if n.location.valid():
                    x,y=n.lon,n.lat
                    if math.isfinite(x) and math.isfinite(y):segment.append([x,y]);continue
                finish();segment=[]
            finish()
    handler=Roads()
    handler.apply_file(str(source),locations=True)
    if not handler.features:raise ValueError('Aucune route dans la zone')
    result={'type':'FeatureCollection','features':handler.features}
    raw=json.dumps(result,ensure_ascii=False,separators=(',',':')).encode('utf-8')
    # Tronquer proprement par objets si les noms rendent le GeoJSON trop gros.
    while len(raw)>max_bytes and result['features']:
        removed=result['features'].pop()
        handler.points-=len(removed['geometry']['coordinates'])
        raw=json.dumps(result,ensure_ascii=False,separators=(',',':')).encode('utf-8')
    if not result['features']:raise ValueError('Zone trop volumineuse pour la limite de taille')
    output.parent.mkdir(parents=True,exist_ok=True)
    output.write_bytes(raw)
    return len(result['features']),handler.points,len(raw),handler.skipped

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('source',type=pathlib.Path)
    p.add_argument('--output',type=pathlib.Path,default=pathlib.Path('routes-roadguard.geojson'))
    p.add_argument('--bbox',required=True,help='ouest,sud,est,nord')
    p.add_argument('--max-points',type=int,default=3900)
    args=p.parse_args()
    if not args.source.is_file():p.error('Fichier PBF introuvable')
    try:
        bbox=tuple(map(float,args.bbox.split(',')))
        if len(bbox)!=4:raise ValueError('Quatre coordonnees attendues')
        n,points,size,skipped=convert(args.source,args.output,bbox,args.max_points)
        print(f'{n} routes, {points} points, {size} octets, {skipped} segments ignores')
        print('Attribution obligatoire : © contributeurs OpenStreetMap (ODbL 1.0)')
    except (ValueError,RuntimeError) as e:p.error(str(e))
if __name__=='__main__':main()
