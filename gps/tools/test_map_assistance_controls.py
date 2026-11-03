from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
JAVA=(ROOT/'app/src/main/java/fr/roadguard/app/MainActivity.java').read_text()
MAP=(ROOT/'app/src/main/assets/map.html').read_text()
ML=(ROOT/'app/src/main/assets/map_maplibre.html').read_text()
assert 'addMapPreferenceSwitch(content,"Zoom adaptatif"' in JAVA
assert 'putBoolean("adaptive_zoom",adaptiveZoom)' in JAVA
assert 'addMapPreferenceSwitch(content,"Anticipation du trajet"' in JAVA
assert 'putBoolean("look_ahead",lookAhead)' in JAVA
assert 'addMapPreferenceSwitch(content,"Réduction des commandes en roulant"' in JAVA
assert 'putBoolean("auto_declutter",autoDeclutter)' in JAVA
assert 'updateDrivingDeclutter(lastSpeedKmh>0)' in JAVA
assert 'rgVisualFix(point,follow,bearing,speedKmh,lookAhead)' in MAP
assert 'function rgLookAheadCenter(position,bearing,speedKmh,enabled)' in ML
assert 'center=rgLookAheadCenter(lastPos,heading,speed,lookAhead)' in ML
print('Réglages de suivi GPS exposés et anticipation MapLibre vérifiés')
