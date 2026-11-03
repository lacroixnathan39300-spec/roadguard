# ROADGUARD 11.14.92 — Contraste des autoroutes et retour raster fiable

## Réalisé
- Ajout d'un contour spécifique aux autoroutes vectorielles OpenMapTiles (sous la chaussée rouge).
- Adaptation du contour aux palettes jour/nuit, y compris pendant un changement différé.
- Retour à la carte raster : remise à zéro de l'identifiant de pack actif et des compteurs de diagnostic, pour éviter de fausses indications de pack actif.
- Indication de chargement du pack sans prétendre que les tuiles ont effectivement été affichées.
- Suppression d'un compteur JavaScript inutilisé.
- Aucun changement volontaire au GPS, itinéraires, alertes, caméra ni politique de cache.

## Vérifications et limites
- Syntaxe JavaScript et intégrité ZIP à contrôler avant livraison.
- Pas de compilation Android ni test Xiaomi demandé.
- Source vectorielle PMTiles compatible toujours nécessaire : aucun rendu premium réel garanti.
- Ne demander de compilation qu'après un lot substantiel. Caméra en dernier.
