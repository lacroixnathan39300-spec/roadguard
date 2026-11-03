# Validation ROADGUARD 11.14.172

- Capture de 11.14.171 comparée à la maquette : saturation des pictogrammes confirmée.
- Regroupement déterministe des points visibles en cellules d’écran, sans supprimer de points de l’inventaire ni modifier le service d’alertes GPS.
- Test Node `tools/test_radar_icons.js` : 300 points dans une petite zone produisent des groupes; clic d’un groupe et visibilité des catégories vérifiés. Syntaxe Leaflet et MapLibre vérifiée.
- Tests Python de visibilité, de rendu cartographique, de navigation de référence, MapLibre et bases embarquées réussis.
- Navigation basse et variation négative des estimations vérifiées au niveau source.
- Sources Java vérifiées avec ECJ/Android API 34; aucun assemblage APK, dex ni signature.

Limites : regroupement visuel non testé dans WebView sur Xiaomi; à zoom maximal, des positions identiques restent dans un groupe. La carte locale affiche les groupes, mais n’a pas de touche de groupe dédiée : utiliser son zoom. La reproduction de la maquette sur tous les écrans demande une comparaison après installation.
