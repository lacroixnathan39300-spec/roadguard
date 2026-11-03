# ROADGUARD 11.14.84 — Pack vectoriel Premium V1

## Fait
- Deux packs de palette exportables : `app/src/main/assets/roadguard-styles/premium-night.json` et `premium-day.json`.
- Palette réellement branchée sur les couches vectorielles MapLibre déjà présentes (`map_maplibre.html`).
- Changement jour/nuit transmis par `applyMapTheme()` lorsque le mode vectoriel est actif.
- Couleurs graphite, routes gris clair, bâtiments discrets, eau bleu foncé, voies ferrées atténuées.
- Mode raster Leaflet actuel conservé comme solution de secours. GPS et caméra non modifiés.

## Limites importantes
- **Pas d'activation forcée de MapLibre** : la carte vectorielle nécessite un pack PMTiles vectoriel compatible déjà installé et le moteur MapLibre embarqué. Sans cela, la carte raster actuelle reste affichée.
- Les JSON sont des packs de palette portables, pas des données géographiques ni des tuiles.
- Les couches Protomaps/OpenMapTiles ne sont pas totalement uniformes; le rendu des voies et noms doit être testé sur un vrai pack.
- Aucune compilation APK ni test Xiaomi réalisés ici.

## À tester
1. Compiler dans CodeAssist et vérifier la carte actuelle.
2. Si un pack vectoriel compatible est présent, activer l'aperçu MapLibre depuis les cartes et tester le thème.
3. Vérifier routes, noms, position GPS, zoom, rotation, cache, retour à la carte classique.

## Prochaines tâches
- Fournisseur de tuiles vectorielles en ligne sous licence, avec cache temporaire borné, sans téléchargement massif.
- Contrôle de thème dans les paramètres et sélection du pack.
- Vérification du schéma source-layer des données vectorielles.
- Caméra toujours reportée à la fin.
