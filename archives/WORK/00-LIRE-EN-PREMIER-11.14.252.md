# ROADGUARD 11.14.252 — cartes locales ensemble

Dans « Cache de la carte → Cartes à télécharger », les entrées installées affichent un bouton vert « Téléchargée », les autres un bouton rouge « Télécharger ». Toucher une carte installée ouvre la carte en conservant toutes les autres cartes locales. Le mode de carte locale est restauré au démarrage si au moins un pack vectoriel compatible est présent. Le fournisseur de tuiles PMTiles choisit automatiquement parmi tous les packs installés pour chaque tuile visible. Si le réseau existe, le fond OSM reste disponible au-delà des régions enregistrées ; sans réseau, les régions enregistrées restent affichables.

Les archives régionales ne sont pas incluses dans ce ZIP : 93 cartes sont proposées au téléchargement à la demande, avec contrôle de taille et de SHA-256. Les options Jour/Nuit et les trois palettes du projet restent disponibles. Android Auto est reporté.

## Vérifications de ce lot

- `InstalledPmtilesMosaic.java` compilé avec le compilateur Java 17 ; contrôle exécutable sur deux archives PMTiles artificielles tirées de l'en-tête du fichier de démonstration : sélection France Sud-Est, Suisse et aucune carte hors emprise. Cela teste le choix des candidats, pas le rendu Android.
- Neuf tests existants du format d'en-tête PMTiles exécutés avec succès.
- JavaScript inline de `map_maplibre.html` vérifié avec `node --check`.
- Le SDK Android, la distribution Gradle et un appareil Xiaomi ne sont pas disponibles ici. Aucun APK 11.14.252 n'est produit ; le passage exact entre régions et le rendu WebView restent à essayer sur le téléphone.

## Essai sur téléphone

Importer le dossier ROADGUARD-11.14.252 dans CodeAssist puis compiler l'application téléphone. Dans Cache de la carte, vérifier les boutons rouges et verts, télécharger France Sud-Est et Suisse, ouvrir la carte, parcourir chaque région puis traverser la frontière. Refaire l'essai en mode avion après installation et relancer l'application. Signaler le texte exact du bandeau d'erreur WebView s'il apparaît : il inclut maintenant le message JavaScript précis.

🔴 INTERVENTION OBLIGATOIRE — PLUS TARD : compilation CodeAssist et essai du rendu sur Xiaomi. Le ZIP contient les sources complètes, pas un APK compilé.
