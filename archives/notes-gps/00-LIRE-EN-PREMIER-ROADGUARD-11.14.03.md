# ROADGUARD 11.14.03 — HUD premium GPS
Base : 11.14.02.
Réalisé : compteur de vitesse GPS ajouté à la carte WebView, initialisé à -- et mis à jour par updateGPS existant ; styles graphite/rouge adaptés aux petits écrans ; panneau de progression de trajet repositionné ; boutons Leaflet de zoom à 44 px minimum.
Fichier modifié : app/src/main/assets/map.html.
Conservé : cache x10, PMTiles, GPS, alertes radar, navigation Android et caméra inchangés.
Vérification : node --check du script JavaScript intégré ; archive ZIP vérifiée.
Non vérifié : compilation APK, affichage Xiaomi, interaction réelle des commandes.
Priorités suivantes : vérifier sur appareil les chevauchements éventuels avec les commandes Android ; unifier barre d'itinéraire et boutons flottants ; contrôler les écrans secondaires.
