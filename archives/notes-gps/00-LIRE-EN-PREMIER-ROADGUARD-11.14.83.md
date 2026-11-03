# ROADGUARD 11.14.83 — PASSATION

## Réalisé
- Carte de nuit : filtre raster moins saturé et plus sobre, pour réduire la domination des verts/marrons sur les captures Xiaomi.
- Mode contraste nuit : filtre ajusté indépendamment.
- Version Android 11.14.83 (code 283).
- GPS, suivi, rotation de carte, cache, routage, signalements, caméra : code de fonctionnement inchangé.

## Limites
- Les tuiles OSM sont des images raster : impossible de recolorer uniquement les voies ferrées, forêts ou routes sans changer de style/source cartographique.
- Rotation selon cap déjà implémentée dans map.html; pas de modification avant test en déplacement.
- Pas de compilation Android ou test sur Xiaomi dans cet environnement.

## Tests à faire sur Xiaomi
1. Importer ce ZIP comme nouveau projet CodeAssist.
2. Compiler et vérifier la carte de nuit à l'arrêt et en conduite sécurisée.
3. Vérifier GPS, compteur, recentrage, recherche et cache.
4. Comparer capture de carte avec la maquette.

## Prochaines tâches
- Comparaison pixel/proportions de la maquette premium.
- Si rendu cartographique encore trop chargé : étudier un vrai style vectoriel compatible avec les sources/licences et le cache existant, plutôt que multiplier les filtres raster.
- Caméra : à traiter en dernier.

## Blocages
- Pas de validation de build Android dans cet environnement.
