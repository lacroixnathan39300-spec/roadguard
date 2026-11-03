from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = (ROOT / 'app/src/main/java/fr/roadguard/app/MainActivity.java').read_text()
LEAFLET = (ROOT / 'app/src/main/assets/map.html').read_text()
MAPLIBRE = (ROOT / 'app/src/main/assets/map_maplibre.html').read_text()
OFFLINE = (ROOT / 'app/src/main/assets/map_offline.html').read_text()

assert 'getBoolean("radar_markers_visible",true)' in JAVA
assert 'putBoolean("radar_markers_visible",radarMarkersVisible)' in JAVA
assert 'void pushDangers()' in JAVA and 'setRadarMarkersVisible("+radarMarkersVisible+")' in JAVA
assert 'Radars sur la carte' in JAVA
assert 'alertes vocales conservées' in JAVA
assert "radarMarkersVisible||!RoadguardRadarIcons.isRadar(String(d[2]||''))" in LEAFLET
for html in (LEAFLET, MAPLIBRE, OFFLINE):
    assert 'function setRadarMarkersVisible(show)' in html
assert 'dangers.filter(p=>radarMarkersVisible||!RoadguardRadarIcons.isRadar(p[2]))' in OFFLINE
assert "rgShowRadarMarkers||!RoadguardRadarIcons.isRadar(String(d[2]||''))" in MAPLIBRE
assert 'radar:RoadguardRadarIcons.isRadar(type)' in MAPLIBRE
print('Contrôle ciblé visibilité des marqueurs radar réussi')
