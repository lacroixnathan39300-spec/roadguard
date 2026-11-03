# ROADGUARD 11.13.61 — PASSATION

## État exact
Livraison du projet SOURCE uniquement, pas d'APK Android. Progression globale historique : 42 % (estimation non auditée).

## Réalisé
- `OfflinePackSync.java` : synchronisation d'un seul pack manquant par appel, sélection par GPS et rayon, priorité au pack le plus proche, déduplication persistante via `OfflineCellStore`, contrôle post-enregistrement, remontée des erreurs.
- `tests/OfflinePackSyncTest.java` : Jura, Lyon, Marseille, retour Jura, réouverture du registre, erreur réseau sans faux enregistrement.
- `app/build.gradle` : versionName 11.13.61, versionCode 190.
- Aucun changement au code GPS bleu, caméra et rendu MapLibre.

## Tests exécutés
- Compilation javac de 6 classes Java : OK.
- `OfflinePackSyncTest` : OK.
- `RegionSelectorTest` : OK.
- `OfflinePackCatalogTest` : OK.
- APK, GPS réel, Android WorkManager, réseau HTTPS distant, PMTiles dans MapLibre : NON TESTÉS.

## Limites et prochaines tâches (ordre)
1. Obtenir un catalogue **réel** de PMTiles régionaux hébergés légalement (HTTPS, empreintes SHA-256). Les URL `example.org` ne sont que des fixtures.
2. Brancher `OfflinePackSync.step` dans un orchestrateur Android (hors thread UI), avec politique réseau/batterie, reprise et cadence maîtrisée.
3. Connecter le stockage `OfflineCellStore` au lecteur PMTiles et au rendu MapLibre, tester sans réseau.
4. Compiler l'APK, valider GPS bleu/caméra et bibliothèque sur Xiaomi.
5. Mettre à jour les pourcentages seulement après tests fonctionnels réels.

## Exigence utilisateur
Bibliothèque cumulative conservée à vie (sauf suppression manuelle), GPS 50 km, téléchargement des seules régions manquantes, pas de purge automatique, offline réel, journal de bord et pourcentages à chaque ZIP.
