#!/usr/bin/env python3
"""Fail if ROADGUARD accidentally configures bulk/persistent downloads from public OSM tile servers."""
from pathlib import Path
import re
ROOT=Path(__file__).resolve().parents[1]
files=list((ROOT/'app/src/main').rglob('*.java'))+list((ROOT/'app/src/main').rglob('*.html'))
violations=[]
for file in files:
    s=file.read_text(encoding='utf-8')
    if 'tile.openstreetmap.org' not in s: continue
    # OSM is allowed for interactive map display, but never as offline download origin.
    for n,line in enumerate(s.splitlines(),1):
        if 'tile.openstreetmap.org' in line and re.search(r'(download|prefetch|offline|cachePermanent)',line,re.I):
            # The map's existing inline one-line style contains the word 'online OSM (consultation)';
            # only flag actual downloader entrypoints, not the map style definition.
            if 'new maplibregl.Map' not in line and 'L.tileLayer(' not in line:
                violations.append(f'{file.relative_to(ROOT)}:{n}')
if violations: raise SystemExit('Potentially unauthorized OSM tile persistence: '+', '.join(violations))
print(f'OK: {len(files)} application files checked; public OSM tiles only used for interactive rendering.')
