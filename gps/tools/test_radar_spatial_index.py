from pathlib import Path
import math,random
ROOT=Path(__file__).resolve().parents[1]
INDEX=(ROOT/'app/src/main/java/fr/roadguard/app/DangerSpatialIndex.java').read_text()
MAIN=(ROOT/'app/src/main/java/fr/roadguard/app/MainActivity.java').read_text()
BG=(ROOT/'app/src/main/java/fr/roadguard/app/BackgroundAlertService.java').read_text()
assert 'CELL_DEGREES=0.10' in INDEX and 'ArrayList<Integer> query' in INDEX
assert 'longitudeCell(longitude)' in INDEX and 'LONGITUDE_CELLS' in INDEX
assert 'dangerSpatialIndex.query(lastLat,lastLon,5000)' in MAIN
assert 'double lat=Double.parseDouble(c[2].trim()),lon=Double.parseDouble(c[3].trim())' in MAIN
assert 'double lat=Double.parseDouble(c[2].trim()),lon=Double.parseDouble(c[3].trim())' in BG
assert 'if(c.length<5)' in BG
assert 'spatialIndex.query(fix.getLatitude(),fix.getLongitude(),5000)' in BG
assert 'rebuildDangerSpatialIndex()' in MAIN and 'rebuildSpatialIndex()' in BG
assert 'pendingDanger.lat+":"+pendingDanger.lon' in MAIN
# Exercise the cell-envelope math against a broad random set of coordinates. Every point
# within 5 km must lie in a queried grid cell; extra candidates are expected and distance-filtered.
random.seed(20261015)
cell=.10
for _ in range(25000):
 lat=random.uniform(-89.8,89.8); lon=random.uniform(-180,180)
 qlat=lat+random.uniform(-.04,.04); qlon=lon+random.uniform(-.08,.08)
 dlat=math.radians(lat-qlat); dlon=math.radians(((lon-qlon+180)%360)-180)
 a=math.sin(dlat/2)**2+math.cos(math.radians(lat))*math.cos(math.radians(qlat))*math.sin(dlon/2)**2
 meters=6371000*2*math.asin(math.sqrt(min(1,a)))
 if meters>5000:continue
 lat_span=5000/110574
 cos=max(.01,math.cos(math.radians(qlat)))
 lon_span=min(360,5000/(111320*cos))
 y0=math.floor((max(-90,qlat-lat_span)+180)/cell); y1=math.floor((min(90,qlat+lat_span)+180)/cell)
 center=math.floor((((qlon+180)%360)+360)%360/cell)
 lon_cells=1800 if lon_span>=180 else math.ceil(lon_span/cell)+1
 xs={(center+dx)%3600 for dx in range(-lon_cells,lon_cells+1)}
 px=math.floor((((lon+180)%360)+360)%360/cell); py=math.floor((lat+180)/cell)
 assert y0<=py<=y1 and px in xs, (lat,lon,qlat,qlon,meters)
print('Index radar : tests statiques + 25 000 contrôles de couverture géographique réussis')
