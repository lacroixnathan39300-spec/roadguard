## 11.14.274 — connexion native
- Inscription et connexion numéro + mot de passe dans l’application, sans navigateur ni SMS.
- Site v10 publié : routes natives, mot de passe dérivé, limites de tentatives et correction du 404 du propriétaire.
- Jeton par téléphone conservé dans Android Keystore ; anciens comptes site à migrer de manière vérifiée.
- Compilation Android sur téléphone encore nécessaire.

## 11.14.273 — parcours simplifiés
- Accueil, menu et réglages regroupés autour des actions courantes.
- Téléchargement des cartes mis en avant ; filtres installées et à télécharger.
- Détails du cache et outils occasionnels accessibles à la demande.
- Connexion obligatoire conservée. Compilation Android à réaliser.

## 11.14.272 — connexion obligatoire
- Écran Mon compte avant tout accès à la carte ; confirmation du téléphone depuis le site, ouverture automatique après association.
- GPS, import radar et surveillance arrière-plan fermés avant connexion et après révocation.
- Session déjà confirmée conservée hors réseau pour les cartes locales ; contrôle de session au retour au premier plan.
- Compilation Android à effectuer sur appareil outillé.

# ROADGUARD 11.14.173 — maquette avant compilation

Huit vues secondaires en WebView locale, carte jour/nuit plus proche de la référence, fond d’accueil généré, pont Android avec données/actions réelles. Tests de source réussis; rendu Android non vérifié. Interface 80 %, global 94 % sans variation; compilation utilisateur différée.

# ROADGUARD 11.14.172 — regroupement cartographique

Capture Xiaomi 11.14.171 : carte saturée. Groupes numérotés sur les trois cartes, zoom au toucher sur Leaflet/MapLibre, alertes GPS et inventaires FR/CH conservés. Interface estimée 85→80 % suite au constat, global 94→94 %. Source testée localement, APK non assemblé. La maquette complète reste la cible.

# ROADGUARD 11.14.171 — fidélité visuelle de la Carte

Deux captures Xiaomi 11.14.170 comparées à la maquette validée : barre haute trop présente et action Signaler insuffisamment dominante. La Carte gagne de l’espace, Menu et Avancement restent accessibles, Signaler est central, le panneau replié est réduit, le trajet réel devient bleu. Code testé localement, pas d’APK 11.14.171. Estimation visuelle 100→85 %, global 95→94 % (révision honnête après retour).

# ROADGUARD 11.14.170 — caméra des trois cartes

Zoom manuel et pause unifiés; carte locale avec suivi, dégagement du panneau et aperçu de route; MapLibre garde son cadrage et la pause durant les changements de style. Source prête pour compilation finale sur Xiaomi. Caméra estimée 40→70 %, MapLibre 89→90 %, global 94→95 %, valeurs historiques non auditées; pas d’APK construit.

# ROADGUARD 11.14.169 — caméra vectorielle

La carte MapLibre ajuste le cadrage au panneau de trajet et garde la pause d’exploration pendant les changements de style. La reprise ⌖ utilise la dernière position et l’anticipation; la vue globale de l’itinéraire réserve la place du panneau. Données et alertes inchangées. Projet source, APK 11.14.169 non assemblé; estimation globale 94 % (+0 point).

# 11.14.168 — 16/10/2026

Pictogrammes vectoriels de radars routiers à la place des appareils photo/émojis : 14 variantes partagées Leaflet/MapLibre/Canvas, sources SVG locales. Code 368; global historique 94 %, +0 pt. Rendu local et adaptateurs de carte vérifiés; APK/Xiaomi restants.

# 11.14.167 — 16/10/2026

Suisse activée dans le sélecteur commun carte/service GPS; 1777 points CH par défaut dont 194 lieux mobiles non confirmés réglables. Imports CH utilisables, ancien POI-Warner géographiquement ambigu isolé. Code 367, global estimé 94 %, +0 pt; APK à compiler sur CodeAssist.

# 11.14.166 — 16/10/2026

Bases embarquées FR 24429 points / CH 1777 points, sources recoupées et dédoublonnées; lieux historiques en option; exclusion suisse carte/alertes conservée. Java vérifié; aucun APK assemblé. Global estimé 94 %, +0 pt.

# ROADGUARD 11.14.160 — téléchargement multi-sources Suisse

- Ajout d’une action unique pour télécharger MapAtlas, OpenStreetMap et Lufop Radardroid sans clé API.
- Fichiers et licences distincts; index OSM séparé de Lufop pour éviter tout écrasement. Résumé des comptes et annulation gérés.
- Les données CH restent stockées uniquement et ne sont pas chargées sur la carte ni dans les alertes.
- versionName 11.14.160 / versionCode 360. Compilation non lancée.

# ROADGUARD 11.14.160 — correctif crash des filtres radar

- `showSection` calcule maintenant l’index des catégories par `rr+cc`, sans dépasser les 10 éléments.
- Corrige `ArrayIndexOutOfBoundsException: length=10; index=10` à l’ouverture des Alertes.
- Le correctif de distance double/float de 11.14.158 est conservé.
- versionName 11.14.160 / versionCode 360. Compilation non lancée.

# ROADGUARD 11.14.160 — correctif de type radar

- `showRadarDetails` conserve la distance retournée par `geoMeters` en `double`; sentinelle et test de validité adaptés à `Double`.
- Corrige l’erreur CodeAssist « cannot convert from double to float » signalée sur cette ligne.
- versionName 11.14.160 / versionCode 360. Compilation non lancée.

# ROADGUARD 11.14.156 — position GPS sur le trajet

- Correspondance GPS au segment le plus proche avec curseur de progression monotone et marge de retour limitée.
- Distance des prochaines consignes mesurée depuis la projection sur la route.
- Contrôles statiques uniquement; intersections, routes parallèles et demi-tours à tester sur téléphone.
- versionName 11.14.156 / versionCode 356.

# ROADGUARD 11.14.155 — instructions de manœuvre

- Messages OSRM enrichis : sorties de rond-point avec numéro si disponible; sortie de giratoire distincte de son entrée.
- Demi-tour reconnu avant tout modificateur gauche/droite; virages légers et serrés distingués.
- Tests statiques uniquement; annonces réelles à écouter sur Xiaomi.
- versionName 11.14.155 / versionCode 355.

# ROADGUARD 11.14.154 — précision du recalcul de trajet

