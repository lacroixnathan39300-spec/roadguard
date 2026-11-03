#!/usr/bin/env python3
"""Keep the fixed 39-task backlog and credit only recorded build/test results."""
import copy,json,hashlib
from pathlib import Path
from xml.etree import ElementTree
from verify_preserved_data import verify
ROOT=Path(__file__).resolve().parents[1];VERSION='11.14.246';PREVIOUS='11.14.245'
ledger=copy.deepcopy(json.loads((ROOT/f'app/src/main/assets/roadguard_progress_11_14_245.json').read_text()))
ledger.update(version=VERSION,baseline_version=PREVIOUS,date='2026-10-21',method='39 tâches, 100 points et contrats inchangés. Deux compilations APK réalisées (+4 points de développement). Contrôles automatiques M1/D3/U3/C1/Q2/Q3/Q4 (+13 points de validation). Aucun point matériel ou rendu visuel réel.',active_work='Lot démarrage/radars/cache/pack graphique compilé et vérifié automatiquement. Rendu et comportement Xiaomi/autoradio à essayer en fin de lot ; fusion direction/fiabilité et propriété des alertes multimodes restent à terminer.')
tasks={t['id']:t for m in ledger['modules'] for t in m['tasks']}
for t in tasks.values():
 t['baseline_coded']=t['coded']
 for c in t['checks']:
  c['baseline_passed']=c['passed']
  if c['passed']:c['carried_from_version']=PREVIOUS
proof=json.loads((ROOT/f'verification/targeted-results-{VERSION}.json').read_text());executed={r['name']:r for r in proof}
required=['compile-core-java8','visible-tile-cache','radar-supplements','tile-recovery-and-pack','sqlite-fixtures','canonical-sqlite-store','download-errors-queue','catalog-additive-rollback','alert-distance-types','gps-motion','java-syntax','map-inline-syntax','real-7z-sqlite-import']
assert all(executed[name]['exit_code']==0 for name in required)
native=json.loads((ROOT/f'verification/android-native-results-{VERSION}.json').read_text())
assert native['tests']==86 and native['failures']==native['errors']==native['skipped']==0
assert len(native['suites'])==8
for variant in ['phone','auto']:
 log=(ROOT/f'verification/gradle-{variant}-{VERSION}.log').read_text()
 assert 'BUILD SUCCESSFUL' in log and 'GRADLE_EXIT=0' in log,variant
apks=json.loads((ROOT/f'verification/apk-{VERSION}.json').read_text())
assert {a['variant'] for a in apks}=={'TELEPHONE','ANDROID-AUTO'}
for apk in apks:
 file=ROOT/apk['file'];assert hashlib.sha256(file.read_bytes()).hexdigest()==apk['sha256']
 assert apk['signature_exit']==0 and "versionCode='446'" in apk['badging'] and "versionName='11.14.246'" in apk['badging']
def coded(id,evidence):tasks[id].update(coded=True,date='2026-10-21',version=VERSION,code_evidence=evidence)
def checked(id,evidence):
 c=tasks[id]['checks'][0];c.update(passed=True,evidence=evidence);c.pop('carried_from_version',None)
coded('M1','MainActivity : vrai splash à deux frames et carte affichée avant le catalogue sur worker. StartupRuntimeTest bloque le catalogue et conserve un point personnel ajouté pendant le chargement. HTTP externe isolé ; WebView simulé, aucun rendu matériel.')
checked('M1',f'verification/robolectric-{VERSION}/TEST-fr.roadguard.app.StartupRuntimeTest.xml et LocalPageRuntimeTest.xml : API 26/36, démarrage et pages locales, zéro échec.')
coded('D3','RadarDownloadActivity : états réception/import/consommation distincts et inventaire chargé. CountryManagerRuntimeTest contrôle comptages, bouton Installer et indisponibilité ; CanonicalSqliteRuntimeTest vérifie le reçu après consommation. Rechargement à chaud téléphone également exécuté.')
checked('D3',f'verification/robolectric-{VERSION}/TEST-fr.roadguard.app.CountryManagerRuntimeTest.xml ; CanonicalSqliteRuntimeTest.xml ; RadarCatalogReloadRuntimeTest.xml : API 26/36.')
coded('D5','Extracteur 7z borné conservé ; bibliothèques isolées dans fr.roadguard.vendor contre les anciennes classes du framework Android 16. JAR runtime préfourni, cinq originaux et licences conservés. Aucun changement aux limites mémoire/taille/CRC ni au remplacement atomique.')
checked('D5',f'verification/targeted-checks-{VERSION}.log : 25 scénarios avec le runtime isolé ; verification/robolectric-{VERSION}/TEST-fr.roadguard.app.RealSevenZipRuntimeTest.xml : import FR/CH avec SQLite natif, API 26/36. Pas d’essai matériel.')
tasks['D6']['code_evidence']+=' ; import lecture seule et métadonnées maintenant exercés avec SQLite natif Robolectric 26/36.'
checked('D6',f'verification/targeted-checks-{VERSION}.log ; verification/robolectric-{VERSION}/TEST-fr.roadguard.app.CanonicalSqliteRuntimeTest.xml et RealSevenZipRuntimeTest.xml.')
tasks['U1']['code_evidence']='Graphite/rouge conservé ; trois styles locaux. MapAppearanceRuntimeTest vérifie préférence et commandes carte sur API 26/36 ; contrôle JavaScript exécuté. Ces contrôles ne couvrent pas les pixels : aucun crédit theme-ui ou matériel ajouté.'
tasks['U2']['code_evidence']='CountryManagerRuntimeTest contrôle les insets de son écran sur API 26/36. ModernAndroidRuntimeTest et revue des formats complets non exécutés dans ce lot : contrat modern-insets non crédité.'
checked('U3',f'verification/robolectric-{VERSION}/TEST-fr.roadguard.app.CountryManagerRuntimeTest.xml : tri français et recherche sans accents, API 26/36.')
checked('C1',f'verification/gradle-phone-{VERSION}.log : assembleDebug avec roadguardAndroidAuto=false ; APK téléphone contrôlé par apksigner/aapt2.')
coded('Q1','app/build.gradle : 11.14.246 / 446 ; ledger 246 et deltas depuis 245 raccordés ; package et SDK conservés.')
checked('Q1',f'verification/release-contract-{VERSION}.json')
coded('Q2','tools/run_targeted_checks.py, collect_android_test_results.py, update_progress_11_14_246.py et render_progress_report.py : contrôles ciblés, XML conservés et calcul pondéré reproductible. Les anciens audits caméra restent historiques, non réexécutés.')
checked('Q2',f'verification/targeted-results-{VERSION}.json ; android-native-results-{VERSION}.json ; release-contract-{VERSION}.json ; backlog fixe 39 tâches / 100 points vérifié.')
for id,variant in [('Q3','phone'),('Q4','auto')]:
 coded(id,f'APK {variant} réellement produit avec SDK 36, AGP 8.10.1 / Gradle 8.11.1 et JDK 17. Sorties nettoyées avant changement de variante ; signature debug, aucune signature CodeAssist remplacée.')
 checked(id,f'verification/gradle-{variant}-{VERSION}.log ; verification/apk-{VERSION}.json : signature, manifeste, version et assets contrôlés.')
