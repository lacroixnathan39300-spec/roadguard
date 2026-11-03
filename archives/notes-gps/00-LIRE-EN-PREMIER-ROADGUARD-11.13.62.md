# ROADGUARD 11.13.62 — reprise

Source de départ : 11.13.61. **Code source uniquement, pas d’APK.** Avancement global historique estimé : 42 %, non audité.

## Réalisé
- `MainActivity.java` : appel `maybeSyncNearbyPacks()` depuis le GPS après la planification 50 km.
- Travail réseau hors thread UI via exécuteur mono-thread ; limitation à un lancement toutes les 30 secondes, ou 5 minutes dans la même cellule.
- Vérification réseau, option utilisateur, lecture de `assets/offline_pack_catalog.tsv` au format strict défini par `OfflinePackCatalog`, puis `OfflinePackSync.step` (un pack maximum par déclenchement).
- Sans catalogue, aucune URL n’est fabriquée et aucun téléchargement n’est lancé. GPS et caméra inchangés.
- Version Gradle 11.13.62 (versionCode 191).

## Tests réellement exécutés
`javac` puis `java` : OfflinePackSyncTest, RegionSelectorTest, OfflinePackCatalogTest — réussis. Pas de compilation Android : SDK/Gradle indisponibles. Pas de test sur Xiaomi.

## Reste à faire (priorité)
1. Obtenir/héberger légalement des PMTiles de vraies régions et générer le catalogue TSV avec SHA-256.
2. Tester et corriger la compilation Android puis vérifier réseau et GPS sur Xiaomi.
3. Gérer les téléchargements persistants en arrière-plan (WorkManager/foreground service), les erreurs/reprises et la notification utilisateur. **Actuellement : uniquement pendant que l’activité est en vie.**
4. Brancher la bibliothèque des packs téléchargés au moteur MapLibre hors ligne.
5. Revoir le déclenchement automatique pour remplir plusieurs packs sans devoir attendre de nouvelles positions GPS.

## Attention
L’enregistrement dans `offline-cells` ne signifie pas que MapLibre sait encore afficher le pack. Le code n’a pas été validé dans une APK. Ne pas afficher de réussite cartographique hors ligne.
