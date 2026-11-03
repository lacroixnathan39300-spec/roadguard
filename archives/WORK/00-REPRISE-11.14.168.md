# ROADGUARD 11.14.168 — pictogrammes de radars routiers

Base : 11.14.167. Android versionCode 368. Projet source à importer dans CodeAssist; aucun APK assemblé.

## Modifications

Les appareils photo et les émojis des marqueurs sont remplacés par des pictogrammes vectoriels de matériel routier. La cabine fixe est verticale avec deux fenêtres et une bande jaune/noire. Les tourelles et radars urbains ont un support sur poteau; le mobile repose sur un trépied. Feux rouges, tronçons, tunnels, limitations variables, passages à niveau, covoiturage, zones possibles, chantiers et dangers disposent de dessins distincts.

La même bibliothèque `radar_icons.js` fournit les icônes à Leaflet, MapLibre et à la carte locale Canvas. Les dessins sources SVG sont dans `app/src/main/assets/radar-icons/`. Ils ne nécessitent ni téléchargement ni police d’émojis. Les tailles petites/normales/grandes, le masquage et les clics des marqueurs sont conservés.

Les données restent celles de 11.14.167 : 7325 points français et 1777 points suisses sélectionnés par défaut, dont 194 lieux mobiles suisses non confirmés. Cette version ne modifie pas les alertes, leurs réglages ou les coordonnées. La caméra/navigation reste pour le dernier chantier.

## Tests et limite

Les 14 SVG ont été rendus et inspectés à grande et petite taille. Le vrai code de rendu Leaflet/MapLibre a passé des tests avec des adaptateurs de carte : type d’icône, viewport, popup, visibilité, taille, enregistrement des images, réutilisation du cache après changement de style. Contrôles de syntaxe JS, régressions de carte/GPS, données FR/CH et passation exécutés; détail dans WORK/VALIDATION-11.14.168.md.

Ces tests ne remplacent pas la WebView du Xiaomi. Aucun APK ni test téléphone de cette nouvelle version. La capture fournie montre seulement le fonctionnement de la carte et de ses anciens marqueurs en 11.14.167.

## Intervention finale

Importer le ZIP dans CodeAssist puis compiler 11.14.168 / 368. Sur Xiaomi, vérifier les nouveaux dessins en jour/nuit, les trois tailles et le masquage des radars; toucher une cabine pour ouvrir sa fiche, puis déplacer la carte et confirmer que le suivi reste en pause jusqu’au bouton ⌖.

Le suivi global historique reste 94 %, variation +0 point. Ces pourcentages ne mesurent pas la couverture des bases ni une validation générale sur appareil.

## Réalisé

- Création de 14 variantes vectorielles originales sans dépendance aux polices/émojis : cabine fixe, discriminant, tourelle, urbain, mobile sur trépied, feu rouge, tronçon, vitesse variable, tunnel, passage à niveau, covoiturage, zone possible, chantier et danger.
- Remplacement des appareils photo et autres émojis dans les marqueurs Leaflet.
- MapLibre affiche les mêmes pictogrammes via des images locales de densité double, avec enregistrement après un changement de style.
- La carte locale Canvas utilise les mêmes dessins; les trois tailles et la visibilité des marqueurs sont conservées.
- Les dessins SVG sources sont embarqués et disponibles hors réseau; coordonnées, bases FR/CH et paramètres des alertes restent ceux de 11.14.167.
- Rendu des 14 pictogrammes contrôlé à grande et petite taille; comportement Leaflet/MapLibre testé avec leurs vrais blocs de rendu et des adaptateurs de carte.

## Reste à faire / blocages

- Aucun APK assemblé ni essai Xiaomi de cette version.
- Le contrôle visuel local des pictogrammes et les adaptateurs de carte ne remplacent pas un test de WebView sur téléphone.
- Les bases FR/CH ont les mêmes comptes et limites de fraîcheur que 11.14.167; les lieux mobiles ne confirment pas une présence actuelle.
- Caméra reportée à la fin du projet.
