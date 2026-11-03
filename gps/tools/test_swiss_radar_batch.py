from pathlib import Path
root=Path(__file__).resolve().parents[1]
java=(root/'app/src/main/java/fr/roadguard/app/MainActivity.java').read_text()
assert 'Télécharger les 2 bases publiques en série' in java
assert 'void confirmSwissRadarBatch(String name)' in java
assert 'downloadMapAtlasCountry("CH",name)' in java
assert 'downloadOsmCountry("CH",name)' in java
assert 'downloadRadardroidLufopCountry("CH",name)' not in java
assert 'Importer un ZIP CSV/ASC Lufop' in java
assert 'Importer un ZIP CSV/ASC Lufop · Suisse' in java
assert 'chooseLufopSwissCsvZip()' in java
assert 'String osmPackId(String country)' in java
assert 'String id=osmPackId(country);' in java
assert 'String id=lufopPackId(country);' in java
assert 'swissRadarBatchCancelled=true' in java
assert 'finishSwissRadarBatchSource();' in java
assert 'startsWith("CH"))continue;' not in java
assert 'RadarSourcePolicy.quarantined(id)' in java
assert 'Les points suisses seront utilisés sur la carte et pour les alertes GPS' in java
assert 'Les données suisses sont disponibles sur la carte et dans les alertes GPS.' in java
print('Suisse : téléchargement groupé des sources publiques et import séparé du ZIP Lufop vérifiés')
