from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
JAVA=(ROOT/'app/src/main/java/fr/roadguard/app/MainActivity.java').read_text()
POLICY=(ROOT/'app/src/main/java/fr/roadguard/app/RouteRequestPolicy.java').read_text()
assert 'alternatives=3' in POLICY
assert 'MAX_ROUTE_CHOICES=4' in JAVA
assert 'computed.add(parseOsrmRoute(routeArray.getJSONObject(ri)))' in JAVA
assert 'm.optInt("exit",0)' in JAVA
assert 'Dans le rond-point, prendre la sortie "+exitNumber' in JAVA
assert 'exit roundabout' in JAVA and 'exit rotary' in JAVA
assert 'if(modifier.contains("uturn"))action="Faire demi-tour"' in JAVA
assert 'Obliquer légèrement à gauche' in JAVA and 'Obliquer légèrement à droite' in JAVA
assert 'Double.compare(a.seconds,b.seconds)' in JAVA
assert 'computed.get(i).meters<computed.get(shortest).meters' in JAVA
assert 'ITINÉRAIRES DISPONIBLES' in JAVA
assert 'applyRouteOption(routeIndex,true)' in JAVA
assert 'o.put("alternatives",alternatives)' in JAVA
assert 'availableRoutes.add(option)' in JAVA
assert 'RouteRequestPolicy.distanceToPolylineMeters(lastLat,lastLon,navigationPoints)' in JAVA
assert 'RouteRequestPolicy.matchPolyline(fix.getLatitude(),fix.getLongitude(),navigationPoints,earliest)' in JAVA
assert 'guidanceProgressSegment=Math.max(guidanceProgressSegment,match.segmentIndex)' in JAVA
assert 'next.routeIndex>match.segmentIndex' in JAVA
assert '!location.hasSpeed()||location.getSpeed()<1.5f' in JAVA
assert 'distanceToPolylineMeters(double lat,double lon,List<double[]> points)' in POLICY
assert 'wrappedLongitudeDelta(a[1],lon)' in POLICY
assert 'Math.max(0,Math.min(1,-(ax*dx+ay*dy)/denominator))' in POLICY
assert 'void clearNavigation()' in JAVA and 'availableRoutes.clear()' in JAVA
print('Itinéraires : alternatives OSRM, distance au segment et recalcul auto protégé vérifiés')
