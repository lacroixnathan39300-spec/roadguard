from pathlib import Path
s=(Path(__file__).resolve().parents[1]/'app/src/main/assets/map.html').read_text()
assert 'position:fixed;inset:0;width:100vw;height:100dvh' in s
assert 'new ResizeObserver(rgSyncMapSize)' in s
assert 'setInterval(rgSyncMapSize,1500)' in s
assert 'rgTileFailures>=8&&rgTileSuccesses===0' in s
assert 'rgTileSuccesses++' in s
assert '<div id="rg-speed"' not in s
assert '<div id="rg-compass"' not in s
print('7 viewport and HUD regression checks passed')
