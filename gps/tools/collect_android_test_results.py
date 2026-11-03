#!/usr/bin/env python3
"""Keep the executed XML evidence before another build clears app/build."""
from pathlib import Path
import json,shutil,xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[1];VERSION='11.14.250'
source=ROOT/'app/build/test-results/testDebugUnitTest';destination=ROOT/f'verification/robolectric-{VERSION}';destination.mkdir(exist_ok=True)
required={'RealSevenZipRuntimeTest','CanonicalSqliteRuntimeTest','StartupRuntimeTest','LocalPageRuntimeTest','RadarCatalogReloadRuntimeTest','MapAppearanceRuntimeTest','TileCacheRuntimeTest','CountryManagerRuntimeTest'}
records=[]
for path in sorted(source.glob('TEST-*.xml')):
 root=ET.parse(path).getroot();record={key:root.attrib[key] for key in ['name','tests','skipped','failures','errors','time','timestamp']}
 record['cases']=[case.attrib['name'] for case in root.findall('testcase')]
 records.append(record);shutil.copy2(path,destination/path.name)
assert required.issubset({r['name'].split('.')[-1] for r in records}),'Missing expected suite'
assert all(int(r['tests'])>0 and all(int(r[k])==0 for k in ['failures','errors','skipped']) for r in records),'A suite failed or skipped'
assert 'BUILD SUCCESSFUL' in (ROOT/f'verification/gradle-tests-{VERSION}.log').read_text()
result={'version':VERSION,'runner':'Robolectric 4.16.1','android_apis':[26,28,34,36],'scope':'All compiled unit test suites, including eight radar/map startup suites; Android APIs 26/36 and tile cache API 28/34. Native SQLite mode for the two SQLite import suites. Shadow WebView; no real browser pixel rendering or physical device.','tests':sum(int(r['tests']) for r in records),'failures':0,'errors':0,'skipped':0,'suites':records}
(ROOT/f'verification/android-native-results-{VERSION}.json').write_text(json.dumps(result,indent=2,ensure_ascii=False)+'\n')
print(json.dumps({k:v for k,v in result.items() if k!='suites'},indent=2))