for id in ['M2','M6','A2','A3','D1','D8','S2']:
 for c in tasks[id]['checks']:
  if c['passed']:c['evidence']+=f' ; réexécuté : verification/targeted-checks-{VERSION}.log';c.pop('carried_from_version',None)
tasks['Q5']['code_evidence']='Aucune installation CodeAssist, aucun essai Xiaomi, DHU ou autoradio 246. APK debug fournis pour vérification ; mise à jour avec les sources et la signature habituelle pour conserver les données.'
gradle=(ROOT/'app/build.gradle').read_text()
for text in ['versionCode 446',"versionName '11.14.246'","applicationId 'fr.roadguard.app'",'compileSdk 36','targetSdk 36','minSdk 26']:assert text in gradle
assert 'roadguard_progress_11_14_246.json' in (ROOT/'app/src/main/java/fr/roadguard/app/MainActivity.java').read_text()
for variant in ['main','auto']:ElementTree.parse(ROOT/f'app/src/{variant}/AndroidManifest.xml')
integrity=verify();assert not integrity['failures'];(ROOT/f'verification/data-integrity-{VERSION}.json').write_text(json.dumps(integrity,indent=2)+'\n')
tasks['S1']['checks'][0]['evidence']+=f' ; verification/data-integrity-{VERSION}.json ; contrôle du ZIP au conditionnement'
total=dict(development=0,validation=0,previous_development=0,previous_validation=0)
assert len(tasks)==39 and sum(g['weight'] for g in ledger['modules'])==100
for g in ledger['modules']:
 assert sum(t['weight'] for t in g['tasks'])==g['weight']
 for t in g['tasks']:
  assert sum(c['weight'] for c in t['checks'])==t['weight']
  total['development']+=t['weight'] if t['coded'] else 0;total['previous_development']+=t['weight'] if t['baseline_coded'] else 0
  for c in t['checks']:
   if c['passed']:assert t['coded'] and c['evidence']
   total['validation']+=c['weight'] if c['passed'] else 0;total['previous_validation']+=c['weight'] if c['baseline_passed'] else 0
assert total==dict(development=85,validation=44,previous_development=81,previous_validation=31),total
(ROOT/'app/src/main/assets/roadguard_progress_11_14_246.json').write_text(json.dumps(ledger,indent=2,ensure_ascii=False)+'\n')
contract={'version':VERSION,'version_code':446,'application_id':'fr.roadguard.app','baseline':PREVIOUS,'task_count':39,'weight':100,'totals':total,'new_coded_tasks':['Q3','Q4'],'new_validation_tasks':{'M1':2,'D3':2,'U3':1,'C1':2,'Q2':2,'Q3':2,'Q4':2},'android_native_runtime_executed':True,'robolectric_tests':86,'physical_device_executed':False,'apk_produced':['phone','auto'],'automatic_camera_reaudit':False,'real_webview_render_executed':False,'original_data':integrity,'scope':native['scope']}
(ROOT/f'verification/release-contract-{VERSION}.json').write_text(json.dumps(contract,indent=2,ensure_ascii=False)+'\n')
print(json.dumps(contract,indent=2,ensure_ascii=False))