- Distance d’écart mesurée sur le segment routier le plus proche au lieu du sommet le plus proche.
- Le recalcul automatique ne prend en compte que les fixes ayant une vitesse GPS connue; seuils existants conservés.
- Tests statiques seulement; essai réel nécessaire pour vérifier les faux positifs.
- versionName 11.14.154 / versionCode 354.

# ROADGUARD 11.14.153 — suivi GPS réglable

- Trois aides de suivi exposées dans Apparence : zoom adaptatif, anticipation et réduction d’interface en conduite.
- Anticipation géodésique au centre MapLibre, marqueur conservé sur la position GPS.
- Réglages persistants, changements reflétés en direct sur la carte.
- Tests source/statique; compilation et Xiaomi exclus de cette livraison.
- versionName 11.14.153 / versionCode 353.

## 11.13.8 — Limites PMTiles pays
Correction des validations de taille France/catalogue international : 2 Gio PMTiles, 600 Mio XYZ ZIP. Tests de régression ajoutés. Aucun changement GPS/caméra.

## 11.13.2 — Outil pilote Jura PMTiles, vérification et catalogue optionnel. Aucun pack hébergé.\n\n# ROADGUARD 11.12.7 — Réduction des flashs blancs

- Fond sombre opaque pour la WebView et les couches cartographiques, y compris pendant le remplacement de tuiles.
- Buffer en ligne légèrement augmenté (7 à 9) sans téléchargement spéculatif massif.
- Rotation : suppression du rafraîchissement explicite des tuiles tous les 8 degrés ; l’emprise diagonale 360° étant indépendante du cap, ces rafraîchissements étaient redondants et pouvaient contribuer à la charge.
- Animation de fondu Leaflet conservée, pour ne pas masquer les tuiles existantes brutalement.
- Aucun préchargement massif du serveur public OSM. Rayon local 20 km conservé sous condition de pack raster PMTiles compatible.
- Vérification syntaxique JavaScript uniquement ; APK non compilé, comportement Xiaomi non testé.
- Si les flashs persistent, profiler WebView et migrer vers un moteur GPU natif.
- Importer dans CodeAssist, compiler, installer et tester comme passager en ligne droite et dans les virages.
- versionName 11.12.7 / versionCode 158.

# ROADGUARD 11.12.6 — Préparation locale progressive à 20 km

- Cible géographique : rayon 20 km (diamètre 40 km), **uniquement si un pack raster PMTiles local est installé et contient les données**.
- Couverture éloignée à zoom réduit (zoom 8 à 12) ; couverture détaillée proche conservée (7×7 + grille visible).
- Renouvellement au déplacement de 1,5 km / après 45 s, minimum 10 s entre cycles, maximum 180 nouvelles tuiles par cycle et 3 chargements simultanés.
- **Aucun téléchargement massif OpenStreetMap**, aucune promesse de pack France disponible. Le rayon de 20 km n'est pas du détail routier au zoom 18.
- Ne pas confondre données PMTiles locales existantes et téléchargement de cartes : ce patch ne fournit aucun pack.
- Risque à tester : coexistence du préchauffage 7×7 et de la file 20 km, utilisation mémoire, fluidité caméra, tuiles absentes.
- Compilation APK et tests sur Xiaomi NON réalisés ici. Importer dans CodeAssist, compiler, tester en passager.
- versionName 11.12.6, versionCode 157. Maquette d'origine conservée.

# ROADGUARD 11.12.5 — Couverture anticipée des virages / packs locaux

## Réalisé
- Couverture géométrique calculée sur la diagonale du viewport : le carré englobant couvre tous les angles de rotation, sans attendre le rafraîchissement du cap de 8°.
- Marge modérée en ligne (3,5 %) pour ne pas précharger de secteurs lointains sur les serveurs publics OSM.
- Marge plus large (14 %) et conservation accrue (keepBuffer 12) pour les PMTiles raster locaux.
- Préchargement local élargi de 5×5 à 7×7 tuiles autour du point anticipé, avec trois chargements simultanés au maximum ; uniquement pour les packs locaux déjà installés.
- Rafraîchissement de couverture au redimensionnement et après zoom.
- Préservation des réglages et du mode route vers le haut.

## Limites et vérification
- Pas de nouveau pack France intégré, pas de serveur de téléchargement configuré, pas de moteur GPU de production ajouté.
- Ce changement augmente la surface de tuiles demandées pour la vue tournée ; il ne garantit pas leur arrivée instantanée si le réseau est lent.
- Vérification de syntaxe JS et tests statiques réalisés. Compilation APK et tests Xiaomi non réalisés.
- Vérifier sur téléphone la disparition des coins blancs, les virages, le zoom, la consommation de données et les gestes.

## Étapes CodeAssist
1. Importer le projet Android source.
2. Compiler et installer l'APK.
3. Tester comme passager, comparer à la version 11.12.4.
4. Si la carte saccade encore, profiler WebView puis migrer vers MapLibre/renderer GPU avec un fournisseur de tuiles autorisant le préchargement ou des packs hors ligne.

## Version
versionName 11.12.5 ; versionCode 156.

# ROADGUARD 11.12.4 — Couverture des tuiles pendant les rotations

## Réalisé
- Correction de la zone triangulaire blanche constatée sur la capture Xiaomi : Leaflet calculait les tuiles pour un rectangle non tourné.
- Calcul des limites de tuiles à partir du rectangle tourné à l'angle de navigation, avec marge de sécurité de 12 %.
- Actualisation des tuiles lors des changements de cap d'au moins 8° ; conserve les tuiles existantes via keepBuffer 7 en ligne et 9 en local.
- Pas de préchargement massif du serveur public OpenStreetMap : seule la couverture de la vue tournée est demandée. Un grand périmètre hors écran exige un fournisseur autorisant explicitement le préchargement ou des packs locaux PMTiles.
- Rotation fluide, GPS et mode route vers le haut conservés.
- Version 11.12.4, versionCode 155.

## Tests et limites
- Vérification de syntaxe JS et contrôles de code ; compilation APK et test Xiaomi non effectués.
- Les chargements dépendent de la connexion. Une zone blanche peut persister brièvement pendant la réception des tuiles.
- À long terme : moteur vectoriel GPU (MapLibre) et stockage de cartes locales pour une fluidité comparable à Waze.

## Action utilisateur
Importer dans CodeAssist, compiler, installer, tester comme passager en virage et contrôler la disparition des triangles blancs, le zoom et les gestes.

