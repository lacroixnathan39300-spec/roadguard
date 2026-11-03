# ROADGUARD 11.13.73 — PRIORITÉ AUX PACKS VÉRIFIÉS

## État réel
- Correction MainActivity.serveOfflineTile : les PMTiles de `offline-cells` dont le SHA-256 est vérifié sont désormais prioritaires sur les archives historiques de `pmtiles`.
- Nouvelle classe `LocalPmtilesResolver.java` : résolution sécurisée d'identifiant, priorité vérifiée, repli explicite vers les imports historiques.
- Nouveau test `tests/LocalPmtilesResolverTest.java` : priorité, repli historique, rejet identifiant dangereux, corruption du pack.
- Test Java exécuté et réussi avec `samples/demo-firenze/demo-firenze.pmtiles`.
- Le GPS bleu, la caméra et la logique de suivi n'ont pas été modifiés.

## Commande de test
`javac -d classes app/src/main/java/fr/roadguard/app/{LocalPmtilesResolver,OfflineCellStore,NearbyOfflinePlanner,OfflinePackRegionSelector,VerifiedMapDownloader}.java tests/LocalPmtilesResolverTest.java`
`java -cp classes LocalPmtilesResolverTest samples/demo-firenze/demo-firenze.pmtiles`

## Blocages
- Pas de carte du Jura téléchargée et hébergée en HTTPS.
- Pas de compilation APK Android ni de test sur Xiaomi dans cette session.
- Le repli sur les imports historiques ne constitue pas une vérification SHA-256 ; cette compatibilité reste volontaire.
- Pour une carte hors ligne réellement fonctionnelle : obtenir pack Jura, valider les tuiles, intégrer et tester APK.

## Avancement estimé (inchangé, pas de validation mobile)
Global historique 42 %. GPS 100 %, carte en ligne 85 %, planification 50 km 70 %, bibliothèque 55 %, téléchargement réel 20 %, arrière-plan 5 %, affichage hors ligne 30 %, tests Xiaomi 10 %.

## Prochaines tâches prioritaires
1. Obtenir un vrai pack PMTiles du Jura depuis une source autorisant l'extraction, et le vérifier.
2. Tester lecture réelle de tuiles et rendu vectoriel, pas seulement le chargement d'en-tête.
3. Héberger le pack et publier catalogue HTTPS avec tailles et empreintes SHA-256.
4. Construire APK, vérifier sur Xiaomi carte/GPS/caméra en mode avion.
