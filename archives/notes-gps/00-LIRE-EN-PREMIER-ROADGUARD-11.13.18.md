# ROADGUARD 11.13.18 — Passation

## Correction réelle
Le bouton France dans la liste des pays appelle désormais `downloadFranceOneTap()` au lieu de `downloadPublicFranceCandidate()`, qui pointe vers le serveur indisponible map.comptoir.net. Le chemin corrigé recherche d’abord une archive locale et, sinon, exige une entrée de catalogue HTTPS avec SHA-256 et taille déclarée.

## Test
`python tools/test_france_button_111318.py` : contrôle statique du branchement. Aucune compilation Android ni validation sur Xiaomi.

## Bloqué
Il manque toujours une archive PMTiles France + Corse produite, validée, hébergée, ainsi que son entrée de catalogue accessible. Le script `tools/prepare_france_pmtiles.sh` peut extraire une région d’une source Protomaps choisie ; `tools/prepare_verified_france_catalog.py` crée l’entrée après préparation. La carte hors ligne n’est PAS fonctionnelle à ce stade.

## Prochaines actions
1. Produire et vérifier l’archive France + Corse et les couches vectorielles.
2. Héberger l’archive sur une URL HTTPS stable.
3. Générer et configurer le catalogue ROADGUARD.
4. Compiler APK, tester téléchargement/reprise, puis rendu en mode avion sur Xiaomi.
5. Ne pas modifier le GPS bleu actuellement fonctionnel.
