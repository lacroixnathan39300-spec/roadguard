from pathlib import Path
root=Path(__file__).resolve().parents[1]
java=(root/'app/src/main/java/fr/roadguard/app/MainActivity.java').read_text()
leaflet=(root/'app/src/main/assets/map.html').read_text()
maplibre=(root/'app/src/main/assets/map_maplibre.html').read_text()
assert 'WebSettings.LOAD_CACHE_ONLY' in java and 'WebSettings.LOAD_DEFAULT' in java
assert 'maybePrefetch50Km' not in java and 'maybeSyncNearbyPacks' not in java
assert 'rgWarmAhead' not in leaflet and 'rgWarm20km' not in leaflet
assert 'rgPrefetch' not in leaflet and 'new Image()' not in leaflet
assert 'keepBuffer:2' in leaflet
assert 'setLicensedEmergencyMap' not in leaflet and 'setLicensedEmergencyMap' not in maplibre
assert 'CACHE DE LA CARTE' in java and 'tuiles déjà consultées' in java
print('OK: carte en ligne et cache WebView ordinaire, aucune anticipation automatique')
