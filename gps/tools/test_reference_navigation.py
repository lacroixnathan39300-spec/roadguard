"""Guard the visible navigation changes requested after Xiaomi screenshot review."""
from pathlib import Path

root=Path(__file__).resolve().parents[1]
java=(root/'app/src/main/java/fr/roadguard/app/MainActivity.java').read_text()
leaf=(root/'app/src/main/assets/map.html').read_text()
vector=(root/'app/src/main/assets/map_maplibre.html').read_text()
local=(root/'app/src/main/assets/map_offline.html').read_text()

assert 'if(!selected.equals("Carte")&&legacySection)' in java
assert 'ReferenceUi.show(this)' in java
assert 'boolean mapMenu=selected.equals("Carte")&&name.equals("Carte");' in java
assert 'if(mapMenu){showRoadguardDrawer();return;}' in java
assert 'drawerItem(items,"▰","Avancement"' in java
assert 'boolean report=selected.equals("Carte")&&name.equals("Signaler");' in java
assert 'b.setMinHeight(pxDp(64))' in java
assert 'driveSheetHandle.setMinHeight(pxDp(driveSheetLevel==0?24:37))' in java
assert 'routeColor||"#2eafff",weight:6' in leaf  # Blue fallback, now user-configurable.
assert "'line-color':'#37afff','line-width':5" in vector
assert "ctx.strokeStyle='#2eafff';ctx.lineWidth=5" in local
assert "'circle-color':'#20bfff'" in vector
assert "dot(gps,'#20bfff',9)" in local
print('Navigation visuelle : carte dégagée, menu/progression accessibles, signalement central, trajet bleu, GPS bleu conservé')
