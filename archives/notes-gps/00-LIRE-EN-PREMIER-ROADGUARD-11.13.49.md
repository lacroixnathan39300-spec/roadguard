# ROADGUARD 11.13.49 — Carte en ligne prioritaire sur PMTiles actif

## Problème réel identifié
Le bouton « Afficher les routes avec Internet (MapLibre) » activait `mapLibrePreview=true`, mais `onPageFinished` rechargeait systématiquement le pack PMTiles vectoriel actif via `setVectorPmtiles`. Si un pack local était sélectionné, la carte en ligne était donc immédiatement remplacée par la carte locale (éventuellement vide ou hors zone).

## Correctif
- Ajout d'un drapeau transitoire `forceOnlineMapLibre`.
- Le bouton « Afficher les routes avec Internet » active ce drapeau.
- Le chargement de la carte MapLibre ne remplace plus le style OSM en ligne par un PMTiles actif lorsque ce drapeau est actif.
- « Prévisualiser ce pack vectoriel » désactive ce drapeau pour rendre à nouveau possible le style local.
- Version Android 11.13.49 (code 179).
- Aucun changement au code de position GPS ni à la caméra.

## Vérifications
`python tools/test_online_mode_111349.py` : contrôles statiques uniquement.
Aucune compilation Android, aucun test sur Xiaomi, aucun téléchargement France effectué.

## Priorités restantes
1. Source cartographique autorisant la mise en cache hors ligne et livraison d'une carte réelle.
2. Compilation APK Android et test d'affichage en ligne + mode avion.
3. Téléchargement de régions directement dans l'app, sans saisie de catalogue.
4. Vérifier récupération des données existantes sans effacement.

## Intervention utilisateur
Aucune action nécessaire pour ce correctif source. Une APK compilée sera indispensable pour constater le changement sur téléphone.
