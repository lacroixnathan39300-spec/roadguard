# ROADGUARD 11.13.70 — passation

## Travail réalisé
- Ajout de `tools/publish_verified_catalog.py` : produit un catalogue TSV conforme aux neuf colonnes du lecteur Android `OfflinePackCatalog` à partir de `extraction-results.json` et de fichiers PMTiles locaux.
- Vérifie fichier présent, format PMTiles v3 de base, taille, SHA-256, coordonnées, ID unique et URL HTTPS.
- Ajout de `tests/test_publish_verified_catalog.py` (5 tests réussis).
- Aucune suppression automatique de la bibliothèque de cartes.

## Ce qui n'a PAS été réalisé
- Aucune extraction réelle Jura/France, aucun hébergement HTTPS, aucune nouvelle APK, aucun test Xiaomi.
- Un catalogue généré ne rend pas les packs disponibles sur Internet. L'URL HTTPS doit réellement héberger les fichiers.
- Vérification PMTiles superficielle, pas de vérification complète de toutes les tuiles.

## Prochaines étapes prioritaires
1. Produire un vrai pack Jura depuis VersaTiles et valider la compatibilité des couches/styles.
2. Héberger le pack sur un service HTTPS contrôlé, vérifier la disponibilité et les permissions.
3. Générer le catalogue via `python tools/publish_verified_catalog.py extraction-results.json --pack-dir prepared-regions --base-url https://DOMAINE/maps/ --out catalog.tsv`.
4. Raccorder catalogue réel au cycle Android, télécharger les secteurs GPS 50 km et les afficher avec MapLibre hors ligne.
5. Compiler APK et tester sur Xiaomi.

## Avancement historique estimé
Global 42 % (inchangé). GPS 100 %, carte en ligne 85 %, planification 50 km 70 %, mémoire 55 %, téléchargement réel 20 %, arrière-plan 5 %, affichage hors ligne 30 %, APK Xiaomi 10 %. Ces chiffres ne sont pas des validations Android.

## Test
`python -m unittest discover -s tests -p test_publish_verified_catalog.py -v` : 5 tests OK.
