#!/usr/bin/env python3
"""Targeted catalog checks. JVM file/preferences adapters; no Android/device validation."""
from pathlib import Path
import subprocess,tempfile
root=Path(__file__).resolve().parents[1]
names='RadarArchivePolicy RadarArchiveDownloader RadarCountryBatch RadarDownloadSource RadarCatalogRevision DangerTypePolicy RadarAlertPolicy RadarIdentity RadarCatalogMerge AlertMilestones AlertPresentation BundledRadarCatalog NeighborRadarCatalog LufopEuropeCatalog UpdatedRadarCatalog GpsMotion NavigationIntentInput DestinationCoordinates CarProjection RouteRequestPolicy CanonicalRadar CanonicalRadarStore CanonicalRadarCatalog SpeedAlertSqliteImporter RadarActivationState VisibleTileCache RadarSupplementCatalog RadarSourcePolicy'.split()
logs=[]
def run(args):
 p=subprocess.run(list(map(str,args)),cwd=root,capture_output=True,text=True,timeout=120)
 out=p.stdout+p.stderr;logs.append(out);print(out,flush=True)
 if p.returncode:raise SystemExit(p.returncode)
try:
 with tempfile.TemporaryDirectory(prefix='roadguard-merge-') as build:
  sources=[root/'app/src/main/java/fr/roadguard/app'/(name+'.java') for name in names]+list((root/'tools/jvm-support').rglob('*.java'))+[root/'tools'/name for name in ['TestCatalogCore.java','TestRadarMerge.java','TestRadarSupplements.java']]
  run(['java','com.sun.tools.javac.Main','--release','8','-d',build]+sources)
  for name in ['TestRadarMerge','TestRadarSupplements','TestCatalogCore']:
   run(['java','-Xmx768m','-cp',build,'fr.roadguard.app.'+name,root/'app/src/main/assets'])
finally:
 (root/'verification/radar-merge-11.14.284.log').write_text('\n'.join(logs))