# ROADGUARD 11.12.3 — Caméra et virages plus stables

Changements: lissage angulaire dépendant du temps, vitesse de rotation limitée à 160°/s, suppression des transformations DOM inutiles, compensation GPU des mouvements de caméra jusqu’à 9 px avant recentrage Leaflet, cache raster PMTiles local élargi à keepBuffer=9 (pas de préchargement supplémentaire sur serveur public OSM).

Tests: syntaxe JS et contrôles statiques. Compilation APK et performances réelles sur Xiaomi non testées. Vérifier virages, tuiles, alignement marqueurs, zoom et gestes. Si les saccades persistent, profiler le WebView/Leaflet ou migrer vers moteur de rendu natif/vectoriel.

## ROADGUARD 11.12.2 — Orientation navigation
Rotation des couches Leaflet autour du centre visible (au lieu du centre propre à chaque pane), mode ROUTE activé par défaut pour les nouvelles installations, maintien du réglage existant. Pas encore de guidage violet sans itinéraire calculé. À tester sur Xiaomi.

# Historique ROADGUARD (résumé vérifiable)

- **11.9.0** : séparation des statuts XYZ/PMTiles et contrôles de catalogue, selon note de transmission source.
- **11.9.1** : ajout d'une note de reprise persistante et mise à jour du numéro de version ; aucune fonction cartographique ajoutée.

Consulter les notes de chaque version pour les détails. Les affirmations antérieures non testées sur appareil ne sont pas des validations.
\n## 11.9.2 — 10 octobre 2026\n- Bandeau version corrigé (versionName Android).\n- Diagnostic PMTiles installés et non affichables dans la bibliothèque.\n- Note de reprise actualisée. Aucun pack France ni lecteur vectoriel livré.\n
## 11.9.3 — 10/10/2026
- Ajout de la vraie maquette ROADGUARD originale dans `references/`.
- Suivi esthétique séparé du suivi technique (estimation initiale 25 %).
- Note de reprise et critères de comparaison visuelle mis à jour.
- Aucun code d'interface modifié dans cette version documentaire.

## 11.9.4 — 10/10/2026
- Tableau complet de 14 barres en Markdown et JSON.
- Consignes de suivi et note de reprise actualisées.
- Maquette originale conservée.
- Aucun changement du code Android ; compilation non effectuée.

## 11.9.5 — 10/10/2026
- Tableau de bord de 14 barres intégré dans l’application, accessible par appui sur le titre ROADGUARD.
- Visionneuse de la maquette originale embarquée en asset.
- Source modifiée, compilation Android non effectuée.

## 11.9.6 — 10/10/2026
- Barres et commentaires de suivi lus dans le JSON intégré à l'application (source unique au runtime).
- Version du suivi affichée, maquette et dossier de passation conservés.
- Pas de modification du rendu carte ni des fonctionnalités PMTiles.

## 11.9.7 — 10/10/2026
- Ajustement du filtre CSS de nuit et du contraste des panneaux dans `map.html`.
- Suivi JSON actualisé, valeurs inchangées en attente de test Xiaomi.
- versionCode 128 ; pas de compilation ni de validation sur appareil.

## 11.9.8 — 10 octobre 2026
- HUD de navigation bleu nuit, meilleur contraste et ajustement des petits écrans.
- Maquette et 14 chantiers conservés. Compilation non testée.

## 11.9.9 — 10/10/2026
- Contrôle PMTiles v3 plus strict : offsets des quatre sections, types, compressions, protections de débordement.
- Neuf tests unitaires Python. Lecteur PMTiles non intégré.

## 11.10.0 — 10/10/2026
- Prototype lecteur PMTiles raster local, couche Leaflet, choix pack dans bibliothèque.
- Test JVM sur archive synthétique, 3 assertions ; Android non compilé.
- Pas de lecteur vectoriel ni de pack France.

## 11.10.1 — 10/10/2026
- Aperçu PMTiles raster raccordé à la carte locale sans connexion réseau ; possibilité de désactiver.
- Contrôles de câblage source. Pas de test Android ni de lecteur vectoriel.

## 11.10.2 — 10/10/2026
- Contrôles anti-overflow du lecteur PMTiles Java.
- 13 nouveaux contrôles d'intégration avec archives synthétiques (gzip et leaf compris), 3 contrôles historiques validés.
- Pas de compilation Android, pas de carte France réelle.

## 11.10.4 — 10/10/2026
- Réponses 404 locales pour tuiles PMTiles/XYZ absentes ou incorrectes, pour éviter le fallback réseau.
- Contrôles statiques de non-régression ajoutés (7 réussis).
- Tests Java PMTiles réussis ; aucune compilation Android ni validation Xiaomi.

## 11.10.6 — 10/10/2026
- Aperçu MapLibre expérimental avec tuiles vectorielles PMTiles locales, moteur toujours en ligne (CDN).
- Sélection du pack depuis la bibliothèque et indicateur orange sur chantier actif.
- Tests Java et syntaxe JS réussis ; rendu réel non validé.

## 11.10.8 — 10/10/2026
- Références MapLibre locales, script de récupération version 4.7.1, garde de compilation.
- BLOQUÉ : assets MapLibre absents du ZIP (DNS indisponible), compilation impossible sans exécuter le script en ligne.

## 11.10.9 — 10/10/2026
- Gradle télécharge automatiquement MapLibre 4.7.1 (JS/CSS) au premier build en ligne, avec CDN de secours, délais et taille maximum.
- Les builds suivants réutilisent les fichiers locaux ; échec explicite sans connexion au premier build.
- Tests statiques et Java réussis ; APK et Xiaomi non testés.

## 11.11.0 — 10/10/2026
- Correction des libellés de bibliothèque PMTiles et du statut après rechargement du style vectoriel.
- Tests de régression Java/JS ; pas de compilation Android ni de rendu réel validé.

## 11.11.1 — 10/10/2026
- Correction erreur Java CodeAssist : `lowestZoom` / `highestZoom` non définis dans `importTileZip`.
- Bornes zoom XYZ calculées et enregistrées. Compilation Android à retester.

## 11.11.2 — Suivi GPS et diagnostic carte
- Premier fix GPS recentre la carte, même à l'arrêt.
- Redimensionnement WebView/Leaflet et recalcul des tuiles.
- Message explicite en cas d'échecs répétés de chargement des tuiles.
- Position réseau de secours si disponible ; vérification JS WebView.
- Tests statiques seulement, compilation CodeAssist/Xiaomi non effectuée.

