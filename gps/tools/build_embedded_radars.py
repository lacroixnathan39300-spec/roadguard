#!/usr/bin/env python3
"""Build the bundled FR/CH catalogue from preserved, attributed source bytes.

No network call. Run with Python 3: python3 tools/build_embedded_radars.py
The CSVs are a derivative database under ODbL 1.0. Source rights are preserved.
"""
from pathlib import Path
import collections
import csv
import hashlib
import io
import json
import math
import re
import struct
import zipfile

ROOT = Path(__file__).resolve().parents[1]
SOURCES = ROOT / 'radar_sources/2026-10-16'
ASSETS = ROOT / 'app/src/main/assets'
FIELDS = ['id', 'type', 'lat', 'lon', 'country', 'category', 'speed_kmh',
          'direction_deg', 'status', 'sources', 'source_ids', 'speed_conflicts']
NAMES = {'fixed': 'Radar fixe', 'discriminant': 'Radar discriminant',
         'turret': 'Radar tourelle', 'urban': 'Radar urbain',
         'redlight': 'Radar feu rouge', 'average': 'Radar vitesse moyenne',
         'average_start': 'Radar tronçon début', 'average_end': 'Radar tronçon fin',
         'crossing': 'Radar passage à niveau', 'carpool': 'Radar voie covoiturage',
         'variable': 'Radar variable', 'tunnel': 'Radar fixe · tunnel',
         'mobile': 'Radar mobile · lieu signalé, présence non confirmée',
         'temporary': 'Zone de contrôle possible · présence non confirmée',
         'works': 'Radar chantier · lieu signalé, présence non confirmée'}
OFFICIAL = dict(zip(['ETF', 'ETD', 'ETT', 'ETU', 'ETVM', 'ETFR', 'ETPN'],
                    ['fixed', 'discriminant', 'turret', 'urban', 'average', 'redlight', 'crossing']))
RECORDS = []
AUDIT = collections.Counter()
RAW_COUNTS = collections.defaultdict(collections.Counter)


def rows(text, delimiter=','):
    return csv.reader(io.StringIO(text), delimiter=delimiter, skipinitialspace=True)


def source_record(source, country, category, lat, lon, ident, speed='', direction=''):
    lat, lon = float(lat), float(lon)
    if not (math.isfinite(lat) and math.isfinite(lon) and abs(lat) <= 90 and abs(lon) <= 180):
        AUDIT['invalid_coordinates'] += 1
        return
    if country not in ('FR', 'CH') or category not in NAMES:
        return
    if country == 'CH' and not (45.6 <= lat <= 47.9 and 5.8 <= lon <= 10.7):
        AUDIT['ch_label_outside_bounds'] += 1
        return
    speed = str(speed or '').strip()
    speed = str(int(float(speed))) if re.fullmatch(r'\d+(?:\.\d+)?', speed) and 0 < float(speed) <= 200 else ''
    try:
        direction = str(int(round(float(direction))) % 360) if str(direction) != '' else ''
    except ValueError:
        direction = ''
    status = 'historical' if category in ('mobile', 'temporary', 'works') else 'inventory'
    RECORDS.append(dict(source=source, country=country, category=category,
                        lat=lat, lon=lon, ident=str(ident), speed=speed,
                        direction=direction, status=status))
    RAW_COUNTS[source][country] += 1


def parse_lufop_name(name):
    name = Path(name).name.upper()
    country = name[:2]
    if country not in ('FR', 'CH'):
        return None
    if 'COVOITURAGE' in name: category = 'carpool'
    elif 'PASSAGENIVEAU' in name: category = 'crossing'
    elif 'POID' in name: category = 'discriminant'
    elif 'FEUROUGE' in name: category = 'redlight'
    elif 'TRONCONDEBUT' in name: category = 'average_start'
    elif 'TRONCONFIN' in name: category = 'average_end'
    elif 'CHANTIER' in name: category = 'works'
    elif 'TEMPORAIRE' in name: category = 'temporary'
    elif 'MOBILE' in name: category = 'mobile'
    elif 'TUNNEL' in name: category = 'tunnel'
    elif 'FIXE' in name: category = 'fixed'
    else: return None
    speed = re.search(r'(\d+)\.(ASC|CSV)$', name)
    return country, category, speed.group(1) if speed else ''


