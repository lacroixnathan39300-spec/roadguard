#!/usr/bin/env python3
"""Vérifie les invariants documentaires du dernier ZIP ROADGUARD."""
import json, pathlib, re, sys
root=pathlib.Path(__file__).resolve().parents[1]
p=json.loads((root/'app/src/main/assets/roadguard_progress.json').read_text())
g=(root/'app/build.gradle').read_text()
required=['00-LIRE-EN-PREMIER-'+p['version']+'.md','SUIVI-AVANCEMENT.md','TRAVAUX-REALISES.md','TACHES-RESTANTES.md','TESTS-ET-BLOCAGES.md']
errors=[]
for name in required:
    if not (root/name).is_file(): errors.append('Absent: '+name)
if not re.search(r"versionName\s+'"+re.escape(p['version'])+r"'",g): errors.append('Version Gradle différente')
if not re.search(r'\b'+re.escape(p['version'])+r'\b',(root/('00-LIRE-EN-PREMIER-'+p['version']+'.md')).read_text()): errors.append('Passation version différente')
if len(p['workstreams'])<8: errors.append('Chantiers manquants')
if any(not isinstance(t['percent_estimate'],int) or not 0<=t['percent_estimate']<=100 for t in p['workstreams']): errors.append('Pourcentage invalide')
if p['global_percent_audited'] is not False: errors.append('Audit global déclaré sans preuve')
if p['artifact_type'] not in ('source_zip_not_apk','source_zip_with_separate_debug_apk'): errors.append('Type de livraison incorrect')
if p['artifact_type']=='source_zip_with_separate_debug_apk' and not p.get('validation_'+p['version'].replace('.','_'),{}).get('apk_assembled',False): errors.append('APK déclarée sans construction')
if errors:
    print('ECHEC:', '; '.join(errors)); sys.exit(1)
print('OK: suivi, version, statuts et documents cohérents pour '+p['version'])
