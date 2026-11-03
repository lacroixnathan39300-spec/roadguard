# Validation ROADGUARD 11.14.169

- `tools/test_vector_camera.js` : vraies fonctions de la caméra MapLibre exécutées avec un adaptateur déterministe; dégagement du panneau, anticipation, pause manuelle, mise à jour GPS pendant pause, reprise à la dernière position, aperçu avec marge, garde du style.
- `tools/test_radar_icons.js` : 14 pictogrammes, classification et marqueurs Leaflet/MapLibre.
- Syntaxe des scripts inline Leaflet et MapLibre validée avec Node; 13 groupes Python de régression exécutés avec succès (carte, GPS, radars FR/CH, visibilité, données et passation).
- Sources Java recompilées localement avec ECJ/Android API 34 sans erreur. Le code Java est inchangé hormis une référence documentaire rectifiée vers le fichier réel de sources radar.

Non testé : assemblage Gradle/dex/signature d’APK 11.14.169, rendu réel WebView/MapLibre, navigation en conduite et essai Xiaomi de ce cadrage. Les assertions statiques et les adaptateurs ne certifient pas le GPU du téléphone.

CSV et SHA-256 inchangés depuis 11.14.167, voir `SOURCES-RADARS-11.14.167.md` et le manifeste embarqué. Pas de nouveau téléchargement de données ni de préchargement permanent.
