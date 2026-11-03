from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
MAP=(ROOT/'app/src/main/assets/map.html').read_text()
ML=(ROOT/'app/src/main/assets/map_maplibre.html').read_text()
assert 'let rgUserCameraPaused=false;' in MAP
assert "map.on('dragstart',function(){" in MAP
assert 'rgUserCameraPaused=true;userCameraHoldUntil=0' in MAP
assert 'if(rgAutoZooming||!rgVisualGps.follow||rgUserCameraPaused||Date.now()<userCameraHoldUntil' in MAP
assert 'if(follow&&!rgUserCameraPaused)map.setView' in MAP
assert 'if(follow && !rgUserCameraPaused && Date.now()>=userCameraHoldUntil && adaptiveZoom)' in MAP
assert 'resumeCamera();map.setView(center' in MAP
assert 'rgUserCameraPaused=false;' in MAP
assert 'manualCameraPaused=false' in ML
assert "map.on('dragstart',()=>{manualCameraPaused=true;followOn=false;})" in ML
assert 'followOn=!!follow&&!manualCameraPaused' in ML
assert 'function resumeCamera(){manualCameraPaused=false;followOn=true;' in ML
assert 'lastPos=[+lon,+lat];ensureGpsMarker();followOn=!!follow&&!manualCameraPaused;' in ML
print('Exploration manuelle persistante dans Leaflet et MapLibre; position GPS continue à se mettre à jour; recentrage volontaire vérifié')
