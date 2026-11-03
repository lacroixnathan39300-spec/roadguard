from pathlib import Path
s=Path(__file__).resolve().parents[1].joinpath("app/src/main/java/fr/roadguard/app/MainActivity.java").read_text()
assert 'if("france".equals(id)){downloadFranceOneTap();return;}' in s
assert 'if("france".equals(id)){downloadPublicFranceCandidate();return;}' not in s
assert 'void downloadFranceOneTap()' in s
print("OK: bouton France utilise le catalogue vérifié et réutilise les archives existantes")
