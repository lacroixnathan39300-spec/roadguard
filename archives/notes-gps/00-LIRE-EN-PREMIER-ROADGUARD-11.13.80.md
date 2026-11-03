# ROADGUARD 11.13.80 — cache disque temporaire, non raccordé

## Réalisé
- Nouvelle classe Java `EmergencyTileDiskCache` : stockage atomique des octets de tuiles, lecture, TTL, éviction par ancienneté, quota disque, validation des coordonnées.
- Nouveau test `tests/EmergencyTileDiskCacheTest.java` : écriture/lecture, éviction, expiration, coordonnées invalides, dépassement de taille.
- `javac` et test Java réussis sur l'environnement de travail.

## Non réalisé / blocages
- Aucun téléchargement automatique de 50 km, aucun raccordement du cache au WebView/MapLibre, aucun test de rendu hors connexion.
- Le fournisseur actuel `tile.openstreetmap.org` interdit le préchargement massif. Ne PAS brancher de préchargement automatique sur ce serveur. Configurer un fournisseur explicitement autorisé ou un serveur de tuiles propre avant intégration réseau.
- Pas de compilation APK, ni test réel Xiaomi. Le cache disque est un composant autonome testé, non une fonction utilisable dans l'application.
- Le TTL basé sur la date de modification peut être prolongé par une lecture (durée d'inactivité), ce n'est pas une durée absolue depuis téléchargement.
- Les anciens modules PMTiles sont encore présents dans le dépôt historique; ils ne constituent plus la cible produit.

## Priorités
1. Source cartographique autorisant la mise en cache anticipée, conditions et attribution vérifiées.
2. Relier un gestionnaire réseau avec quotas/consentement à `EmergencyTilePlanner` et `EmergencyTileDiskCache`.
3. Intercepter uniquement les requêtes de la source autorisée dans WebView et servir les tuiles cache lorsque hors réseau.
4. Valider sur APK Xiaomi le GPS bleu, la caméra, les tuiles visibles et la coupure/retour réseau.

## Suivi estimatif
GPS 100% (historique, non revalidé); carte en ligne 85% (historique); bascule réseau 50%; planification 45%; cache disque autonome 40%; préchargement réel 0%; coupure 25%; reprise 35%; validation Xiaomi 0%. Global non calculé. Ces pourcentages sont des estimations et ne signifient pas une validation réelle.
