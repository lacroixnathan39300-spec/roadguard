#!/usr/bin/env python3
"""Keep the 241 backlog/weights intact and credit only evidence recorded for this lot."""
import copy,json,re
from pathlib import Path
from xml.etree import ElementTree
from verify_preserved_data import verify
ROOT=Path(__file__).resolve().parents[1]
VERSION='11.14.242';PREVIOUS='11.14.241'
old=json.loads((ROOT/'app/src/main/assets/roadguard_progress_11_14_241.json').read_text())
ledger=copy.deepcopy(old)
ledger.update(version=VERSION,baseline_version=PREVIOUS,date='2026-10-21',
    method='Backlog et poids de 11.14.241 inchangés. Deltas contre ce checkpoint. Validations héritées conservées avec preuves datées ; seuls D1, D8 et Q1 reçoivent un nouveau crédit pour des contrôles effectivement exécutés. Les adaptateurs JVM ne sont pas des essais Android.',
    active_work='Import Android 7z → SQLite → activation réelle ; compilation du lot et tests du rechargement sur Xiaomi encore nécessaires.')
for group in ledger['modules']:
    for task in group['tasks']:
        task['baseline_coded']=task['coded']
        for check in task['checks']:
            check['baseline_passed']=check['passed']
            if check['passed']:check['carried_from_version']=PREVIOUS
tests=json.loads((ROOT/f'verification/targeted-results-{VERSION}.json').read_text())
executed={r['name']:r for r in tests}
def require(name):assert name in executed and executed[name]['exit_code']==0,name
for name in ['compile-core-java8','download-errors-queue','catalog-additive-rollback','alert-distance-types','gps-motion','java-syntax','test_camera_anchor.js','test_vector_camera.js','test_camera_motion.js']:require(name)
gradle=(ROOT/'app/build.gradle').read_text()
for text in ["applicationId 'fr.roadguard.app'",'compileSdk 36','minSdk 26','targetSdk 36','versionCode 442',"versionName '11.14.242'"]:assert text in gradle,text
assert "project.findProperty('roadguardAndroidAuto') == 'true'" in gradle
assert 'roadguard_progress_11_14_242.json' in (ROOT/'app/src/main/java/fr/roadguard/app/MainActivity.java').read_text()
for name in ['main','auto']:ElementTree.parse(ROOT/f'app/src/{name}/AndroidManifest.xml')
integrity=verify();assert not integrity['failures']
(ROOT/f'verification/data-integrity-{VERSION}.json').write_text(json.dumps(integrity,indent=2)+'\n')
by_id={t['id']:t for g in ledger['modules'] for t in g['tasks']}
assert len(by_id)==39 and sum(g['weight'] for g in ledger['modules'])==100
new_evidence={
 'D1':('catalog-additive-rollback','UpdatedRadarCatalog.active; LufopEuropeCatalog.all : originaux + surcouche, tests sur les vraies ressources'),
 'D8':('download-errors-queue','RadarArchiveDownloader; RadarCountryBatch : annulation, bornes, HTTP simulé, fichiers réels et remplacement atomique'),
 'Q1':('version-contract','app/build.gradle : 11.14.242 / 442 ; applicationId fr.roadguard.app et SDK inchangés'),
}
for id,(test,code) in new_evidence.items():
    task=by_id[id];task.update(code_evidence=code,date='2026-10-21',version=VERSION)
    check=task['checks'][0];check.update(passed=True,evidence=f'verification/release-contract-{VERSION}.json' if id=='Q1' else f'verification/targeted-checks-{VERSION}.log ({test}; adaptateurs JVM)')
    check.pop('carried_from_version',None)
by_id['D5']['code_evidence']='RadarArchivePolicy contrôle les en-têtes ; aucune extraction 7z Android, aucune activation : tâche toujours à faire.'
by_id['D7']['code_evidence']='Rechargement premier plan ajouté (requestRadarCatalogReload, RadarCatalogRevision). Sens, fiabilité, fusion complète et essai Android encore incomplets : aucun point de développement ajouté.'
by_id['Q3']['code_evidence']='Tentative Gradle téléphone bloquée avant compilation : Network is unreachable ; verification/gradle-phone-11.14.242.log'
by_id['Q5']['code_evidence']='Aucun essai CodeAssist/Xiaomi de la 11.14.242 dans cette session.'
for id in ['M2','M3','M4','M5','A2','A3']:
    for check in by_id[id]['checks']:
        if check['passed']:
            check['evidence']+=f' ; réexécuté : verification/targeted-checks-{VERSION}.log'
            check.pop('carried_from_version',None)
by_id['S1']['checks'][0]['evidence']+=f' ; verification/data-integrity-{VERSION}.json (CRC du nouveau ZIP contrôlé au packaging)'
out=ROOT/f'app/src/main/assets/roadguard_progress_11_14_242.json'
out.write_text(json.dumps(ledger,indent=2,ensure_ascii=False)+'\n')
totals={'development':0,'validation':0,'previous_development':0,'previous_validation':0}
for group in ledger['modules']:
    assert sum(t['weight'] for t in group['tasks'])==group['weight']
    for task in group['tasks']:
        assert sum(c['weight'] for c in task['checks'])==task['weight']
        totals['development']+=task['weight'] if task['coded'] else 0
        totals['previous_development']+=task['weight'] if task['baseline_coded'] else 0
        for check in task['checks']:
            if check['passed']:assert task['coded'] and check['evidence']
            totals['validation']+=check['weight'] if check['passed'] else 0
            totals['previous_validation']+=check['weight'] if check['baseline_passed'] else 0
assert totals=={'development':75,'validation':25,'previous_development':75,'previous_validation':18},totals
contract={'version':VERSION,'version_code':442,'application_id':'fr.roadguard.app','source_checkpoint':PREVIOUS,'module_count':7,'task_count':39,'total_weight':100,'totals':totals,'new_validation_points':7,'new_credit_tasks':list(new_evidence),'android_compiled':False,'robolectric_executed':False,'android_device_tested':False,'sevenzip_extracted_on_android':False,'original_files':integrity,'limits':'JVM adapters test catalog/core logic only; inherited 241 evidence remains dated and is not claimed as rerun unless stated.'}
(ROOT/f'verification/release-contract-{VERSION}.json').write_text(json.dumps(contract,indent=2,ensure_ascii=False)+'\n')
print(json.dumps(contract,ensure_ascii=False,indent=2))
