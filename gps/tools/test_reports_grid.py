from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
JAVA=(ROOT/'app/src/main/java/fr/roadguard/app/MainActivity.java').read_text()
labels=['Radar fixe','Radar mobile','Contrôle police','Accident','Danger','Travaux','Véhicule arrêté','Bouchon','Route fermée','Objet sur la route','Météo / pluie','Autre']
for label in labels: assert label in JAVA,label
assert 'for(int hi=0;hi<hazards.length;hi+=3)' in JAVA
assert 'Math.min(hi+3,hazards.length)' in JAVA
assert 'confirmLocalReport(hazard)' in JAVA
print('Signalement : 12 catégories, trois colonnes et enregistrement local vérifiés')
