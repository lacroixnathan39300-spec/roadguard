#!/usr/bin/env python3
"""Fixed ledger: bounded 7z extraction coded and checked on real FR/CH archives."""
import copy,json
from pathlib import Path
from xml.etree import ElementTree
from verify_preserved_data import verify
ROOT=Path(__file__).resolve().parents[1];VERSION='11.14.245';PREVIOUS='11.14.244'
previous=json.loads((ROOT/f'app/src/main/assets/roadguard_progress_11_14_244.json').read_text());ledger=copy.deepcopy(previous)
ledger.update(version=VERSION,baseline_version=PREVIOUS,date='2026-10-21',method='39 tâches et 100 points inchangés. D5 extraction 7z codée (+3 points) et contrôle automatique réalisé sur vraies archives (+2 points). Aucun crédit de compilation Android, Robolectric ou matériel.',active_work='Extraction 7z et import automatiques FR/CH intégrés, fichiers publics tous récupérés et inclus. Suite : compilation Android, essais du lot sur Xiaomi, fusion directionnelle et confirmations multimodes encore à terminer.')
tasks={t['id']:t for m in ledger['modules'] for t in m['tasks']}
for t in tasks.values():
 t['baseline_coded']=t['coded']
 for c in t['checks']:
  c['baseline_passed']=c['passed']
  if c['passed']:c['carried_from_version']=PREVIOUS
proof=json.loads((ROOT/f'verification/targeted-results-{VERSION}.json').read_text());executed={r['name']:r for r in proof}
required=['compile-core-java8','visible-tile-cache','radar-supplements','tile-recovery-and-pack','sqlite-fixtures','canonical-sqlite-store','download-errors-queue','catalog-additive-rollback','alert-distance-types','gps-motion','java-syntax','map-inline-syntax','real-7z-sqlite-import']
for name in required:assert executed[name]['exit_code']==0,name
assert '25 scenarios' in executed['real-7z-sqlite-import']['output']
assert '17 real-file' in executed['visible-tile-cache']['output']
assert '11 tile recovery' in executed['tile-recovery-and-pack']['output']
tasks['D5'].update(coded=True,date='2026-10-21',version=VERSION,code_evidence='RadarSevenZipExtractor : nom unique, limites 32 Mo/32 Mio mémoire, codecs COPY/LZMA/LZMA2, taille/CRC, annulation et nettoyage ; RadarArchiveInstaller : SHA épinglé, SQLite lecture seule et CanonicalRadarStore ; cinq JAR réels fournis dans app/libs et deux archives FR/CH intégrées.')
tasks['D5']['checks'][0].update(passed=True,evidence='verification/targeted-checks-11.14.245.log : real-7z-sqlite-import, 25 scénarios avec vraies archives et bibliothèques, SQLite réel via adaptateur JVM. La partie extraction Java réelle est exécutée ; aucun Android natif, Robolectric ou matériel.')
tasks['D3']['code_evidence']='RadarDownloadActivity : réception/extraction/import sur worker ; archives FR/CH intégrées, état importé jusqu’au reçu du consommateur, annulation des étapes import distincte du transfert. Tests UI Android non exécutés.'
tasks['D7']['code_evidence']='Rechargement téléphone par révision et surcouche additive ; direction, conflits/fiabilité et confirmations propres au service et à la voiture restent incomplets. Aucun crédit de tâche complète.'
tasks['Q1'].update(version=VERSION,date='2026-10-21',code_evidence='app/build.gradle : 11.14.245 / 445 ; package/SDK conservés ; ledger 245 et référence des deltas 244 raccordés.')
tasks['Q1']['checks'][0]['evidence']=f'verification/release-contract-{VERSION}.json';tasks['Q1']['checks'][0].pop('carried_from_version',None)
tasks['Q3']['code_evidence']='Aucun APK 245 : téléchargement SDK/Gradle refusé par le contrôle d’approbation automatique, autorisation considérée limitée à GitHub. environment-11.14.245.json. Aucun contournement ni build déclaré.'
tasks['Q5']['code_evidence']='Aucun essai matériel 245. Captures utilisateur antérieures sans version visible conservées comme retour, sans crédit de compilation/validation de cette livraison.'
for id in ['M2','M6','A2','A3','D1','D6','D8','S2']:
 for c in tasks[id]['checks']:
  if c['passed']:c['evidence']+=f' ; réexécuté : verification/targeted-checks-{VERSION}.log';c.pop('carried_from_version',None)
gradle=(ROOT/'app/build.gradle').read_text()
for text in ['versionCode 445',"versionName '11.14.245'","applicationId 'fr.roadguard.app'",'compileSdk 36','targetSdk 36','minSdk 26']:assert text in gradle,text
assert 'roadguard_progress_11_14_245.json' in (ROOT/'app/src/main/java/fr/roadguard/app/MainActivity.java').read_text()
for variant in ['main','auto']:ElementTree.parse(ROOT/f'app/src/{variant}/AndroidManifest.xml')
integrity=verify();assert not integrity['failures'];(ROOT/f'verification/data-integrity-{VERSION}.json').write_text(json.dumps(integrity,indent=2)+'\n')
tasks['S1']['checks'][0]['evidence']+=f' ; verification/data-integrity-{VERSION}.json ; CRC du ZIP contrôlé au conditionnement'
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
assert total==dict(development=81,validation=31,previous_development=78,previous_validation=29),total
(ROOT/'app/src/main/assets/roadguard_progress_11_14_245.json').write_text(json.dumps(ledger,indent=2,ensure_ascii=False)+'\n')
contract={'version':VERSION,'version_code':445,'application_id':'fr.roadguard.app','baseline':PREVIOUS,'task_count':39,'weight':100,'totals':total,'new_coded_tasks':['D5'],'new_validation_tasks':{'D5':2},'android_native_runtime_executed':False,'apk_produced':False,'sevenzip_extraction_executed':True,'automatic_camera_reaudit':False,'browser_render_executed':False,'original_data':integrity,'scope':'25 tests extraction/import réels avec les 2 archives FR/CH et 5 JAR officiels. 3824/593 entrées ; 2197/86 identifiants consommés après déduplication actuelle, historique FR/CH activé. Régressions 244 réexécutées. Android SQLite adapté pour JVM, aucun Android natif.'}
(ROOT/f'verification/release-contract-{VERSION}.json').write_text(json.dumps(contract,indent=2,ensure_ascii=False)+'\n')
print(json.dumps(contract,indent=2,ensure_ascii=False))
