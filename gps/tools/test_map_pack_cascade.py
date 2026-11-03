#!/usr/bin/env python3
"""Check the real bundled CSS cascade against the previous night/contrast rules."""
from pathlib import Path
import re
root=Path(__file__).resolve().parents[1]/'app/src/main/assets'
base=(root/'map.html').read_text().split('</style>',1)[0]
pack=(root/'map_packs/roadguard.css').read_text()

def specificity(selector):
    return (selector.count('#'),len(re.findall(r'\.[\w-]+|\[[^]]+\]',selector)),len(re.findall(r'(?<![.#\w-])(?:body|img)(?![\w-])',selector)))

def rules(css):
    return [(sel.strip(),m.group(2).strip(),specificity(sel.strip())) for m in re.finditer(r'([^{}]+)\{[^{}]*?filter\s*:\s*([^;{}]+)!important',css,re.S) for sel in m.group(1).split(',') if 'leaflet-tile' in sel]
previous=[r for r in rules(base) if 'rg-night' in r[0] or 'rg-contrast' in r[0]]
assert previous, 'Missing previous rules to test against'
selected=rules(pack)
filters=[]
for mode in ('graphite','readable','original'):
    matching=[r for r in selected if 'data-rg-map-pack="'+mode+'"' in r[0]]
    assert len(matching)>=2 and all(r[2]>max(x[2] for x in previous) for r in matching),(mode,matching)
    day=next(r for r in matching if '.rg-night' not in r[0])
    night=next(r for r in matching if '.rg-night' in r[0])
    assert night[2]>day[2] and day[1]!=night[1],mode
    filters.extend([day[1],night[1]])
assert len(set(filters))==6,filters
assert 'function setRoadguardMapPack(value)' in (root/'map_maplibre.html').read_text()
assert 'function setRoadguardMapPack(value)' in (root/'map_offline.html').read_text()
assert 'ctx.filter=rgTileFilter()' in (root/'map_offline.html').read_text()
print('PASS six distinct day/night filters override legacy night/contrast rules; MapLibre and local canvas expose the pack')
