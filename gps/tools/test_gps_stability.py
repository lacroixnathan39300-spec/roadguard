"""Regression checks for GPS filtering and native fix sanity checks."""
from pathlib import Path

r=Path(__file__).resolve().parents[1]
s=(r/'app/src/main/assets/map.html').read_text()
j=(r/'app/src/main/java/fr/roadguard/app/MainActivity.java').read_text()
for key in ('function rgFilterGps(','rgFilteredAt','distance>maxTravel','rgFilterGps(lat,lon,accuracy,speedKmh)','elapsed<12'):
 assert key in s,key
for key in ('previousAcceptedGps','getElapsedRealtimeNanos()','new Location(l)','gap>Math.max(35f'):
 assert key in j,key
assert 'map.setView([lat,lon],Math.max(15,map.getZoom()),{animate:false})' in s
assert 'map.panTo(map.unproject([c.x,c.y],z),{animate:false})' in s
print('7 current GPS stability checks passed')
