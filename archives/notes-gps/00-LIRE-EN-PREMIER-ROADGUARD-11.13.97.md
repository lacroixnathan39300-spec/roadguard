# ROADGUARD 11.13.97 — FIABILISATION DU PRÉCHARGEMENT LOCAL

Base : 11.13.96 (tampon x10 conservé à l’identique : local 720/360, source autorisée 600, OSM public 9).

## Correctif réellement effectué
- `app/src/main/assets/map.html` : la file de tuiles PMTiles locales à faible zoom conserve les requêtes encore en attente lorsqu’un nouveau lot arrive, au lieu de les écraser ; fusion sans doublons et limite de 220 candidats.
- Les callbacks de génération ancienne n’ajoutent plus d’entrées au cache de suivi après un changement de pack.
- Le cache de suivi `seen` est borné à 1 200 entrées même pendant les seules mises en cache de la couronne 20 km.
- Aucune modification de GPS, radars, caméra, cache réseau public ou valeur x10.

## Validation
- Contrôle de syntaxe JavaScript Node si disponible.
- Tests statiques des garde-fous.
- Pas d’APK compilé et pas de test physique Xiaomi. Les gains restent à confirmer.

## Prochaines étapes
- Compiler APK, essais en conduite sur Xiaomi, reprise réseau, mode avion, mesure mémoire et stabilité GPS.
