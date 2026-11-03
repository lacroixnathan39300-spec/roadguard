# ROADGUARD 11.13.58 — Téléchargeur HTTPS vérifié (socle Java)

Version précédente : 11.13.57. Progression globale historique estimée **42 %**, non auditée. Ce ZIP est un projet source, **pas une APK**.

## Fait et testé
- Ajout `app/src/main/java/fr/roadguard/app/VerifiedMapDownloader.java` : télécharge un pack dont l'URL, l'identifiant, le SHA-256 et la taille maximale sont connus, puis l'importe dans `OfflineCellStore`.
- Ne retélécharge pas un pack déjà vérifié. HTTPS obligatoire en production; redirections refusées; délais réseau; taille maximale et SHA-256 vérifiés avant inscription au registre.
- Test Java avec serveur HTTP local : transfert, persistance après réouverture, anti-doublons, mauvais SHA, taille excessive, redirection et refus HTTP de production. Tous réussis.
- `app/build.gradle` : versionName 11.13.58, versionCode 187.

## Limites critiques
- **Aucun fournisseur / catalogue HTTPS de packs France+Corse n'est configuré**. Le téléchargeur ne peut pas fabriquer des données cartographiques à partir de l'affichage en ligne existant.
- **Non raccordé à MainActivity ni au GPS, ni à un worker Android** : pas de téléchargement automatique 50 km dans l'APK.
- Aucun pack de France hébergé, aucune APK compilée, aucun test Xiaomi, aucun affichage hors ligne validé.
- L'outil utilise un téléchargement synchrone et un import atomique; prévoir exécution hors thread UI, gestion réseau Android et reprise par blocs.
- Les données de test sont générées localement; elles ne sont pas des cartes PMTiles.

## Priorités
1. Obtenir une source autorisée de packs PMTiles géographiques avec catalogue HTTPS et SHA-256, ou construire et héberger ces packs à partir d'un jeu de données autorisé.
2. Brancher un catalogue fiable (IDs compatibles `NearbyOfflinePlanner`) et un téléchargement Android hors UI.
3. Relier l'inventaire à MapLibre hors ligne, tests appareil GPS/caméra.
4. Vérifier autorisations réseau, quota stockage, reprise et fonctionnement sur Xiaomi.

## Reprise
Ouvrir ce fichier, `SUIVI-AVANCEMENT.md`, `TRAVAUX-REALISES.md`, `TACHES-RESTANTES.md` et `TESTS-ET-BLOCAGES.md`. Ne pas déclarer téléchargement automatique fonctionnel.
