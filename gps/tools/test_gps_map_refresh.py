from pathlib import Path
r=Path(__file__).resolve().parents[1]
js=(r/'app/src/main/assets/map.html').read_text()
java=(r/'app/src/main/java/fr/roadguard/app/MainActivity.java').read_text()
for term in ['rgFirstGpsFix','map.invalidateSize','rgTileStatus','tileerror','tileload','map.setView([lat,lon]']:
 assert term in js,term
for term in ['NETWORK_PROVIDER','typeof updateGPS','getLastKnownLocation(LocationManager.NETWORK_PROVIDER)']:
 assert term in java,term
print('9 GPS/map refresh checks passed')
