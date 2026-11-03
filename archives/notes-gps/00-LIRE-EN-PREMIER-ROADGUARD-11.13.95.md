# ROADGUARD 11.13.95 — tampon de rétention triplé

Base: 11.13.94. Fichier modifié: `app/src/main/assets/map.html`.

- PMTiles raster local : `keepBuffer` 24 -> 72 pour zoom <17, 12 -> 36 pour zoom >=17.
- Source raster distante explicitement autorisée : `keepBuffer` 20 -> 60.
- Serveur public OpenStreetMap : `keepBuffer` 9 inchangé. Pas de préchargement réseau massif.
- GPS, radars, caméra et cache WebView : code inchangé.

`keepBuffer` est un nombre de tuiles Leaflet conservées autour de l'écran et non un rayon en km. Un tampon triplé en largeur peut fortement accroître la RAM consommée; pas de promesse de fluidité. À mesurer sur téléphone. Ne pas confondre avec le chargement anticipé de tuiles jamais vues.

Contrôles effectués : analyse syntaxique des scripts JS intégrés via Node, assertions statiques des paramètres, intégrité ZIP. APK non compilé, tests Xiaomi non effectués. Avancement global estimé inchangé à 63 % faute de validation réelle.

Prochaines priorités: compiler APK, essais en déplacement zooms 15/17, mesurer mémoire/FPS, vérifier absence de tuiles vides, GPS bleu, radar et caméra. Si saccades ou crash, revenir aux paramètres 11.13.94 (24/12/20).
