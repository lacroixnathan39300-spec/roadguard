# ROADGUARD 11.13.93 — tampon doublé sous conditions

Base : 11.13.92. Fichier modifié : `app/src/main/assets/map.html`.

- Source explicitement autorisée (`setLicensedEmergencyMap(true)`): `keepBuffer` Leaflet 10 -> 20.
- Cartes raster locales PMTiles: `keepBuffer` 12 -> 24.
- Serveur public OSM : `keepBuffer` reste à 9 (aucun préchargement massif ajouté).
- GPS, caméra, alertes radar et politique de cache existants inchangés.

**Important** : `keepBuffer` conserve des tuiles précédemment chargées à proximité de l'écran ; il ne lance pas à lui seul le téléchargement d'une zone de 60 x 60 tuiles, ni un préchargement directionnel. Le croquis précédent n'est pas à l'échelle réelle. Le doublement peut augmenter l'utilisation mémoire ; mesurer sur Xiaomi.

Tests effectués : vérification statique de la configuration ; syntaxe JavaScript vérifiée si Node disponible. Aucune compilation APK ni test réel sur Xiaomi. Aucun gain de fluidité mesuré.

Prochaines étapes : compiler APK, mesurer FPS et consommation mémoire avec buffers 10/20 et 12/24, tester passage en mode avion, GPS bleu, radars et caméra ; adapter si pression mémoire.
