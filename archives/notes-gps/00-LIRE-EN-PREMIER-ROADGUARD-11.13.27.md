# ROADGUARD 11.13.27 — Passation

## Modifications réellement effectuées
- tools/build_france_pmtiles_catalog.py : vérification des sections PMTiles v3, du type vectoriel MVT, des limites géographiques déclarées, de l'URL HTTPS directe et du SHA-256.
- tools/test_france_pmtiles_catalog_111327.py : test unitaire des cas valide, URL avec paramètres, type raster et section hors fichier.
- Aucun changement dans MainActivity, GPS ou interface Android.

## Tests effectués
- `python -m unittest -v test_france_pmtiles_catalog_111327` : 1 test réussi (4 scénarios).
- ZIP à contrôler après création.

## Bloqué / non réalisé
- Aucun pack France + Corse réel généré ou téléchargé.
- Aucun hébergement HTTPS ni catalogue publié.
- Aucune APK compilée ni test Xiaomi.
- Le test de l'en-tête ne prouve pas l'exhaustivité des tuiles ou le rendu des styles.

## Prochaines tâches prioritaires
1. Identifier une archive PMTiles France + Corse sous licence compatible, la produire et vérifier son contenu.
2. Tester un hébergement HTTPS avec support des requêtes Range et produire le catalogue JSON.
3. Brancher une source fiable et le téléchargement reprenable dans l'application.
4. Compiler l'APK et tester hors ligne sur Xiaomi, en préservant le GPS bleu.

## Suivi
- Pourcentage global historique estimé : 42 %, non augmenté sans fonctionnalité validée.
- Pas d'intervention utilisateur obligatoire pour ce correctif source.
- Pastille rouge seulement pour une action réellement indispensable.
