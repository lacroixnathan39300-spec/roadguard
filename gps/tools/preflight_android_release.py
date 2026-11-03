#!/usr/bin/env python3
"""ROADGUARD Android release preflight: no network, no Android build claimed."""
import os,re,shutil,sys,hashlib
from pathlib import Path
root=Path(__file__).resolve().parents[1]
gradle=(root/'app/build.gradle').read_text(encoding='utf-8')
version=re.search(r"versionName\s+['\"]([^'\"]+)",gradle)
code=re.search(r"versionCode\s+(\d+)",gradle)
errors=[]
if not version or not code: errors.append('Android versionName/versionCode missing')
for rel in ('app/src/main/AndroidManifest.xml','app/src/main/java/fr/roadguard/app/MainActivity.java','app/src/main/assets/map_maplibre.html','app/libs/brotli-dec-0.1.2.jar'):
 if not (root/rel).is_file():errors.append('Missing '+rel)
assets={'maplibre-gl.js':'be9633c4d870e26fb37f1cfe5c5a77181667114003ea16207ac7850d8da8add1','maplibre-gl.css':'576b085fdd9487a65a19215328c1e086c07ce5bf6da09b666b3806d3d008dae9'}
for name,want in assets.items():
 f=root/'app/src/main/assets/vendor'/name
 if not f.is_file():errors.append('Missing bundled asset '+name)
 elif hashlib.sha256(f.read_bytes()).hexdigest()!=want:errors.append('Invalid bundled asset '+name)
sdk=os.getenv('ANDROID_HOME') or os.getenv('ANDROID_SDK_ROOT')
if not sdk:
 for candidate in ('/opt/android-sdk','/opt/android','/usr/local/lib/android/sdk'):
  if Path(candidate,'platforms/android-34/android.jar').exists():sdk=candidate;break
if not sdk or not Path(sdk,'platforms/android-34/android.jar').is_file():errors.append('Android SDK platform 34 unavailable')
if not (root/'gradlew').is_file() and not shutil.which('gradle'):errors.append('Gradle and Gradle wrapper unavailable')
print('ROADGUARD version:',version.group(1) if version else 'UNKNOWN','code:',code.group(1) if code else 'UNKNOWN')
for e in errors:print('BLOCKED:',e)
if errors:sys.exit(2)
print('Preflight passed. Build still required.')
