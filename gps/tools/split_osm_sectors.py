#!/usr/bin/env python3
"""Decoupe un GeoJSON routier en secteurs sans tronquer silencieusement les routes.
Usage: python tools/split_osm_sectors.py routes.geojson dossier_sortie --max-points 3900
Les segments sont coupes aux frontieres des secteurs; aucune traverse n est perdue.
Les secteurs trop grands provoquent une erreur explicite. Pas de telechargement.
"""
import argparse, collections, json, math, pathlib

def split(data, max_points=3900, max_bytes=340000, cell=0.05):
    if data.get('type')!='FeatureCollection': raise ValueError('FeatureCollection attendu')
    if not math.isfinite(cell) or cell<=0 or max_points<2 or max_bytes<100: raise ValueError('Parametres invalides')
    buckets=collections.defaultdict(list)
    def clip(a,b,box):
        west,south,east,north=box
        x0,y0=a[:2];x1,y1=b[:2];dx=x1-x0;dy=y1-y0;u0=0.;u1=1.
        for p,q in ((-dx,x0-west),(dx,east-x0),(-dy,y0-south),(dy,north-y0)):
            if p==0:
                if q<0:return None
            else:
                t=q/p
                if p<0:u0=max(u0,t)
                else:u1=min(u1,t)
                if u0>u1:return None
        return [[x0+u0*dx,y0+u0*dy],[x0+u1*dx,y0+u1*dy]] if u0<=u1 else None
    for feature in data.get('features',[]):
        geo=feature.get('geometry') or {}
        kind=geo.get('type')
        if kind not in ('LineString','MultiLineString'):continue
        lines=[geo.get('coordinates',[])] if kind=='LineString' else geo.get('coordinates',[])
        for line in lines:
            if len(line)<2:continue
            if any(not isinstance(pt,(list,tuple)) or len(pt)<2 or any(isinstance(v,bool) or not isinstance(v,(int,float)) or not math.isfinite(v) for v in pt[:2]) or not(-180<=pt[0]<=180 and -90<=pt[1]<=90) for pt in line):raise ValueError('Coordonnees invalides')
            for a,b in zip(line,line[1:]):
                if a[:2]==b[:2]:continue
                ix0=math.floor(min(a[0],b[0])/cell);ix1=math.floor(max(a[0],b[0])/cell)
                iy0=math.floor(min(a[1],b[1])/cell);iy1=math.floor(max(a[1],b[1])/cell)
                if (ix1-ix0+1)*(iy1-iy0+1)>20000:raise ValueError('Segment trop long pour la grille')
                for iy in range(iy0,iy1+1):
                    for ix in range(ix0,ix1+1):
                        segment=clip(a,b,(ix*cell,iy*cell,(ix+1)*cell,(iy+1)*cell))
                        if segment is None or segment[0]==segment[1]:continue
                        f={'type':'Feature','properties':feature.get('properties',{}),'geometry':{'type':'LineString','coordinates':segment}}
                        buckets[(iy,ix)].append(f)
    output={}
    for key,features in sorted(buckets.items()):
        batch=[];points=0;index=0
        for f in features:
            g=f['geometry'];lines=[g['coordinates']] if g['type']=='LineString' else g['coordinates']
            count=sum(len(line) for line in lines)
            if count>max_points:raise ValueError('Une route depasse max-points; reduire les donnees source')
            candidate={'type':'FeatureCollection','features':batch+[f]}
            if batch and (points+count>max_points or len(json.dumps(candidate,ensure_ascii=False).encode())>max_bytes):
                output[f'secteur_{key[0]}_{key[1]}_{index}.geojson']={'type':'FeatureCollection','features':batch}
                index+=1;batch=[];points=0
            candidate={'type':'FeatureCollection','features':[f]}
            if len(json.dumps(candidate,ensure_ascii=False).encode())>max_bytes:raise ValueError('Une route depasse max-bytes')
            batch.append(f);points+=count
        if batch:output[f'secteur_{key[0]}_{key[1]}_{index}.geojson']={'type':'FeatureCollection','features':batch}
    return output

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('source',type=pathlib.Path);p.add_argument('output',type=pathlib.Path)
    p.add_argument('--max-points',type=int,default=3900);p.add_argument('--max-bytes',type=int,default=340000)
    p.add_argument('--cell',type=float,default=.05)
    a=p.parse_args()
    if a.max_points<2 or a.max_bytes<100 or not 0<a.cell<=10:p.error('Parametres invalides')
    data=json.loads(a.source.read_text(encoding='utf-8'))
    sectors=split(data,a.max_points,a.max_bytes,a.cell)
    if not sectors:raise SystemExit('Aucune route trouvee')
    a.output.mkdir(parents=True,exist_ok=True)
    manifest={'format':'roadguard-osm-sectors-v1','source':str(a.source),'cell':a.cell,'attribution':'© OpenStreetMap contributors (ODbL)','sectors':[],'note':'Copier manifest.json et les secteurs dans app/src/main/assets/osm-sectors/ ; affichage local GPS uniquement.'}
    for name,geo in sectors.items():
        (a.output/name).write_text(json.dumps(geo,ensure_ascii=False,separators=(',',':')),encoding='utf-8')
        manifest['sectors'].append(name)
    (a.output/'manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2),encoding='utf-8')
    print(len(sectors),'secteurs crees; aucune route supprimee')
if __name__=='__main__':main()
