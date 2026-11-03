from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
POLICY=(ROOT/'app/src/main/java/fr/roadguard/app/DangerTypePolicy.java').read_text()
MAIN=(ROOT/'app/src/main/java/fr/roadguard/app/MainActivity.java').read_text()
LEAFLET=(ROOT/'app/src/main/assets/map.html').read_text()
VECTOR=(ROOT/'app/src/main/assets/map_maplibre.html').read_text()
ICONS=(ROOT/'app/src/main/assets/radar_icons.js').read_text()

for key, token in {
    'fixed':'fixe', 'mobile':'mobile', 'tunnel':'tunnel', 'redlight':'feu rouge',
    'average':'vitesse moyenne', 'variable':'variable', 'works':'chantier', 'danger':'zone radar', 'crossing':'passage à niveau'
}.items():
    assert f'"{key}".equals(filter)' in POLICY
    assert token in POLICY.lower()
assert 'lufopRadarType(' in MAIN
assert 'place.contains("tunnel")||place.contains("tunel")' in MAIN
assert 'parseLufopCsvRow(String line)' in MAIN and 'fields.size()>2?fields.get(2)' in MAIN
assert 'Radar fixe · tunnel (OSM)' in MAIN
assert 'place.contains("tunnel")||place.contains("tunel")' in MAIN
assert 'CHANTIERS' in MAIN
assert 'String.valueOf(countActiveRadars(radarFilter))' in MAIN
assert 'dangerSpatialIndex.query(lastLat,lastLon,50000)' in MAIN
assert 'pendingDanger.type+" dans "+distance' in MAIN
assert 'names.get(index)+" dans "' in (ROOT/'app/src/main/java/fr/roadguard/app/BackgroundAlertService.java').read_text()
for category in ['fixed','mobile','tunnel','redlight','average','variable','crossing','danger','works']:
    assert f"'{category}'" in ICONS or f"{category}:" in ICONS
    assert 'RoadguardRadarIcons.describe(type)' in LEAFLET and 'RoadguardRadarIcons.describe(type)' in VECTOR
print('Catégories radar : filtres, import tunnel et rendu différencié vérifiés')
