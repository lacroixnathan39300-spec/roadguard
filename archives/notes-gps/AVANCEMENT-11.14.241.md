# ROADGUARD 11.14.241 — avancement pondéré

Référence : source 11.14.240 inspectée le 20/10/2026. Les **48,0000 % historiques sont une estimation non auditée** ; ils ne servent pas de base au calcul ci-dessous.

39 tâches explicites totalisent 100 points. Le développement compte les tâches codées selon leur contrat ; une tâche partielle reste à faire. La validation additionne seulement les contrôles réussis, pondérés à l’avance. Les essais matériels ont leur propre poids et restent non validés.

| Module | Poids | Développement | Validation | Δ développement (pts du module) | Δ validation (pts du module) |
|---|---:|---:|---:|---:|---:|
| Carte / GPS | 20,0000 | 100,0000 % | 35,0000 % | 35,0000 | 25,0000 |
| Alertes | 20,0000 | 85,0000 % | 20,0000 % | 75,0000 | 20,0000 |
| Sources / activation radar | 25,0000 | 64,0000 % | 12,0000 % | 44,0000 | 12,0000 |
| UI / ergonomie | 10,0000 | 80,0000 % | 0,0000 % | 20,0000 | 0,0000 |
| Android Auto | 10,0000 | 50,0000 % | 0,0000 % | 0,0000 | 0,0000 |
| Qualité / tests / compilation | 10,0000 | 40,0000 % | 0,0000 % | 20,0000 | 0,0000 |
| Sécurité / données | 5,0000 | 100,0000 % | 80,0000 % | 60,0000 | 80,0000 |
| **TOTAL pondéré** | **100,0000** | **75,0000 %** | **18,0000 %** | **40,0000** | **16,0000** |

Référence reconstituée sur ce backlog : développement **35,0000 %**, validation **2,0000 %**. Une validation automatique ne constitue pas un essai Xiaomi ou Android Auto.

**Chantier actif :** Finaliser l’import Android 7z → SQLite → surcouche canonique, puis les transitions d’alertes et les essais Xiaomi / Android Auto.

## Backlog et preuves

