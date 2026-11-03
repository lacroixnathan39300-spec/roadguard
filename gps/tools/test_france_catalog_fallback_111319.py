from pathlib import Path
s=(Path(__file__).resolve().parents[1]/'app/src/main/java/fr/roadguard/app/MainActivity.java').read_text()
method=s.split('void downloadFranceOneTap(){',1)[1].split('void downloadPublicFranceCandidate(){',1)[0]
assert 'chosen=item;break;' in method
assert method.index('chosen=item;break;') > method.index('hash.matches(')
assert 'rejected++' in method
assert 'if(chosen!=null){confirmMapDownload(chosen);return;}' in method
assert 'activateExistingFrancePmtiles();' in method
assert 'downloadPublicFranceCandidate()' not in method
print('OK: catalogue France: sélection après validation, repli, GPS préservé')
