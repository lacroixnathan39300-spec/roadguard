# ROADGUARD 11.14.192 — Android Auto / Android 16

L'APK de vérification contient à la fois le téléphone et Android Auto. Il est également livré directement, sans devoir compiler. Le ZIP conserve les sources, le dossier WORK et une copie de l'APK dans LIVRAISON.

## Nouveau dans cette version

- Recherche d'une ville/adresse depuis l'écran voiture, avec six résultats maximum, gestion du réseau et coordonnées sans serveur de recherche. Les favoris du téléphone restent disponibles.
- Cible et compilation Android 16 / API36; permissions GPS fine et approximative demandées ensemble, notifications Android13+, retour Android16 et marges adaptées aux barres système, encoche et clavier.
- Perte ou retrait du GPS : les manœuvres obsolètes sont retirées du trajet transmis à l'hôte. Reprise d'une session depuis le trajet enregistré.
- Libération des surfaces voiture lors d'un remplacement, débranchement ou arrêt; arrêt des callbacks et traitements associés.
- 156 tests Android simulés réussis, dont 95 dans les suites voiture; lint sans erreur bloquante. Ce résultat n'est pas un essai de voiture réelle.

## Compilation reproductible

JDK17 minimum pour compiler; JDK21 pour les tests Robolectric API36. SDK36, build-tools35, Gradle8.11.1 et AGP8.10.1. Le wrapper Gradle livré est épinglé par SHA256.

- Android Auto : `./gradlew -ProadguardAndroidAuto=true :app:clean :app:assembleDebug`.
- Téléphone seul : `./gradlew -ProadguardAndroidAuto=false :app:clean :app:assembleDebug`.
- Tests et analyse : `./gradlew :app:testDebugUnitTest :app:lintDebug` sous JDK21.

Le nettoyage est requis lors d'une bascule de mode. `tools/build_verified_apk.sh --auto` ou `--phone` le fait automatiquement. Android Auto est activé par défaut dans gradle.properties. Les dépendances nécessitent l'accès aux dépôts Maven au premier build; compatibilité de l'environnement CodeAssist personnel avec ce nouveau toolchain non vérifiée ici.

## Signature et données

L'APK utilise la même clé de vérification que la 11.14.191 fournie ici. Une ancienne installation CodeAssist peut utiliser une autre signature. Si Android refuse la mise à jour, ne désinstalle pas ton application pour forcer l'installation : reconstruis avec ta clé habituelle pour conserver tes données. La clé de vérification incluse n'est pas une clé de publication.

## Limite actuelle Android Auto

La cible API36 supprime le blocage de cible Android ancienne, mais n'effectue ni inscription ni validation Google Play. Pour les applications Car App Library, une installation directe ne garantit pas la visibilité dans un vrai véhicule; Google indique d'utiliser une installation approuvée telle que le partage interne ou une piste de test Play. L'option « sources inconnues » ne s'applique pas à cette catégorie.

Aucun compte Play, téléphone ou écran voiture n'est connecté à cet environnement. Le démarrage téléphone verrouillé, les interruptions audio, les tailles réelles d'écran et la reconnexion doivent encore être éprouvés sur un véritable hôte. Il n'existe pas ici de preuve de fonctionnement à 100 % dans ta voiture.

Documentation vérifiée le 18/10/2026 :
- https://developer.android.com/training/cars/testing
- https://developer.android.com/google/play/requirements/target-sdk
- https://developer.android.com/about/versions/16/behavior-changes-16

Voir WORK/VALIDATION-11.14.192.md et WORK/ANDROID-AUTO-RECETTE-11.14.192.md.