## 11.11.3 — 10/10/2026
- Leaflet viewport plein écran, observateur de redimensionnement, alerte réseau plus fiable, suppression HUD HTML doublon.
- Tests statiques/PMTiles/JS réussis ; validation Xiaomi à faire.

## 11.11.4 — 10/10/2026
Anticipation de tuiles Leaflet en avant du GPS, buffer agrandi, limites de concurrence et fréquence. Test réel requis.

## 11.11.5 — 10/10/2026
Préchargement 5×5 local raster PMTiles, priorité centrale, limites de concurrence, préchargement réseau public désactivé pour conformité.

## 11.11.6 — 10/10/2026
Filtrage sauts GPS Java/Leaflet et réduction des animations concurrentes de caméra. Test Xiaomi nécessaire.

## 11.11.7 — 10/10/2026
Interpolation visuelle GPS requestAnimationFrame, caméra synchronisée, anti-conflit des animations. Tests réels requis.

## 11.11.8 — 10/10/2026
Interpolation GPS linéaire, durée ajustée à la cadence GPS, suivi caméra plus fréquent. Test Xiaomi requis.

## 11.11.9 — 10/10/2026
Mode expérimental route vers le haut, rotation progressive de panes, point GPS décalé vers le bas.

## 11.12.0 — 10/10/2026
Caméra suivie avec amortissement temporel en pixels, recalcul jusqu’à 60 FPS et remise à zéro sur zoom/reprise. Test Xiaomi requis.

## 11.12.1 — 10/10/2026
Compensation subpixel GPU de la caméra Leaflet ; tests statiques et JS OK ; test Xiaomi requis.

### 11.12.8 — Téléchargement direct sécurisé (2026-10-10)
- Ajout d'un formulaire dans Bibliothèque de cartes pour installer un pack HTTPS autorisé sans catalogue central.
- Contrôle de l'URL, du format ZIP XYZ / PMTiles, de la taille exacte et de l'empreinte SHA-256 ; réutilisation du téléchargement avec annulation et installation sécurisée.
- Aucune carte France hébergée ou fournie ; aperçu vectoriel PMTiles expérimental ; navigation hors ligne complète indisponible.
- Base stable 11.12.7 conservée pour GPS, rotation, caméra et anti-flashs.


## 11.12.9 — Bouton France en un clic
Ajout du bouton principal « Télécharger la carte de France », sans saisie technique. Vérification des métadonnées du catalogue avant téléchargement. En absence de pack France hébergé, message explicite, aucun faux téléchargement. Options techniques déplacées dans un menu avancé. Base 11.12.7 de carte préservée. Compilation et tests Xiaomi non effectués.

## 11.13.0 — Catalogue multi-pays
Choix de 28 pays, recherche et raccordement au catalogue HTTPS existant. Aucun téléchargement fictif, aucun pack livré. Carte/GPS inchangés.

## 11.13.1
Auto-rafraîchissement catalogue HTTPS une fois par processus; scripts de préparation PMTiles pour 10 pays et catalogue SHA-256. Aucun pack hébergé, moteur vectoriel encore expérimental.

## 11.13.3 — Raccordement contrôlé du pack France
- Le bouton en un clic accepte les entrées `scope=country` uniquement; les packs régionaux sont écartés.
- Le générateur permet de déclarer explicitement une couverture nationale après vérification, et marque les autres données comme régionales.
- Attribution affichée pour la source OpenMapTiles/OpenStreetMap.
- Aucun binaire/cataloque France ni serveur n'est fourni; téléchargement et rendu réel restent bloqués/non testés.


## Retour Work — audit de la source 11.13.2 (10/10/2026) — supersédé par 11.13.3
- Assertions de test historique mises à jour pour la source 11.13.2 ; compilateur Java lancé par le module disponible.
- 54 tests Python OK ; contrôles JS et essais PMTiles synthétiques OK.
- Note historique : cette livraison n’avait pas modifié le code applicatif. Le correctif de catalogue est livré en 11.13.3 / versionCode 164.
- Aucun pack/cataloque HTTPS publié, pas de build Android, et rendu Xiaomi hors réseau non testé. Détails dans `RETOUR-WORK-ROADGUARD-CARTES.md`.


## 11.13.4 — source publique de carte France
- Bouton France relié à `https://map.comptoir.net/france-openmaptiles.pmtiles` comme candidat public; aucun catalogue Roadguard à saisir.
- Contrôles avant installation : TLS/host fixe, taille HEAD ≤2 Gio, espace disque, structure PMTiles v3 et emprise d’en-tête correspondant à France métropolitaine + Corse; empreinte SHA-256 calculée et stockée localement.
- La page source annonce des PMTiles France au schéma OpenMapTiles et crédite OpenStreetMap; le binaire réel n’a pas pu être inspecté depuis cet environnement. Téléchargement et rendu Xiaomi restent non testés.


## 11.13.5 — correction du parcours de sélection des pays
- Ferme le sélecteur avant de lancer l’action pays; la recherche différée suit le même ordre pour éviter les dialogues empilés.
- Ajout d’une protection/log d’erreurs autour des actions lancées après fermeture.
- 59 tests Python OK. Le plantage affiché sur la capture reste à confirmer avec une APK recompilée sur le Xiaomi.


## 11.13.6 — correctif SecurityException au clic pays
- La capture Xiaomi montrait `SecurityException` pendant le contrôle de connectivité. Ajout de `android.permission.ACCESS_NETWORK_STATE`, nécessaire à la lecture de l’état réseau.
- Le dialogue pays est fermé avant l’action (correctif maintenu).
- 60 tests Python OK. Confirmation sur une APK installée reste à faire.

## 11.13.7 — autorisation des téléchargements sur réseau mobile
- 4G/5G autorisée par défaut pour la France et les packs du catalogue ; réglage accessible pour forcer le Wi-Fi.
- Avant transfert mobile, la confirmation affiche la taille annoncée et prévient de l'utilisation du forfait.
- Mise à jour du cahier des charges et ajout d'un test de régression du comportement réseau. APK et essai Xiaomi restent à compiler/valider.


