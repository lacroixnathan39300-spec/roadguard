#!/usr/bin/env python3
"""Carry the fixed 39-task contract forward without granting device credit."""
import json
from pathlib import Path
from verify_preserved_data import verify

root=Path(__file__).resolve().parents[1]
version='11.14.247';old='11.14.246'
ledger=json.loads((root/f'app/src/main/assets/roadguard_progress_{old.replace(".","_")}.json').read_text())
ledger.update(version=version,baseline_version=old,date='2026-10-21',
  method='39 tâches / 100 points, mêmes poids et contrats. Compatibilité de compilation CodeAssist pour le lecteur 7z corrigée et contrôlée sans JAR sur le classpath Java ; 25 scénarios 7z/SQLite, tests Android simulés, deux APK locaux. Aucun nouveau contrat crédité : essai CodeAssist sur Xiaomi et rendu réel encore nécessaires.',
  active_work='Correctif de compilation 7z dans CodeAssist ; navigateur de carte et pack Graphite/Lisibilité/Couleurs OSM intégrés. Essais Xiaomi et Android Auto encore à effectuer. Géométrie/mouvement caméra différés.')
tasks={t['id']:t for m in ledger['modules'] for t in m['tasks']}
assert len(tasks)==39 and sum(m['weight'] for m in ledger['modules'])==100
for t in tasks.values():
  t['baseline_coded']=t['coded']
  for c in t['checks']:c['baseline_passed']=c['passed']
tasks['D5']['code_evidence']+=' ; 11.14.247 : interface Java par réflexion pour éviter le classpath JAR absent dans CodeAssist, garde-fous 7z inchangés ; test compilateur sans JAR et vrais fichiers FR/CH.'
tasks['D5']['checks'][0]['evidence']+=f' ; verification/targeted-checks-{version}.log : 25 scénarios 7z/SQLite et compilation sans JAR.'
assert not tasks['Q5']['coded'] or not all(c['passed'] for c in tasks['Q5']['checks']), 'Do not claim a device CodeAssist build'
data=verify();assert not data['failures'] and data['unchanged']==59
path=root/f'app/src/main/assets/roadguard_progress_{version.replace(".","_")}.json'
path.write_text(json.dumps(ledger,indent=2,ensure_ascii=False)+'\n')
totals={'development':sum(t['weight'] for t in tasks.values() if t['coded']),
        'validation':sum(c['weight'] for t in tasks.values() for c in t['checks'] if c['passed'])}
assert totals=={'development':85,'validation':44.0},totals
report={'version':version,'version_code':447,'application_id':'fr.roadguard.app','baseline':old,
        'task_count':39,'weight':100,'totals':{**totals,'previous_development':85,'previous_validation':44},
        'new_coded_tasks':[],'new_validation_tasks':{},'physical_device_executed':False,
        'codeassist_build_executed':False,'real_webview_render_executed':False,
        'original_data':data,'scope':'Classpath-free production extractor compilation; real 7z/SQLite JVM fixtures; Android Robolectric and APK proof recorded separately.'}
(root/f'verification/release-contract-{version}.json').write_text(json.dumps(report,indent=2,ensure_ascii=False)+'\n')
print('PASS fixed ledger: development 85,0000 %, validation 44,0000 %, deltas 0,0000; originals 59/59')
