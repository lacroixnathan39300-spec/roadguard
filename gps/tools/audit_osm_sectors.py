#!/usr/bin/env python3
"""Audite un pack ROADGUARD sans le modifier.
Usage: python tools/audit_osm_sectors.py DOSSIER_SECTEURS
"""
import argparse,json,math,pathlib,re,sys

def audit(folder):
    manifest=json.loads((folder/'manifest.json').read_text(encoding='utf-8'))
    if manifest.get('format')!='roadguard-osm-sectors-v1': raise ValueError('Format inconnu')
    cell=manifest.get('cell')
    if isinstance(cell,bool) or not isinstance(cell,(int,float)) or not math.isfinite(cell) or not 0<cell<=10:raise ValueError('Grille invalide')
    names=manifest.get('sectors')
    if not isinstance(names,list) or not names or len(names)!=len(set(names)):raise ValueError('Index vide ou doublons')
    total_points=0;total_lines=0;total_bytes=0;bounds=[180,90,-180,-90]
    for name in names:
        if not isinstance(name,str) or not re.fullmatch(r'secteur_-?\d+_-?\d+_\d+\.geojson',name):raise ValueError('Nom incorrect')
        path=folder/name
        if not path.is_file() or path.is_symlink():raise ValueError('Secteur absent : '+name)
        size=path.stat().st_size
        if size>350000:raise ValueError('Secteur trop volumineux : '+name)
        total_bytes+=size
        data=json.loads(path.read_text(encoding='utf-8'))
        if data.get('type')!='FeatureCollection' or not isinstance(data.get('features'),list):raise ValueError('GeoJSON incorrect')
        count=0
        for f in data['features']:
            geo=f.get('geometry') or {};typ=geo.get('type')
            if typ not in ('LineString','MultiLineString'):raise ValueError('Geometrie incorrecte')
            lines=[geo['coordinates']] if typ=='LineString' else geo['coordinates']
            for line in lines:
                if len(line)<2:raise ValueError('Route sans segment')
                total_lines+=1
                for point in line:
                    if len(point)<2 or any(isinstance(v,bool) or not isinstance(v,(float,int)) or not math.isfinite(v) for v in point[:2]):raise ValueError('Point invalide')
                    lon,lat=point[:2]
                    if not (-180<=lon<=180 and -90<=lat<=90):raise ValueError('Coordonnees hors limites')
                    bounds[0]=min(bounds[0],lon);bounds[1]=min(bounds[1],lat)
                    bounds[2]=max(bounds[2],lon);bounds[3]=max(bounds[3],lat)
                    count+=1
        if count>4000:raise ValueError('Secteur depasse 4000 points : '+name)
        total_points+=count
    return {'secteurs':len(names),'lignes':total_lines,'points':total_points,'octets':total_bytes,'bbox_lon_lat':bounds,'attribution':manifest.get('attribution','NON FOURNIE')}

def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('dossier',type=pathlib.Path);a=p.parse_args()
    try:print(json.dumps(audit(a.dossier),ensure_ascii=False,indent=2))
    except (ValueError,KeyError,OSError,TypeError,json.JSONDecodeError) as e:print('ECHEC:',e,file=sys.stderr);sys.exit(1)
if __name__=='__main__':main()