## 11.13.9 — limite PMTiles pays
- Plafond PMTiles porté à 8 Gio avec compteurs `long`; XYZ reste plafonné à 600 Mio.
- Taille serveur par HEAD/Range contrôlée; le ZIP d'entrée 11.13.10 contenait encore des tests et notes de versions précédentes, corrigés dans le présent paquet.

## 11.13.10 — rendu PMTiles hors ligne (source, non validée appareil)
- MapLibre GL JS/CSS 4.7.1, ressources vérifiées par SHA-256 et licence BSD incluses; polices Noto Sans et OFL incluses.
- Lecteur PMTiles prend en charge Brotli via jar local MIT; tests synthétiques de tuiles Brotli ajoutés.
- Couches locales de routes/noms, glyph endpoint avec CORS, détection au démarrage de l'archive France déjà présente et import SAF d'une archive existante sans réseau.
- Pas de nouveau téléchargement des ~2,76 Go rapportés sur le Xiaomi. Fichier réel non joint; compilation Android et validation avion/Xiaomi restent BLOQUÉES/NON TESTÉES. GPS/caméra principaux non modifiés.


## 11.14.105 — 15 octobre 2026
CTA Trajet visible même sans destination; état vide Radars en carte dédiée. Progression estimée du code : global +1, visuel +2, itinéraires +2, radars +2. Pas de compilation ni test Xiaomi.


## 11.14.106 — 15 octobre 2026
Écran Signaler : panneau de confidentialité compacte et état vide de l’historique moins haut; actions inchangées. Estimations code : global +1, architecture visuelle +1, Signalements +2. Pas de build/test Xiaomi.


## 11.14.107 — 15 octobre 2026
Lot visuel multi-écrans : cartes et résumé de réglages en pastilles; Radars affiche un compteur aligné sur les points visibles/valides; Itinéraires différencie l’attente, la destination et le trajet calculé; composants communs harmonisés sur Accueil et Signaler. Estimations code : global 44→46 (+2), architecture visuelle 28→34 (+6), détail des écrans +20 points cumulés. Tests statiques et Python ciblés; pas de compilation ni validation Xiaomi. GPS et caméra inchangés.


## 11.14.108 — 15 octobre 2026
Écran Cache cartographique : statut d’activation, couverture mesurée uniquement si le GPS et une source valide le permettent, inventaire en tuiles/Mio et dernière activité. Détails techniques repliables et limites de couverture explicites. Progression du code estimée : global 46→47 (+1), architecture visuelle 34→35 (+1); fonctionnalité de cache reste à 0 % sans source autorisée. Pas de compilation ni validation Xiaomi.


## 11.14.109 — 15 octobre 2026
Accueil : badge GPS dynamique (permission requise, dernière position récente ou attente), ouvre la carte au toucher. Le statut du cache n’utilise plus une position ancienne en mémoire pour annoncer une couverture. Estimations : global 47→48 (+1), interface +1, architecture +2, Accueil +2. Cache fonctionnel toujours inactif sans source permise; aucun build ni test Xiaomi.


## 11.14.110 — 15 octobre 2026
Plafond de cache temporaire de 1–1024 Mo configurable (95 Mo par défaut); la limite de 100 MB par appareil documentée par Stadia Maps est appliquée sur ses hôtes. Note d’abonnement actif et attestation du fournisseur ajoutées au réglage; pas de compte/clé intégré. Estimations code : global 48→49 (+1), interface +1, visuel +1, planification +1. Test Java préparé, non exécuté faute de javac; pas de compilation Android ni de test Xiaomi.

## 11.14.111 — finition cockpit graphite/rouge

Cockpit carte et commandes harmonisés autour de la palette ROADGUARD, avec panneau de destination et gestes conservés. Pas de changement du moteur GPS, des sources de carte ou des tuiles. Estimations : global 49→50, interface 69→71, architecture visuelle 38→41. APK et validation Xiaomi non effectuées.

## 11.14.112 — refonte de structure du cockpit

Suite au retour que la passe couleur précédente ne ressemblait pas suffisamment à un GPS Waze-like, cockpit réorganisé : destination toujours visible, tiroir deux états avec raccourcis Domicile/Travail/Favoris, rail orientation/signalement/recentrage, vitesse flottante et itinéraire rouge. GPS bleu préservé. Estimations : global 50→51, Interface 71→74, Architecture visuelle 41→44, Navigation jour/nuit 48→52, Itinéraires 48→49. Source seulement; pas de compilation.

## 11.14.113 — finition visuelle des six écrans
Tous les écrans natifs reçoivent une finition commune graphite/rouge, l’accueil recentre l’action sur la carte, les marges protègent le contenu de la barre d’onglets, la position GPS bleue est distincte du trajet rouge sur MapLibre et hors ligne, et la carte raster est désaturée modérément en mode nuit. Pourcentages d’implémentation visuelle : interface et architecture 100 %, six écrans 100 %. Global estimé 51→56 (+5), car les fonctions métier restent incomplètes. Contrôles statiques/Python effectués; APK non compilée et rendu Xiaomi en attente.

## 11.14.114 — retouches après captures utilisateur
Après retour que le rendu 11.14.113 est encore trop plat, remplissages des cartes éclaircis vers un graphite plus différencié, élévation discrète sur actions, grille Signalements ramenée à 76 dp, barre de défilement masquée et marge basse portée à 104 dp. Les notes évaluent interface/architecture visuelle à 92 %, Signalements et Paramètres à 95 %, en attendant la comparaison compilée. Global estimé 56 % (stable). Pas de build Xiaomi.

## 11.14.115 — badges de signalement et contraste
Après les captures montrant des bandes rouges sur les catégories de danger, glyphs fixés à 46×38 dp et centrés dans les tuiles; graphite et bordures affinés, hauteur tuile 82 dp. Interface/architecture estimées 92→95 %, Signaler 95→98 %, Paramètres 95→97 %, global stable à 56 %. Tests ciblés et syntaxe JS; aucune compilation ni vérification Xiaomi.

## 11.14.116 — finition commune des six écrans
Composants d’action, graphite, titres de groupes, badges, états de route et onglets harmonisés. Interface/architecture estimées 95→100 %, Signalements 98→100 %, Paramètres 97→100 %, global stable à 56 %. Tests ciblés, syntaxe JS et ZIP contrôlés. Pas d’APK ni de capture Xiaomi : 100 % correspond à l’implémentation source.

