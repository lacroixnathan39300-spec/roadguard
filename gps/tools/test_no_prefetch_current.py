from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]
java=(root/'app/src/main/java/fr/roadguard/app/MainActivity.java').read_text()
leaflet=(root/'app/src/main/assets/map.html').read_text()
maplibre=(root/'app/src/main/assets/map_maplibre.html').read_text()
data=json.loads((root/'app/src/main/assets/roadguard_progress.json').read_text())
assert 'maybePrefetch50Km' not in java and 'maybeSyncNearbyPacks' not in java
assert 'OfflinePackSync.drain' not in java and 'setLicensedEmergencyMap' not in java+leaflet+maplibre
assert 'rgWarmAhead' not in leaflet and 'rgWarm20km' not in leaflet and 'rgPrefetch' not in leaflet
assert 'new Image()' not in leaflet and 'keepBuffer:2' in leaflet
assert 'WebSettings.LOAD_DEFAULT' in java and 'WebSettings.LOAD_CACHE_ONLY' in java
assert 'tuiles déjà consultées' in java and 'couverture ne sont pas garanties' in java
assert not any('50 km' in x['name'] or 'préchargement' in x['name'].lower() for x in data['workstreams'])
for p in (root/'app/src/main/java/fr/roadguard/app').glob('Emergency*.java'):
 raise AssertionError('obsolete prefetch class remains: '+p.name)
print('OK: aucun préchargement cartographique; cache normal WebView conservé')
