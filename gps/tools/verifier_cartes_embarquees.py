#!/usr/bin/env python3
"""Diagnostic strict des cartes routieres OSM reellement integrees a l'APK.
Exit 0: secteurs valides et non vides; exit 2: absents ou invalides.
Exemple: python tools/verifier_cartes_embarquees.py --project .
"""
import argparse,json,pathlib,sys
from install_osm_sectors import validate

def verify(project):
    root=pathlib.Path(project)/'app/src/main/assets/osm-sectors'
    if not (root/'manifest.json').is_file():
        raise ValueError("AUCUNE CARTE ROUTIERE EMBARQUEE : manifest.json absent")
    names=validate(root)
    features=points=bytes_total=0
    for name in names:
        file=root/name
        data=json.loads(file.read_text(encoding='utf-8'))
        for f in data['features']:
            geom=f['geometry']
            lines=[geom['coordinates']] if geom['type']=='LineString' else geom['coordinates']
            features+=1
            points+=sum(len(line) for line in lines)
        bytes_total+=file.stat().st_size
    if features==0 or points<2:
        raise ValueError("CARTE VIDE : aucune route exploitable")
    return {'secteurs':len(names),'routes':features,'points':points,'octets':bytes_total,
            'note':'Donnees presentes dans les assets; compilation et rendu Android non verifies'}
def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--project',type=pathlib.Path,default=pathlib.Path(__file__).resolve().parents[1])
    args=p.parse_args()
    try:
        print(json.dumps(verify(args.project),ensure_ascii=False,indent=2))
    except (ValueError,OSError,KeyError,TypeError) as e:
        print('BLOQUE : '+str(e),file=sys.stderr)
        return 2
    return 0
if __name__=='__main__':sys.exit(main())