## 11.14.117 — robustesse navigation et cache
Requête OSRM durcie par validation des coordonnées, endpoint fixe, réponse JSON et taille/métriques bornées; états pending nettoyés si succès, erreur ou destination modifiée. Validation de source et politique Stadia resserrées. Estimations : global 56→57, navigation 35→40, plan cache 71→74, secours coupure 30→35, radars 30→35. Préflight confirme SDK 34/Gradle/wrapper absents; pas d’APK. Caméra non touchée.


## 11.14.118 — cache cartographique sans préchargement
- Version source 11.14.118 / versionCode 318; archive source, APK non compilée.
- Suppression du préchargement GPS, des réglages cache/fournisseur et du chargement directionnel/hors écran.
- Carte en ligne conservée avec cache Android/WebView normal des tuiles demandées; aucune garantie hors réseau.
- Build et vérification Xiaomi requis; caméra non modifiée et toujours prévue pour la fin.


## 11.14.119 — validation des itinéraires
- Version source 11.14.119 / versionCode 319; archive source, APK non compilée.
- Coordonnées et métriques d’itinéraire vérifiées; géométrie trop longue rejetée plutôt que tronquée.
- Navigation estimée +5 points (40→45 %); tableau de bord in-app affiche maintenant les deltas et barres vertes. Global estimé +1 (57→58 %), non audité.
- Cache en ligne sans préchargement; caméra intacte et toujours à terminer en dernier.

## ROADGUARD 11.14.120 — fiabilisation radars locaux

- Version 11.14.120 / code 320, archive source uniquement.
- Import dangers : rejet coordonnées NaN/infini, hors bornes et labels vides/trop longs.
- Expiration contrôlée dès chargement et ouverture Alertes, en plus du suivi GPS et arrière-plan.
- Radars estimés +5, global estimé +1; estimation non auditée.
- Aucun changement à la caméra, au GPS bleu ou aux écrans graphiques. Préchargement reste retiré.


## ROADGUARD 11.14.121 — base officielle radars France

- Version 11.14.121 / code 321, source uniquement.
- Dataset ministère de l’Intérieur via data.gouv.fr : 3 309 points, 7 types, 30/12/2025, Licence Ouverte 2.0.
- Intégré carte et alertes; points personnels séparés.
- Radars estimés 40→55 % (+15), global 59→63 % (+4), non audités. APK/Xiaomi à valider.
- Caméra et graphisme non modifiés; pas de préchargement.


## ROADGUARD 11.14.122 — index des alertes radar

- Version 11.14.122 / code 322, archive source sans APK.
- Correction des colonnes GPS du dataset lues par le service d’alertes écran verrouillé.
- Index spatial foreground/background; 25 000 cas de couverture simulés et chargement des radars officiels optimisé.
- Radars +5 (55→60 %); global +1 (63→64 %), estimation non auditée.
- Navigation GPS, présentation et caméra inchangées; aucun préchargement.


## ROADGUARD 11.14.121 — base officielle radars France

- Version 11.14.121 / code 321, source uniquement.
- Dataset ministère de l’Intérieur via data.gouv.fr : 3 309 points, 7 types, 30/12/2025, Licence Ouverte 2.0.
- Intégré carte et alertes; points personnels séparés.
- Radars estimés 40→55 % (+15), global 59→63 % (+4), non audités. APK/Xiaomi à valider.
- Caméra et graphisme non modifiés; pas de préchargement.


## ROADGUARD 11.14.123 — affichage radar allégé

- 3 309 points conservés pour le guidage; marqueurs rendus seulement dans la fenêtre cartographique.
- Popups distinguent base officielle et points locaux; dédoublonnage de points importés à proximité.
- Estimation radars +5 (60→65 %), global +1 (64→65 %), non audités.
- Pas de build Xiaomi; caméra, GPS et politique sans préchargement conservés.


## ROADGUARD 11.14.124 — compteurs par provenance

- Compteur officiel indépendant des types similaires dans les données utilisateur.
- Compteurs de filtres personnels calculés sur les entrées valides et non expirées.
- Les radars officiels restent actifs pour les alertes quel que soit le filtre personnel.
- Estimations radar +3, global +1; source non compilée.


## ROADGUARD 11.14.125 — précision des alertes GPS

- Les alertes au premier plan ignorent désormais les positions sans précision GPS connue, comme le service d’alerte en arrière-plan.
- Seuil commun : précision annoncée de 60 m maximum; anti-doublon persistant conservé.
- Tests statiques ciblés passés; compilation et validation Xiaomi restent à faire.
- Estimations radars 68→69 % (+1), global 66→67 % (+1), non auditées.


## ROADGUARD 11.14.126 — classification du radar mobile

- « Contrôle mobile (2 h) » est classé comme radar par le suivi au premier plan et en arrière-plan.
- Portée progressive jusqu’à 5 km et double bip rétablis pour ce type.
- Test statique dédié ajouté; compilation et validation Xiaomi restent à faire.
- Estimations radar 69→71 % (+2), global 67→68 % (+1), non auditées.


## ROADGUARD 11.14.127 — politique radar partagée

- Centralisation de la classification radar, du filtre fixe/mobile et de la validation des labels.
- Les données personnelles sans type valable ne sont plus transformées en radar fixe dans le service arrière-plan.
- Tests statiques ciblés passés; compilation et Xiaomi restent requis.
- Estimations radar 71→72 % (+1), global 68→69 % (+1), non auditées.


## ROADGUARD 11.14.128 — correctif de compilation

- Imports `java.io.BufferedReader` et `java.io.InputStreamReader` ajoutés dans le service d’alertes arrière-plan.
- Corrige les symboles Java introuvables rapportés par CodeAssist à la ligne 47.
- Pas de changement fonctionnel ni de progression estimée; rebuild CodeAssist à relancer.


## ROADGUARD 11.14.129 — recherche inline sur la carte

- Le champ de destination du cockpit Carte accepte la saisie et le lancement clavier.
- Adresses et coordonnées affichent leurs résultats dans le panneau sans changer d’écran.
- La sélection enregistre la destination et lance le calcul si le GPS est disponible.
- Estimations navigation 45→48 % (+3), global 69→70 % (+1); géocodage et Xiaomi restent à vérifier.


## ROADGUARD 11.14.130 — bases radar téléchargeables par pays

