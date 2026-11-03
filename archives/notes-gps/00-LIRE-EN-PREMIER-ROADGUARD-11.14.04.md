# ROADGUARD 11.14.04 — Interface graphite épurée

Base : 11.14.03.

Modifications réelles :
- Panneau « Où va-t-on ? » replié par défaut à chaque ouverture de la carte, et toujours dépliable au toucher ou glissement.
- Suppression du raccourci « Signaler » en double dans ce panneau ; le bouton rouge principal et l’onglet restent accessibles.
- Habillage graphite/gris des panneaux, compteur GPS et boutons d’options, rouge pour le compteur.
- Libellé du panneau réduit quand il est replié.

Fichier modifié : app/src/main/java/fr/roadguard/app/MainActivity.java.

Fonctionnalités préservées : GPS, cache ×10, PMTiles, alertes radar, fonctions de navigation, caméra (non modifiée).

Contrôles : ZIP testé. Compilation APK et rendu Xiaomi non testés. Attention : l’ouverture repliée par défaut ne mémorise pas un état déplié entre changements d’onglet.

Suite : tester le panneau sur Xiaomi ; régler les éventuels chevauchements de commandes ; vérifier la lisibilité réelle des tuiles de carte sans modifier les données cartographiques.
