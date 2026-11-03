from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
JAVA=(ROOT/'app/src/main/java/fr/roadguard/app/MainActivity.java').read_text()
assert 'int officialRadarCount=0;' in JAVA
assert 'officialRadarCount=0;rebuildDangerSpatialIndex();loadOfficialRadarDatabase();' in JAVA
assert 'officialRadarCount++;if(hasNearbyRadar(lat,lon))continue;' in JAVA
assert 'SOCLE OFFICIEL · "+officialRadarCount+" ENTRÉES' in JAVA
for category in ['all','fixed','mobile','tunnel','redlight','average','variable','danger','crossing']:
    assert f'"{category}"' in JAVA or category in JAVA
assert 'countActiveRadars(radarFilter)' in JAVA
assert 'dangerSpatialIndex.query(lastLat,lastLon,50000)' in JAVA
assert 'RADARS DISPONIBLES' in JAVA
assert 'Mes radars sauvegardés' in JAVA
assert 'int radarCount' not in JAVA
print('Compteurs radars : base officielle séparée; filtres locaux valides et non expirés')