- Ajout d’un catalogue de pays HTTPS configurable dans l’écran Alertes; seules les bases publiées sont listées.
- Taille et SHA-256 vérifiés; CSV versionné validé, provenance/licence exigées, installation atomique en stockage privé.
- Les bases installées alimentent les alertes de la carte et le service GPS arrière-plan même hors connexion.
- Recherche inline Carte de 11.14.129 conservée.
- Estimations radars 72→76 % (+4), global 70→71 % (+1), non auditées. Pas de fournisseur international hébergé; build Xiaomi à effectuer.


## ROADGUARD 11.14.131 — générateur de catalogue radar

- Outil Python ajouté pour mapper des colonnes CSV nationales, filtrer lignes non radar/coordonnées invalides, produire les CSV compatibles et créer le catalogue avec taille exacte et SHA-256.
- Tests sur données synthétiques couvrent l’empreinte, la normalisation, le filtrage et le rejet des liens HTTP.
- Les flux de téléchargement et d’alertes hors connexion de 11.14.130 sont conservés; catalogue réel toujours à héberger.
- Estimations radars 76→77 % (+1), global stable à 71 %, non auditées; APK et Xiaomi à valider.


## ROADGUARD 11.14.132 — liste des pays et gestion des bases

- L’écran Alertes affiche les pays habituels même sans catalogue, avec leur état publié/non publié; la France signale sa base déjà intégrée.
- Les pays du catalogue qui ne figurent pas dans la liste habituelle sont ajoutés automatiquement.
- Les bases déjà installées peuvent être mises à jour ou supprimées depuis le sélecteur.
- Estimations radars 77→78 % (+1), global stable à 71 %, non auditées; catalogue réel et tests Xiaomi à faire.


## ROADGUARD 11.14.133 — téléchargement radar contrôlable

Progression visible, affichage des octets reçus, annulation avec conservation de l’ancienne base et verrou contre les téléchargements concurrents. Les réponses tardives de catalogue sont ignorées et l’entrée est revérifiée avant l’installation. Estimations radars 78→79 %, global 71→72 %; non auditées. Tests ciblés; aucune APK/Xiaomi.


## ROADGUARD 11.14.134 — interruption immédiate du téléchargement radar

Le bouton Annuler déconnecte maintenant la requête HTTP active; les exceptions déclenchées par cette fermeture sont traitées comme une annulation. Le fichier partiel est effacé et l’ancienne base reste installée. Estimation radars 79→80 % (+1); global stable à 72 %, non audité. Tests ciblés statiques; APK et essai Xiaomi à faire.


## ROADGUARD 11.14.135 — recherche auto et import catalogue radar

La recherche carte démarre 750 ms après la saisie, garde le clavier ouverts, ignore les réponses obsolètes et classe les candidats par correspondance textuelle. Ajout d’un import de catalogue JSON local avec les mêmes validations de sécurité que le catalogue HTTPS. Aucun pack international fourni; Geocoder/Xiaomi non testé. Estimations navigation 48→50 %, radars 80→81 %, global 72→73 %, non auditées.


## ROADGUARD 11.14.136 — clarification radar

Le compteur local est désormais clairement réservé aux points ajoutés manuellement, séparé de la base officielle française de 3 309 entrées. Suppression du bouton de configuration de catalogue vide; import JSON documenté conservé. Le sélecteur indique que les avertissements radar ne sont pas proposés en Suisse. Aucun pack étranger réel n’est inclus. Estimations radars 81→82 % et global 73→74 %, non auditées; aucune APK compilée.


## ROADGUARD 11.14.137 — fournisseurs radar

Ajout dans Alertes d’une liste de fournisseurs inspirée de la capture RadarDroid : Lufop, SCDB.info, GPS Data Team et Kostenlos DB. Les deux premières fiches résument les informations vérifiées et ouvrent leur site. Les deux autres sont marquées explicitement comme non vérifiées pour ROADGUARD. L’interface explique que les ZIP Radardroid et les fichiers SCDB ne sont pas automatiquement importables et ne sont pas intégrés. Estimations radars 82→83 % et global 74→75 %, non auditées; source non compilée.


## ROADGUARD 11.14.139 — téléchargements radar depuis l’app

Ajout d’un parcours Lufop API dans Alertes → Bases radar par pays : configuration de clé, téléchargement HTTPS par pays, conversion JSON vers CSV ROADGUARD, validation des points, installation/mise à jour/suppression locale et attribution ODbL. Les réponses vides, celles atteignant 200 (Free), 10 000 (Pro) ou 30 000 (Business) sont refusées pour éviter une base incomplète. La couverture dépend de la clé et du forfait; aucun appel API réel ni compilation Android n’a été fait. La France embarquée reste active; Suisse désactivée. Tests ciblés réussis. Estimations non auditées : radars 83→87 %, global 75→76 %. Version source uniquement, code 338.


## ROADGUARD 11.14.140 — base suisse téléchargeable

Le sélecteur autorise maintenant le téléchargement et le stockage de la base suisse par Lufop ou catalogue ROADGUARD. Les chargeurs de radars premier plan et service background ignorent les bases dont le code pays commence par CH, et l’écran précise que ces données ne sont pas affichées ni utilisées pour les alertes. Cela distingue l’acquisition locale de l’activation de l’avertisseur. Tests statiques et de régression ciblés; aucune compilation ni validation Xiaomi. Version source uniquement, code 340. Estimations non auditées : radars 87→88 %, global 76→77 %.


## ROADGUARD 11.14.141 — base pays sans clé

Ajout d’un téléchargement OpenStreetMap/Overpass sans compte ni clé API, par pays, des points cartographiés highway=speed_camera. Stockage local CSV, attribution © OpenStreetMap contributors et licence ODbL 1.0. Couverture contributive, non exhaustive, sans garantie de radars mobiles; limites de disponibilité Overpass. Lufop reste optionnel. Suisse stockable mais exclue des alertes et de la carte. Tests statiques/Python; pas de compilation ni test téléphone. Estimations non auditées radars 88→89 %, global 77→78 %. Code 341.


## ROADGUARD 11.14.142 — reprise automatique Overpass

Après le HTTP 502 observé lors d’un téléchargement, le client tente jusqu’à trois instances publiques Overpass en séquence, sans lancer de requêtes concurrentes. L’annulation, les limites de réponse, la validation CSV et la conservation de la base précédente sont maintenues. Tests source à exécuter; compilation et test Xiaomi non effectués. Source seule, code 342. Estimations non auditées : radars 89→90 %, global 78→79 %.


