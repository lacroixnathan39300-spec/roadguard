#!/usr/bin/env python3
"""Focused 287 routing geometry and cancellable HTTP checks; no SDK or external network."""
from pathlib import Path
import subprocess,tempfile
root=Path(__file__).resolve().parents[1]
names=['RouteRequestPolicy','NavigationProgress','RadarApproachFilter','LatestRouteHttp']
tests=['TestRouteDirection','TestLatestRouteHttp','TestRouteContinuity','RadarApproachFilterProbe']
logs=[]
def run(label,args):
 p=subprocess.run(list(map(str,args)),cwd=root,capture_output=True,text=True,timeout=90);out=p.stdout+p.stderr
 logs.append(label+'\n'+out+'\nexit='+str(p.returncode));print(label+'\n'+out,flush=True)
 if p.returncode:raise SystemExit(p.returncode)
try:
 with tempfile.TemporaryDirectory(prefix='roadguard-route-') as build:
  run('compile-java8',['java','com.sun.tools.javac.Main','--release','8','-d',build]+[root/'app/src/main/java/fr/roadguard/app'/(n+'.java') for n in names]+[root/'tools'/(n+'.java') for n in tests])
  for test in tests:run(test,['java','-cp',build,'fr.roadguard.app.'+test])
finally:
 (root/'verification/route-recovery-11.14.287.log').write_text('\n\n'.join(logs)+'\n')
