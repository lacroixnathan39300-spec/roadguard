#!/usr/bin/env python3
"""Installation verifiee de secteurs OSM dans un projet ROADGUARD.
Usage: python tools/install_osm_sectors.py SECTEURS --project PROJET
"""
import argparse, json, math, pathlib, shutil, tempfile


def validate(source):
    manifest=json.loads((source/'manifest.json').read_text(encoding='utf-8'))
    if manifest.get('format')!='roadguard-osm-sectors-v1': raise ValueError('Format de manifeste inconnu')
    cell=manifest.get('cell')
    if isinstance(cell,bool) or not isinstance(cell,(int,float)) or not math.isfinite(cell) or not 0<cell<=10:
        raise ValueError('Taille de grille invalide')
    names=manifest.get('sectors')
    if not isinstance(names,list) or not names or len(names)>10000 or len(set(names))!=len(names):
        raise ValueError('Liste des secteurs vide, dupliquee ou trop longue')
    total=0
    for name in names:
        if not isinstance(name,str) or pathlib.Path(name).name!=name or not name.startswith('secteur_') or not name.endswith('.geojson'):
            raise ValueError('Nom de secteur invalide')
        file=source/name
        if not file.is_file() or file.is_symlink() or file.stat().st_size>350000:
            raise ValueError('Secteur absent ou trop volumineux : '+name)
        total+=file.stat().st_size
        if total>250000000: raise ValueError('Pack trop volumineux')
        data=json.loads(file.read_text(encoding='utf-8'))
        if data.get('type')!='FeatureCollection' or not isinstance(data.get('features'),list):
            raise ValueError('GeoJSON invalide : '+name)
        # Chaque fichier doit rester dans la cellule annoncee par son nom.
        # Evite les routes affichees dans un mauvais secteur GPS.
        import re
        match=re.fullmatch(r'secteur_(-?[0-9]+)_(-?[0-9]+)_([0-9]+)[.]geojson',name)
        if not match: raise ValueError('Nom de secteur non conforme : '+name)
        iy,ix,_=map(int,match.groups())
        west,south,east,north=ix*cell,iy*cell,(ix+1)*cell,(iy+1)*cell
        eps=1e-7
        points=0
        for feature in data['features']:
            if not isinstance(feature,dict): raise ValueError('Feature invalide')
            geometry=feature.get('geometry') or {}
            kind=geometry.get('type')
            if kind not in ('LineString','MultiLineString'): raise ValueError('Geometrie non routiere')
            lines=[geometry.get('coordinates')] if kind=='LineString' else geometry.get('coordinates')
            if not isinstance(lines,list): raise ValueError('Coordonnees invalides')
            for line in lines:
                if not isinstance(line,list): raise ValueError('Ligne invalide')
                for pt in line:
                    if not isinstance(pt,list) or len(pt)<2 or any(isinstance(v,bool) or not isinstance(v,(int,float)) or not math.isfinite(v) for v in pt[:2]):
                        raise ValueError('Point invalide')
                    if not (-180<=pt[0]<=180 and -90<=pt[1]<=90): raise ValueError('Point hors limites')
                    if not (west-eps<=pt[0]<=east+eps and south-eps<=pt[1]<=north+eps):
                        raise ValueError('Route hors du secteur annonce : '+name)
                    points+=1
                    if points>4000: raise ValueError('Trop de points : '+name)
    return names


def install(source,project):
    names=validate(source)  # Ne rien modifier avant validation complete
    target=project/'app/src/main/assets/osm-sectors'
    target.mkdir(parents=True,exist_ok=True)
    # Sauvegarde de secours, puis remplacement. En cas d'erreur, restauration.
    with tempfile.TemporaryDirectory(prefix='rg-sectors-') as tmp:
        backup=pathlib.Path(tmp)/'backup';backup.mkdir()
        old=list(target.glob('secteur_*.geojson'))+[target/'manifest.json']
        for file in old:
            if file.is_file(): shutil.copy2(file,backup/file.name)
        try:
            for file in target.glob('secteur_*.geojson'):file.unlink()
            for name in names:shutil.copy2(source/name,target/name)
            shutil.copy2(source/'manifest.json',target/'manifest.json')
        except Exception:
            for file in target.glob('secteur_*.geojson'):file.unlink()
            (target/'manifest.json').unlink(missing_ok=True)
            for file in backup.iterdir():shutil.copy2(file,target/file.name)
            raise
    return len(names)


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('secteurs',type=pathlib.Path)
    parser.add_argument('--project',type=pathlib.Path,default=pathlib.Path('.'))
    args=parser.parse_args()
    try: count=install(args.secteurs,args.project)
    except (ValueError,OSError,KeyError,TypeError,json.JSONDecodeError) as exc:parser.error(str(exc))
    print(count,'secteurs verifies et installes')

if __name__=='__main__':main()
