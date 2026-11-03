"""Static checks for the current offline MapLibre build-time bundling path."""
from pathlib import Path

root=Path(__file__).resolve().parents[1]
g=(root/'app/build.gradle').read_text()
h=(root/'app/src/main/assets/map_maplibre.html').read_text()
assert "tasks.register('prepareBundledMapLibre')" in g
assert "dependsOn tasks.named('prepareBundledMapLibre')" in g
assert 'setConnectTimeout(12000)' in g and 'setReadTimeout(25000)' in g
assert 'maplibre-gl@4.7.1' in g
assert 'temp.delete()' in g
assert 'versionCode 171' in g and "versionName '11.13.10'" in g
assert '© OpenMapTiles © OpenStreetMap contributors' in h
assert 'vendor/maplibre-gl.js' in h and 'vendor/maplibre-gl.css' in h
assets=root/'app/src/main/assets'
assert (assets/'vendor/maplibre-gl.js').stat().st_size == 803086
assert (assets/'vendor/maplibre-gl.css').stat().st_size == 65534
assert 'LICENSE-maplibre-gl.txt' in [p.name for p in (assets/'vendor').iterdir()]
assert 'LICENSE-google-brotli.txt' in [p.name for p in (assets/'vendor').iterdir()]
assert (root/'app/libs/brotli-dec-0.1.2.jar').stat().st_size == 98115
assert (assets/'glyphs/OFL-Noto-Sans.txt').is_file()
for rng in ('0-255','256-511','512-767'):
    assert (assets/f'glyphs/Noto Sans Regular/{rng}.pbf').is_file()
assert 'text-field' in h and "'source-layer':'place'" in h and "'source-layer':'transportation_name'" in h
assert 'tiles.openfreemap.org' not in h
print('16 current MapLibre bundling, license, glyph and label checks passed')
