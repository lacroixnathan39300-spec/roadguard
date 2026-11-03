# Reprise ROADGUARD 11.14.118

## Décision prise
Le préchargement cartographique est abandonné. Aucun téléchargement anticipé autour du GPS ou vers des zones hors écran. L’OS conserve le cache normal des tuiles visibles/demandées; ne pas présenter cela comme une couverture hors ligne garantie.

## Changements
- Suppression du préchargement GPS 50 km, des réglages fournisseur/cache, des sources XYZ auxiliaires et des diagnostics de couverture.
- Suppression du réchauffement 20 km et directionnel dans la carte, et retour au tampon Leaflet standard de 2 tuiles.
- Nettoyage des anciennes préférences de préchargement au démarrage sans effacer les données locales/cartes déjà présentes.
- Aucun changement GPS, caméra, itinéraire ou apparence.

## État et validation
Code 318 / version 11.14.118. L’APK n’a pas pu être compilée ici : plateforme Android 34, Gradle et wrapper absents. Contrôler la carte/cache sur Xiaomi via CodeAssist, puis vérifier trajet réel et radars. Caméra à terminer en dernier.
