#!/usr/bin/env python3
import json,re
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
VERSION=re.search(r"versionName\s+'([^']+)'",(ROOT/'app/build.gradle').read_text()).group(1)
ledger=json.loads((ROOT/('app/src/main/assets/roadguard_progress_'+VERSION.replace('.','_')+'.json')).read_text())
def n(value):return f'{value:.4f}'.replace('.',',')
rows=[];totals=[0.,0.,0.,0.]
for module in ledger['modules']:
 dev=sum(t['weight'] for t in module['tasks'] if t['coded']);old=sum(t['weight'] for t in module['tasks'] if t['baseline_coded'])
 val=sum(c['weight'] for t in module['tasks'] for c in t['checks'] if c['passed']);oldval=sum(c['weight'] for t in module['tasks'] for c in t['checks'] if c.get('baseline_passed',False))
 w=module['weight'];rows.append(f"| {module['name']} | {n(w)} | {n(dev/w*100)} % | {n(val/w*100)} % | {n((dev-old)/w*100)} | {n((val-oldval)/w*100)} |")
 totals=[a+b for a,b in zip(totals,[dev,val,old,oldval])]
dev,val,old,oldval=totals
out=[f'# ROADGUARD {VERSION} — avancement pondéré','',f"Référence des deltas : {ledger['baseline_version']}, développement {n(old)} %, validation {n(oldval)} %. Les 48,0000 % historiques restent une estimation non auditée séparée.",'',ledger['method'],'','| Module | Poids | Développement | Validation | Δ développement (pts module) | Δ validation (pts module) |','|---|---:|---:|---:|---:|---:|']+rows
out += [f'| **TOTAL pondéré** | **100,0000** | **{n(dev)} %** | **{n(val)} %** | **{n(dev-old)}** | **{n(val-oldval)}** |','','**Chantier actif :** '+ledger['active_work'],'','## Tâches et preuves','','| ID | Tâche | Poids | État | Contrôles réussis / poids | Preuve |','|---|---|---:|---|---:|---|']
for m in ledger['modules']:
 for t in m['tasks']:
  checked=sum(c['weight'] for c in t['checks'] if c['passed']);state='à faire' if not t['coded'] else 'testé' if checked==t['weight'] else 'testé partiellement' if checked>0 else 'codé'
  evidence=t['code_evidence']+' ; '+ '; '.join(c['evidence'] for c in t['checks'] if c['passed'])
  out.append(f"| {t['id']} | {t['name']} | {n(t['weight'])} | {state} | {n(checked)} / {n(t['weight'])} | {evidence.replace('|','/')} |")
out += ['', 'Le catalogue radar complet reste disponible pour les alertes GPS ; la carte ne reçoit que 600 marqueurs au maximum dans le secteur visible. Le résultat réel sur Xiaomi reste à valider.', '', 'Le pack local Graphite/Lisibilité/Couleurs OSM modifie le rendu des tuiles : il ne télécharge pas de couverture géographique. Le cache de tuiles reste temporaire ; une zone encore jamais affichée exige du réseau ou un pack PMTiles compatible.', '', '🔴 INTERVENTION OBLIGATOIRE — PLUS TARD : importer le dossier ROADGUARD-11.14.250 du ZIP dans CodeAssist et compiler, puis ouvrir la carte sur Xiaomi, sélectionner « ▧ Pack de textures » et vérifier FR/CH. Si Android signale encore « ne répond pas », transmettre la capture complète de « Voir l’historique » pour connaître la pile ANR. Android Auto reste à essayer sur véhicule.']
(ROOT/f'AVANCEMENT-{VERSION}.md').write_text('\n'.join(out)+'\n')
print('Development',n(dev),'Validation',n(val),'delta',n(dev-old),n(val-oldval))
