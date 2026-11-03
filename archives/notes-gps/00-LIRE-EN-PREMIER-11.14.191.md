# ROADGUARD 11.14.191 — variante Android Auto

La propriété `roadguardAndroidAuto=true` est activée dans `gradle.properties` : une compilation normale inclut les cinq classes voiture, les bibliothèques AndroidX Car et le manifeste Android Auto. Le même APK conserve l'application téléphone.

Build Gradle : `gradle -ProadguardAndroidAuto=true :app:clean :app:assembleDebug`.
Téléphone seul si CodeAssist ne résout pas AndroidX : `gradle -ProadguardAndroidAuto=false :app:clean :app:assembleDebug`, ou mettre la propriété à false dans gradle.properties. Les tests Android Auto sont exclus de ce build téléphone. Activer true ne répare pas à lui seul la résolution des dépendances dans CodeAssist : Google Maven doit être accessible.

## Installation et données

L'APK de vérification éventuellement inclus dans `LIVRAISON/` utilise la clé de test du projet, distincte de ta clé CodeAssist. Il peut remplacer une ancienne APK seulement si les signatures correspondent. Si Android refuse la mise à jour pour signature différente, ne désinstalle pas ton application pour forcer l'installation : reconstruis les sources avec ta signature CodeAssist afin de conserver tes données. La clé de vérification n'est pas une clé de publication.

## Affichage dans la voiture

L'APK installé directement permet de préparer l'application sur téléphone, mais ne garantit pas son apparition sur l'écran du véhicule. D'après Google, les applications Android for Cars App Library comme ROADGUARD doivent être installées depuis une source approuvée pour un essai réel : partage interne Google Play ou piste de test. L'option « sources inconnues » n'est pas applicable à cette catégorie. Aucun envoi vers Google Play n'a été effectué. Le projet conserve targetSdk 28 et nécessite une modernisation avant une diffusion Play conforme aux exigences en vigueur.

Référence vérifiée le 18/10/2026 : https://developer.android.com/training/cars/testing#real-vehicles

## Protocole restant

1. Sur téléphone : préparer un trajet et des favoris, autoriser la localisation précise et activer le GPS.
2. Sur DHU : vérifier carte, position, favoris, manœuvres, arrivée, arrêt et changement de trajet; tester aussi l'absence de permission et la perte réseau.
3. Depuis un canal de test Play accepté : contrôler la visibilité dans le véhicule, les déconnexions/reconnexions, les appels et la musique, puis le guidage dans les conditions réelles.

Les contrôles de compilation et tests effectués sont consignés dans WORK/VALIDATION-11.14.191.md. Aucun test DHU, véhicule ou Xiaomi réalisé ici.

Toujours nettoyer le build en passant de téléphone seul à Android Auto ou inversement : les deux modes utilisent le même nom de variante Gradle debug. Le script le fait automatiquement.
