# ROADGUARD 11.13.59 — Reprise prioritaire

## État réel
Projet source, pas d'APK compilée. Avancement global historique 42 % (estimation non auditée). Le GPS, la caméra et l'UI existants n'ont pas été modifiés dans cette version.

## Travail effectué
- `OfflinePackRegionSelector.java` : sélection de packs cartographiques d'un catalogue fiable intersectant le rayon GPS 50 km, classement par proximité, exclusion des packs déjà vérifiés dans `OfflineCellStore`.
- Prise en charge des régions traversant l'antiméridien.
- `tests/RegionSelectorTest.java` : simulation Jura → Lyon → Marseille, persistance et absence de doublons.
- Version Gradle 11.13.59 (code 188).

## Tests réellement exécutés
`javac` sur NearbyOfflinePlanner, OfflineCellStore, VerifiedMapDownloader, OfflinePackRegionSelector et RegionSelectorTest : réussi.
`java RegionSelectorTest` : réussi.
Aucun build Android, aucun test Xiaomi, aucune connexion au fournisseur en production.

## Limitations majeures
- Pas de catalogue distant réel autorisé et disponible; les URL du test sont des fixtures et ne doivent jamais être utilisées comme fournisseur.
- Le sélecteur n'est PAS encore raccordé à MainActivity ni à WorkManager; aucun téléchargement GPS automatique effectif.
- Les packs régionaux ne correspondent pas aux cellules de 5 km du planificateur; faire converger l'inventaire sur des identifiants de packs et leurs vraies bounding boxes, ne pas marquer les cellules fictivement téléchargées.
- Pas de serveur de cartes, pas de couverture France/Corse garantie, pas de rendu MapLibre de la bibliothèque vérifié.

## Suite prioritaire
1. Produire/obtenir un catalogue HTTPS autorisé de packs PMTiles régionaux avec bbox, URL, SHA-256, taille, attribution et licence; assurer leur hébergement.
2. Charger/valider le catalogue dans Android, raccorder `OfflinePackRegionSelector` à la position GPS et à `VerifiedMapDownloader`, avec limites de données et réseau.
3. Ajouter un vrai service de travail en arrière-plan et les règles Android.
4. Brancher les packs téléchargés au rendu MapLibre hors connexion; tests sur Xiaomi.
5. Garder les archives précédentes en bibliothèque sans purge automatique.

## Règle de passation
Chaque ZIP doit inclure les changements réellement effectués, tests, blocages, pourcentages et prochaines actions. Ne jamais annoncer une APK ou un téléchargement effectif non testés.
