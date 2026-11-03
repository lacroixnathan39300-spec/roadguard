# Reprise ROADGUARD 11.14.121

Version Android 11.14.121 / code 321. Source uniquement, APK non compilée.

## Réalisé
- Base officielle du ministère de l’Intérieur via data.gouv.fr intégrée : `app/src/main/assets/radars_france_20251230.csv`.
- 3 309 entrées uniques, 7 types (ETF, ETD, ETT, ETU, ETVM, ETFR, ETPN), coordonnées vérifiées.
- Source/date et Licence Ouverte 2.0 conservées. Carte et alertes foreground/background utilisent la base; points personnels séparés.
- Aucun changement au GPS bleu, graphisme ou caméra. Préchargement absent.

## Estimations
Radars 40→55 % (+15); global 59→63 % (+4), non audités. Écrans visuels restent 100 % côté source et à vérifier en APK.

## Suite
Lancer `python3 tools/test_radar_dataset.py`, tests de passation et compiler avec CodeAssist. Tester charge, proximité, direction et annonces sur Xiaomi. Vérifier la prochaine publication officielle avant mise à jour. Caméra en dernier.
