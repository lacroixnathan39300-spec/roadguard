#!/usr/bin/env python3
"""Fixed ledger, with only the cache automatic check newly validated in this lot."""
import copy,json
from pathlib import Path
from xml.etree import ElementTree
from verify_preserved_data import verify
ROOT=Path(__file__).resolve().parents[1];VERSION='11.14.244';PREVIOUS='11.14.243'
previous=json.loads((ROOT/f'app/src/main/assets/roadguard_progress_11_14_243.json').read_text());ledger=copy.deepcopy(previous)
ledger.update(version=VERSION,baseline_version=PREVIOUS,date='2026-10-21',method='39 tâches et 100 points inchangés. Corrections sur tâches déjà codées : développement inchangé. M6 +1,5 point de validation automatique par fichiers réels et JS exécuté avec adaptateurs. Aucun crédit appareil, build Android ou extraction 7z.',active_work='Démarrage carte sans attente du catalogue, reprise des tuiles et pack graphique local réalisés ; validation Android à regrouper en fin de lot. Import 7z automatique en attente des deux archives FR/CH et des cinq JAR demandés.')
tasks={t['id']:t for m in ledger['modules'] for t in m['tasks']}
for t in tasks.values():
 t['baseline_coded']=t['coded']
 for c in t['checks']:
  c['baseline_passed']=c['passed']
  if c['passed']:c['carried_from_version']=PREVIOUS
proof=json.loads((ROOT/f'verification/targeted-results-{VERSION}.json').read_text());executed={r['name']:r for r in proof}
required=['compile-core-java8','visible-tile-cache','radar-supplements','tile-recovery-and-pack','sqlite-fixtures','canonical-sqlite-store','download-errors-queue','catalog-additive-rollback','alert-distance-types','gps-motion','java-syntax','map-inline-syntax']
for name in required:assert executed[name]['exit_code']==0,name
assert '17 real-file' in executed['visible-tile-cache']['output']
assert '11 tile recovery' in executed['tile-recovery-and-pack']['output']
tasks['M6'].update(date='2026-10-21',version=VERSION,code_evidence='VisibleTileCache : cache HTTP, budget partagé 4 Gio, réserve 512 Mio, compteurs et reprise après retour du réseau ; tile_recovery.js et map.html : quatre reprises bornées des échecs visibles, conservation des bonnes tuiles. Callback réseau Android ajouté, non essayé sur appareil.')
tasks['M6']['checks'][0].update(passed=True,evidence='verification/targeted-checks-11.14.244.log : visible-tile-cache (17 scénarios, fichiers réels, HTTP injecté) et tile-recovery-and-pack (11 scénarios JS/DOM/Leaflet adaptés ; aucun WebView réel).')
tasks['M1'].update(date='2026-10-21',version=VERSION,code_evidence='MainActivity : carte avant catalogue ; catalogue/surcouches assemblés sur worker puis publication complète ; CommunitySync.prepareStartup sans attente de réseau ni de cache ; StartupRuntimeTest avec worker bloqué ajouté mais non exécuté. Aucun point Android natif ajouté.')
tasks['D3'].update(date='2026-10-21',version=VERSION,code_evidence='Gestionnaire : base locale et archive reçue affichées ensemble ; archive reçue sans prétendre à l’activation. Bouton non redondant une fois l’archive vérifiée ; confirmation du consommateur inchangée. Aucun essai natif ajouté.')
tasks['U1']['code_evidence']='Thème graphite/rouge conservé ; pack local facultatif Graphite/Lisibilité/Couleurs OSM dans Apparence. Filtre de pixels raster uniquement. Aucun essai visuel Android ajouté.'
tasks['Q1'].update(version=VERSION,date='2026-10-21',code_evidence='app/build.gradle : 11.14.244 / 444 ; package/SDK conservés ; ledger 244 et référence des deltas 243 raccordés.')
tasks['Q1']['checks'][0]['evidence']=f'verification/release-contract-{VERSION}.json';tasks['Q1']['checks'][0].pop('carried_from_version',None)
tasks['Q3']['code_evidence']='Aucun APK 244 : SDK absent, environnement consigné dans environment-11.14.244.json. Blocage Gradle précédent conservé, aucune compilation déclarée.'
tasks['Q5']['code_evidence']='Captures utilisateur reçues le 21/10/2026 : problèmes radars/cache/démarrage et réglages externes. Version non visible dans ces captures ; aucun crédit de compilation ou de validation complète 243/244.'
for id in ['M2','A2','A3','D1','D6','D8','S2']:
 for c in tasks[id]['checks']:
  if c['passed']:c['evidence']+=f' ; réexécuté : verification/targeted-checks-{VERSION}.log';c.pop('carried_from_version',None)
gradle=(ROOT/'app/build.gradle').read_text()
for text in ['versionCode 444',"versionName '11.14.244'","applicationId 'fr.roadguard.app'",'compileSdk 36','targetSdk 36','minSdk 26']:assert text in gradle,text
assert 'roadguard_progress_11_14_244.json' in (ROOT/'app/src/main/java/fr/roadguard/app/MainActivity.java').read_text()
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
assert total==dict(development=78,validation=29,previous_development=78,previous_validation=27.5),total
(ROOT/'app/src/main/assets/roadguard_progress_11_14_244.json').write_text(json.dumps(ledger,indent=2,ensure_ascii=False)+'\n')
contract={'version':VERSION,'version_code':444,'application_id':'fr.roadguard.app','baseline':PREVIOUS,'task_count':39,'weight':100,'totals':total,'new_coded_tasks':[],'new_validation_tasks':{'M6':1.5},'android_native_runtime_executed':False,'apk_produced':False,'sevenzip_extraction_executed':False,'automatic_camera_reaudit':False,'browser_render_executed':False,'original_data':integrity,'scope':'17 tests cache fichiers réels/HTTP injecté ; 10 tests fusion surcouches ; 11 tests JS exécuté, adaptateurs DOM/Leaflet. Régressions 243 réexécutées. Aucun Android natif ni vrai serveur de tuiles.'}
(ROOT/f'verification/release-contract-{VERSION}.json').write_text(json.dumps(contract,indent=2,ensure_ascii=False)+'\n')
print(json.dumps(contract,indent=2,ensure_ascii=False))
