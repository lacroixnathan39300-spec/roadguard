# ROADGUARD 11.13.68 — Reprise et état vérifié

## Travail réellement réalisé
- `tools/execute_versatiles_plan.py`: vérification des archives existantes avant réutilisation, calcul SHA-256 et taille; signalement d'une archive existante corrompue sans l'écraser; factorisation du contrôle léger PMTiles.
- `tests/test_execute_versatiles_plan.py`: deux nouveaux tests, six tests réussis.
- Aucun fichier PMTiles réel France/Jura n'a été produit; aucun hébergement, APK ou test Xiaomi effectué.
- Contrôle de l'en-tête PMTiles et SHA-256 ne constitue PAS une validation de toutes les tuiles ni du rendu MapLibre.

## Priorités
1. Obtenir un extrait réel VersaTiles du Jura (source officielle https://download.versatiles.org/osm.versatiles) sur une machine avec réseau et binaire `versatiles`.
2. Vérifier l'archive produite avec `pmtiles verify` et le rendu vectoriel avec style Shortbread compatible.
3. Héberger des packs versionnés sur HTTPS et générer un catalogue réel avec SHA-256, taille et emprise.
4. Brancher les téléchargements GPS Android et affichage offline, puis compiler et tester sur Xiaomi.

## Tests
`python -m unittest discover -s tests -p 'test_execute_versatiles_plan.py' -v` : 6/6 réussis, données de simulation uniquement.

## Avancement
Global historique estimé 42%; GPS bleu 100%; carte en ligne 85%; planification 50 km 70%; mémoire cumulative 55%; téléchargement réel 20%; arrière-plan Android 5%; affichage hors ligne 30%; Xiaomi 10%. Ces valeurs ne sont pas des tests Android.
