# ROADGUARD 11.14.119 — validation des itinéraires

- Version Android : 11.14.119 / code 319. Archive source uniquement; APK non compilée.
- Le tableau de bord indique les points gagnés par module avec une barre verte quand il progresse.
- Le parseur de route refuse désormais les coordonnées GPS hors limites et les géométries trop longues; il ne coupe plus discrètement une route au-delà de 15 000 points.
- Les métriques de distance et durée doivent rester finies et dans les bornes acceptées. Un trajet invalide ne remplace pas le parcours précédemment sauvegardé.
- Préchargement cartographique toujours retiré. Carte en ligne avec cache WebView ordinaire uniquement.
- Interface/graphisme six écrans 100 % côté source; vérification compilée sur Xiaomi nécessaire. Caméra laissée intacte.
- Build impossible ici : Android SDK 34, Gradle et wrapper absents.

Consulter `SUIVI-ACTIF-ROADGUARD.md`, `TESTS-ET-BLOCAGES.md` et `TACHES-RESTANTES.md`.
