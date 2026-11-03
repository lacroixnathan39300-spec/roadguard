# ROADGUARD — mise à jour cartes du 26 octobre 2026

DRIVE 1.9.0 : APK universel ARM 32/64 bits, Android API 27 minimum. Reprend les corrections API 27 de 1.8.1. Le compagnon téléphone reste en 1.0.2 (APK intégré identique).
GPS seul 11.14.292 : API 26 minimum, sans bibliothèque native dépendante de l’architecture.

Cartes téléchargées prioritaires, même avec Internet : PMTiles vectorielles et tuiles XYZ importées. Le réseau complète les tuiles absentes. Préchargement local limité autour de la position GPS ; caches bornés pour l’autoradio 2 Go. Le démarrage du moteur vectoriel reçoit la configuration locale avant de construire la carte. Les vues natives DRIVE utilisent également les cartes locales.

Couleurs : six palettes jour/nuit et personnalisation de dix couleurs vectorielles, enregistrées séparément pour le jour et la nuit. Les cartes images acceptent une teinte globale ; leurs routes et bâtiments ne peuvent pas être recolorés individuellement. DRIVE : Réglages > Couleurs de la carte. GPS : choix de palette / Personnaliser. Aucun changement des cartes d’applications tierces.

Validation : compilation des deux APK ; signatures conservées ; ZIP CRC ; architectures ELF ; compagnon intégré identique ; 30 tests DRIVE ciblés API 27/28 ; 8 tests GPS ciblés API 27/28 ; pipeline de tuiles locales 12 contrôles ; vérifications JavaScript démarrage/palettes/réseau ; lint API DRIVE sans problème. Les tests ne remplacent pas un essai sur l’autoradio et le téléphone.

Un ancien test statique test_pmtiles_local_import.py contient des assertions dépassées sur le nom de fichier et le texte d’import ; il ne fait pas partie de ces contrôles réussis. Le chemin d’import existant n’a pas été modifié.

Provenance DRIVE : 155 ressources référencées dans BASELINE.json, dont quatre vues cartographiques modifiées et deux ressources ajoutées. Les données de radars ne sont pas modifiées. Voir roadguard/MAP-UPDATE-11.14.292.json.

Reconstruction : JDK 21, Gradle 8.13, Android SDK 36, build-tools 35.0.0. DRIVE : :app:assembleGithubDebug, par défaut armeabi-v7a + arm64-v8a. GPS : :app:assembleDebug. Le compagnon peut être reconstruit depuis les sources incluses. Pour conserver la signature d’une installation existante, les clés de signature doivent être fournies séparément via ROADGUARD_DEBUG_KEYSTORE / ROADGUARD_GPS_DEBUG_KEYSTORE ; aucune clé privée n’est présente dans ces archives.
