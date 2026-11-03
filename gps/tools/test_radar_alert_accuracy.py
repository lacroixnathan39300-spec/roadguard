from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
MAIN=(ROOT/'app/src/main/java/fr/roadguard/app/MainActivity.java').read_text()
BG=(ROOT/'app/src/main/java/fr/roadguard/app/BackgroundAlertService.java').read_text()
# Foreground alerts must not fire when location accuracy is absent or too poor.
assert 'Double.isNaN(lastLat)||lastAccuracy<=0||lastAccuracy>60' in MAIN
# Background mode uses the same 60 m ceiling and explicitly requires a reported accuracy.
assert '!fix.hasAccuracy()||fix.getAccuracy()>60' in BG
# Both modes share persistent milestone claims to avoid duplicate announcements.
assert 'AlertMilestones.claim(this,pendingDanger.lat,pendingDanger.lon,stage)' in MAIN
assert 'AlertMilestones.claim(this,points.get(index)[0],points.get(index)[1],stage)' in BG
# Stale GPS fixes and disabled alerts must be ignored by the background service.
assert 'System.currentTimeMillis()-fix.getTime()>15000' in BG
assert 'getBoolean("Alertes de danger",true)' in BG and 'getBoolean("Alertes de danger",true)' in MAIN
print('Alertes radar : précision GPS, âge du fix et anti-doublon premier plan/arrière-plan vérifiés')
