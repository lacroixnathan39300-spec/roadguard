# ROADGUARD 11.13.71 — reprise et état réel

Base : ZIP ROADGUARD 11.13.70. Version Android source 11.13.71 (versionCode 194).

## Réalisé
- `OfflinePackSync.drain` : traite 1 à 16 packs maximum par déclenchement (Android : 4), en ordre de proximité, arrête sur échec, absence de pack ou désactivation.
- `MainActivity.maybeSyncNearbyPacks` : appelle `drain`, vérifie connexion et option utilisateur entre les packs, consigne le nombre enregistré et restant.
- Test `tests/OfflinePackBatchTest.java` : 6 packs simulés, 4+2, aucun doublon, arrêt, échec, limite de lot.
- Test de régression `tests/OfflinePackSyncTest.java` réussi.

## Limites critiques
- **Aucun catalogue `app/src/main/assets/offline_pack_catalog.tsv` fourni** : aucun téléchargement de carte réelle n'est possible sur Xiaomi.
- Les données de test ne sont pas des cartes. Aucun hébergement HTTPS opérationnel, aucune extraction réelle Jura.
- Pas de compilation Android / APK, ni test Xiaomi. Pas de WorkManager ni de service persistant en arrière-plan.
- L'affichage MapLibre des packs enregistrés n'est pas validé. La source OSM publique ne doit pas servir au préchargement massif.
- Le déclenchement dépend encore des événements GPS et du premier plan de synchronisation ; un lot restant sera repris au déclenchement suivant.

## Prochaines priorités
1. Produire un petit PMTiles réel avec source compatible et contrôler son affichage.
2. Publier un catalogue HTTPS autorisé avec packs PMTiles et empreintes SHA-256.
3. Raccorder la lecture des packs persistants à MapLibre et tester hors réseau.
4. Implémenter un travail Android durable (WorkManager) avec reprise, puis compiler et tester sur Xiaomi.

## Avancement
Global 42 % **historique estimé**, inchangé. GPS 100 % historique, carte en ligne 85 %, planification 50 km 70 %, mémoire 55 %, téléchargement réel 20 %, arrière-plan 5 %, affichage hors ligne 30 %, APK/Xiaomi 10 %. Aucun chiffre ne constitue une validation APK.
