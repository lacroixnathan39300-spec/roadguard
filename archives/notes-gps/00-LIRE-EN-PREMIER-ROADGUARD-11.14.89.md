# ROADGUARD 11.14.89 — relief des routes vectorielles

Base : 11.14.88. Lot sans compilation utilisateur.

## Effectué
- Ajout de contours de routes (OpenMapTiles transportation/road et Protomaps roads) sous les couches de chaussée pour une meilleure lisibilité sur fond graphite et clair.
- Le changement de thème applique aussi les couleurs des nouveaux contours, sans rechargement des tuiles si le style est prêt.
- GPS, cache, alertes, itinéraire, caméra et sélection des packs conservés sans modification.
- Version Android 11.14.89.

## Limites et blocages
- Le rendu vectoriel dépend d'un pack PMTiles compatible et de ressources MapLibre locales.
- Les polices Noto Sans sont présentes dans les assets, mais le fonctionnement des étiquettes doit encore être validé sur téléphone.
- Aucune compilation Android ni test Xiaomi réalisé.
- Le mode raster historique reste la solution de repli.

## À faire
- Choisir et intégrer une source vectorielle licenciée, contrôler ses source-layers.
- Vérifier le rendu des libellés et les transitions de thème sur Xiaomi lors d'un test groupé, pas maintenant.
- Garder la caméra pour la fin.
