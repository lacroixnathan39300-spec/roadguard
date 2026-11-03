# ROADGUARD 11.14.67 — Lisibilité cartographique

Base : 11.14.66.

## Réalisé
- Style MapLibre vectoriel expérimental : routes OpenMapTiles/Protomaps élargies, noms de routes et de villes agrandis, halos de texte renforcés.
- Carte classique Leaflet : contraste et luminosité du filtre nuit améliorés.
- Pas de changement du moteur GPS, du marqueur bleu, des fournisseurs de tuiles, du cache, des radars ou de la caméra.
- Version Android : 267 / 11.14.67.

## Limites
- Le rendu vectoriel dépend du schéma des données PMTiles et des glyphes disponibles. Ces modifications ne garantissent pas l'affichage de toutes les routes ni des noms sur un pack incompatible.
- Les tuiles raster OSM restent des images : leurs libellés ne peuvent pas être redimensionnés individuellement.
- Vérifications statiques et intégrité ZIP uniquement ; APK non compilé, Xiaomi non testé.

## Suite
- Vérifier sur Xiaomi la présence des glyphes, le rendu réel des PMTiles, la visibilité du point GPS et le contraste jour/nuit.
- Garder la caméra pour la toute fin du projet.
