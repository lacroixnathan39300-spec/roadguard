# ROADGUARD 11.14.244 — avancement pondéré

Référence des deltas : version 11.14.243 (développement 78,0000 %, validation 27,5000 %). Les **48,0000 % historiques restent une estimation non auditée**, séparée de ce calcul.

39 tâches explicites totalisent 100 points. Le développement compte les tâches codées selon leur contrat ; une tâche partielle reste à faire. La validation additionne seulement les contrôles réussis, pondérés à l’avance. Les essais matériels ont leur propre poids et restent non validés.

| Module | Poids | Développement | Validation | Δ développement (pts du module) | Δ validation (pts du module) |
|---|---:|---:|---:|---:|---:|
| Carte / GPS | 20,0000 | 100,0000 % | 42,5000 % | 0,0000 | 7,5000 |
| Alertes | 20,0000 | 85,0000 % | 20,0000 % | 0,0000 | 0,0000 |
| Sources / activation radar | 25,0000 | 76,0000 % | 40,0000 % | 0,0000 | 0,0000 |
| UI / ergonomie | 10,0000 | 80,0000 % | 0,0000 % | 0,0000 | 0,0000 |
| Android Auto | 10,0000 | 50,0000 % | 0,0000 % | 0,0000 | 0,0000 |
| Qualité / tests / compilation | 10,0000 | 40,0000 % | 20,0000 % | 0,0000 | 0,0000 |
| Sécurité / données | 5,0000 | 100,0000 % | 90,0000 % | 0,0000 | 0,0000 |
| **TOTAL pondéré** | **100,0000** | **78,0000 %** | **29,0000 %** | **0,0000** | **1,5000** |

État de la version 11.14.243 : développement **78,0000 %**, validation **27,5000 %**. Une validation automatique ne constitue pas un essai Xiaomi ou Android Auto.

**Chantier actif :** Démarrage carte sans attente du catalogue, reprise des tuiles et pack graphique local réalisés ; validation Android à regrouper en fin de lot. Import 7z automatique en attente des deux archives FR/CH et des cinq JAR demandés.

## Backlog et preuves

