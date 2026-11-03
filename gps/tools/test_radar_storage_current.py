from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
JAVA=(ROOT/'app/src/main/java/fr/roadguard/app/MainActivity.java').read_text()
BG=(ROOT/'app/src/main/java/fr/roadguard/app/BackgroundAlertService.java').read_text()
# Imported and persisted radar data must reject NaN/infinity, out-of-range coordinates and unusable labels.
assert '!Double.isFinite(lat)||!Double.isFinite(lon)||Math.abs(lat)>90||Math.abs(lon)>180' in JAVA
assert 'type.isEmpty()||type.length()>80' in JAVA
assert 'DangerTypePolicy.isValidLabel(type)' in BG
# Personal expired points are excluded at load/render time, even without another GPS callback.
assert 'long expiry=o.optLong("expires",0L);if(expiry>0&&expiry<=now)continue;' in JAVA
assert 'removeExpiredRadars();\n    premiumVisualBanner(content,"◎","VIGILANCE SUR LA ROUTE"' in JAVA
assert 'if(expiry>0&&expiry<=now)continue;' in JAVA
assert 'expires>0&&expires<=now' in BG
# Expiry maintenance still persists cleanup and refreshes both foreground map and active dangers.
assert 'putString("custom_radars",keep.toString()).apply();loadDangers();loadCustomRadars();pushDangers();' in JAVA
# Local storage model and 2-hour mobile expiry remain intact.
assert 'choice==2?now+7200000L:0' in JAVA
assert 'custom_radars' in JAVA and 'custom_radars' in BG
print('Contrôles radars locaux: import, limites, expiration et rafraîchissement réussis')
