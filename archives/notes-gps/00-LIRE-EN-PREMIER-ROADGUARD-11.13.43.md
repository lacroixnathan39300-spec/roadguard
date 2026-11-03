# ROADGUARD 11.13.43 — Installation locale avec restauration

## Etat confirmé
- La capture du Xiaomi affiche encore l'APK BÊTA 11.13.10 ; le ZIP source ne met pas à jour l'APK.
- Aucun pack France + Corse réel ni APK compilée n'est inclus dans ce ZIP.
- Le code GPS bleu et la caméra ne sont pas modifiés.

## Modification réellement effectuée
- `MainActivity.java` : installation de l'archive PMTiles importée en deux étapes (.new, .old), restauration automatique de l'ancienne carte si le remplacement échoue, conservation de l'empreinte après installation.
- Le sélecteur Android de fichiers de 11.13.42 reste en place. Il nécessite une sélection explicite de l'utilisateur et copie le fichier : **aucune lecture zéro-copie n'est implémentée**.
- Android 11+ peut interdire la sélection des fichiers stockés dans Android/data ou Android/obb via ACTION_OPEN_DOCUMENT. La récupération des 2,76 Go reste non confirmée.

## Validation
- Vérification statique des points d'intégration et ZIP testzip. Pas de compilation Java/Android ni de test Xiaomi.

## Priorités
1. Obtenir un véritable fichier PMTiles France + Corse ou récupérer l'archive du téléphone.
2. Compiler et installer une APK récente, vérifier MapLibre, GPS, carte hors ligne.
3. Vérifier l'emplacement et le format des 2,76 Go avant de promettre leur réutilisation.

## Blocage
L'environnement ne dispose pas d'un SDK Android/Gradle installé, et aucun fichier cartographique France réel n'a été fourni.
