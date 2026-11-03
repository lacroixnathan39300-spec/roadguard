# ROADGUARD 11.13.60 — passation

## Réalisé
- Ajout `OfflinePackCatalog.java` : lecture d'un catalogue TSV UTF-8 strict (ID, HTTPS, SHA-256, taille maximale, coordonnées, format PMTiles).
- Refus des ID en double, URL HTTP, identifiants URL, fragments, limites invalides, formats non PMTiles et catalogues >1 Mio / 10 000 entrées.
- Tests `OfflinePackCatalogTest.java` et `RegionSelectorTest.java` compilés et exécutés via javac/java avec succès.
- `app/build.gradle` versionName 11.13.60 / versionCode 189.

## État réel
- Le lecteur de catalogue est un composant Java testé, **pas encore branché à MainActivity**.
- Aucun catalogue réel hébergé, aucune URL de carte autorisée opérationnelle, aucune carte France+Corse prête.
- Aucun téléchargement GPS automatique Android, aucune exécution en arrière-plan ni validation MapLibre hors ligne.
- Aucun APK construit ni test Xiaomi effectué. GPS bleu et caméra non modifiés.

## Prochaines étapes par priorité
1. Produire et héberger des archives PMTiles réelles sous licence compatible, obtenir URL HTTPS et SHA-256.
2. Relier le catalogue validé au sélecteur GPS et au téléchargeur dans le cycle Android.
3. Ajouter exécution de téléchargement en arrière-plan, reprise et suivi des échecs.
4. Servir les packs locaux à MapLibre, vérifier hors connexion.
5. Construire APK et tester sur Xiaomi, GPS bleu et caméra inclus.

## Pourcentages historiques estimés
Global 42 % ; GPS 100 % ; carte en ligne 85 % ; planification 50 km 70 % ; mémoire 55 % ; téléchargement réel 20 % ; arrière-plan 5 % ; hors ligne 30 % ; Xiaomi 10 %. Ces chiffres ne sont pas des preuves de fonctionnement sur téléphone.
