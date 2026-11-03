#!/usr/bin/env python3
"""Fetch hash-pinned MapLibre GL JS 4.7.1 browser assets and license."""
from pathlib import Path
from urllib.request import urlopen, Request
import hashlib
VERSION="4.7.1"
DEST=Path(__file__).resolve().parents[1]/"app/src/main/assets/vendor"
FILES={
 "maplibre-gl.js":(300_000,15_000_000,"be9633c4d870e26fb37f1cfe5c5a77181667114003ea16207ac7850d8da8add1"),
 "maplibre-gl.css":(10_000,1_000_000,"576b085fdd9487a65a19215328c1e086c07ce5bf6da09b666b3806d3d008dae9"),
}
DEST.mkdir(parents=True,exist_ok=True)
for name,(minimum,maximum,expected_sha) in FILES.items():
    urls=[f"https://unpkg.com/maplibre-gl@{VERSION}/dist/{name}",f"https://cdn.jsdelivr.net/npm/maplibre-gl@{VERSION}/dist/{name}"]
    error=None
    for url in urls:
        try:
            print("Fetching",url)
            with urlopen(Request(url,headers={"User-Agent":"ROADGUARD-asset-vendor/1.0"}),timeout=40) as response:
                data=response.read(maximum+1)
            digest=hashlib.sha256(data).hexdigest()
            if not minimum<=len(data)<=maximum: raise RuntimeError(f"Unexpected asset size: {len(data)}")
            if digest!=expected_sha: raise RuntimeError(f"SHA256 mismatch: {digest}")
            if name.endswith(".js") and b"maplibregl" not in data: raise RuntimeError("Unexpected JS content")
            if name.endswith(".css") and b"maplibregl" not in data: raise RuntimeError("Unexpected CSS content")
            (DEST/name).write_bytes(data)
            print(name,len(data),"SHA256",digest)
            error=None
            break
        except Exception as exc: error=exc
    if error: raise RuntimeError(f"Could not get verified {name} from the pinned sources: {error}")
license_url=f"https://unpkg.com/maplibre-gl@{VERSION}/LICENSE.txt"
with urlopen(Request(license_url,headers={"User-Agent":"ROADGUARD-asset-vendor/1.0"}),timeout=40) as response:
    license=response.read(100_000)
if b"Redistribution and use in source and binary forms" not in license: raise RuntimeError("MapLibre license text missing")
(DEST/"LICENSE-maplibre-gl.txt").write_bytes(license)
print("Verified MapLibre 4.7.1 assets and license copied into the APK assets.")