## ROADGUARD 11.14.143 — serveurs et sources radar

Après l’erreur DNS `overpass.nchc.org.tw`, l’instance a été retirée et remplacée par des instances globales visibles dans la documentation actuelle d’OpenStreetMap. L’écran Fournisseurs présente aussi la base française Ministère (déjà intégrée, Licence Ouverte 2.0) et Open GATSO POI (lien seul, licence agrégée non confirmée). Aucun fichier tiers ambigu empaqueté. Source seule, code 343; tests statiques, compile/téléchargement Xiaomi à faire. Estimations non auditées radars 90→91 %, global 79→80 %.

## ROADGUARD 11.14.144 — base radar MapAtlas multi-pays

Ajoute MapAtlas comme téléchargement direct intégré : catalogue des 106 pays/régions, choix d’un pays, récupération du GeoJSON, conversion et stockage CSV local. Catégories intégrées : radar fixe, feu rouge, vitesse moyenne et variable. Les bases restent séparées par source, portent attribution MapAtlas et licence CC BY 4.0; l’installation est validée et remplaçable sans perdre la base précédente en cas d’échec. Base communautaire non officielle, snapshot 2024-05-21, exhaustivité non garantie. Code 344. Tests de source à exécuter, compilation/test Xiaomi non effectués. Estimations non auditées radars 91→92 %, global 80→81 %.


## ROADGUARD 11.14.145 — Lufop Radardroid direct

Ajoute dans les bases radar par pays le téléchargement direct depuis les URL Lufop Radardroid, sans clé API ni ZIP manuel. Le CSV est converti en format local pour les types fixes, mobiles, feux rouges, sections de vitesse moyenne, zones radar et passages à niveau. Redirections HTTPS limitées au domaine Lufop, réponse et nombre de lignes bornés, coordonnées validées, base remplacée uniquement après validation et attribution ODbL conservée. Les bases restent séparées par fournisseur; Suisse téléchargée mais non activée dans ROADGUARD. Source courante mise à jour par la communauté selon Lufop; exhaustivité non garantie. Code 345; compilation et Xiaomi à faire. Estimations non auditées radars 92→93 %, global 81→82 %.


## ROADGUARD 11.14.146 — clarifie le choix des sources radar

Après la capture montrant uniquement « MapAtlas · pays et bases disponibles », le sélecteur indique maintenant dans le titre qu’il s’agit du choix d’un pays et, sur chaque ligne, affiche MapAtlas, Lufop Radardroid et OpenStreetMap. Toucher un pays ouvre ensuite le choix détaillé des fournisseurs. Le téléchargement Lufop Radardroid sans clé de la version 11.14.145 est conservé. Code 346. Estimations non auditées inchangées : radars 93 %, global 82 %. Compilation/téléphone à faire.


## ROADGUARD 11.14.148 — menu latéral et détail radar

Ajoute un menu latéral relié aux écrans existants et ouvre la recherche destination directement sur la carte. Chaque radar proche présente maintenant son type, sa distance selon le GPS et ses coordonnées; un bouton recentre la carte sur le point. Les pictogrammes varient selon le type reconnu. Le service de démonstration ne retourne qu’un seul itinéraire, les alternatives visuelles de la maquette ne sont donc pas simulées. Source seule, code 348; compilation et captures Xiaomi à faire.


## ROADGUARD 11.14.149 — options d’itinéraire réelles

Demande jusqu’à trois alternatives supplémentaires au service OSRM. Les routes réellement reçues sont validées, classées, présentées à l’utilisateur, sélectionnables et conservées avec le trajet. Si le moteur n’en renvoie qu’une, ROADGUARD ne crée pas de faux choix. Tests statiques; compilation et vérification réseau/Xiaomi à faire. Source seule, code 349. Estimations non auditées navigation 52→58 %, global 83→84 %.


## ROADGUARD 11.14.150 — grille de signalement à 12 catégories

Réorganise l’écran de signalement en trois colonnes et douze catégories proches de la maquette : radar fixe/mobile, contrôle police, accident, danger, travaux, véhicule arrêté, bouchon, route fermée, objet, météo/pluie et autre. Confirmation et stockage local conservés. Source seule, code 350; compilation et capture Xiaomi à faire. Estimations non auditées : global 84→85 %.


## ROADGUARD 11.14.151 — visibilité des marqueurs radar

Ajoute un réglage persistant dans Apparence pour masquer ou réafficher les marqueurs radar de la carte, raccordé à Leaflet et MapLibre. Les alertes vocales natives restent actives. Tests statiques; compilation et capture Xiaomi à faire. Source seule, code 351.


## ROADGUARD 11.14.152 — taille des icônes radar et événements

Ajoute un réglage Petite/Normale/Grande persistant dans Apparence. Le rendu se met à l’échelle sur Leaflet, MapLibre et la carte locale hors connexion. La visibilité des radars reste indépendante des alertes vocales. Tests statiques et syntaxe JS; compilation/Xiaomi à faire. Source seule, code 352.
# ROADGUARD 11.14.157 — navigation 100 % côté code

- Le module Navigation est maintenant compté à 100 % côté code, conformément à la demande utilisateur.
- Recherche, calcul/choix d’itinéraires, sauvegarde/reprise, consignes OSRM, suivi segmentaire, recalcul prudent et guidage vocal sont documentés comme présents.
- Compilation et essais Xiaomi reportés après cette livraison; aucun APK produit.
- versionName 11.14.157 / versionCode 357.

# ROADGUARD 11.14.164 — correction du recentrage GPS

- La pause caméra après glissement est désormais indépendante du booléen de suivi GPS dans MapLibre.
- Leaflet suspend au `dragstart` sans dépendre de `originalEvent`, parfois absent dans WebView.
- Le point GPS continue de suivre les positions; recentrage uniquement par le bouton ⌖.
- Tests de régression statiques ajoutés pour MapLibre et Leaflet; aucun APK compilé.
- versionName 11.14.164 / versionCode 364.

# ROADGUARD 11.14.165 — correctif import Lufop

- Correction de la référence `baseType` inexistante dans le sélecteur des entrées CSV/ASC : utilisation de `type` calculé à partir du nom de fichier.
- Ajout d’un contrôle de régression ciblé sur l’import ZIP Lufop.
- Pause caméra GPS de la version 11.14.164 conservée.
- versionName 11.14.165 / versionCode 365. Pas de compilation Android lancée.

