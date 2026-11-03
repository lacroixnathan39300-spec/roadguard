# ROADGUARD 11.14.244 — projet complet en un ZIP

Cette livraison reprend intégralement la 243 et ajoute :

- Ouverture de la carte pendant le chargement des catalogues radar sur un worker.
- Reprise bornée des tuiles visibles en échec et compteurs réseau explicites.
- Pack graphique local Graphite / Lisibilité / Couleurs OSM dans Apparence.
- Explication des sources externes facultatives WazeAPI et OpenWeb Ninja.
- Affichage séparé des points locaux, archives reçues et points effectivement importés.

À lire : `WORK-11.14.244.md`, `AVANCEMENT-11.14.244.md`, puis `FICHIERS-POUR-IMPORT-7Z.md` pour les sept fichiers toujours attendus. Sources, Gradle, ressources, radars d’origine, historique WORK et résultats des vérifications sont inclus.

Version 11.14.244 / 444, package inchangé, SDK 36 minimum 26. Android Auto reste activé par la propriété existante ; `-ProadguardAndroidAuto=false` isole le téléphone.

17 scénarios cache, 10 surcouches et 11 JavaScript passent, avec régressions de la 243. Le SDK Android, la compilation APK, la WebView réelle et le matériel ne sont pas validés ici. Aucun APK 244 n’est fourni. L’APK 197 conservé est historique.

Le pack graphique ne remplace pas les données routières. Aucun pack France permanent ni préchargement massif OSM n’est réactivé. La chaîne 7z attend encore les deux archives et cinq bibliothèques listées ; les radars d’origine restent présents.
