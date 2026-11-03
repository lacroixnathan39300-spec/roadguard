# ROADGUARD 11.14.93 — Protection des transitions de style MapLibre

## Réalisé
- Ajout d'un compteur de génération de style lors du passage raster/vectoriel et du changement de pack.
- Le rappel asynchrone `style.load` lié à un ancien pack ne peut plus manipuler le GPS ou l'affichage du nouveau pack.
- Numéro de génération ajouté au diagnostic pour faciliter l'analyse des changements de style.
- Version Android portée à 11.14.93.

## Conservé
- Carte raster de secours, GPS, alertes, navigation, cache et caméra existants.

## Limites / tests
- Vérification statique JS et intégrité ZIP uniquement ; aucune compilation Android ni test Xiaomi.
- Aucun fournisseur vectoriel en ligne ajouté ; la carte vectorielle exige toujours un PMTiles compatible.
- Prochaines étapes : vérifier les schémas de couches sur un vrai pack licencié, contrôler la couverture des glyphes et tester une seule fois sur Xiaomi en fin de lot.
