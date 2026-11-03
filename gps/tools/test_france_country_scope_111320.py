from pathlib import Path
s=(Path(__file__).resolve().parents[1]/'app/src/main/java/fr/roadguard/app/MainActivity.java').read_text()
m=s.split('void downloadFranceOneTap(){',1)[1].split('void downloadPublicFranceCandidate(){',1)[0]
assert 'if(!fullCountry)continue;' in m
assert 'id.equals("france")' not in m
assert 'if(chosen!=null){confirmMapDownload(chosen);return;}' in m
assert 'if(rejected>0){' not in m
assert 'Aucun pack France + Corse vérifié' in m
assert 'activateExistingFrancePmtiles();' in m
print('OK: France requires country-wide metadata, rejects misleading IDs and shows unified error')