| ID | Tâche | Poids | État | Contrôles réussis / poids | Preuve code et test |
|---|---|---:|---|---:|---|
| M1 | Démarrage local sans attendre les mises à jour réseau | 3,0000 | codé | 0,0000 / 3,0000 | MainActivity : carte avant catalogue ; catalogue/surcouches assemblés sur worker puis publication complète ; CommunitySync.prepareStartup sans attente de réseau ni de cache ; StartupRuntimeTest avec worker bloqué ajouté mais non exécuté. Aucun point Android natif ajouté. ;  |
| M2 | Filtrage de la vitesse, du cap et des sauts GPS | 3,0000 | testé partiellement | 2,0000 / 3,0000 | GpsMotion; MainActivity.startGPS ; verification/java-policy-tests-11.14.241.log ; réexécuté : verification/targeted-checks-11.14.242.log ; réexécuté : verification/targeted-checks-11.14.243.log ; réexécuté : verification/targeted-checks-11.14.244.log |
| M3 | Ancrage horizontal sous rotation et hauteur réglable | 4,0000 | testé partiellement | 3,0000 / 4,0000 | camera_geometry.js; rgVisualCamera; rgCameraOffset; MainActivity.pushPosition ; verification/camera-tests-11.14.241.log ; réexécuté : verification/targeted-checks-11.14.242.log |
| M4 | Exploration manuelle et recentrage volontaire | 2,0000 | testé partiellement | 1,0000 / 2,0000 | resumeCamera; rgManualZoom; showRouteOverview ; verification/camera-tests-11.14.241.log ; réexécuté : verification/targeted-checks-11.14.242.log |
| M5 | Zoom adaptatif avec hystérésis | 2,0000 | testé partiellement | 1,0000 / 2,0000 | camera_motion.js; updateGPS ; verification/camera-tests-11.14.241.log ; réexécuté : verification/targeted-checks-11.14.242.log |
| M6 | Cache temporaire de tuiles, limites et récupération | 3,0000 | testé partiellement | 1,5000 / 3,0000 | VisibleTileCache : cache HTTP, budget partagé 4 Gio, réserve 512 Mio, compteurs et reprise après retour du réseau ; tile_recovery.js et map.html : quatre reprises bornées des échecs visibles, conservation des bonnes tuiles. Callback réseau Android ajouté, non essayé sur appareil. ; verification/targeted-checks-11.14.244.log : visible-tile-cache (17 scénarios, fichiers réels, HTTP injecté) et tile-recovery-and-pack (11 scénarios JS/DOM/Leaflet adaptés ; aucun WebView réel). |
| M7 | Itinéraire, cache et reprise du guidage | 3,0000 | codé | 0,0000 / 3,0000 | RouteRequestPolicy; MainActivity navigation methods ;  |
| A1 | Refus du GPS périmé et effacement des avertissements | 3,0000 | codé | 0,0000 / 3,0000 | AlertPresentation.fresh; gpsFreshnessPulse; BackgroundAlertService.autoPulse ;  |
| A2 | Même distance arrondie pour texte, bulle et voix | 3,0000 | testé partiellement | 2,0000 / 3,0000 | AlertPresentation; MainActivity.checkDangers; BackgroundAlertService.check; RadarBubbleOverlay ; verification/java-policy-tests-11.14.241.log ; réexécuté : verification/targeted-checks-11.14.242.log ; réexécuté : verification/targeted-checks-11.14.243.log ; réexécuté : verification/targeted-checks-11.14.244.log |
| A3 | Feu rouge et passage à niveau sans fausse limitation | 3,0000 | testé partiellement | 2,0000 / 3,0000 | RadarAlertPolicy.applicableLimit; CarRouteData.Radar ; verification/java-policy-tests-11.14.241.log ; réexécuté : verification/targeted-checks-11.14.242.log ; réexécuté : verification/targeted-checks-11.14.243.log ; réexécuté : verification/targeted-checks-11.14.244.log |
| A4 | Paliers partagés par type et position | 3,0000 | codé | 0,0000 / 3,0000 | AlertMilestones; RadarIdentity; ReferenceUi.radarSeen; RoadguardCarScreen ;  |
| A5 | Bandeau contextuel, hystérésis et état nettoyé | 3,0000 | codé | 0,0000 / 3,0000 | MainActivity.checkDangers; clearDangerWarning ;  |
| A6 | Sens source et routes parallèles réellement distingués | 3,0000 | à faire | 0,0000 / 3,0000 | Filtre cap/couloir présent, direction source et cas parallèles encore incomplets ;  |
| A7 | Audio de proximité, vitesse inconnue et temporisation | 2,0000 | codé | 0,0000 / 2,0000 | RadarAlertAudio; RadarAlertsRuntimeTest ;  |
| D1 | Surcouches additives sans retirer un original | 5,0000 | testé partiellement | 4,0000 / 5,0000 | UpdatedRadarCatalog.active; LufopEuropeCatalog.all : originaux + surcouche, tests sur les vraies ressources ; verification/targeted-checks-11.14.242.log (catalog-additive-rollback; adaptateurs JVM) ; réexécuté : verification/targeted-checks-11.14.243.log ; réexécuté : verification/targeted-checks-11.14.244.log |
| D2 | Catalogues d’origine et Lufop communs téléphone/service/voiture | 3,0000 | codé | 0,0000 / 3,0000 | UpdatedRadarCatalog; BundledRadarCatalog; NeighborRadarCatalog; LufopEuropeCatalog ;  |
| D3 | États véridiques et comptages du catalogue chargé | 3,0000 | codé | 0,0000 / 3,0000 | Gestionnaire : base locale et archive reçue affichées ensemble ; archive reçue sans prétendre à l’activation. Bouton non redondant une fois l’archive vérifiée ; confirmation du consommateur inchangée. Aucun essai natif ajouté. ;  |
| D4 | Sept sources publiques contrôlées et téléchargements épinglés | 3,0000 | testé | 3,0000 / 3,0000 | RadarDownloadSource; verified public release europe-202606 ; verification/java-policy-tests-11.14.241.log ; verification/speedalert-source-audit.json |
| D5 | Extraction 7z bornée dans Android | 3,0000 | à faire | 0,0000 / 3,0000 | Extraction Android 7z non intégrée : cinq JAR et vraies archives FR/CH manquent pour le test. Aucun point acquis pour une vérification de signature seule. ;  |
| D6 | SQLite vers modèle canonique avec métadonnées | 3,0000 | testé partiellement | 2,0000 / 3,0000 | SpeedAlertSqliteImporter : lecture seule, schéma/tailles/types ; CanonicalRadar : provenance, champs inconnus et vitesse/sens inconnus préservés ; import local raccordé à RadarDownloadActivity ; verification/targeted-checks-11.14.243.log (canonical-sqlite-store : SQLite réel via adaptateurs JVM ; Android natif non exécuté) ; réexécuté : verification/targeted-checks-11.14.244.log |
| D7 | Fusion avec sens/fiabilité et rechargement complet à chaud | 3,0000 | à faire | 0,0000 / 3,0000 | Import canonique additif et accusé de chargement téléphone ajoutés ; sens, conflits, fiabilité/fusion complète et confirmations service/voiture encore incomplets. ;  |
| D8 | File séquentielle, annulation et remplacement atomique | 2,0000 | testé partiellement | 1,0000 / 2,0000 | RadarArchiveDownloader; RadarCountryBatch : annulation, bornes, HTTP simulé, fichiers réels et remplacement atomique ; verification/targeted-checks-11.14.242.log (download-errors-queue; adaptateurs JVM) ; réexécuté : verification/targeted-checks-11.14.243.log ; réexécuté : verification/targeted-checks-11.14.244.log |
| U1 | Thème graphite et rouge conservé | 3,0000 | codé | 0,0000 / 3,0000 | Thème graphite/rouge conservé ; pack local facultatif Graphite/Lisibilité/Couleurs OSM dans Apparence. Filtre de pixels raster uniquement. Aucun essai visuel Android ajouté. ;  |
| U2 | Insets Android récents et formats d’écran | 3,0000 | codé | 0,0000 / 3,0000 | MainActivity.configureModernAndroid; RadarDownloadActivity.onCreate ;  |
| U3 | Pays triés et recherche avec/sans accents | 2,0000 | codé | 0,0000 / 2,0000 | RadarDownloadActivity.searchKey; Collator ;  |
| U4 | Revue globale d’accessibilité et paysage | 2,0000 | à faire | 0,0000 / 2,0000 | Descriptions ciblées présentes; revue complète non réalisée ;  |
| C1 | Variant téléphone indépendant de la bibliothèque Car | 2,0000 | codé | 0,0000 / 2,0000 | app/build.gradle conditional roadguardAndroidAuto ;  |
| C2 | Service et écrans natifs de navigation Car App | 3,0000 | codé | 0,0000 / 3,0000 | app/src/auto; AndroidRuntimeTest; CarSurfaceRuntimeTest ;  |
| C3 | Propriété des alertes et guidage cohérents entre appareils | 3,0000 | à faire | 0,0000 / 3,0000 | Catalogue partagé et limites corrigées; transition multimode à finaliser ;  |
| C4 | Validation DHU et autoradio du parcours complet | 2,0000 | à faire | 0,0000 / 2,0000 | Essai DHU/autoradio non réalisé ;  |
| Q1 | Version Gradle alignée sur la livraison | 2,0000 | testé | 2,0000 / 2,0000 | app/build.gradle : 11.14.244 / 444 ; package/SDK conservés ; ledger 244 et référence des deltas 243 raccordés. ; verification/release-contract-11.14.244.json |
| Q2 | Tests ciblés et progression pondérée reproductible | 2,0000 | codé | 0,0000 / 2,0000 | tools/test_camera_anchor.js; AlertConsistencyRuntimeTest; ProgressLedger ;  |
| Q3 | APK téléphone produit avec SDK 36 | 2,0000 | à faire | 0,0000 / 2,0000 | Aucun APK 244 : SDK absent, environnement consigné dans environment-11.14.244.json. Blocage Gradle précédent conservé, aucune compilation déclarée. ;  |
| Q4 | APK Android Auto compilé séparément | 2,0000 | à faire | 0,0000 / 2,0000 | Gradle assembleDebug -ProadguardAndroidAuto=true ;  |
| Q5 | Installation CodeAssist et retours du lot sur matériel | 2,0000 | à faire | 0,0000 / 2,0000 | Captures utilisateur reçues le 21/10/2026 : problèmes radars/cache/démarrage et réglages externes. Version non visible dans ces captures ; aucun crédit de compilation ou de validation complète 243/244. ;  |
| S1 | Empreintes des 59 ressources d’origine et ZIP contrôlé | 2,0000 | testé | 2,0000 / 2,0000 | verification/original-data-11.14.240.json; integrity report ; verification/data-integrity-11.14.241.json ; ZIP CRC checked during packaging ; verification/data-integrity-11.14.242.json (CRC du nouveau ZIP contrôlé au packaging) ; verification/data-integrity-11.14.243.json ; CRC du ZIP contrôlé au conditionnement ; verification/data-integrity-11.14.244.json ; CRC du ZIP contrôlé au conditionnement |
| S2 | Mises à jour séparées, atomiques et repli | 1,0000 | testé partiellement | 0,5000 / 1,0000 | CanonicalRadarStore : fichier indépendant, validation avant remplacement, renommage atomique, deux sauvegardes, annulation et repli contrôlés. Chemins CSV/Lufop historiques conservés, non réaudités dans ce lot. ; verification/targeted-checks-11.14.243.log (canonical-sqlite-store : SQLite réel via adaptateurs JVM ; Android natif non exécuté) ; réexécuté : verification/targeted-checks-11.14.244.log |
| S3 | Provenance et licence des nouvelles sources consignées | 1,0000 | testé | 1,0000 / 1,0000 | RadarDownloadSource; verification/speedalert-source-audit.json ; verification/java-policy-tests-11.14.241.log ; verification/speedalert-source-audit.json |
| S4 | Limites des imports et destinations HTTPS contrôlées | 1,0000 | testé | 1,0000 / 1,0000 | RadarSourcePolicy; SecureHttp; RadarDownloadSource.allowed ; verification/ledger-and-http-tests-11.14.241.log |

