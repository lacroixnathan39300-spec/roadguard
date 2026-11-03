"""Behavioural/data QA for the bundled multi-source catalogue (no network)."""
from pathlib import Path
import csv
import hashlib
import importlib.util
import json

ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('builder',ROOT/'tools/build_embedded_radars.py')
b=importlib.util.module_from_spec(spec);spec.loader.exec_module(b)
manifest=json.loads((b.ASSETS/'radar_bundle_manifest.json').read_text())
ids=set();official=set()
for code,pack in manifest['countries'].items():
    path=b.ASSETS/pack['file'];data=path.read_bytes()
    assert hashlib.sha256(data).hexdigest()==pack['sha256']
    rows=list(csv.DictReader(data.decode().splitlines(),delimiter=';'))
    assert len(rows)==pack['count']
    assert sum(r['status']=='inventory' for r in rows)==pack['static_count']
    assert sum(r['status']=='historical' for r in rows)==pack['historical_count']
    assert len({r['id'] for r in rows})==len(rows)
    for r in rows:
        assert r['country']==code and r['id'] not in ids
        ids.add(r['id']);assert len(r['type'])<=80
        assert -90<=float(r['lat'])<=90 and -180<=float(r['lon'])<=180
        assert r['sources'] and r['source_ids']
        for ident in r['source_ids'].split('|'):
            if ident.startswith('interieur:'):official.add(ident)
        assert (r['status']=='historical')==(r['category'] in ('mobile','temporary','works'))
    if code=='CH':assert 'active' in pack['app_usage']
assert len(official)==3309,len(official)
assert manifest['audit']['identical_export_files_skipped']==31
assert manifest['audit'].get('additional_unmatched_payloads',0)==0
assert manifest['audit']['copilot_FR_records']==manifest['audit']['copilot_FR_corroborated']
assert manifest['geneva_infrastructure_only']==41
assert manifest['default_selection']=={'french_historical':False,'swiss_historical':True,'FR':7325,'CH':1777,'total':9102}

def sample(source='lufop',direction='',category='fixed',lat=46.2,ident='a'):
    return dict(source=source,country='FR',category=category,lat=lat,lon=5.,
                ident=ident,speed='50',direction=direction,status='inventory')

# Corroboration at the same location merges; nearby distinct hardware/directions
# and different categories must survive. These exercise the actual build code.
assert len(b.deduplicate([sample(),sample('mapatlas',lat=46.2001)]))==1
assert len(b.deduplicate([sample(direction='0'),sample('mapatlas',direction='180')]))==2
assert len(b.deduplicate([sample(),sample('mapatlas',category='redlight')]))==2
assert len(b.deduplicate([sample(),sample(lat=46.2001,ident='b')]))==2
assert len(b.deduplicate([sample('interieur'),sample('interieur',ident='b')]))==2

# Recursive OSM skeleton nodes must not erase full tagged node records.
b.RECORDS.clear()
b.load_osm({'elements':[{'type':'node','id':1,'lat':46.2,'lon':6.2,
                       'tags':{'highway':'speed_camera'}},
                      {'type':'node','id':1,'lat':46.2,'lon':6.2}]},'CH')
assert len(b.RECORDS)==1
print('Data QA OK: hashes, all 3309 official IDs, country separation, categories, provenance, deduplication and OSM recursion')
