# ROADGUARD 11.13.94 — garde-fous fluidité du tampon doublé

Base : 11.13.93. Modification : `app/src/main/assets/map.html`.

- Conserve `keepBuffer=24` pour les cartes raster locales PMTiles au zoom inférieur à 17.
- Réduit à `keepBuffer=12` au zoom 17 et supérieur pour limiter la pression mémoire et les saccades possibles. Retour automatique à 24 en dézoomant.
- Borne à 1 200 entrées l’historique temporaire de tuiles locales traitées (`rgPrefetch.seen`) pour éviter son accumulation pendant les longs trajets.
- Ne modifie pas les sources publiques OpenStreetMap (`keepBuffer=9`), le GPS, la caméra, ni les radars.
- Aucun préchargement réseau non autorisé ajouté. `keepBuffer` n'est pas une distance de préchargement.

Tests : syntaxe JS via `node --check`, tests statiques Python. Aucun APK compilé ni essai Xiaomi; aucun gain de fluidité mesuré. Prochaine étape : compiler, mesurer la RAM et les FPS en conditions réelles, GPS/radars/caméra.
