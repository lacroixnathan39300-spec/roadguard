# ROADGUARD 11.14.253 — palettes visibles sur les cartes téléchargées

Les trois palettes Graphite, Lisibilité et Couleurs OSM conservent Jour et Nuit. La carte vectorielle PMTiles applique maintenant chaque palette au fond, à l'eau, à la végétation, aux bâtiments, aux routes, à leurs contours et aux noms. Le fond OSM disponible avec le réseau est atténué sous les données locales (24 % Graphite, 48 % Lisibilité, 90 % Couleurs OSM), pour que le changement soit perceptible sur une carte téléchargée. Les zones dépourvues de pack gardent le fond en ligne, mais celui-ci est également atténué dans ces modes. MapLibre demande l'anticrénelage WebGL pour améliorer le bord des routes et des caractères.

Ces cartes PMTiles restent vectorielles : elles se redessinent à la résolution du téléphone. Aucun téléchargement « texture 4K » n'est inclus ni promis ; le niveau de détail géographique dépend du zoom maximal enregistré dans les archives (généralement 14 pour les régions, 15 pour les villes). Grossir au-delà du zoom source ne crée pas de nouvelles rues ou bâtiments.

Contrôles : test JavaScript des six combinaisons sur les couches raster/vectoriel/canvas, des trois opacités de fond et de l'option d'anticrénelage ; analyse syntaxique du JavaScript inline. Aucun rendu pixel ni APK 11.14.253 n'a été produit ici. La validation visuelle et la performance WebGL sur Xiaomi restent à effectuer.

🔴 INTERVENTION OBLIGATOIRE — PLUS TARD : compiler 11.14.253 dans CodeAssist ; sur une carte PMTiles enregistrée, basculer Graphite Jour, Lisibilité Jour et Couleurs OSM Jour, puis répéter de nuit. Vérifier la qualité et la fluidité sur téléphone.
