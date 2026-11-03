from pathlib import Path
import re
root=Path(__file__).resolve().parents[1]
s=(root/'app/src/main/java/fr/roadguard/app/MainActivity.java').read_text()
assert 'boolean forceOnlineMapLibre=false;' in s
assert 'if(mapLibrePreview&&!forceOnlineMapLibre)' in s
assert 'mapLibrePreview=true;forceOnlineMapLibre=true;selected="Carte";render();' in s
assert 'mapLibrePreview=true;forceOnlineMapLibre=false;selected="Carte";render();' in s
assert 'versionName \'11.13.49\'' in (root/'app/build.gradle').read_text()
assert 'https://tile.openstreetmap.org/{z}/{x}/{y}.png' in (root/'app/src/main/assets/map_maplibre.html').read_text()
print('6 contrôles statiques OK : mode en ligne distinct du pack local')
