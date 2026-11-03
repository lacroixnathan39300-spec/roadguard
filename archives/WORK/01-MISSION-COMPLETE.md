# WORK — Mission complète ROADGUARD cartes hors ligne

## Objectif non négociable
Livrer un Android ROADGUARD capable de **télécharger, installer, afficher et réutiliser hors connexion une vraie carte**, d'abord Jura (pilote), puis France, Suisse, Allemagne, Belgique, Italie, Espagne et extensible à tous les pays autorisés. UX : pays → Télécharger → progression → Disponible hors ligne. Aucun champ URL/empreinte/taille pour l'utilisateur. Ne jamais marquer Disponible tant que l'affichage offline n'a pas été validé.

## Base et invariants
- Source fournie dans ce même ZIP : ROADGUARD 11.13.10, Android Java, package fr.roadguard.app, minSdk 26, compileSdk 34, versionCode 171.
- Conserver GPS, rotation road-up, caméra, marqueur, fluidité, radars, recherche, alertes et UI actuels ; comparer à 11.12.7 connue comme stable. Ne pas supprimer la maquette `references/ROADGUARD-MAQUETTE-ORIGINALE.png`.
- Lire `app/build.gradle`, `app/src/main/assets/map.html`, `app/src/main/java/fr/roadguard/app/MainActivity.java`, scripts tools/ et tests avant modification. Corriger si besoin les scripts et notes hérités contradictoires.
- Ne pas aspirer massivement les tuiles publiques `tile.openstreetmap.org` ; respecter licences ODbL, attribution et règles d'usage fournisseur. Vérifier **expressément** les droits de téléchargement hors ligne, stockage, redistribution et quota du fournisseur retenu. OpenFreeMap en ligne n'est PAS automatiquement une autorisation de téléchargement de pays entiers.

## État de la mission 11.13.10

| Demande | Intégration source | Vérification qui reste |
|---|---|---|
| MapLibre réellement embarqué | JS/CSS 4.7.1 dans les assets du projet pour inclusion à la compilation, SHA et BSD vérifiés | build Android absent ici |
| Brancher la France PMTiles existante | auto-détection de l'archive privée au lancement; sélection de fichier/import local sans réseau | archive ~2,76 Go inaccessible à ce poste; vérifier son nom, type, couches et tuiles sur l'appareil |
| Routes et noms hors ligne | couches OpenMapTiles `transportation`, `transportation_name`, `place`; plages de glyphes Noto Sans locales | style/schema doit correspondre au vrai PMTiles et l'affichage doit être vu en mode avion |
| Réutiliser sans nouveau téléchargement | aucun téléchargement de carte ajouté; l'archive privée existante n'est pas copiée ni supprimée | import depuis un autre dossier fait une copie locale et a besoin de 2,76 Go libres supplémentaires |
| GPS et caméra | fonctions et fichiers de la carte principale non modifiés | vérifier l'expérience caméra/GPS avec la vue MapLibre sur Xiaomi |
| Xiaomi avant autres pays | Suisse/Allemagne non activées dans cette mission | build, installation et recette Xiaomi bloqués faute SDK/appareil |

## Plan d'exécution (faire réellement, pas seulement documenter)
1. **Auditer le moteur** : identifier la meilleure architecture intégrable et compilable dans CodeAssist : MapLibre Android natif et OfflineManager/OfflineRegion avec source compatible offline, OU PMTiles v3 avec lecteur/rendu vectoriel local et packs autorisés. Le rendu MapLibre GL JS en WebView et MapLibre natif sont deux architectures distinctes ; choisir et tester, ne pas les confondre. Vérifier versions/dépendances/compatibilité SDK et build hors réseau après mise en cache.
2. **Obtenir une vraie donnée** : utiliser source officielle/licenciée avec URL directe stable ou générer légalement un extrait Jura PMTiles. Vérifier le fichier réel (signature, metadata, taille, SHA256, contenu). Ne pas mettre d'URL fictive, de catalogue vide ni d'archive OSM PBF brute dans l'UI comme si elle était affichable.
3. **Installer un vrai flux** : depuis l'UI, téléchargement en tâche persistante, HTTPS, timeout, reprises robustes si supportées, gestion erreurs et annulation, stockage privé, intégrité SHA256, espace disque, état après redémarrage, suppression et mise à jour ; permissions Android et notification de progression si nécessaire. Séparer cache temporaire / carte installée ; éviter chargement RAM du pays entier.
4. **Afficher sans réseau** : activer le style/les glyphes/sprites/fonts nécessaires au rendu local, ou les embarquer sous licences adéquates. Vérifier cartes et libellés, zooms utiles, rotation, suivi GPS, absence de tuiles blanches, repli sur carte online si pack absent. Le pilote doit marcher téléphone en mode avion avec GPS actif.
5. **Catalogue mondial** : interface liste/recherche pays avec taille, statut, version et bouton Télécharger. Pays prioritaires FR, CH, DE, BE, IT, ES ; autres pays suivant couverture autorisée. Pour grands pays, régions automatiques et gestion stockage. Publier de vrais fichiers et un manifeste versionné HTTPS, si l'accès à un hébergement autorisé existe ; sinon documenter exactement ce blocage, fournir les packs produits et scripts reproductibles sans prétendre à la disponibilité publique.
6. **Préchargement 20 km** : quand un pack local existe, lecture anticipée graduelle autour du GPS, priorité aux zooms/axes proches, limiter mémoire, CPU et batterie ; ne jamais précharger en masse depuis serveurs raster publics. Garder caméra fluide.
7. **Recette et livraison** : tests unitaires/intégration, compilation Gradle/CodeAssist, réutilisation de l'archive France déjà présente (~2,76 Go) sans requête de téléchargement, affichage avion sur Xiaomi ; ne pas déclarer les tests terrain non exécutés. Vérifier Wi-Fi et 4G/5G, taille et avertissement forfait. VersionCode supérieur à 171 si code modifié. Fournir UN ZIP complet, notes et rapport précis.

## Définition de « terminé »
- Un bouton réel lance un téléchargement réel et affiche progression, erreurs, installation.
- Au moins Jura : carte réellement téléchargée, reconnue et affichée sans Internet.
- Un pack pays n'est déclaré disponible que si source, hébergement, intégrité, compatibilité et licence sont vérifiés.
- L'utilisateur n'a pas besoin de Colab, de saisir URL ou SHA256.
- GPS/caméra restent au moins aussi fluides que sur 11.12.7.
- Si hébergement/publication impossible, marquer BLOQUÉ, raison et étapes exactes nécessaires ; ne pas simuler le succès.

## Fichiers de sortie exigés
- `00-LIRE-EN-PREMIER-WORK.txt` actualisé ; `RETOUR-WORK-ROADGUARD-CARTES.md` (faits, preuves, non-faits, versions, captures/logs si possible).
- `HISTORIQUE-VERSIONS-ROADGUARD.md`, `TABLEAU-DE-BORD-AVANCEMENT-ROADGUARD.md` et JSON à jour.
- Projet Android complet + maquette originale + tests + scripts d'obtention/licence/catalogue + instructions CodeAssist.
- Un seul ZIP final cohérent, aucune dépendance manquante dissimulée.
