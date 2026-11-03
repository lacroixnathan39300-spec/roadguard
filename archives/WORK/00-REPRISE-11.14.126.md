# Reprise ROADGUARD 11.14.126

Version 11.14.126 / code 326. Source ZIP, sans APK.

## Réalisé
- « Contrôle mobile (2 h) » classé radar par les moteurs au premier plan et en arrière-plan.
- Portée radar progressive jusqu’à 5 km, jalons partagés et double bip.
- Test statique `tools/test_mobile_radar_classification.py`.

## Estimation
Radars 69→71 % (+2); global 67→68 % (+1), non audités.

## À faire
Compiler avec CodeAssist et vérifier sur Xiaomi à plusieurs distances, en premier plan et écran verrouillé. La compilation locale reste bloquée par l’absence SDK/Gradle. Caméra en dernière étape; aucun préchargement.
