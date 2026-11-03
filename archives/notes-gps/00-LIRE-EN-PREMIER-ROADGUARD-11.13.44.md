# ROADGUARD 11.13.44 — Préparation APK / diagnostic reproductible

## Réalisé
- Corrigé `app/build.gradle` : `versionName 11.13.44`, `versionCode 174`. Les ZIP sources précédents conservaient 11.13.10 : une APK recompilée aurait donc affiché une version trompeuse.
- Ajouté `tools/preflight_android_release.py` : vérifie la version, les fichiers indispensables, les empreintes SHA-256 des deux bibliothèques MapLibre, la plateforme Android SDK 34 et la disponibilité de Gradle.
- Le code GPS et caméra reste inchangé.

## État réel / blocages
- Aucun APK compilé : environnement courant sans Gradle ni SDK Android 34.
- Aucune carte France+Corse PMTiles réelle produite, hébergée ou testée sur Xiaomi.
- L’import local utilise une copie et nécessite de l’espace libre ; il ne réutilise pas directement une archive SAF de 2,76 Go sans copie.
- L’APK actuellement installée sur le Xiaomi reste 11.13.10.

## Prochaine priorité
1. Préparer un environnement de compilation Android (Gradle + SDK 34), compiler APK debug/release puis vérifier son démarrage.
2. Localiser réellement le pack de 2,76 Go et confirmer format/accès, éviter tout effacement.
3. Tester GPS bleu, carte hors ligne, noms de routes et caméra sur Xiaomi.

## Exécution
`python3 tools/preflight_android_release.py` (code retour 2 = environnement incomplet).