def load_lufop():
    payloads = set()
    # Identical payloads in CSV/ASC/POI-Warner count only once.
    for archive in ['Lufop-Zones-de-danger-EU-ASC.zip', 'Radar_france_CSV_PL.zip', 'POI-Warner_CSV.zip']:
        with zipfile.ZipFile(SOURCES / archive) as z:
            for name in sorted(z.namelist()):
                info = parse_lufop_name(name)
                if info is None or not name.lower().endswith(('.asc', '.csv')):
                    continue
                data = z.read(name)
                canonical = b'\n'.join(sorted(line.strip() for line in data.splitlines() if line.strip()))
                key = (info, hashlib.sha256(canonical).hexdigest())
                if key in payloads:
                    AUDIT['identical_export_files_skipped'] += 1
                    continue
                # The supplied CSV and POI-Warner payloads are cross-checked
                # against the main Lufop export; unknown extra payloads are audited.
                if archive != 'Lufop-Zones-de-danger-EU-ASC.zip':
                    AUDIT['additional_unmatched_payloads'] += 1
                payloads.add(key)
                country, category, fixed_speed = info
                for i, fields in enumerate(rows(data.decode('cp1252'))):
                    if len(fields) < 2:
                        continue
                    try:
                        label = fields[2] if len(fields) > 2 else ''
                        match = re.search(r'\b(\d{2,3})\s*$', label)
                        speed = fixed_speed or (match.group(1) if match and category == 'works' else '')
                        source_record('lufop', country, category, fields[1], fields[0],
                                      Path(name).stem + ':' + str(i + 1), speed)
                    except ValueError:
                        AUDIT['invalid_lufop_rows'] += 1


def audit_ov2():
    """Inspect user CoPilot export; use only points corroborated by Lufop.

    No standalone licence is present in this archive. New records from it are
    retained only as candidates in the audit, never licensed as Lufop by guess.
    """
    bins = collections.defaultdict(list)
    for r in RECORDS:
        if r['source'] == 'lufop':
            bins[(round(r['lat'], 3), round(r['lon'], 3))].append(r)
    with zipfile.ZipFile(SOURCES / 'Copilot_avec_Zones_Temporaires.zip') as z:
        data = z.read('safetycam.ov2')
    pos = 0
    while pos < len(data):
        if pos + 5 > len(data):
            raise ValueError('Truncated OV2 header')
        typ, length = data[pos], struct.unpack_from('<I', data, pos + 1)[0]
        if length < 5 or pos + length > len(data):
            raise ValueError('Invalid OV2 record length')
        if typ == 2 and length >= 14:
            lon, lat = (v / 100000 for v in struct.unpack_from('<ii', data, pos + 5))
            label = data[pos + 13:pos + length].rstrip(b'\0').decode('cp1252')
            match = re.search(r'\b(FR|CH)\b', label)
            if match:
                code = match.group(1)
                AUDIT['copilot_' + code + '_records'] += 1
                nearby = []
                for dy in (-1, 0, 1):
                    for dx in (-1, 0, 1):
                        nearby.extend(bins[(round(round(lat, 3) + dy * .001, 3),
                                            round(round(lon, 3) + dx * .001, 3))])
                if any(r['country'] == code and meters(lat, lon, r['lat'], r['lon']) < 3 for r in nearby):
                    AUDIT['copilot_' + code + '_corroborated'] += 1
                else:
                    AUDIT['copilot_' + code + '_uncorroborated_excluded'] += 1
        pos += length


def meters(a, b, c, d):
    lat1, lat2 = math.radians(a), math.radians(c)
    h = math.sin((lat2 - lat1) / 2) ** 2 + math.cos(lat1) * math.cos(lat2) * math.sin(math.radians(d - b) / 2) ** 2
    return 12742000 * math.asin(math.sqrt(min(1, h)))


