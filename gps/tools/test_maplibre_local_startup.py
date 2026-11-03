"""Static invariants for the current bundled MapLibre/raster cockpit."""
from pathlib import Path
s=(Path(__file__).resolve().parents[1]/'app/src/main/assets/map_maplibre.html').read_text()
assert "name:'ROADGUARD online OSM (consultation)'" in s
assert 'tiles.openfreemap.org' not in s
assert "https://tile.openstreetmap.org/{z}/{x}/{y}.png" in s
assert "https://roadguard.local/pmtiles/" in s
assert "map.on('style.load'" in s
assert "if(!window.maplibregl)throw Error" in s
assert "background:#20bfff" in s and "circle-color':'#20bfff" in s
assert "line-color':'#37afff" in s and "line-color':'#146cff" in s
assert "raster-saturation',night?-.38:-.08" in s
assert 'vendor/maplibre-gl.js' in s and 'glyphs' in s
print('11 contrôles MapLibre locaux réussis')
