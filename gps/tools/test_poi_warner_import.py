from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
JAVA=(ROOT/'app/src/main/java/fr/roadguard/app/MainActivity.java').read_text()
assert 'Importer un ZIP POI-Warner · source à vérifier' in JAVA
assert 'void choosePoiWarnerSwissZip()' in JAVA and 'void importPoiWarnerSwissZip(Uri uri)' in JAVA
assert 'String poiWarnerSwissType(String entryName,String rowLabel)' in JAVA
assert 'Radar chantier · ' in JAVA
for name in ('FRFEUROUGEFR.CSV','FRCHANTIERFR.CSV','FRTRONCONDEBUTFR.CSV','FRTRONCONFINFR.CSV','FRFIXEFRPASSAGENIVEAU.CSV','FRFIXEFRCOVOITURAGE.CSV'):
 assert name in JAVA
assert 'lat<45.6||lat>47.9||lon<5.8||lon>10.7' in JAVA
assert 'poi_warner_ch' in JAVA and 'Non précisée dans l’archive · stockage local uniquement' in JAVA
assert 'request==64&&result==RESULT_OK' in JAVA
print('Import POI-Warner séparé, catégories, filtre géographique et métadonnées de prudence vérifiés')
