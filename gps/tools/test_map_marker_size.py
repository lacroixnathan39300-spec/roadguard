from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = (ROOT / 'app/src/main/java/fr/roadguard/app/MainActivity.java').read_text()
MAPS = [(ROOT / f'app/src/main/assets/{name}').read_text() for name in ('map.html','map_maplibre.html','map_offline.html')]
assert 'getInt("map_marker_size",1)' in JAVA
assert 'putInt("map_marker_size",mapMarkerSize)' in JAVA
assert 'Taille des icônes' in JAVA and 'mapMarkerSizeLabel()' in JAVA
assert 'setMapMarkerScale("+mapMarkerSize+")' in JAVA
for source in MAPS:
    assert 'function setMapMarkerScale(size)' in source
assert '[.78,1,1.3][mapMarkerSize]' in MAPS[0]
assert 'rgMapMarkerScale' in MAPS[1]
assert '[3,5,7][mapMarkerSize]' in MAPS[2]
print('Contrôle ciblé des tailles d’icônes réussi sur les trois cartes')
