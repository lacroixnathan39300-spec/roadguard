# ROADGUARD 11.13.74 — Passation

## Réalisé
- Correction dans MainActivity.showMap() : le zoom PMTiles vectoriel est lu depuis le pack effectivement sélectionné par LocalPmtilesResolver, qui privilégie les archives GPS vérifiées dans offline-cells et conserve le fallback pmtiles.
- Ajout OfflineVectorZoom.java : lecture contrôlée du zoom maximum depuis un en-tête PMTiles v3.
- Ajout tests/OfflineVectorZoomTest.java.
- Test exécuté avec la vraie archive samples/demo-firenze/demo-firenze.pmtiles : PASS (max zoom 14), rejet d'une archive invalide : PASS.
- GPS bleu et caméra non modifiés.

## Blocage découvert
- app/src/main/assets/offline_pack_catalog.tsv est ABSENT. MainActivity essaie pourtant de l'ouvrir pour télécharger et activer les cartes GPS. Sans catalogue réel et approuvé, ces fonctions échouent. NE PAS inventer d'URL ni créer de faux catalogue.
- Aucune archive du Jura disponible ni hébergée ; aucun test APK Xiaomi ni compilation Android réalisés.

## Prochaines priorités
1. Produire un vrai pack Jura avec une source autorisant les extractions hors ligne, contrôler le PMTiles.
2. Héberger ce pack en HTTPS et publier un catalogue TSV conforme au parseur OfflinePackCatalog (9 colonnes : id, url, sha256, maxbytes, south, west, north, east, pmtiles).
3. Inclure le catalogue dans les assets Android, tester sélection et téléchargement réel.
4. Compiler l'APK et vérifier affichage hors ligne et GPS sur Xiaomi.

## Pourcentages historiques (non validés)
Global 42 % ; GPS 100 % ; carte en ligne 85 % ; planification 50 km 70 % ; bibliothèque 55 % ; téléchargement réel 20 % ; arrière-plan 5 % ; affichage hors ligne 30 % ; tests Xiaomi 10 %. Pas d'augmentation non justifiée.