| ID | Tâche | Poids | État | Contrôles réussis / poids | Preuve code et test |
|---|---|---:|---|---:|---|
| M1 | Démarrage local sans attendre les mises à jour réseau | 3,0000 | codé | 0,0000 / 3,0000 | CommunitySync.prepareStartup; MainActivity.showMap; LocalPageRuntimeTest ;  |
| M2 | Filtrage de la vitesse, du cap et des sauts GPS | 3,0000 | testé partiellement | 2,0000 / 3,0000 | GpsMotion; MainActivity.startGPS ; verification/java-policy-tests-11.14.241.log |
| M3 | Ancrage horizontal sous rotation et hauteur réglable | 4,0000 | testé partiellement | 3,0000 / 4,0000 | camera_geometry.js; rgVisualCamera; rgCameraOffset; MainActivity.pushPosition ; verification/camera-tests-11.14.241.log |
| M4 | Exploration manuelle et recentrage volontaire | 2,0000 | testé partiellement | 1,0000 / 2,0000 | resumeCamera; rgManualZoom; showRouteOverview ; verification/camera-tests-11.14.241.log |
| M5 | Zoom adaptatif avec hystérésis | 2,0000 | testé partiellement | 1,0000 / 2,0000 | camera_motion.js; updateGPS ; verification/camera-tests-11.14.241.log |
| M6 | Cache temporaire de tuiles, limites et récupération | 3,0000 | codé | 0,0000 / 3,0000 | VisibleTileCache; OfflineCellStore; TileCacheRuntimeTest ;  |
| M7 | Itinéraire, cache et reprise du guidage | 3,0000 | codé | 0,0000 / 3,0000 | RouteRequestPolicy; MainActivity navigation methods ;  |
| A1 | Refus du GPS périmé et effacement des avertissements | 3,0000 | codé | 0,0000 / 3,0000 | AlertPresentation.fresh; gpsFreshnessPulse; BackgroundAlertService.autoPulse ;  |
| A2 | Même distance arrondie pour texte, bulle et voix | 3,0000 | testé partiellement | 2,0000 / 3,0000 | AlertPresentation; MainActivity.checkDangers; BackgroundAlertService.check; RadarBubbleOverlay ; verification/java-policy-tests-11.14.241.log |
| A3 | Feu rouge et passage à niveau sans fausse limitation | 3,0000 | testé partiellement | 2,0000 / 3,0000 | RadarAlertPolicy.applicableLimit; CarRouteData.Radar ; verification/java-policy-tests-11.14.241.log |
| A4 | Paliers partagés par type et position | 3,0000 | codé | 0,0000 / 3,0000 | AlertMilestones; RadarIdentity; ReferenceUi.radarSeen; RoadguardCarScreen ;  |
| A5 | Bandeau contextuel, hystérésis et état nettoyé | 3,0000 | codé | 0,0000 / 3,0000 | MainActivity.checkDangers; clearDangerWarning ;  |
| A6 | Sens source et routes parallèles réellement distingués | 3,0000 | à faire | 0,0000 / 3,0000 | Filtre cap/couloir présent, direction source et cas parallèles encore incomplets ;  |
| A7 | Audio de proximité, vitesse inconnue et temporisation | 2,0000 | codé | 0,0000 / 2,0000 | RadarAlertAudio; RadarAlertsRuntimeTest ;  |
| D1 | Surcouches additives sans retirer un original | 5,0000 | codé | 0,0000 / 5,0000 | UpdatedRadarCatalog.load; active; MainActivity.loadBundledRadarDatabase ;  |
| D2 | Catalogues d’origine et Lufop communs téléphone/service/voiture | 3,0000 | codé | 0,0000 / 3,0000 | UpdatedRadarCatalog; BundledRadarCatalog; NeighborRadarCatalog; LufopEuropeCatalog ;  |
| D3 | États véridiques et comptages du catalogue chargé | 3,0000 | codé | 0,0000 / 3,0000 | RadarDownloadActivity.loadActiveCounts; renderCountries ;  |
| D4 | Sept sources publiques contrôlées et téléchargements épinglés | 3,0000 | testé | 3,0000 / 3,0000 | RadarDownloadSource; verified public release europe-202606 ; verification/java-policy-tests-11.14.241.log ; verification/speedalert-source-audit.json |
| D5 | Extraction 7z bornée dans Android | 3,0000 | à faire | 0,0000 / 3,0000 | Schéma réel inspecté hors application; extracteur Android à intégrer ;  |
| D6 | SQLite vers modèle canonique avec métadonnées | 3,0000 | à faire | 0,0000 / 3,0000 | cameras(id,lat,lon,type) observée; convertisseur Android à faire ;  |
| D7 | Fusion avec sens/fiabilité et rechargement complet à chaud | 3,0000 | à faire | 0,0000 / 3,0000 | Déduplication type/position ajoutée; direction, identifiants et rechargement foreground à compléter ;  |
| D8 | File séquentielle, annulation et remplacement atomique | 2,0000 | codé | 0,0000 / 2,0000 | RadarDownloadActivity.startCountryBatch; downloadCountryArchive ;  |
| U1 | Thème graphite et rouge conservé | 3,0000 | codé | 0,0000 / 3,0000 | MainActivity; reference_ui.html; ressources visuelles d’origine ;  |
| U2 | Insets Android récents et formats d’écran | 3,0000 | codé | 0,0000 / 3,0000 | MainActivity.configureModernAndroid; RadarDownloadActivity.onCreate ;  |
| U3 | Pays triés et recherche avec/sans accents | 2,0000 | codé | 0,0000 / 2,0000 | RadarDownloadActivity.searchKey; Collator ;  |
| U4 | Revue globale d’accessibilité et paysage | 2,0000 | à faire | 0,0000 / 2,0000 | Descriptions ciblées présentes; revue complète non réalisée ;  |
| C1 | Variant téléphone indépendant de la bibliothèque Car | 2,0000 | codé | 0,0000 / 2,0000 | app/build.gradle conditional roadguardAndroidAuto ;  |
| C2 | Service et écrans natifs de navigation Car App | 3,0000 | codé | 0,0000 / 3,0000 | app/src/auto; AndroidRuntimeTest; CarSurfaceRuntimeTest ;  |
| C3 | Propriété des alertes et guidage cohérents entre appareils | 3,0000 | à faire | 0,0000 / 3,0000 | Catalogue partagé et limites corrigées; transition multimode à finaliser ;  |
| C4 | Validation DHU et autoradio du parcours complet | 2,0000 | à faire | 0,0000 / 2,0000 | Essai DHU/autoradio non réalisé ;  |
| Q1 | Version Gradle alignée sur la livraison | 2,0000 | codé | 0,0000 / 2,0000 | app/build.gradle: 11.14.241 / 441 ;  |
| Q2 | Tests ciblés et progression pondérée reproductible | 2,0000 | codé | 0,0000 / 2,0000 | tools/test_camera_anchor.js; AlertConsistencyRuntimeTest; ProgressLedger ;  |
| Q3 | APK téléphone produit avec SDK 36 | 2,0000 | à faire | 0,0000 / 2,0000 | Gradle assembleDebug -ProadguardAndroidAuto=false ;  |
| Q4 | APK Android Auto compilé séparément | 2,0000 | à faire | 0,0000 / 2,0000 | Gradle assembleDebug -ProadguardAndroidAuto=true ;  |
| Q5 | Installation CodeAssist et retours du lot sur matériel | 2,0000 | à faire | 0,0000 / 2,0000 | Aucun retour utilisateur 11.14.241 à cette date ;  |
| S1 | Empreintes des 59 ressources d’origine et ZIP contrôlé | 2,0000 | testé | 2,0000 / 2,0000 | verification/original-data-11.14.240.json; integrity report ; verification/data-integrity-11.14.241.json ; ZIP CRC checked during packaging |
| S2 | Mises à jour séparées, atomiques et repli | 1,0000 | codé | 0,0000 / 1,0000 | AuthorizedRadarImporter; LufopEuropeCatalog; RadarDownloadActivity ;  |
| S3 | Provenance et licence des nouvelles sources consignées | 1,0000 | testé | 1,0000 / 1,0000 | RadarDownloadSource; verification/speedalert-source-audit.json ; verification/java-policy-tests-11.14.241.log ; verification/speedalert-source-audit.json |
| S4 | Limites des imports et destinations HTTPS contrôlées | 1,0000 | testé | 1,0000 / 1,0000 | RadarSourcePolicy; SecureHttp; RadarDownloadSource.allowed ; verification/ledger-and-http-tests-11.14.241.log |

Date du relevé : **20/10/2026**. Les résultats exécutés figurent dans `verification/`. Les anciennes notes et l’ancien tableau JSON sont conservés ; ce relevé versionné est la référence de cette livraison.

**🔴 INTERVENTION OBLIGATOIRE — PLUS TARD :** un essai du lot sur Xiaomi et un essai Android Auto sur matériel compatible restent nécessaires. Aucun de ces essais n’est déclaré réalisé.
