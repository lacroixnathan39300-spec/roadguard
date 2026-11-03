#!/usr/bin/env python3
"""Convertit un extrait OSM XML en GeoJSON ROADGUARD.
Exemple: python3 tools/osm_to_roadguard.py extrait.osm --bbox 5.7,46.7,6.2,47.1
Le fichier doit contenir les noeuds des ways. Aucun telechargement reseau.
"""
import argparse, json, math, pathlib, xml.etree.ElementTree as ET
HIGHWAYS={'motorway','trunk','primary','secondary','tertiary','unclassified','residential','living_street','service','motorway_link','trunk_link','primary_link','secondary_link','tertiary_link'}

def convert(source, output, max_points=3900, bbox=None):
    if not 2<=max_points<=4000: raise ValueError('max_points doit etre entre 2 et 4000')
    if bbox is not None:
        west,south,east,north=bbox
        if not (-180<=west<east<=180 and -90<=south<north<=90):
            raise ValueError('Emprise bbox invalide (ou passage de l antimeridien non pris en charge)')
    nodes={}; ways=[]; missing=0; count=0; skipped=0
    # XML OSM usuel: nodes avant ways. On conserve seulement les noeuds utiles a la zone.
    for _, elem in ET.iterparse(source, events=('end',)):
        if elem.tag=='node':
            try:
                ident=elem.get('id');lat=float(elem.get('lat'));lon=float(elem.get('lon'))
                if math.isfinite(lat) and math.isfinite(lon) and -90<=lat<=90 and -180<=lon<=180:
                    # Garde les noeuds de la zone + marge, pour conserver les segments frontaliers.
                    if bbox is None or (west-.02<=lon<=east+.02 and south-.02<=lat<=north+.02):
                        nodes[ident]=[lon,lat]
            except (ValueError,TypeError):pass
            elem.clear()
        elif elem.tag=='way':
            tags={t.get('k'):t.get('v') for t in elem.findall('tag')}
            if tags.get('highway') in HIGHWAYS and tags.get('area')!='yes':
                refs=[n.get('ref') for n in elem.findall('nd')]
                coords=[nodes[r] for r in refs if r in nodes]
                missing+=len(refs)-len(coords)
                # Evite de dessiner des lignes artificielles a travers des noeuds absents.
                segment=[]
                for ref in refs+[None]:
                    xy=nodes.get(ref)
                    if xy is not None:
                        segment.append(xy)
                    else:
                        if len(segment)>=2 and (bbox is None or any(west<=p[0]<=east and south<=p[1]<=north for p in segment)):
                            if count+len(segment)<=max_points:
                                ways.append({'type':'Feature','properties':{'highway':tags['highway'],'name':tags.get('name','')},'geometry':{'type':'LineString','coordinates':segment}})
                                count+=len(segment)
                            else:skipped+=1
                        segment=[]
            elem.clear()
    if not ways:raise ValueError('Aucune route utilisable: verifier la zone, le fichier et les noeuds')
    result={'type':'FeatureCollection','features':ways}
    output.parent.mkdir(parents=True,exist_ok=True)
    output.write_text(json.dumps(result,ensure_ascii=False,separators=(',',':')),encoding='utf-8')
    print(f'Routes: {len(ways)} segments, {count} points; {missing} references hors extrait/zone; {skipped} segments ignores par limite')
    print(f'GeoJSON: {output} ({output.stat().st_size} octets)')
    print('© contributeurs OpenStreetMap — ODbL 1.0. Afficher l attribution dans l application.')

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('source',type=pathlib.Path)
    p.add_argument('--output',type=pathlib.Path,default=pathlib.Path('routes-roadguard.geojson'))
    p.add_argument('--max-points',type=int,default=3900)
    p.add_argument('--bbox',help='ouest,sud,est,nord en degres WGS84 (zone cible)')
    a=p.parse_args()
    if not a.source.is_file():p.error('Extrait OSM XML introuvable')
    try:
        bbox=tuple(map(float,a.bbox.split(','))) if a.bbox else None
        if bbox is not None and len(bbox)!=4:raise ValueError('4 valeurs requises')
        convert(a.source,a.output,a.max_points,bbox)
    except ValueError as e:p.error(str(e))
if __name__=='__main__':main()
