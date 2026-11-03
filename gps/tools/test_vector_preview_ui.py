from pathlib import Path
r=Path(__file__).resolve().parents[1]
main=(r/'app/src/main/java/fr/roadguard/app/MainActivity.java').read_text()
html=(r/'app/src/main/assets/map_maplibre.html').read_text()
checks={
 'vector selection wording': 'Prévisualiser ce pack vectoriel (MapLibre local)' in main,
 'no stale vector unavailable text': 'vectoriel non pris en charge' not in main and 'PMTiles vectoriels non affichables' not in main,
 'no CDN runtime': 'unpkg.com' not in html and 'tiles.openfreemap.org' not in html,
 'selected pack state': 'activeVectorPack=id;ready=false;' in html,
 'correct status after style load': "say(activeVectorPack?'PMTILES" in html,
 'dark initial background': 'background:#101c2a' in html,
 'local tile endpoint': 'https://roadguard.local/pmtiles/' in html,
}
for name,passed in checks.items():print(('PASS' if passed else 'FAIL'),name)
assert all(checks.values())
