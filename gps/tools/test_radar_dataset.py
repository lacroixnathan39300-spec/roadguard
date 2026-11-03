from pathlib import Path
import csv, collections, hashlib
ROOT=Path(__file__).resolve().parents[1]
DATA=ROOT/'app/src/main/assets/radars_france_20251230.csv'
MAIN=(ROOT/'app/src/main/java/fr/roadguard/app/MainActivity.java').read_text()
BG=(ROOT/'app/src/main/java/fr/roadguard/app/BackgroundAlertService.java').read_text()
raw=DATA.read_text(encoding='utf-8').splitlines()
assert raw[0]=='# ROADGUARD official radar dataset | source: French Ministry of Interior via data.gouv.fr | updated 2025-12-30 | Open Licence 2.0'
rows=list(csv.DictReader([line for line in raw if not line.startswith('#')],delimiter=';'))
assert len(rows)==3309, len(rows)
assert len({r['id'] for r in rows})==len(rows)
valid={'ETF','ETD','ETT','ETU','ETVM','ETFR','ETPN'}
assert set(r['type'] for r in rows)==valid
for r in rows:
 lat=float(r['latitude']);lon=float(r['longitude'])
 assert -90<=lat<=90 and -180<=lon<=180
assert 'radars_france_20251230.csv' in MAIN and 'radars_france_20251230.csv' in BG
for code,name in [('ETF','Radar fixe'),('ETD','Radar discriminant'),('ETT','Radar tourelle'),('ETU','Radar urbain'),('ETVM','Radar vitesse moyenne'),('ETFR','Radar feu rouge'),('ETPN','Radar passage à niveau')]:
 assert f'case "{code}":return "{name}"' in MAIN
 assert f'case "{code}":return "{name}"' in BG
assert 'DATA.GOUV.FR' in MAIN and '30/12/2025' in MAIN
print(f'Base radars: {len(rows)} entrées uniques valides; 7 types; attribution et licence embarquées')
print('SHA-256:',hashlib.sha256(DATA.read_bytes()).hexdigest())