def load_others():
    text = (ASSETS / 'radars_france_20251230.csv').read_text()
    for row in csv.DictReader((line for line in text.splitlines() if not line.startswith('#')), delimiter=';'):
        source_record('interieur', 'FR', OFFICIAL[row['type']], row['latitude'], row['longitude'],
                      row['id'], row['speed_limit'])
    for code in ('fr', 'ch'):
        obj = json.loads((SOURCES / ('mapatlas_' + code + '.geojson')).read_text())
        if obj.get('licence') != 'CC BY 4.0' or obj.get('region') != code.upper():
            raise ValueError('Invalid MapAtlas provenance')
        for f in obj['features']:
            prop = f['properties']
            cat = {'spot': 'fixed', 'signal': 'redlight', 'corridor': 'average', 'adaptive': 'variable'}.get(prop['class'])
            lon, lat = f['geometry']['coordinates'][:2]
            source_record('mapatlas', code.upper(), cat, lat, lon, prop['ref'],
                          prop.get('limitKph', ''), prop.get('facingDeg', ''))
        text = (SOURCES / ('speedcams_' + code + '.csv')).read_text()
        for row in csv.DictReader(line for line in text.splitlines() if not line.startswith('#')):
            cat = {'fixed': 'fixed', 'red_light': 'redlight', 'red-light': 'redlight', 'red light': 'redlight',
                   'average': 'average', 'average_speed': 'average', 'section': 'average', 'variable': 'variable'}.get(row['type'])
            if cat:
                source_record('osm_snapshot', code.upper(), cat, row['latitude'], row['longitude'],
                              'node/' + row['id'], row['maxspeed'] if row['unit'] in ('kmh', 'km/h', '') else '')
        file = SOURCES / ('overpass_' + code + '.json')
        if file.exists():
            load_osm(json.loads(file.read_text()), code.upper())


def load_osm(obj, code):
    if obj.get('remark'):
        raise ValueError('Incomplete Overpass response must not be used')
    elements = {}
    # Recursion may repeat a tagged node as a skeleton. Preserve its tags.
    for element in obj['elements']:
        key = (element['type'], element['id'])
        elements.setdefault(key, {}).update(element)
    for e in elements.values():
        tags = e.get('tags', {})
        if e['type'] == 'node' and tags.get('highway') == 'speed_camera':
            if tags.get('disused') == 'yes' or tags.get('abandoned') == 'yes':
                AUDIT['osm_disused_excluded'] += 1
                continue
            cat = 'tunnel' if tags.get('tunnel') == 'yes' else 'fixed'
            if any(t in tags.get('enforcement', '') for t in ('traffic_signals', 'red_light_camera')): cat = 'redlight'
            source_record('osm_live', code, cat, e['lat'], e['lon'], 'node/' + str(e['id']),
                          tags.get('maxspeed', ''), tags.get('direction', ''))
        if e['type'] == 'relation' and tags.get('type') == 'enforcement':
            enforcement = tags.get('enforcement', '')
            if any(t in enforcement for t in ('traffic_signals', 'red_light_camera')): cat = 'redlight'
            elif 'average_speed' in enforcement: cat = 'average'
            elif 'maxspeed' in enforcement: cat = 'fixed'
            else: cat = None
            if not cat:
                continue
            for m in e.get('members', []):
                if m['type'] != 'node': continue
                role = m.get('role')
                if cat == 'average':
                    category = {'from': 'average_start', 'to': 'average_end', 'device': 'average'}.get(role)
                else:
                    category = cat if role == 'device' else None
                node = elements.get(('node', m['ref']))
                if category and node and 'lat' in node:
                    source_record('osm_live', code, category, node['lat'], node['lon'],
                                  'relation/' + str(e['id']) + '/' + role + '/node/' + str(m['ref']),
                                  tags.get('maxspeed', ''))


def ring_contains(ring, x, y):
    inside = False
    for (x1, y1), (x2, y2) in zip(ring, ring[1:] + ring[:1]):
        if (y1 > y) != (y2 > y) and x < (x2 - x1) * (y - y1) / (y2 - y1) + x1:
            inside = not inside
    return inside


