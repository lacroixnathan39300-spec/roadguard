# WORK — ROADGUARD 11.14.192

## Fait / changements

Voir TRAVAUX-REALISES.md. Cible API36 et compilation modernisée; recherche depuis Android Auto; permissions et notifications modernes; retrait GPS et données périmées sans manœuvre trompeuse; surfaces libérées, callbacks détachés et reprise de session. Le moteur existant de caméra n'a pas été modifié. Aucun téléchargement France intégrale ajouté.

## Tests réels de cette passe

- OpenJDK21 / Gradle8.11.1 / AGP8.10.1 / compileSdk et targetSdk36.
- assembleDebug Android Auto réussi; mode téléphone seul assembleDebug et compilation de ses tests réussis. Les classes voiture et tests voiture sont exclus du mode téléphone.
- 156 tests Robolectric réussis, zéro échec : 68 AndroidRuntime, 18 recherche voiture, 9 surfaces voiture, 5 intégration Android16, 36 téléphone et 20 cache. Les suites utilisent API26/28/34/36 ou un sous-ensemble indiqué dans leur source.
- Lint : zéro erreur bloquante, 61 avertissements. Les avertissements restent visibles; aucun baseline ou désactivation globale n'a été ajouté pour obtenir ce résultat. Deux constantes LinearLayout héritées ont été remplacées par les constantes nommées.
- Contrat statique Android Auto, test JavaScript des 13 écrans, syntaxe shell et contrôle de passation réussis.
- Recherche publique du serveur : requête Champagnole réussie, deux résultats; réponse conservée dans public-place-search-11.14.192.json. Aucun compte connecté utilisé pour ce contrôle.
- JDK21 requis pour les tests API36 selon Robolectric4.16.1; compilation compatible JDK17 minimum. La confiance TLS et le proxy du runtime ont été configurés dans l'environnement de vérification seulement, sans affaiblir TLS dans l'application.

## Ce qui n'est pas validé

Aucun téléphone réel, hôte DHU, écran voiture, musique/appel réel ou trajet mesuré. Les tests simulent les composants Android et ne valident pas tous les contrôles système des services GPS en arrière-plan. La cible36 satisfait le niveau de cible publié, mais ne constitue ni publication ni approbation Google Play. Aucun dépôt ni modification de compte externe effectué.

L'apparition en voiture doit être testée depuis une installation approuvée, selon la documentation Google. Les clés de publication et la configuration du canal Play de Nathan ne sont pas disponibles ici. L'APK est signé avec la clé de vérification déjà employée en191; une signature CodeAssist différente exige une recompilation avec la clé habituelle pour conserver les données.

La recette détaillée est dans ANDROID-AUTO-RECETTE-11.14.192.md. Restent également les tests Xiaomi et compte communautaire, et une source autorisant le préchargement pour garantir50km; le cache actuel n'offre pas cette garantie.

## Avancement

Estimations historiques non auditées : Android Auto85→90% (+5 points de réalisation), global pondéré94,61→95,22% (+0,61 point), affichage arrondi95% inchangé. Le fonctionnement à100% sur véhicule n'est pas revendiqué.

## Sources officielles consultées le18/10/2026

- https://developer.android.com/google/play/requirements/target-sdk
- https://developer.android.com/training/cars/testing
- https://developer.android.com/about/versions/16/behavior-changes-16
- https://developer.android.com/build/releases/agp-8-10-0-release-notes
- https://developer.android.com/reference/androidx/car/app/SurfaceCallback
- https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start
- https://github.com/robolectric/robolectric/releases/

## APK livrée

APK finale recompilée après contrôle téléphone seul; classes Android Auto et assets embarqués vérifiés. Signature v2/v3 et alignement4octets vérifiés. SHA256 : `9bc7eb02472e0d5ef17024256f1c337ce764089126bf37132b4fa53d3825d4c5`.
