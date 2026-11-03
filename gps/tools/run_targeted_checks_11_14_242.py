#!/usr/bin/env python3
"""Reproducible offline checks. No SDK, no APK, no simulated claim of device validation."""
import json,subprocess,tempfile,sys,shutil
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
VERSION='11.14.242'
JAVAC=['javac'] if shutil.which('javac') else ['java','-m','jdk.compiler/com.sun.tools.javac.Main']
proof=ROOT/'verification'/f'targeted-checks-{VERSION}.log'
results=[]
def run(name,args,timeout=60):
    completed=subprocess.run([str(x) for x in args],cwd=ROOT,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=timeout)
    results.append({'name':name,'exit_code':completed.returncode,'output':completed.stdout})
    print(name,completed.returncode,completed.stdout[-1600:],flush=True)
    if completed.returncode:raise RuntimeError(name+' failed')
try:
    with tempfile.TemporaryDirectory(prefix='roadguard-jvm-') as temporary:
        build=Path(temporary);classes=build/'classes';classes.mkdir();parser=build/'parser';parser.mkdir()
        java=ROOT/'app/src/main/java/fr/roadguard/app'
        names=['RadarArchivePolicy','RadarArchiveDownloader','RadarCountryBatch','RadarDownloadSource','RadarCatalogRevision','DangerTypePolicy','RadarAlertPolicy','RadarIdentity','AlertMilestones','AlertPresentation','BundledRadarCatalog','NeighborRadarCatalog','LufopEuropeCatalog','UpdatedRadarCatalog','GpsMotion','NavigationIntentInput','CarProjection','RouteRequestPolicy']
        sources=[java/(n+'.java') for n in names]+list((ROOT/'tools/jvm-support').rglob('*.java'))
        sources += [ROOT/'tools'/name for name in ['TestRadarDownloads.java','TestCatalogCore.java','TestAlertPolicies.java','motion-tests/TestMotion.java']]
        run('compile-core-java8',JAVAC+['--release','8','-d',classes]+sources)
        run('download-errors-queue',['java','-cp',classes,'fr.roadguard.app.TestRadarDownloads'])
        run('catalog-additive-rollback',['java','-Xmx768m','-cp',classes,'fr.roadguard.app.TestCatalogCore',ROOT/'app/src/main/assets'])
        run('alert-distance-types',['java','-cp',classes,'fr.roadguard.app.TestAlertPolicies'])
        run('gps-motion',['java','-cp',classes,'fr.roadguard.app.TestMotion'])
        run('compile-parser',JAVAC+['-d',parser,ROOT/'tools/ParseJavaSources.java'])
        run('java-syntax',['java','-cp',parser,'ParseJavaSources',ROOT/'app/src'])
        for script in ['test_camera_anchor.js','test_vector_camera.js','test_camera_motion.js']:
            run(script,['node',ROOT/'tools'/script])
    print('PASS all targeted checks; Android build and Robolectric NOT executed',flush=True)
except Exception as error:
    print(type(error).__name__,str(error),flush=True);sys.exit(1)
finally:
    proof.write_text('\n\n'.join(r['name']+'\nexit='+str(r['exit_code'])+'\n'+r['output'] for r in results)+'\n')
    (ROOT/'verification'/f'targeted-results-{VERSION}.json').write_text(json.dumps(results,indent=2,ensure_ascii=False)+'\n')
