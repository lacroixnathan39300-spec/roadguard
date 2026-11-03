# ROADGUARD 11.13.86 — Correctifs d’intégration du tampon 20 km

## Modifications réellement effectuées
- Suppression d'une méthode Android `onDestroy()` dupliquée dans MainActivity : fusion du nettoyage des deux exécuteurs et des ressources GPS/radar.
- Activation du raster en cache sur **Leaflet** (map.html), qui est le moteur par défaut, et non plus seulement sur MapLibre.
- La carte ne bascule vers la source cache que si l'utilisateur a explicitement coché la permission de préchargement et que le modèle URL est valide.
- Rotation du curseur de planification entre lots de 12 tentatives, évitant de recommencer systématiquement les mêmes premières tuiles.
- Diagnostic du curseur dans l'interface. Le curseur mesure la progression de visite, **pas** une couverture complète des 20 km.

## Limites et blocages
- Aucun fournisseur autorisant la conservation et le préchargement n'est livré par défaut.
- Les serveurs publics OSM ne sont pas utilisés pour le préchargement de masse.
- Le plan utilise les zooms 9–11 et un plafond de 240 tuiles : il ne garantit pas le détail de toutes les rues à 20 km.
- Aucune validation sur téléphone Xiaomi. APK non compilé ici si SDK absent.
- À faire : essais instrumentés, politique de durée/stockage du fournisseur, meilleure stratégie de couverture par trajet.
