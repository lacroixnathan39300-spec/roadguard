# ROADGUARD 11.14.246 — compilations et pack graphique

21 octobre 2026 — versionCode 446 — package `fr.roadguard.app` — reprise intégrale de la 245. Un seul ZIP contient sources, données, dépendances, deux APK debug, notices, WORK, suivi pondéré et preuves.

## Résultat

Les outils Google/Gradle précédemment refusés ont été téléchargés après autorisation explicite de l’utilisateur. SDK 36, Build Tools 35.0.0, Gradle 8.11.1 et AGP 8.10.1 sont utilisés. Les APK téléphone et Android Auto ont été compilés séparément avec JDK 17. Les tests API 36 ont été exécutés avec JDK 21.

L’environnement a nécessité un JDK complet et le magasin de certificats système. Les paramètres réseau temporaires restent hors du projet. Les premières tentatives et leurs erreurs sont conservées comme historique dans verification ; elles ne sont pas présentées comme des réussites.

Le changement de la propriété `roadguardAndroidAuto` dans le même dossier de build avait conservé des classes R générées. La suppression complète des sorties `app/build` a permis les deux compilations. Le lanceur `tools/run_android_build.py` réalise maintenant ce nettoyage avant chaque invocation. Les sources et données utilisateur ne sont pas nettoyées.

## Conflit 7z détecté et corrigé

Le premier passage Robolectric a exécuté 86 tests : un import 7z échouait sur API 36 avec `NoSuchMethodError: SevenZFile.builder()`. L’image android-all Android 16 contient une ancienne classe Commons Compress et une classe XZ portant les mêmes noms de paquets. Ce constat porte sur l’image de test ; le boot classpath d’un téléphone physique n’a pas été inspecté.

Les cinq bibliothèques fournies sont regroupées dans `app/libs/roadguard-radar-runtime-1.jar`, avec leurs paquets déplacés sous `fr.roadguard.vendor`. L’application et le banc Java utilisent cette copie isolée. Les cinq JAR originaux restent dans app/libs pour provenance et reproduction ; Gradle ne les ajoute pas à l’APK en plus du runtime. Brotli conserve sa dépendance précédente.

La transformation utilise JarJar Assembly 1.14.1, outil externe de build vérifié et épinglé. Les règles et le script de reproduction sont inclus. Le résultat contient 1 627 classes Java 8 ; sa reconstruction a donné exactement les mêmes octets. `verification/radar-relocation-11.14.246.json` consigne entrées, outil, règles et SHA-256. Le runtime et les notices sont livrés ; aucun téléchargement de JarJar n’est nécessaire pour compiler normalement ce projet.

Les limites d’extraction de la 245 sont conservées : un seul nom attendu, taille maximale, mémoire bornée, codecs autorisés, CRC, signature SQLite, annulation et nettoyage. La surcouche reste additive aux originaux, atomique et sauvegardée. Les archives FR/CH intégrées sont inchangées. Les données amont `speed` restent de type inconnu, sans vitesse ou sens inventés.

## Pack graphique et carte

Dans **Apparence → Pack graphique local**, trois choix sont intégrés : **Graphite**, **Lisibilité**, **Couleurs OSM**. Le choix est mémorisé et appliqué à l’ouverture de la carte. Graphite conserve le style existant ; Lisibilité ajuste contraste/saturation selon Jour/Nuit ; Couleurs OSM retire le filtre des tuiles.

Ce sont des filtres sur les images raster. Ils ne redessinent pas les catégories de routes, n’ajoutent pas de zones et ne constituent pas une carte vectorielle complète ou un téléchargement permanent de la France. Le cache temporaire et la reprise bornée des tuiles en erreur restent en place. `PACK-GRAPHIQUE-CARTE.md` détaille l’usage et les limites.

Les contrôles JavaScript et `MapAppearanceRuntimeTest` passent pour les trois identifiants, la préférence, les commandes carte et le refus d’une valeur inconnue. **Ils ne valident pas les pixels d’un vrai WebView ni la lisibilité sur Xiaomi.** Aucun point visuel U1 n’est ajouté.

Le catalogue radar continue à charger sur worker après l’affichage de la carte. Le test Startup utilise maintenant le vrai splash à deux frames, bloque le worker et vérifie que la carte ainsi qu’un point personnel restent disponibles. Les appels HTTP externes sont isolés dans ce test. La collecte WazeAPI/OpenWeb Ninja reste un service externe facultatif distinct des bases radar et du pack ; aucun APK Waze/Sygic n’est demandé.

## Vérifications exécutées

