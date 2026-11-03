"""Regression checks for the current ROADGUARD camera filter."""
from pathlib import Path

s=(Path(__file__).resolve().parents[1]/'app/src/main/assets/map.html').read_text()
checks=(
 'const rgCameraSmooth=',
 'Math.exp(-dt/115)',
 'now-rgVisualGps.lastCameraFrame<12',
 'map.panTo(map.unproject([c.x,c.y],z),{animate:false})',
 'rgSubpixelCamera.x=Math.max(-10',
 'translate3d(',
 'rgCameraSmooth.x=null',
)
for term in checks:
 assert term in s,term
assert 'map.distance(map.getCenter(),center)>0.35' not in s
print(f'{len(checks)+1} current camera smoothing checks passed')
