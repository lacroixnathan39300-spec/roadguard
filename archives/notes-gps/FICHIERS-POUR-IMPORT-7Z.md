# Fichiers 7z — tous présents dans la 11.14.246

Les anciennes demandes dans WORK 243/244 sont historiques : **aucun des sept fichiers ne manque maintenant**. Aucun envoi des APK Waze/Sygic n’est demandé.

| Fichier | Emplacement |
|---|---|
| fr-camera.sqlite.7z | app/src/main/assets/radar_archives et radar_sources/speedalert-europe-202606 |
| ch-camera.sqlite.7z | app/src/main/assets/radar_archives et radar_sources/speedalert-europe-202606 |
| commons-compress-1.28.0.jar | app/libs |
| commons-io-2.20.0.jar | app/libs |
| commons-lang3-3.18.0.jar | app/libs |
| commons-codec-1.19.0.jar | app/libs |
| xz-1.12.jar | app/libs |
| roadguard-radar-runtime-1.jar | app/libs — version isolée des cinq bibliothèques, utilisée dans l’APK |

Empreintes/provenance : verification/dependencies-11.14.245.json. Contrôle local avant build : app/radar-dependencies.json. Licences : app/src/main/assets/vendor.

Les cinq JAR d’origine sont conservés pour provenance ; Gradle utilise leur runtime isolé pour éviter le conflit de classes rencontré dans l’image Android 16. Règles et script de reproduction fournis dans tools. Le runtime est déjà livré : aucun outil de transformation à télécharger pour compiler l’application.

Les deux variantes Android compilent et les tests d’extraction/import avec SQLite natif passent sur API 26/36. Les anciennes demandes d’autorisation ont été résolues après accord explicite ; aucun fichier ou accord supplémentaire demandé maintenant. Essais Xiaomi et autoradio regroupés en fin de lot. Voir WORK-11.14.246.md et verification/android-native-results-11.14.246.json.
