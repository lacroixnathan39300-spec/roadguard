# ROADGUARD 11.14.190 — correctif de compilation CodeAssist

Le journal transmis montre que CodeAssist ne trouve pas la bibliothèque AndroidX Car App (`androidx.car.app`) ni ses dépendances AndroidX. La déclaration Gradle était déjà présente, mais ne suffisait pas sur cette installation.

La compilation par défaut contient désormais l'application téléphone, sans aucune classe dépendant d'AndroidX Car dans `app/src/main/java` ni service Android Auto dans le manifeste. Les cinq classes Android Auto restent dans `app/src/auto/java`; leur manifeste est dans `app/src/auto/AndroidManifest.xml`.

Pour construire plus tard la variante Android Auto, après avoir configuré CodeAssist pour résoudre les bibliothèques depuis Google Maven, compiler avec `-ProadguardAndroidAuto=true`. La variante téléphone construite par défaut **n'apparaît pas dans Android Auto**. Les correctifs 11.14.189 du bandeau de trajet et de la couleur sont inclus dans les deux variantes.

Importer ce projet neuf dans CodeAssist et lancer la compilation habituelle sans propriété additionnelle. Garder la signature habituelle pour installer par-dessus la version déjà utilisée. Voir `WORK/VALIDATION-11.14.190.md`.
