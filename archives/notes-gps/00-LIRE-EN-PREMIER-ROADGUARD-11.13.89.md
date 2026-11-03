# ROADGUARD 11.13.89 — isolation du cache par fournisseur

## Modifications effectivement réalisées
- Ajout `EmergencyProviderCache.java` : dossier SHA-256 déterministe par URL complète de fournisseur, sans exposer la clé API dans le nom du dossier.
- `MainActivity.java` : le préchargement, le diagnostic et l'interception WebView utilisent le même dossier propre au fournisseur.
- L'interception des tuiles de secours vérifie également l'accord explicite de préchargement.
- Ajout du test `EmergencyProviderCacheTest.java`.

## Tests
- `javac` et six tests Java réussis : BufferAudit, PlanCursor, ProviderCache, TileDiskCache, TileGateway, TilePlanner.
- Pas de compilation APK, pas de validation sur Xiaomi, pas de test réel de perte de réseau.

## Limites et blocages
- Aucun fournisseur avec droits de préchargement n'est préconfiguré. L'application ne précharge donc rien par défaut.
- Rayon 20 km, plan de tuiles limité aux zooms 9–11 et à 240 tuiles. La couverture détaillée de toutes les rues n'est pas garantie.
- Les anciens fichiers du cache partagé `emergency-tiles` ne sont pas réutilisés, afin d'éviter les mélanges de fournisseurs.
- Cache par fournisseur plafonné à 128 Mio; plusieurs fournisseurs peuvent occuper davantage au total. Nettoyage global à développer.
- Les taux d'avancement sont des estimations, non des validations sur téléphone.

## Prochaines étapes prioritaires
1. Configurer une source HTTPS XYZ qui autorise expressément préchargement et conservation temporaire.
2. Construire l'APK Android, puis vérifier GPS bleu, radar et caméra.
3. Tester réseau disponible, préchargement, coupure réseau, retour du réseau sur Xiaomi.
4. Ajouter un plafond de stockage global multi-fournisseurs et un diagnostic de couverture détaillée.
