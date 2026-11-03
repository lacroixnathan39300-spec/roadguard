# ROADGUARD 11.14.199 — intégration lecture des bases radar autorisées

## Réalisé
- Ajout de UpdatedRadarCatalog : lit les bases FR/CH téléchargées dans files/radar_updates, vérifie leur format via BundledRadarCatalog.read, et revient aux bases embarquées si absentes ou invalides.
- Branché sur MainActivity, BackgroundAlertService et CarRouteData (Android Auto).
- Le téléchargeur AuthorizedRadarImporter de 11.14.198 reste disponible : appeler `AuthorizedRadarImporter.downloadAndVerify(source, UpdatedRadarCatalog.directory(context))` sur un thread de fond avec URL HTTPS et SHA-256 autorisés.

## Restant
- Fournir les URLs et SHA-256 de bases autorisées ; aucune URL privée n'est inventée.
- Ajouter l'interface de sélection/téléchargement et un rechargement après mise à jour ; pour le moment, les données téléchargées sont lues au prochain chargement des radars.
- Vérifier licences, qualité, doublons, dates et réglementation par pays.
- Compiler et tester sur téléphone et Android Auto.

## Validation
- Inspection statique des trois appels remplacés. Aucune compilation ni test sur appareil effectué.
- Avancement global non chiffré faute d'audit fonctionnel complet. Ne pas extrapoler de pourcentage.
