from pathlib import Path
r=Path(__file__).resolve().parents[1]
html=(r/'app/src/main/assets/map_maplibre.html').read_text()
gradle=(r/'app/build.gradle').read_text()
assert 'src="vendor/maplibre-gl.js"' in html
assert 'href="vendor/maplibre-gl.css"' in html
assert 'unpkg.com' not in html
assert 'tiles.openfreemap.org' not in html
assert 'prepareBundledMapLibre' in gradle
assert 'cdn.jsdelivr.net/npm/maplibre-gl@4.7.1' in gradle
assert 'https://roadguard.local/pmtiles/' in html
assert 'https://roadguard.local/fonts/{fontstack}/{range}.pbf' in html
assert 'localTileCorsHeaders' in (r/'app/src/main/java/fr/roadguard/app/MainActivity.java').read_text()
print('9 local MapLibre integration checks passed')
