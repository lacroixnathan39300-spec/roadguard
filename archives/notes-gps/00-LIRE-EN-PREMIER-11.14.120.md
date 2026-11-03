# ROADGUARD 11.14.120 — radars locaux

- Version Android : 11.14.120 / code 320. Archive source uniquement, APK non compilée.
- L’import des données radar rejette les coordonnées non finies, hors limites et les libellés invalides.
- L’expiration est appliquée dès le chargement et l’ouverture des alertes, sans attendre un fix GPS; le service arrière-plan filtre également les points périmés.
- Radars estimés 35→40 %; global estimé 58→59 %, non audité.
- Les six écrans graphiques restent estimés à 100 % côté source; captures Xiaomi à confirmer.
- Préchargement retiré, cache WebView normal; caméra reportée à la toute fin.
- Lire `SUIVI-ACTIF-ROADGUARD.md`, `TESTS-ET-BLOCAGES.md` et `TACHES-RESTANTES.md`.
