#!/usr/bin/env python3
"""Télécharge un extrait OSM Geofabrik avec contrôle d'intégrité puis l'intègre à ROADGUARD.

Commande développeur : python tools/preparer_carte_jura.py --project .
Télécharge ~120 Mo, nécessite Internet et osmium; pas exécuté par l'app Android.
"""
import argparse, hashlib, pathlib, re, tempfile, urllib.request
from integrer_carte_jura import integrate
from verifier_cartes_embarquees import verify

PBF_URL='https://download.geofabrik.de/europe/france/franche-comte-latest.osm.pbf'
MD5_URL=PBF_URL+'.md5'
MAX_BYTES=250_000_000

def parse_md5(text):
    match=re.search(r'\b([0-9a-fA-F]{32})\b',text)
    if not match:raise ValueError('Somme MD5 Geofabrik illisible')
    return match.group(1).lower()

def download_checked(target, opener=urllib.request.urlopen):
    """Téléchargement dans un fichier temporaire, publié seulement si MD5 correspond."""
    target=pathlib.Path(target)
    with opener(MD5_URL,timeout=30) as response:
        expected=parse_md5(response.read(1024).decode('ascii'))
    if target.exists():raise FileExistsError("Fichier de destination deja present : "+str(target))
    partial=target.with_name(target.name+'.part')
    if partial.exists():raise FileExistsError("Telechargement partiel deja present : "+str(partial))
    digest=hashlib.md5()
    try:
        with opener(PBF_URL,timeout=60) as response,partial.open('wb') as dest:
            total=0
            while True:
                chunk=response.read(1024*1024)
                if not chunk:break
                total+=len(chunk)
                if total>MAX_BYTES:raise ValueError('Fichier PBF anormalement volumineux')
                digest.update(chunk);dest.write(chunk)
        if digest.hexdigest()!=expected:raise ValueError('Empreinte MD5 incorrecte : téléchargement rejeté')
        partial.replace(target)
    finally:
        partial.unlink(missing_ok=True)
    return target

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--project',type=pathlib.Path,default=pathlib.Path(__file__).resolve().parents[1])
    parser.add_argument('--bbox',default='5.84,46.65,6.02,46.83')
    args=parser.parse_args()
    try:
        bbox=tuple(float(v) for v in args.bbox.split(','))
        if len(bbox)!=4:raise ValueError('BBox: ouest,sud,est,nord')
        with tempfile.TemporaryDirectory(prefix='roadguard-geofabrik-') as tmp:
            pbf=download_checked(pathlib.Path(tmp)/'franche-comte.osm.pbf')
            print('PBF Geofabrik téléchargé et empreinte vérifiée')
            result=integrate(pbf,args.project,bbox)
            report=verify(args.project)
            print('Secteurs installés :',result['secteurs_installes'])
            print('Routes embarquées :',report['routes'],'; points :',report['points'])
    except (OSError,ValueError,RuntimeError,KeyError,TypeError) as exc:parser.error(str(exc))
if __name__=='__main__':main()
