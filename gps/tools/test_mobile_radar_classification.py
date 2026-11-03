from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
MAIN=(ROOT/'app/src/main/java/fr/roadguard/app/MainActivity.java').read_text()
BG=(ROOT/'app/src/main/java/fr/roadguard/app/BackgroundAlertService.java').read_text()
# The app's own temporary mobile point label must classify as a radar in both owners.
label='Contrôle mobile (2 h)'
for source in (MAIN,BG):
    assert 'DangerTypePolicy.isRadar(' in source
assert 'value.contains("contrôle mobile")' in (ROOT/'app/src/main/java/fr/roadguard/app/DangerTypePolicy.java').read_text()
assert 'value.contains("controle mobile")' in (ROOT/'app/src/main/java/fr/roadguard/app/DangerTypePolicy.java').read_text()
# Radar-only 1 km detection and shared approach stages apply to temporary mobile controls.
assert 'meters>(isRadarDanger(d.type)?1000:700)' in MAIN
assert 'AlertMilestones.stage(isRadarDanger(d.type),meters)' in MAIN
assert 'AlertMilestones.stage(radar,nearest)' in BG
assert 'bearings[0]>(radarCandidate?1000:700)' in BG
assert label in MAIN
print('Radar mobile temporaire : classification radar, portée 1 km et paliers vocaux cohérents')
