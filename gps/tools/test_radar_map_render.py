from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
MAP=(ROOT/'app/src/main/assets/map.html').read_text()
MAIN=(ROOT/'app/src/main/java/fr/roadguard/app/MainActivity.java').read_text()
BG=(ROOT/'app/src/main/java/fr/roadguard/app/BackgroundAlertService.java').read_text()
assert 'dangerItems=Array.isArray(items)?items:[]' in MAP
assert 'const bounds=map.getBounds().pad(.12)' in MAP
assert 'bounds.contains([d[0],d[1]])' in MAP
assert 'map.on(\'moveend zoomend\',renderVisibleDangers)' in MAP
assert 'Base officielle · data.gouv.fr' in MAP and 'Base téléchargée ou point local' in MAP
assert 'function dangerCategory(type)' in MAP
for category in ['fixed','mobile','tunnel','redlight','average','crossing','danger','works']:
    assert f"'{category}'" in (ROOT/'app/src/main/assets/radar_icons.js').read_text() or f'{category}:' in (ROOT/'app/src/main/assets/radar_icons.js').read_text()
assert 'dangers=parsed;officialRadarCount=0;rebuildDangerSpatialIndex();loadOfficialRadarDatabase();rebuildDangerSpatialIndex();' in MAIN
assert 'String type=officialRadarName(code);if(type==null)continue;officialRadarCount++;if(hasNearbyRadar(lat,lon))continue;' in MAIN
assert 'c[2].trim()),lon=Double.parseDouble(c[3].trim())' in BG
assert 'DangerTypePolicy.isRadar(name)' in BG
assert 'String key=pendingDanger.lat+":"+pendingDanger.lon' in MAIN
print('Rendu carte radar borné au viewport; provenance et dédoublonnage préservés')
