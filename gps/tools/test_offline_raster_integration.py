from pathlib import Path
r=Path(__file__).resolve().parents[1]
java=(r/'app/src/main/java/fr/roadguard/app/MainActivity.java').read_text()
html=(r/'app/src/main/assets/map_offline.html').read_text()
assert 'if(mapLibrePreview&&!forceOnlineMapLibre){String id=' in java
assert 'offlineView=true;mapLibrePreview=false;selected="Carte";render();' in java
assert 'offlineView=false;mapLibrePreview=true;forceOnlineMapLibre=false;selected="Carte";render();' in java
assert 'serveOfflineTile(req.getUrl())' in java
assert 'function setRasterPmtiles(id)' in html
assert "'https://roadguard.local/pmtiles/'" in html
assert 'tileCache.clear();draw();' in html
print('6 current source integration checks passed (not an Android build)')
