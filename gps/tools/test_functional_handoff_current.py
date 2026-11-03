from pathlib import Path
import json,re
ROOT=Path(__file__).resolve().parents[1]
JAVA=(ROOT/'app/src/main/java/fr/roadguard/app/MainActivity.java').read_text()
POLICY=(ROOT/'app/src/main/java/fr/roadguard/app/RouteRequestPolicy.java').read_text()
GRADLE=(ROOT/'app/build.gradle').read_text()
DATA=json.loads((ROOT/'app/src/main/assets/roadguard_progress.json').read_text())
assert ("versionName '"+DATA['version']+"'") in GRADLE and ('versionCode '+str(DATA['versionCode'])) in GRADLE
assert 'RouteRequestPolicy.osrmUrl(fromLat,fromLon,toLat,toLon)' in JAVA
assert 'alternatives=3' in POLICY and 'MAX_ROUTE_CHOICES=4' in JAVA
assert 'https://data.geopf.fr/geocodage/completion/' in JAVA
assert 'RouteRequestPolicy.isSafeResponse' in JAVA and 'RouteRequestPolicy.responseWithinLimit' in JAVA
assert 'routeRequestPending=false;applyRouteOption(0,true)' in JAVA and 'Toast.makeText(this,routes.size()>1' in JAVA
assert 'coordinate(fromLat,fromLon)' in POLICY
assert 'router.project-osrm.org'.lower() in POLICY.lower()
assert 'observedBytes<=1_500_000' in POLICY
assert 'maybePrefetch50Km' not in JAVA and 'maybeSyncNearbyPacks' not in JAVA
assert 'RouteRequestPolicy.MAX_ROUTE_POINTS' in JAVA
assert '!RouteRequestPolicy.coordinate(lat,lon)' in JAVA
assert 'RouteRequestPolicy.isValidRouteSummary(option.meters,option.seconds,option.points.size())' in JAVA
assert 'licensed_emergency_prefetch_allowed' not in JAVA.replace('\"licensed_emergency_prefetch_allowed\"','')
assert ('versionCode '+str(DATA['versionCode'])) in GRADLE
assert DATA['workstreams'][0]['percent_estimate']==100
assert not any('préchargement' in w['name'].lower() or 'prefetch' in w['name'].lower() for w in DATA['workstreams'])
print('Contrôles fonctionnels de passation ROADGUARD '+DATA['version']+' réussis')