def swiss_contains(lat, lon, geometry):
    if not (45.6 <= lat <= 47.9 and 5.8 <= lon <= 10.7): return False
    polys = [geometry['coordinates']] if geometry['type'] == 'Polygon' else geometry['coordinates']
    for poly in polys:
        if ring_contains(poly[0], lon, lat) and not any(ring_contains(hole, lon, lat) for hole in poly[1:]):
            return True
    return False


def deduplicate(records):
    # Preserve categories, opposite directions, and nearby devices from the same
    # source. Merge matching observations across sources within 25 m; 2 m within
    # an export family handles different CSV/OV2 coordinate precision.
    priority = {'interieur': 0, 'lufop': 1, 'osm_live': 2, 'mapatlas': 3, 'osm_snapshot': 4}
    ordered = sorted(records, key=lambda r: (priority[r['source']], r['country'], r['ident']))
    result, bins = [], collections.defaultdict(list)
    for r in ordered:
        cat = r['category']
        family = 'speed' if cat in ('fixed', 'discriminant', 'turret', 'urban', 'tunnel') else cat
        key = (r['country'], family, math.floor(r['lat'] / .001), math.floor(r['lon'] / .001))
        found, nearest = None, 26
        for dy in (-1, 0, 1):
            for dx in (-1, 0, 1):
                for idx in bins[(key[0], key[1], key[2] + dy, key[3] + dx)]:
                    t = result[idx]
                    distance = meters(r['lat'], r['lon'], t['lat'], t['lon'])
                    radius = 2 if r['source'] in t['sources'] else 25
                    if distance > radius: continue
                    if r['direction'] and t['direction']:
                        delta = abs((int(r['direction']) - int(t['direction']) + 180) % 360 - 180)
                        if delta > 45: continue
                    # Do not collapse two official device IDs even on one pole.
                    if r['source'] == 'interieur' and 'interieur' in t['sources']: continue
                    if distance < nearest: found, nearest = idx, distance
        source_id = r['source'] + ':' + r['ident']
        if found is None:
            t = dict(r, sources={r['source']}, source_ids={source_id}, speed_conflicts=set())
            result.append(t)
            bins[key].append(len(result) - 1)
        else:
            t = result[found]
            t['sources'].add(r['source']); t['source_ids'].add(source_id)
            if not t['speed']: t['speed'] = r['speed']
            elif r['speed'] and t['speed'] != r['speed']: t['speed_conflicts'].add(r['speed'])
            if not t['direction']: t['direction'] = r['direction']
            if cat == 'tunnel' and t['category'] == 'fixed': t['category'] = 'tunnel'
            AUDIT['observations_merged'] += 1
    return result