Date du relevé : **21/10/2026**. Les nouveaux résultats ciblés figurent dans `verification/targeted-checks-11.14.244.log`. Les preuves des versions 241/242/243 conservées restent datées et ne sont pas toutes réexécutées. La compilation Android est bloquée avant exécution par un réseau inaccessible ; aucun APK 244 produit. Les adaptateurs JVM et l’exécution JavaScript dans Node ne remplacent pas les essais Android. M6 obtient seulement ses 1,5000 points de contrôle automatique : fichiers et limites du cache, injection HTTP, reprise bornée et maintien des bonnes tuiles dans un adaptateur Leaflet. Aucun essai réel WebView, serveur de tuiles ou appareil n’est ajouté. M1 reste sans crédit automatique Android pour son démarrage modifié. D5 (extraction 7z) reste à faire. D6 (SQLite vers canonique) est codé et testé sur fichiers synthétiques via adaptateurs JVM, sans validation Android native ; aucun crédit 7z donné pour des en-têtes seuls. Les anciennes notes et l’ancien tableau JSON sont conservés ; ce relevé versionné est la référence de cette livraison.

**🔴 INTERVENTION OBLIGATOIRE — MAINTENANT POUR LE 7z :** joindre les deux archives FR/CH et les cinq JAR listés dans FICHIERS-POUR-IMPORT-7Z.md. **PLUS TARD :** compiler une fois le lot final et essayer sur Xiaomi/Android Auto ; aucun de ces essais n’est déclaré réalisé.
