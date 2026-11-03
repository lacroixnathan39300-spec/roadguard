# Validation ROADGUARD 11.14.170

- `tools/test_vector_camera.js` exécute les fonctions MapLibre : dégagement du panneau, pause manuelle, reprise, aperçu de route, zoom natif. Garde de style contrôlée.
- `tools/test_local_camera.js` exécute les fonctions Canvas : suivi GPS, panneau, pause après exploration, mise à jour des données, aperçu, ⌖.
- `tools/test_camera_controls.js` exerce le zoom Leaflet et contrôle le branchement natif Android vers les trois cartes.
- `tools/test_radar_icons.js` vérifie 14 pictogrammes et leurs marqueurs Leaflet/MapLibre.
- 13 groupes Python de régression passent (carte, GPS, radars FR/CH, visibilité et passation). Syntaxe JS Leaflet, MapLibre et carte locale validée avec Node.
- Sources Java passées par ECJ avec Android API 34 sans erreur; cela ne produit ni APK ni dex. Corps de la tâche Gradle des ressources embarquées vérifié via Groovy : MapLibre, Brotli, licences et glyphes.

Limites : aucun APK 11.14.170 assemblé, signé ou installé; pas de mesure FPS ni d’essai en conduite. Les adaptateurs JavaScript ne remplacent pas une WebView Android. Données FR/CH et licences inchangées; consulter SOURCES-RADARS-11.14.167.md.
