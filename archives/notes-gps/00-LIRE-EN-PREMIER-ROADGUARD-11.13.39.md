# ROADGUARD 11.13.39 — zoom vectoriel PMTiles réel

## Modification vérifiable
- `MainActivity.java`: lors de l'activation d'un pack vectoriel installé, lit le zoom maximal dans l'en-tête PMTiles v3 (octet 101), puis le transmet à `setVectorPmtiles`.
- `map_maplibre.html`: utilise ce zoom dans la définition de source MapLibre au lieu de `maxzoom:14` fixe.
- Effet attendu: les cartes extraites avec `--maxzoom=12` sont surzoomées au-delà de z12 plutôt que de déclencher des demandes de tuiles inexistantes aux zooms 13–14.
- Code du marqueur GPS bleu inchangé.

## Vérifications
- Tests de régression statiques Python: réussis. Pas de compilation Android ni test sur Xiaomi.
- ZIP vérifié.

## Limites réelles
- Aucune archive France + Corse réelle générée ou hébergée, aucune APK construite.
- Le correctif ne garantit ni le contenu ni la lisibilité d'un futur jeu de tuiles.
- Prochaine priorité: produire un PMTiles France+Corse réel, le distribuer et tester la carte en mode avion sur appareil.
