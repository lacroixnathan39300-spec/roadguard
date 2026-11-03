#!/usr/bin/env python3
"""Prepare reproducible PMTiles extraction jobs for GPS areas (server-side only).

Uses a stable latitude/longitude grid and a conservative 50 km bounding box.
This tool does NOT download or host maps.
"""
import argparse, json, math, pathlib
from plan_versatiles_regions import build, DEFAULT_SOURCE

KM_PER_DEG = 111.32

def gps_bbox(lat, lon, radius_km=50):
    if not all(math.isfinite(v) for v in (lat,lon,radius_km)) or not (-80 <= lat <= 80 and -180 <= lon <= 180) or not 0 < radius_km <= 200:
        raise ValueError('Invalid GPS position or radius')
    dlat = radius_km / KM_PER_DEG
    dlon = radius_km / (KM_PER_DEG * math.cos(math.radians(min(85, abs(lat)+dlat))))
    if lon - dlon < -180 or lon + dlon > 180:
        raise ValueError('Crosses antimeridian: split into two extraction jobs')
    return [round(lon-dlon,6),round(lat-dlat,6),round(lon+dlon,6),round(lat+dlat,6)]

def make_jobs(points, radius_km=50, maxzoom=14):
    regions=[]
    for p in points:
        regions.append({'id':p['id'],'bbox':gps_bbox(float(p['lat']),float(p['lon']),radius_km)})
    return build(regions, DEFAULT_SOURCE, maxzoom)

if __name__ == '__main__':
    parser=argparse.ArgumentParser()
    parser.add_argument('points_json')
    parser.add_argument('--out',default='gps-extraction-jobs.json')
    parser.add_argument('--radius-km',type=float,default=50)
    parser.add_argument('--maxzoom',type=int,default=14)
    args=parser.parse_args()
    jobs=make_jobs(json.loads(pathlib.Path(args.points_json).read_text()),args.radius_km,args.maxzoom)
    pathlib.Path(args.out).write_text(json.dumps({'status':'PLANNED_NOT_DOWNLOADED','jobs':jobs},indent=2)+'\n')
    print('Prepared',len(jobs),'jobs; no data downloaded')
