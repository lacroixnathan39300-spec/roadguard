# Validation 11.14.190

- Les cinq sources qui importent `androidx.car.app`, `androidx.core` ou `androidx.lifecycle` sont hors de `app/src/main/java` et incluses uniquement avec `-ProadguardAndroidAuto=true`.
- Le manifeste téléphone ne déclare pas le service Android Auto; le manifeste optionnel le conserve. XML des deux manifestes analysé avec succès.
- Les dépendances Car App et Car App Projected sont conditionnelles à la propriété Gradle. `CarRouteData` et `CarLocationService`, utilisés aussi par le téléphone, restent dans le build par défaut.
- Syntaxe des scripts JavaScript de la carte vérifiée; l'archive ZIP est contrôlée.
- Compilation APK et essai Xiaomi non effectués ici faute de SDK Android et de Gradle. La compilation dans CodeAssist reste à vérifier avec le journal complet en cas d'erreur suivante.
