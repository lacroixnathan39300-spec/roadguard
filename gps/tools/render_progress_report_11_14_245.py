#!/usr/bin/env python3
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
ledger=json.loads((ROOT/'app/src/main/assets/roadguard_progress_11_14_245.json').read_text())
def n(value):return f'{value:.4f}'.replace('.',',')
rows=[];totals=[0.,0.,0.,0.]
for module in ledger['modules']:
 dev=sum(t['weight'] for t in module['tasks'] if t['coded']);old=sum(t['weight'] for t in module['tasks'] if t['baseline_coded'])
 val=sum(c['weight'] for t in module['tasks'] for c in t['checks'] if c['passed']);oldval=sum(c['weight'] for t in module['tasks'] for c in t['checks'] if c.get('baseline_passed',False))
 w=module['weight'];rows.append(f"| {module['name']} | {n(w)} | {n(dev/w*100)} % | {n(val/w*100)} % | {n((dev-old)/w*100)} | {n((val-oldval)/w*100)} |")
 totals=[a+b for a,b in zip(totals,[dev,val,old,oldval])]
dev,val,old,oldval=totals
out=['# ROADGUARD 11.14.245 — avancement pondéré','',f'Référence des deltas : 11.14.244, développement {n(old)} %, validation {n(oldval)} %. Les 48,0000 % historiques restent une estimation non auditée séparée.','', '39 tâches, 100 points et poids inchangés. D5 est codé (+3,0000 points) ; son contrôle automatique sur vraies archives apporte +2,0000 points de validation. Aucun crédit Android natif ou matériel.','', '| Module | Poids | Développement | Validation | Δ développement (pts module) | Δ validation (pts module) |','|---|---:|---:|---:|---:|---:|']+rows
out += [f'| **TOTAL pondéré** | **100,0000** | **{n(dev)} %** | **{n(val)} %** | **{n(dev-old)}** | **{n(val-oldval)}** |','', '**Chantier actif :** '+ledger['active_work'],'', '## Tâches et preuves','', '| ID | Tâche | Poids | État | Contrôles réussis / poids | Preuve |','|---|---|---:|---|---:|---|']
for m in ledger['modules']:
 for t in m['tasks']:
  checked=sum(c['weight'] for c in t['checks'] if c['passed']);state='à faire' if not t['coded'] else 'testé' if checked==t['weight'] else 'testé partiellement' if checked>0 else 'codé'
  evidence=t['code_evidence']+' ; '+ '; '.join(c['evidence'] for c in t['checks'] if c['passed'])
  out.append(f"| {t['id']} | {t['name']} | {n(t['weight'])} | {state} | {n(checked)} / {n(t['weight'])} | {evidence.replace('|','/')} |")
out += ['', 'Les 25 scénarios 7z utilisent les vraies bibliothèques et les deux archives officielles. SQLite est réel, derrière des adaptateurs JVM des frontières Android ; Android natif et Robolectric ne sont pas exécutés. La tâche D5 conserve son contrat et ses poids ; seuls les 2 points de contrôle automatique Java sont acquis. Le test Android API 26/36 est fourni mais non exécuté. Résultats : verification/targeted-checks-11.14.245.log.', '', 'Les sept fichiers ne manquent plus : ils sont inclus. Aucun APK 245 produit. Le contrôle d’approbation automatique a rejeté le téléchargement des outils Google/Gradle en considérant que l’autorisation réseau était limitée à GitHub. Une autorisation explicite sera nécessaire pour retenter ce téléchargement ; aucun contournement n’a été effectué. Essais CodeAssist/Xiaomi à regrouper en fin de lot.']
(ROOT/'AVANCEMENT-11.14.245.md').write_text('\n'.join(out)+'\n')
print('Development',n(dev),'Validation',n(val),'delta',n(dev-old),n(val-oldval))
