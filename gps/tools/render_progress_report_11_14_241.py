#!/usr/bin/env python3
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
ledger=json.loads((ROOT/'app/src/main/assets/roadguard_progress_11_14_241.json').read_text())
def n(value):return f'{value:.4f}'.replace('.',',')
rows=[];totals=[0.,0.,0.,0.]
for module in ledger['modules']:
 dev=sum(t['weight'] for t in module['tasks'] if t['coded']);old=sum(t['weight'] for t in module['tasks'] if t['baseline_coded'])
 val=sum(c['weight'] for t in module['tasks'] for c in t['checks'] if c['passed']);oldval=sum(c['weight'] for t in module['tasks'] for c in t['checks'] if c.get('baseline_passed',False))
 w=module['weight'];rows.append(f"| {module['name']} | {n(w)} | {n(dev/w*100)} % | {n(val/w*100)} % | {n((dev-old)/w*100)} | {n((val-oldval)/w*100)} |")
 totals=[a+b for a,b in zip(totals,[dev,val,old,oldval])]
dev,val,old,oldval=totals
out=['# ROADGUARD 11.14.241 — avancement pondéré','', 'Référence : source 11.14.240 inspectée le 20/10/2026. Les **48,0000 % historiques sont une estimation non auditée** ; ils ne servent pas de base au calcul ci-dessous.', '', '39 tâches explicites totalisent 100 points. Le développement compte les tâches codées selon leur contrat ; une tâche partielle reste à faire. La validation additionne seulement les contrôles réussis, pondérés à l’avance. Les essais matériels ont leur propre poids et restent non validés.', '', '| Module | Poids | Développement | Validation | Δ développement (pts du module) | Δ validation (pts du module) |','|---|---:|---:|---:|---:|---:|']+rows
out += [f'| **TOTAL pondéré** | **100,0000** | **{n(dev)} %** | **{n(val)} %** | **{n(dev-old)}** | **{n(val-oldval)}** |','',f'Référence reconstituée sur ce backlog : développement **{n(old)} %**, validation **{n(oldval)} %**. Une validation automatique ne constitue pas un essai Xiaomi ou Android Auto.', '', '**Chantier actif :** '+ledger['active_work'],'', '## Backlog et preuves','', '| ID | Tâche | Poids | État | Contrôles réussis / poids | Preuve code et test |','|---|---|---:|---|---:|---|']
for m in ledger['modules']:
 for t in m['tasks']:
  checked=sum(c['weight'] for c in t['checks'] if c['passed']);state='à faire' if not t['coded'] else 'testé' if checked==t['weight'] else 'testé partiellement' if checked>0 else 'codé'
  evidence=t['code_evidence']+' ; '+ '; '.join(c['evidence'] for c in t['checks'] if c['passed'])
  out.append(f"| {t['id']} | {t['name']} | {n(t['weight'])} | {state} | {n(checked)} / {n(t['weight'])} | {evidence.replace('|','/')} |")
out += ['', 'Date du relevé : **20/10/2026**. Les résultats exécutés figurent dans `verification/`. Les anciennes notes et l’ancien tableau JSON sont conservés ; ce relevé versionné est la référence de cette livraison.', '', '**🔴 INTERVENTION OBLIGATOIRE — PLUS TARD :** un essai du lot sur Xiaomi et un essai Android Auto sur matériel compatible restent nécessaires. Aucun de ces essais n’est déclaré réalisé.']
(ROOT/'AVANCEMENT-11.14.241.md').write_text('\n'.join(out)+'\n')
print('Development',n(dev),'Validation',n(val),'delta',n(dev-old),n(val-oldval))
