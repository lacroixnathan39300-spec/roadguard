# ROADGUARD 11.14.291 — démarrage de la carte hors ligne

Le projet complet remplace la version 11.14.290. applicationId inchangé : fr.roadguard.app ; versionCode 491.

## Changements

- map_lifecycle.js : setTimeout et clearTimeout sont liés à window. L'ancien appel via this.set() provoquait « Illegal invocation » dans Chromium avant la déclaration de map et avant la création du moteur. Les tests simulés ne vérifiaient pas cette contrainte du navigateur.
- map_maplibre.html : setStyle(style,{diff:false}) garantit le nouvel événement style.load dont dépendent la disponibilité du moteur, les surcouches et le GPS. La mise à jour différentielle chargeait des tuiles mais laissait ready=false.
- Tests avec le vrai bundle MapLibre, son worker et WebGL ; suivi/version mis à jour. Pas de modification du catalogue radar, des téléchargements, des comptes ni du site.

## Vérification

Voir verification/browser-startup-11.14.291.log. Scénarios comparatifs : échec original, échec partiellement corrigé, démarrage et tuiles locales corrigés. Les tuiles du test sont synthétiques ; ce résultat ne prouve pas la couverture ni le rendu des packs pays du téléphone.

Tests ciblés réussis : 13 scénarios de démarrage simulés ; 98 contrôles du pipeline Java réel avec adaptateurs ; 49 contrôles de politique d'origine/bridge avec adaptateurs ; syntaxe des deux pages de carte. Journaux dans verification/*11.14.291.log.

Limite du banc ancien : tools/test_map_pack_runtime.js s'arrête sur un helper rgRefreshNightCanvas absent de son contexte simulé (déjà absent dans la version 290) ; ce test n'est pas compté comme réussi. Aucune compilation APK ni validation Android physique pour cette livraison. Les anciens journaux de compilation présents dans l'archive ne concernent pas la version 291.

## 🔴 INTERVENTION OBLIGATOIRE — validation sur téléphone

1. Importer ce projet dans CodeAssist et compiler la version 11.14.291.
2. Installer la mise à jour avec la même signature, sans désinstaller ROADGUARD ni effacer ses données : conserver les packs France/Suisse déjà présents.
3. Ouvrir la carte, choisir Hors ligne puis, si nécessaire, « Relancer la carte » ; tester sans Wi-Fi ni données mobiles.
4. Vérifier l'affichage des routes, déplacer/zoomer la carte et consulter le diagnostic : le compteur de requêtes/tuiles locales doit augmenter dans une zone couverte. Les packs max zoom 14 peuvent être surzoomés.
5. Si le fond reste vide, envoyer les captures de la carte et du diagnostic après quelques secondes, avec le numéro de version.

Le tableau courant est dans SUIVI-ACTIF-ROADGUARD.md. Les pourcentages sont des estimations historiques non réauditées, inchangées ; ils ne constituent pas une validation de la carte hors ligne.
