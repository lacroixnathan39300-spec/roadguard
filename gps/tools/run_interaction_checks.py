#!/usr/bin/env python3
"""Focused checks for lot 285; no Android SDK, real GPS, WebGL or external HTTP."""
import subprocess,tempfile,sys
from pathlib import Path
root=Path(__file__).resolve().parents[1]
names=['TileArchiveInstaller'] if '--tiles-only' in sys.argv else ['RouteRequestPolicy','NavigationProgress','RadarApproachFilter','MapOverlayPlacement','DestinationCoordinates','NavigationIntentInput','LatestSearchHttp','TileArchiveInstaller']
tests=['TestTileArchiveInstaller'] if '--tiles-only' in sys.argv else ['TestRouteContinuity','TestMapInteraction','TestLatestSearchHttp','TestTileArchiveInstaller','RadarApproachFilterProbe']
logs=[]
def run(label,args):
 p=subprocess.run(list(map(str,args)),cwd=root,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=90)
 logs.append(label+'\n'+p.stdout+'\nexit='+str(p.returncode));print(label+'\n'+p.stdout,flush=True)
 if p.returncode:raise SystemExit(p.returncode)
try:
 with tempfile.TemporaryDirectory(prefix='roadguard-interaction-') as build:
  run('compile-java8',['java','com.sun.tools.javac.Main','--release','8','-d',build]+[root/'app/src/main/java/fr/roadguard/app'/(n+'.java') for n in names]+[root/'tools'/(n+'.java') for n in tests])
  for name in tests:run(name,['java','-Xmx256m','-cp',build,'fr.roadguard.app.'+name])
finally:
 (root/'verification/interaction-11.14.285.log').write_text('\n\n'.join(logs)+'\n')
