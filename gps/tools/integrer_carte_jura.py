#!/usr/bin/env python3
"""Intègre en une commande un PBF Geofabrik local dans les assets Android.
Exemple: python tools/integrer_carte_jura.py franche-comte-latest.osm.pbf
L'outil ne télécharge aucune donnée; le fichier doit être obtenu légalement.
"""
import argparse,json,pathlib,tempfile
from pbf_to_sectors import extract,generate
from install_osm_sectors import install,validate

# Fenêtre initiale Champagnole / Ney / environs; ajustable avec --bbox.
DEFAULT_BBOX=(5.84,46.65,6.02,46.83)
def integrate(pbf,project,bbox=DEFAULT_BBOX,cell=.05):
    pbf=pathlib.Path(pbf);project=pathlib.Path(project)
    if not pbf.is_file():raise FileNotFoundError('PBF introuvable : '+str(pbf))
    if not (project/'app/src/main/assets').is_dir():raise FileNotFoundError('Projet Android introuvable')
    with tempfile.TemporaryDirectory(prefix='roadguard-osm-') as tmp:
        count,roads=generate(extract(pbf,bbox),pathlib.Path(tmp),cell)
        # Refuser tout remplacement par un pack vide ou une sortie inattendue.
        names=validate(pathlib.Path(tmp))
        if not names or roads<1:raise ValueError("Aucune route exploitable : cartes existantes conservees")
        installed=install(pathlib.Path(tmp),project)
        if count!=installed:raise RuntimeError('Nombre de secteurs incohérent')
    return {'secteurs_installes':installed,'routes_extraites':roads,'zone_bbox':bbox,'source':pbf.name}
def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('pbf',type=pathlib.Path)
    parser.add_argument('--project',type=pathlib.Path,default=pathlib.Path(__file__).resolve().parents[1])
    parser.add_argument('--bbox',default=','.join(map(str,DEFAULT_BBOX)))
    parser.add_argument('--cell',type=float,default=.05)
    args=parser.parse_args()
    try:
        bbox=tuple(map(float,args.bbox.split(',')))
        if len(bbox)!=4:raise ValueError('BBox attendue: ouest,sud,est,nord')
        result=integrate(args.pbf,args.project,bbox,args.cell)
    except (ValueError,RuntimeError,OSError) as exc:parser.error(str(exc))
    print(json.dumps(result,ensure_ascii=False,indent=2))
if __name__=='__main__':main()
