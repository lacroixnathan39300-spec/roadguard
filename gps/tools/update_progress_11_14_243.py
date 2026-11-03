#!/usr/bin/env python3
"""Advance the fixed 100-point ledger, distinguishing real SQLite tests from Android runtime."""
import copy,json
from pathlib import Path
from xml.etree import ElementTree
from verify_preserved_data import verify
ROOT=Path(__file__).resolve().parents[1];VERSION='11.14.243';PREVIOUS='11.14.242'
previous=json.loads((ROOT/'app/src/main/assets/roadguard_progress_11_14_242.json').read_text());ledger=copy.deepcopy(previous)
ledger.update(version=VERSION,baseline_version=PREVIOUS,date='2026-10-21',method='Poids et 39 tâches inchangés. D6 est codé (SQLite vers canonique) ; validation automatique D6 +2 points via vrais fichiers SQLite et adaptateurs JVM, S2 +0,5 point via fichiers/renommages/sauvegardes réels. Aucun crédit pour extraction 7z ou essai Android natif.',active_work='Récupérer les deux archives FR/CH et les cinq JAR demandés ; intégrer et éprouver extraction 7z puis import automatique et activation sur Android.')
tasks={t['id']:t for g in ledger['modules'] for t in g['tasks']}
for t in tasks.values():
    t['baseline_coded']=t['coded']
    for c in t['checks']:
        c['baseline_passed']=c['passed']
        if c['passed']:c['carried_from_version']=PREVIOUS
proof=json.loads((ROOT/f'verification/targeted-results-{VERSION}.json').read_text());executed={r['name']:r for r in proof}
for name in ['compile-core-java8','sqlite-fixtures','canonical-sqlite-store','download-errors-queue','catalog-additive-rollback','alert-distance-types','gps-motion','java-syntax']:assert executed[name]['exit_code']==0,name
assert '35 scenarios' in executed['canonical-sqlite-store']['output']
evidence=f'verification/targeted-checks-{VERSION}.log (canonical-sqlite-store : SQLite réel via adaptateurs JVM ; Android natif non exécuté)'
tasks['D6'].update(coded=True,date='2026-10-21',version=VERSION,code_evidence='SpeedAlertSqliteImporter : lecture seule, schéma/tailles/types ; CanonicalRadar : provenance, champs inconnus et vitesse/sens inconnus préservés ; import local raccordé à RadarDownloadActivity')
tasks['D6']['checks'][0].update(passed=True,evidence=evidence)
tasks['S2'].update(date='2026-10-21',version=VERSION,code_evidence='CanonicalRadarStore : fichier indépendant, validation avant remplacement, renommage atomique, deux sauvegardes, annulation et repli contrôlés. Chemins CSV/Lufop historiques conservés, non réaudités dans ce lot.')
tasks['S2']['checks'][0].update(passed=True,evidence=evidence)
tasks['D5']['code_evidence']='Extraction Android 7z non intégrée : cinq JAR et vraies archives FR/CH manquent pour le test. Aucun point acquis pour une vérification de signature seule.'
tasks['D7']['code_evidence']='Import canonique additif et accusé de chargement téléphone ajoutés ; sens, conflits, fiabilité/fusion complète et confirmations service/voiture encore incomplets.'
tasks['D3']['code_evidence']='RadarDownloadActivity : inventaire, import local et activité réelle sur reçu RadarActivationState ; doublons/points filtrés distingués du chargement en attente.'
tasks['Q1']['code_evidence']='app/build.gradle : 11.14.243 / 443 ; package/SDK conservés ; ledger versionné 243 raccordé.'
tasks['Q1']['checks'][0]['evidence']=f'verification/release-contract-{VERSION}.json'
tasks['Q1']['checks'][0].pop('carried_from_version',None)
tasks['Q3']['code_evidence']='Aucun APK 243 : SDK absent et téléchargement Gradle bloqué, environnement inchangé. environment-11.14.243.json ; erreur précédente gradle-phone-11.14.242.log conservée.'
tasks['Q5']['code_evidence']='Aucun essai CodeAssist/Xiaomi de la 11.14.243 dans cette session.'
for id in ['M2','A2','A3','D1','D8']:
    for c in tasks[id]['checks']:
        if c['passed']:c['evidence']+=f' ; réexécuté : verification/targeted-checks-{VERSION}.log';c.pop('carried_from_version',None)
gradle=(ROOT/'app/build.gradle').read_text()
for text in ['versionCode 443',"versionName '11.14.243'","applicationId 'fr.roadguard.app'",'compileSdk 36','targetSdk 36','minSdk 26']:assert text in gradle,text
assert 'roadguard_progress_11_14_243.json' in (ROOT/'app/src/main/java/fr/roadguard/app/MainActivity.java').read_text()
for variant in ['main','auto']:ElementTree.parse(ROOT/f'app/src/{variant}/AndroidManifest.xml')
integrity=verify();assert not integrity['failures']
(ROOT/f'verification/data-integrity-{VERSION}.json').write_text(json.dumps(integrity,indent=2)+'\n')
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
assert total==dict(development=78,validation=27.5,previous_development=75,previous_validation=25),total
(ROOT/'app/src/main/assets/roadguard_progress_11_14_243.json').write_text(json.dumps(ledger,indent=2,ensure_ascii=False)+'\n')
contract={'version':VERSION,'version_code':443,'application_id':'fr.roadguard.app','baseline':PREVIOUS,'task_count':39,'weight':100,'totals':total,'new_coded_tasks':['D6'],'new_validation_tasks':{'D6':2,'S2':0.5},'android_native_runtime_executed':False,'apk_produced':False,'sevenzip_extraction_executed':False,'automatic_camera_reaudit':False,'original_data':integrity,'new_test_scope':'35 scénarios canonique/SQLite avec vraies bases synthétiques et adaptateurs de frontière Android JVM ; fichiers et sauvegardes réels.'}
(ROOT/f'verification/release-contract-{VERSION}.json').write_text(json.dumps(contract,indent=2,ensure_ascii=False)+'\n')
print(json.dumps(contract,indent=2,ensure_ascii=False))
