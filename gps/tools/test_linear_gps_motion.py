"""Regression checks for bounded interpolation between GPS fixes."""
from pathlib import Path

s=(Path(__file__).resolve().parents[1]/'app/src/main/assets/map.html').read_text()
for term in ('const ease=t;','lastFixAt:0','fixInterval:850','interval>=200&&interval<=3500','fixInterval*.94','rgVisualGps.lastCameraFrame<12','jump>100?0','cancelAnimationFrame(rgVisualGps.frame)'):
 assert term in s,term
assert 't*t*(3-2*t)' not in s
assert 'Never extrapolate past a fix' in s
print('9 bounded GPS interpolation checks passed')
