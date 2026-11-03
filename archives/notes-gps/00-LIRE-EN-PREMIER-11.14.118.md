# ROADGUARD 11.14.118 — cache cartographique simplifié

- Code Android : 318. Archive source uniquement; aucune APK compilée.
- Le téléchargement anticipé des cartes et sa configuration ont été retirés. La carte reste en ligne; Android/WebView conserve uniquement son cache normal pour les tuiles demandées pendant l’affichage.
- Une coupure réseau peut laisser réapparaître des tuiles consultées, si Android les a gardées. Durée et couverture ne sont pas garanties.
- GPS, navigation, signalements, radars et interface conservés. Caméra laissée intacte pour la fin du projet.
- Finition visuelle des six écrans : 100 % côté code source; vérification sur Xiaomi après compilation encore nécessaire.
- Compilation bloquée dans cet environnement : SDK Android 34, Gradle et wrapper absents.

Voir `SUIVI-ACTIF-ROADGUARD.md`, `TESTS-ET-BLOCAGES.md` et `TACHES-RESTANTES.md`.
