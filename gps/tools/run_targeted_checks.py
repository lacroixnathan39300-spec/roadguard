#!/usr/bin/env python3
"""Reproducible offline checks. No SDK, no APK, no simulated claim of device validation."""
import json,subprocess,tempfile,sys,shutil,re
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
VERSION=re.search(r"versionName\s+'([^']+)'",(ROOT/'app/build.gradle').read_text()).group(1)
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
        names=['RadarArchivePolicy','RadarArchiveDownloader','RadarCountryBatch','RadarDownloadSource','RadarCatalogRevision','DangerTypePolicy','RadarAlertPolicy','RadarIdentity','RadarCatalogMerge','AlertMilestones','AlertPresentation','BundledRadarCatalog','NeighborRadarCatalog','LufopEuropeCatalog','UpdatedRadarCatalog','GpsMotion','NavigationIntentInput','DestinationCoordinates','CarProjection','RouteRequestPolicy','CanonicalRadar','CanonicalRadarStore','CanonicalRadarCatalog','SpeedAlertSqliteImporter','RadarActivationState','VisibleTileCache','RadarSupplementCatalog','RadarSourcePolicy','RadarSevenZipExtractor','RadarArchiveInstaller']
        sources=[java/(n+'.java') for n in names]+list((ROOT/'tools/jvm-support').rglob('*.java'))
        sources += [ROOT/'tools'/name for name in ['TestSevenZipImport.java','TestTileCacheStandalone.java','TestRadarSupplements.java','TestRadarDownloads.java','TestCanonicalRadar.java','TestCatalogCore.java','TestAlertPolicies.java','motion-tests/TestMotion.java']]
        jars=':'.join(str(ROOT/'app/libs'/name) for name in ['brotli-dec-0.1.2.jar','roadguard-radar-runtime-1.jar'])
        classpath=str(classes)+':'+jars
        run('compile-core-java8',JAVAC+['--release','8','-cp',jars,'-d',classes]+sources)
        run('codeassist-no-vendor-classpath',['python3',ROOT/'tools/test_codeassist_7z_classpath.py'])
        run('real-7z-sqlite-import',['java','-Xmx768m','-Droadguard.sqlite.probe='+str(ROOT/'tools/sqlite_jvm_probe.py'),'-cp',classpath,'fr.roadguard.app.TestSevenZipImport',ROOT/'radar_sources/speedalert-europe-202606',ROOT/'app/src/main/assets'])
        run('visible-tile-cache',['java','-cp',classes,'fr.roadguard.app.TestTileCacheStandalone'])
        run('radar-supplements',['java','-Xmx768m','-cp',classes,'fr.roadguard.app.TestRadarSupplements',ROOT/'app/src/main/assets'])
        run('tile-recovery-and-pack',['node',ROOT/'tools/test_tile_recovery_pack.js'])
        run('sqlite-fixtures',['python3',ROOT/'tools/create_sqlite_fixtures.py',build/'sqlite'])
        run('canonical-sqlite-store',['java','-Xmx768m','-Droadguard.sqlite.probe='+str(ROOT/'tools/sqlite_jvm_probe.py'),'-cp',classes,'fr.roadguard.app.TestCanonicalRadar',build/'sqlite',ROOT/'app/src/main/assets'])
        run('download-errors-queue',['java','-cp',classes,'fr.roadguard.app.TestRadarDownloads'])
        run('catalog-additive-rollback',['java','-Xmx768m','-cp',classes,'fr.roadguard.app.TestCatalogCore',ROOT/'app/src/main/assets'])
        run('alert-distance-types',['java','-cp',classes,'fr.roadguard.app.TestAlertPolicies'])
        run('gps-motion',['java','-cp',classes,'fr.roadguard.app.TestMotion'])
        run('compile-parser',JAVAC+['-d',parser,ROOT/'tools/ParseJavaSources.java'])
        run('java-syntax',['java','-cp',parser,'ParseJavaSources',ROOT/'app/src'])
        run('map-inline-syntax',['node',ROOT/'tools/roadguard_map_syntax_check.js'])
        # Camera sources are unchanged; their 242 evidence is retained without a repeated camera audit.
    print('PASS all targeted checks; this script does not run Android builds or Robolectric',flush=True)
except Exception as error:
    print(type(error).__name__,str(error),flush=True);sys.exit(1)
finally:
    proof.write_text('\n\n'.join(r['name']+'\nexit='+str(r['exit_code'])+'\n'+r['output'] for r in results)+'\n')
    (ROOT/'verification'/f'targeted-results-{VERSION}.json').write_text(json.dumps(results,indent=2,ensure_ascii=False)+'\n')
