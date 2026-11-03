# ROADGUARD 11.14.90 — chargement du thème vectoriel

Base : 11.14.89. Aucun test ni compilation demandé à l’utilisateur.

## Réalisé
- Correction du changement jour/nuit pendant un chargement de style MapLibre : le changement est mis en attente puis appliqué au prochain `style.load`, sans relancer inutilement le téléchargement des tuiles.
- La carte raster existante, le GPS, les itinéraires, les alertes, le cache et la caméra restent inchangés.
- Version Android incrémentée à 11.14.90.

## Contrôles
- Contrôle de syntaxe JavaScript et intégrité de l’archive ZIP (voir livraison).
- **Aucune compilation APK ni test sur Xiaomi**. Le rendu vectoriel nécessite toujours un pack compatible.

## Suite prioritaire
1. Intégrer une source de données vectorielles avec licence appropriée et schéma documenté.
2. Valider les étiquettes, le changement de thème et le GPS dans un test groupé, uniquement lorsqu'il sera indispensable.
3. Conserver la caméra pour la fin.
