# Validation de livraison 11.14.179

- Gradle 8.2.1 / AGP 8.2.2 : assembleDebug réussi, compilation Java, ressources AndroidX et DEX générés.
- APK fr.roadguard.app, versionName 11.14.179, versionCode 379; présence des classes CarAppService, CarScreen, GpsMotion et RadarSettings dans les DEX.
- Ressources embarquées map.html, reference_ui.html, camera_motion.js et roadguard_progress.json identiques aux sources livrées.
- Signature APK v2/v3 et alignement 4 octets vérifiés; même certificat de vérification que la 11.14.178.
- 35 groupes de régression réussis : WORK/regressions-11.14.179.json.
- Tests Java réels : WORK/motion-tests-11.14.179.log, WORK/catalog-tests-11.14.179.log, WORK/http-tests-11.14.179.log.
- Contrat Android Auto vérifié statiquement : WORK/android-auto-contract-11.14.179.log; ne remplace pas un essai d’hôte.
- Aucun lancement Android/Xiaomi/DHU/véhicule, aucun test 500 000 connexions, aucune publication Play.

APK SHA-256 : 55b3625c4e6be4dbd9e22d5b03a2093d82a27d92a35ecb525570f3c14711a590
