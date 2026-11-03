# Validation 11.14.182

- Compilation `:app:assembleDebug` réussie avec SDK 34. APK `versionCode=382`, `versionName=11.14.182`.
- Signature de vérification v2/v3 validée; certificat SHA-256 `52ff1a034ff05a11847435e25c0f2077614fe278290818b42b4bad198502ffa3`, identique à l’APK de vérification 11.14.181.
- Relecture du parcours : bouton visible sur la carte, permission de superposition puis localisation, lancement du service depuis l’écran visible, retour d’état selon l’ajout effectif de la vue flottante.
- Test réel Xiaomi, présence de la bulle sur l’accueil/Waze et batterie/arrière-plan : à effectuer sur l’appareil. Les 87 tests Robolectric restent inaccessibles ici à cause de la dépendance Maven instrumentée; aucune réussite simulée nouvelle n’est annoncée.
