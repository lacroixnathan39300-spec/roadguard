# WORK — ROADGUARD 11.14.191

## Changements et terminé

- Variante Android Auto activée dans gradle.properties; cinq classes `src/auto/java`, bibliothèques Car App / Projected 1.4.0 et manifeste voiture sélectionnés.
- Version 11.14.191 / code 391; même identifiant fr.roadguard.app.
- Build téléphone seul conservé avec `-ProadguardAndroidAuto=false`; tests Android Auto exclus dans ce mode.
- Script tools/build_verified_apk.sh : choix explicite --auto / --phone.
- Test de contrat Android Auto adapté aux sources src/auto; simulateur DOM du test UI actualisé pour les API utilisées depuis les correctifs précédents.
- Carte et caméra existantes non modifiées. Aucun chantier France intégrale ajouté.

## Compilation et contrôles réels

- OpenJDK 17, Gradle 8.2, AGP 8.2.2, SDK/build-tools 34 : assembleDebug Android Auto réussi.
- Présence de RoadguardCarAppService, RoadguardCarScreen, CarFavoritesScreen et CarAppService vérifiée dans les DEX.
- Manifeste fusionné : service Android Auto exporté et catégorie navigation; permissions NAVIGATION_TEMPLATES / ACCESS_SURFACE.
- APK signée avec la clé de vérification du projet; signatures v2/v3 vérifiées et alignement 4 octets contrôlé. La signature CodeAssist de Nathan n'est pas disponible ici.
- Assets embarqués roadguard_progress.json, reference_ui.html, map.html, camera_motion.js comparés octet par octet aux sources livrées.
- 45 tests Android Auto réussis, 0 échec, 0 ignoré : 15 cas sur API26/28/34. Preuves : android-auto-tests-11.14.191.xml et .log. Ce sont des tests Robolectric/TestCarContext, pas un lancement sur DHU ou véhicule.
- Tests couvrant permissions, templates et trajet, arrivée/arrêt, rejet GPS périmé, itinéraires invalides, favoris paginés, libération des ressources, disponibilité de la voix, sortie de trajet, recalcul prudent et manœuvres.
- Contrat statique Android Auto et test JavaScript des 13 écrans réussis.
- Première exécution des tests bloquée par le téléchargement des SDK simulés; après configuration du proxy dans l'environnement de test seulement, tous les tests passent. Aucun contournement ajouté à l'application.

## À faire et blocages

- Aucun test DHU, connexion à une voiture ou essai Xiaomi ici. Les tests ne mesurent ni qualité GPS réelle ni fluidité dans un véhicule.
- L'installation directe de cet APK ne suffit pas à garantir sa visibilité Android Auto. La documentation Google réserve cette catégorie App Library à une installation approuvée pour l'essai réel (partage interne/piste de test Play); « sources inconnues » ne s'applique pas.
- targetSdk 28 hérité : modernisation requise avant une distribution Play conforme aux exigences en vigueur. Aucun compte Play ni véhicule connecté disponible dans cet environnement; aucun upload réalisé.
- En cas de signature incompatible, reconstruire avec la signature CodeAssist habituelle; ne pas désinstaller pour forcer une mise à jour au risque de perdre les données.
- Estimations historiques conservées : Android Auto 85 %, global 95 %, +0 pt. Elles ne sont pas un audit de fiabilité.

Source officielle consultée le 18/10/2026 : https://developer.android.com/training/cars/testing#real-vehicles

## Mode téléphone seul et bascule

- Build téléphone seul nettoyé : assembleDebug et compileDebugUnitTestJavaWithJavac réussis; absence des classes voiture dans le DEX et exclusion des tests voiture vérifiées. Tests téléphone compilés, non exécutés dans cette passe.
- Une bascule sans nettoyage a révélé un doublon R$color dans les intermédiaires Gradle. Le script nettoie maintenant automatiquement avant de construire; les commandes du guide incluent clean.

APK Android Auto finale recompilée proprement après le mode téléphone; assets et classes à nouveau vérifiés. SHA-256 : `ca7e2fb71b0f31d2c4e8b5468fc2d9455a19dd90d352185668a405f1ec148f31`.