**86 tests Robolectric réussis, zéro échec, zéro erreur, zéro test ignoré**, dans huit suites :

| Suite | Exécutions | API |
|---|---:|---|
| RealSevenZipRuntimeTest | 2 | 26, 36 |
| CanonicalSqliteRuntimeTest | 2 | 26, 36 |
| StartupRuntimeTest | 2 | 26, 36 |
| LocalPageRuntimeTest | 14 | 26, 36 |
| RadarCatalogReloadRuntimeTest | 6 | 26, 36 |
| MapAppearanceRuntimeTest | 2 | 26, 36 |
| TileCacheRuntimeTest | 52 | 26, 28, 34, 36 |
| CountryManagerRuntimeTest | 6 | 26, 36 |

Les deux suites SQLite utilisent le mode natif Robolectric, avec extraction réelle des archives FR/CH puis lecture Android SQLite. Elles ne sont plus remplacées par les adaptateurs JVM pour cette preuve. Le WebView reste simulé. Les tests pays ont été adaptés au texte Installer et à l’ancien constructeur d’insets encore présent sur API 26 mais absent des stubs SDK 36.

Le banc Java/JavaScript est également réexécuté avec le runtime isolé : 25 scénarios 7z/import, 17 cache, 10 surcouches, 11 reprise/pack/interface, 35 SQLite canonique, 27 téléchargement/file, 14 catalogue, plus politiques d’alertes et mouvement GPS. 89 fichiers Java sont analysés pour la syntaxe ; les compilations Gradle résolvent ensuite les types Android du code de production.

Les signatures, manifestes, version 11.14.246 / 446, minimum 26, cible 36, présence du service Car uniquement dans la variante Auto, assets et classes radar isolées sont contrôlés dans chaque APK. Les 59 fichiers d’origine restent identiques, avec contrôle supplémentaire du ZIP et de son CRC lors du conditionnement.

Preuves : `verification/android-native-results-11.14.246.json`, les XML dans `verification/robolectric-11.14.246`, les trois logs Gradle courants, `targeted-results-11.14.246.json`, `apk-11.14.246.json`, `release-contract-11.14.246.json` et `data-integrity-11.14.246.json`. Les résultats de tentative avant correction sont conservés séparément.

## Reproduction et livraison

`python3 tools/run_targeted_checks.py` exécute le banc local. Le lanceur `run_android_build.py` accepte `--gradle`, `--sdk`, `--cache`, `--variant phone|auto|tests` et des `--test fr.roadguard.app.NomDuTest`. JDK 17 pour les APK, JDK 21 pour les tests API 36. `collect_android_test_results.py` conserve les XML avant nettoyage du prochain build. `verify_release_apks.py --build-tools /chemin/build-tools/35.0.0` contrôle les APK, puis `update_progress_11_14_246.py` et `render_progress_report.py` régénèrent le suivi. Le conditionneur `package_codeassist.py` exclut outils hôte, caches, sorties Gradle et local.properties.

Les deux APK debug de LIVRAISON portent le même package et sont des variantes alternatives. La variante Android Auto comprend aussi l’interface téléphone. Leur clé debug a été générée pour cette compilation et ne prouve pas une compatibilité avec la signature de l’installation actuelle. La clé historique `signing/verification-debug.keystore` n’a pas été utilisée pour eux. Pour mettre à jour en conservant les données, reconstruire avec la signature CodeAssist habituelle. Les anciens APK 197 restent historiques.

## Avancement

39 tâches, 100 points et poids inchangés. Développement **85,0000 %**, variation **+4,0000 points** depuis la 245. Validation **44,0000 %**, variation **+13,0000 points**. Q3 et Q4 sont désormais codés/réalisés ; les nouveaux contrôles automatiques sont M1, D3, U3, C1, Q2, Q3 et Q4. Le tableau complet figure dans `AVANCEMENT-11.14.246.md` et dans l’application.

Aucun essai Xiaomi, DHU ou autoradio n’est déclaré. Aucun point matériel, visuel U1 ou contrôle complet U2 n’est ajouté. Aucun nouvel audit de géométrie/mouvement caméra. D7 direction/fiabilité/fusion et C3 propriété des alertes entre appareils restent incomplets.

**🔴 INTERVENTION OBLIGATOIRE — PLUS TARD, EN FIN DE LOT :** reconstruire une fois avec la signature CodeAssist habituelle, mettre à jour sans effacer les données, ouvrir les bases radar et installer FR/CH, revenir à la carte et vérifier la consommation. Essayer les trois styles, le redémarrage, le cache après une coupure réseau et Android Auto. Aucun fichier ni autorisation supplémentaire demandé maintenant.
