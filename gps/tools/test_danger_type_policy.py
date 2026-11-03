from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
POLICY=(ROOT/'app/src/main/java/fr/roadguard/app/DangerTypePolicy.java').read_text()
MAIN=(ROOT/'app/src/main/java/fr/roadguard/app/MainActivity.java').read_text()
BG=(ROOT/'app/src/main/java/fr/roadguard/app/BackgroundAlertService.java').read_text()
assert 'static boolean isValidLabel(String type)' in POLICY
assert 'return !value.isEmpty() && value.length() <= 80;' in POLICY
assert 'static boolean isRadar(String type)' in POLICY
assert 'static boolean matchesFilter(String type, String filter)' in POLICY
assert 'boolean isRadarDanger(String type){return DangerTypePolicy.isRadar(type);}' in MAIN
assert 'boolean radarMatchesFilter(String type,String filter){return DangerTypePolicy.matchesFilter(type,filter);}' in MAIN
assert 'private boolean isRadar(String name){return DangerTypePolicy.isRadar(name);}' in BG
# Both GPS loaders now omit missing/invalid saved labels instead of inventing a radar type.
assert 'String type=o.optString("type","").trim();if(Double.isFinite(a)&&Double.isFinite(b)' in MAIN
assert 'String type=o.optString("type","").trim();' in BG
assert 'DangerTypePolicy.isValidLabel(type)' in MAIN and 'DangerTypePolicy.isValidLabel(type)' in BG
print('Types radar partagés; compteurs, filtres et deux moteurs GPS utilisent les mêmes règles')
