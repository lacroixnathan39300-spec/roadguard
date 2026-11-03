# ROADGUARD 11.13.37 — Extraction France bornée

## Réalisé
- Script `tools/prepare_france_pmtiles.sh` : maxzoom 12 par défaut, option 0..15 ; vérification réelle PMTiles v3 / type MVT / emprise France+Corse / taille maximale 8 Gio avant promotion.
- Conservation de l'ancien pack et nettoyage du temporaire en cas d'échec.
- Tests de refus de zoom incorrect et d'échec d'extraction simulé.
- Aucun changement au GPS, à la caméra ou au code Android.

## Limites
- La vérification des bornes ne démontre pas la présence de toutes les routes ni de la Corse.
- Aucune archive France réelle créée, aucune URL de pack hébergé configurée, aucun APK compilé ni test Xiaomi.
- Le zoom 12 est un compromis provisoire ; pas encore de validation d'usage routier.

## Prochaines étapes
1. Obtenir une archive source Protomaps valide et réaliser une extraction réelle dans un environnement réseau adapté.
2. Vérifier taille, couches et rendu, puis héberger en HTTPS et publier un catalogue SHA-256.
3. Compiler et tester l'APK sur Xiaomi, en conservant le GPS bleu.
