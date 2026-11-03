"""Source regression checks for ROADGUARD WebView offline tile interception."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
s=(root/'app/src/main/java/fr/roadguard/app/MainActivity.java').read_text()
checks={
 "explicit local 404": 'new WebResourceResponse("text/plain","UTF-8",404,"Not Found"' in s,
 "local PMTiles missing 404": 'return data==null?localTileNotFound()' in s,
 "local XYZ missing 404": '||!tile.isFile())return localTileNotFound()' in s,
 "invalid path 404": 'return localTileNotFound();' in s,
 "nonlocal requests unaffected": 'if(!"roadguard.local".equalsIgnoreCase(uri.getHost()))return null;' in s,
 "https required for local host": '!"https".equalsIgnoreCase(uri.getScheme())' in s,
 "map asset uses https": 'https://roadguard.local' in (root/'app/src/main/assets/map_offline.html').read_text(),
}
for name,ok in checks.items():
 print(("PASS" if ok else "FAIL"),name)
assert all(checks.values())
