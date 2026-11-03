# Validation 11.14.290 — 24/10/2026

Exécutés :
- tools/test_local_map_pipeline.py → local-map-pipeline-11.14.290.log. Corps de localTileNotFound/localTileCorsHeaders/localTileResponse/serveMapResource/serveOfflineTile extraits sans modification de MainActivity, compilés en Java 8 avec adaptateurs URI, AssetManager et WebResourceResponse. 98 contrôles. Vrais fichiers HTML/JS/CSS et lecteur PMTiles réel sur archive synthétique. L’ancienne route HTTPS renvoie bien 404 ; ce n’est pas une reproduction matérielle du défaut.
- tools/test_local_map_policy.py → local-map-policy-11.14.290.log. 49 contrôles des classes LocalMapAssets et LocalPagePolicy ; origine et chemins stricts, passerelle, interdiction des accès fichiers. Les WebSettings sont des adaptateurs d’enregistrement.
- tools/test_offline_pack_discovery.py → offline-packs-11.14.290.log. Registre, mosaïque, octets vectoriels, résolution, SHA, exclusion des fichiers orphelins/partiels et longueur changée.
- tools/run_ui_regressions.py → ui-regressions-11.14.290.log. Régressions des scripts : doublons, couverture partielle, icônes, démarrage, persistance de cadre et caméra. DOM et moteur de rendu simulés.
- ParseJava → syntax-11.14.290.log : MainActivity, LocalPagePolicy, LocalMapAssets et LocalPageRuntimeTest. Syntaxe seulement ; pas de typage Android complet.

Ajouté mais NON exécuté : LocalPageRuntimeTest.httpsMapOriginServesPackagedAssetsWithoutOpeningFileAccess, API26/36, contrôle de l’URL réellement chargée et des réponses AssetManager natives.

NON exécutés : Gradle/SDK Android, APK, Robolectric, Chromium/WebGL, rendu matériel WebView, GPS et appareil Xiaomi. Playwright est installé mais son navigateur exécutable est absent. Le fichier présent sur le téléphone n’est pas accessible depuis ce projet : sa couverture et ses octets n’ont pas été inspectés ici.

Intégrité : tous les CSV/SQLite/DB sont comparés octet par octet au projet 289 ; rapport data-integrity-11.14.290.json. Les inventaires sources restent inchangés. Le seul asset modifié par ce lot est le suivi de progression. ZIP contrôlé par CRC après assemblage.

Pas d’augmentation des estimations historiques. Le rendu effectif hors ligne reste à valider sur Xiaomi. Voir le fichier LIRE-EN-PREMIER courant pour les étapes et les sources officielles.
