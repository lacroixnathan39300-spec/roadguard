# ROADGUARD 11.13.96 — EXPÉRIENCE ZONE ×10

Base : 11.13.95. Modifications : app/src/main/assets/map.html uniquement.

- Cartes PMTiles locales : keepBuffer normal 72 → 720, zoom ≥17 : 36 → 360.
- Source d’urgence explicitement autorisée : 60 → 600.
- OpenStreetMap public : 9 inchangé.
- GPS, radars et caméra : code non modifié.

**Important** : keepBuffer est une marge en nombre de tuiles Leaflet, pas une distance GPS, ni une fonction de préchargement garanti. Les valeurs élevées peuvent consommer beaucoup de RAM, provoquer des saccades ou planter WebView. Le cache hors ligne n'est pas garanti pour les zones jamais affichées.

Contrôles : validation syntaxe JavaScript si disponible et contrôle statique des valeurs. Aucun APK compilé, aucun essai réel Xiaomi.

Prochaine étape : compiler APK, mesurer RAM/FPS sur téléphone, tester zoom, conduite et mode avion ; prévoir un retour à 11.13.95 si instable.