def build():
    RECORDS.clear(); AUDIT.clear(); RAW_COUNTS.clear()
    load_others(); load_lufop(); audit_ov2()
    geometry = json.loads((SOURCES / 'ch_boundary.geojson').read_text())['features'][0]['geometry']
    # Reject FR-labelled points inside the Swiss boundary; do not silently
    # promote them to verified Swiss radars based on a rectangular filter.
    filtered = []
    quarantine = []
    live_node_ids = {(r['country'], r['ident']) for r in RECORDS
                     if r['source'] == 'osm_live' and r['ident'].startswith('node/')}
    for r in RECORDS:
        if r['source'] == 'osm_snapshot' and (r['country'], r['ident']) in live_node_ids:
            AUDIT['older_osm_observations_superseded_by_same_node_id'] += 1
            continue
        if r['country'] == 'FR' and swiss_contains(r['lat'], r['lon'], geometry):
            AUDIT['fr_label_inside_switzerland_excluded'] += 1
            quarantine.append({**r, 'reason': 'FR label inside Swiss boundary; not verified as CH'})
        elif r['country'] == 'CH' and not swiss_contains(r['lat'], r['lon'], geometry):
            AUDIT['ch_label_outside_switzerland_excluded'] += 1
            quarantine.append({**r, 'reason': 'CH label outside Swiss boundary; not verified as CH'})
        else:
            filtered.append(r)
    merged = deduplicate(filtered)
    ASSETS.mkdir(parents=True, exist_ok=True)
    countries = {}
    for code in ('FR', 'CH'):
        records = sorted((r for r in merged if r['country'] == code),
                         key=lambda r: (r['status'], r['lat'], r['lon'], r['category'], r['ident']))
        file = ASSETS / ('radars_embedded_' + code.lower() + '.csv')
        with file.open('w', newline='', encoding='utf-8') as out:
            writer = csv.DictWriter(out, fieldnames=FIELDS, delimiter=';', lineterminator='\n')
            writer.writeheader()
            for r in records:
                identity = '|'.join([code, r['category'], f"{r['lat']:.7f}", f"{r['lon']:.7f}", r['ident']])
                ident = 'rg-' + hashlib.sha256(identity.encode()).hexdigest()[:20]
                name = NAMES[r['category']]
                if r['speed'] and r['status'] != 'historical': name += ' · ' + r['speed'] + ' km/h'
                writer.writerow(dict(id=ident, type=name, lat=f"{r['lat']:.7f}", lon=f"{r['lon']:.7f}",
                                     country=code, category=r['category'], speed_kmh=r['speed'],
                                     direction_deg=r['direction'], status=r['status'],
                                     sources='|'.join(sorted(r['sources'])), source_ids='|'.join(sorted(r['source_ids'])),
                                     speed_conflicts='|'.join(sorted(r['speed_conflicts']))))
        countries[code] = dict(file=file.name, count=len(records),
                               static_count=sum(r['status'] == 'inventory' for r in records),
                               historical_count=sum(r['status'] == 'historical' for r in records),
                               categories=dict(sorted(collections.Counter(r['category'] for r in records).items())),
                               sha256=hashlib.sha256(file.read_bytes()).hexdigest(),
                               app_usage='active; historical opt-in' if code == 'FR' else 'active; Swiss historical locations enabled by default, independently switchable')
    # Geneva data describes physical bases, not guaranteed operational cameras.
    # It remains a separate reference, excluded from the radar totals and alerts.
    infrastructure = json.loads((SOURCES / 'geneva_points.geojson').read_text())
    infrastructure['roadguard_notice'] = 'Socles de radars SITG; présence et activité du dispositif non établies. Exclus des alertes et du total radar.'
    (ASSETS / 'radar_reference_geneva.geojson').write_text(json.dumps(infrastructure, ensure_ascii=False) + '\n')
    (SOURCES / 'quarantined_country_labels.json').write_text(json.dumps(quarantine, ensure_ascii=False, indent=2) + '\n')
    manifest = dict(schema=1, bundle_version='2026-10-16.2', assembled_on='2026-10-16',
                    licence='ODbL 1.0 (derived database; individual source rights retained)',
                    official_fr_count=3309, countries=countries,
                    audit=dict(sorted(AUDIT.items())), raw_counts={k:dict(v) for k,v in RAW_COUNTS.items()},
                    geneva_infrastructure_only=len(infrastructure['features']),
                    deduplication='25 m across compatible source categories; 2 m within an export family; known opposing directions retained; official device IDs retained',
                    limitations=['Not an exhaustive or live register; presence and operation not guaranteed.',
                                 'Publication/extraction date is not a field-validation date.',
                                 'Historical mobile, temporary and works locations are opt-in and unconfirmed.',
                                 'FR and CH points are loaded for map and GPS alerts; all historical presence is unconfirmed.'],
                    default_selection=dict(french_historical=False,swiss_historical=True,
                                           FR=countries['FR']['static_count'],CH=countries['CH']['count'],
                                           total=countries['FR']['static_count']+countries['CH']['count']),
                    sources=json.loads((SOURCES / 'source_provenance.json').read_text()),
                    inputs=[dict(file=p.name, sha256=hashlib.sha256(p.read_bytes()).hexdigest())
                            for p in sorted(SOURCES.iterdir()) if p.is_file() and p.name != 'quarantined_country_labels.json'])
    (ASSETS / 'radar_bundle_manifest.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + '\n')
    print(json.dumps(dict(countries=countries, audit=manifest['audit'], raw_counts=manifest['raw_counts']), ensure_ascii=False, indent=2))
    return manifest


if __name__ == '__main__':
    build()
