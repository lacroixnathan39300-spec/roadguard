# ROADGUARD 11.14.05 — Refonte de la carte, étape 1

Base : archive ROADGUARD 11.14.04.

Travaux réalisés : nouvelle colonne de commandes flottantes graphite/rouge sur la carte (recentrer GPS, zoom +/−, bascule jour/nuit), vraies actions Leaflet, attributs d'accessibilité et dimensions tactiles adaptées. Suppression visuelle des contrôles de zoom Leaflet redondants. Harmonisation des panneaux de carte, du compteur et des informations d'itinéraire.

Fichier de code modifié : `app/src/main/assets/map.html`.

Conservé : GPS natif, cache ×10, PMTiles, radar, caméra, menus Android. Aucun changement aux traitements de données cartographiques.

Vérifié : syntaxe du JavaScript embarqué avec Node `--check` ; intégrité ZIP.

Non vérifié : compilation APK, rendu sur Xiaomi, absence de chevauchement avec les contrôles natifs. La reproduction exacte de la maquette sur tous les écrans n'est PAS terminée.

Suite : remplacer progressivement les panneaux natifs Android par des composants graphite/rouge, vérifier les boutons sur appareil, unifier itinéraires, radars, cartes hors ligne et paramètres.
