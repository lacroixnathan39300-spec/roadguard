# Reprise ROADGUARD 11.14.135

Source Android / code 335, sans APK.

## Fait
- Recherche auto sur la carte après 750 ms de pause de saisie. En France, suggestions via l’API officielle d’autocomplétion de la Géoplateforme IGN, biaisées vers la position GPS; le Geocoder Android sert de secours et pour l’étranger. Les réponses anciennes sont ignorées et la sélection reste inline.
- Import d’un catalogue JSON dans Alertes via le sélecteur de fichiers Android. Limite 256 Kio et contrôles HTTPS, pays uniques, format, taille, SHA-256, provenance et licence.
- Les bases radar peuvent ensuite être téléchargées selon les fiches du catalogue et les contrôles déjà en place. Aucun pack étranger n’est inclus dans cette livraison.

## Tests et limites
Tests statiques ciblés et syntaxe des scripts cartographiques. APK non compilée; comportement réel du Geocoder Android et import SAF à valider sur Xiaomi. La recherche utilise le service de géocodage présent sur le téléphone; Android prévient que les résultats peuvent être indisponibles ou imprécis. Catalogue/radars à fournir, sources et licences à vérifier.

## Estimations
Navigation 48→50 % (+2), radars 80→81 % (+1), global 72→73 % (+1); non audités. Six écrans 100 % côté code, non vérifiés après compilation. Aucun préchargement; caméra en dernier.
