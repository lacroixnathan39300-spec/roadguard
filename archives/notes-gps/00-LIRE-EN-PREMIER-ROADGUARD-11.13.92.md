# ROADGUARD 11.13.92 — fluidité carte / cache

Base : 11.13.91. Modifié : `app/src/main/assets/map.html`.

- Intervalle de mise à jour des tuiles Leaflet de 160 à 100 ms pour réduire la latence lors des déplacements, sans déclencher de téléchargement automatique de zone.
- Cache périphérique Leaflet `keepBuffer` de 9 à 10 uniquement lorsque la source cartographique de cache autorisée est explicitement activée. Source publique OSM : maintien à 9, pas d'extension du tampon.
- GPS, caméra, radars et cache existant non modifiés.

Validation : syntaxe des deux scripts JS dans map.html ; 3 tests Java de classes de cache (Planner, DiskCache, Gateway). Pas de compilation APK, pas de test Xiaomi, pas de garantie de fluidité mesurée. Note : `keepBuffer` garde des tuiles autour de l'écran, et les règles du fournisseur s'appliquent toujours.

Prochaines tâches : essais sur téléphone en déplacement, validation réseau faible et hors ligne, mesure des délais, compilation APK Android, fournisseur explicitement autorisé si besoin de tampon élargi.
