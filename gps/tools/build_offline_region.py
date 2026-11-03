#!/usr/bin/env python3
"""ROADGUARD: emballe des tuiles XYZ dont vous possedez les droits d'utilisation hors ligne.
Usage: python3 tools/build_offline_region.py /chemin/vers/tiles --attribution "Source et licence" 
Arborescence: tiles/z/x/y.png (ou jpg/jpeg). Aucun telechargement reseau.
"""
import argparse, pathlib, re, zipfile, sys

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('tiles',type=pathlib.Path)
    p.add_argument('--attribution',required=True,help='Credit et licence du fournisseur des tuiles')
    p.add_argument('--output',type=pathlib.Path,default=pathlib.Path('app/src/main/assets/offline-region.zip'))
    a=p.parse_args()
    root=a.tiles.resolve()
    if not root.is_dir():p.error('Dossier de tuiles introuvable')
    if not a.attribution.strip():p.error('Attribution obligatoire')
    entries=[];total=0
    for f in root.rglob('*'):
        if not f.is_file():continue
        if f.is_symlink():p.error('Liens symboliques interdits')
        rel=f.relative_to(root).as_posix()
        m=re.fullmatch(r'(\d{1,2})/(\d{1,9})/(\d{1,9})\.(png|jpg|jpeg)',rel,re.I)
        if not m:p.error('Nom de tuile invalide: '+rel)
        z,x,y=map(int,m.group(1,2,3))
        if z>18 or x>=2**z or y>=2**z:p.error('Coordonnees XYZ invalides: '+rel)
        n=f.stat().st_size
        if n<100 or n>2_000_000:p.error('Taille de tuile invalide: '+rel)
        total+=n
        if total>100*1024*1024:p.error('Pack depasse 100 Mio non compresses')
        entries.append((rel,f))
        if len(entries)>20000:p.error('Plus de 20 000 tuiles')
    if not entries:p.error('Aucune tuile trouvee')
    a.output.parent.mkdir(parents=True,exist_ok=True)
    with zipfile.ZipFile(a.output,'w',zipfile.ZIP_DEFLATED,compresslevel=6) as out:
        for rel,f in sorted(entries):out.write(f,'tiles/'+rel)
        out.writestr('ATTRIBUTION.txt',a.attribution.strip()+'\n')
    with zipfile.ZipFile(a.output) as check:
        if check.testzip() is not None:raise RuntimeError('Archive corrompue')
    print(f'Pack cree: {a.output} ({len(entries)} tuiles, {total} octets source)')
    print('Verifier la licence: les serveurs publics tile.openstreetmap.org interdisent le prechargement massif.')
if __name__=='__main__':main()
