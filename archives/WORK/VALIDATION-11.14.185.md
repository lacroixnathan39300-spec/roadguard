# ROADGUARD 11.14.185 · crédit cartographique

- Le logo Leaflet était déjà supprimé. Le texte OpenStreetMap aperçu ensuite provient du crédit des données cartographiques : il reste visible conformément aux règles d’attribution.
- Leaflet, MapLibre et la carte locale affichent maintenant leur crédit en bas à gauche, au-dessus du panneau de recherche. Le lien OpenStreetMap est conservé et le libellé est raccourci sur la carte en ligne. Les sources de tuiles vectorielles conservent leurs attributions propres.
- Changement de version : code 385, 11.14.185. Compilation et signature consignées lors de la livraison.
- Vérification Xiaomi : ouvrir Carte, puis Trajet et Carte locale; confirmer que le coin inférieur droit est libre et que le crédit en bas à gauche reste lisible au-dessus du panneau.

Contrôles locaux : `:app:assembleDebug` réussi. APK code 385/version 11.14.185, signatures v2 et v3 vérifiées, certificat SHA-256 `52ff1a034ff05a11847435e25c0f2077614fe278290818b42b4bad198502ffa3` identique à la version précédente. Les scripts des trois cartes passent le contrôle de syntaxe JavaScript. L’essai visuel sur Xiaomi reste à réaliser.
