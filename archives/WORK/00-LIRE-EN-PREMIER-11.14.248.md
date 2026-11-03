# ROADGUARD 11.14.248 — reprise du blocage radar et textures

## Installation

Le ZIP contient le projet Android entier, les bases et fichiers conservés, ainsi que un seul APK téléphone 11.14.248 dans `LIVRAISON/`. Importer le dossier `ROADGUARD-11.14.248` dans CodeAssist, puis compiler. Conserver les données et utiliser la même signature pour une mise à jour installable par dessus la précédente.

## Blocage au démarrage

Le catalogue radar était déjà chargé sur un worker, mais après ce chargement l'application envoyait tous les points à la WebView en une seule commande sur le thread principal. Avec un catalogue de plus de 100 000 points, cela pouvait bloquer Android. La carte reçoit désormais au plus 600 points du secteur visible ; la liste complète reste indexée en mémoire pour les alertes GPS. Le déplacement de la carte demande une nouvelle tranche après 180 ms. Une erreur différente ne peut être exclue sans la pile de l'ANR du téléphone.

## Pack de textures

Dans la carte, toucher **▧** dans la barre de commandes à droite. Autre accès : Paramètres → **Pack de carte / textures**. Trois apparences : Graphite ROADGUARD, Lisibilité des routes, Couleurs OpenStreetMap. Ce pack règle l'apparence locale des tuiles, il ne contient pas les tuiles géographiques. Une nouvelle zone sans tuiles exige le réseau ou un fichier PMTiles compatible. Le cache de tuiles déjà consultées est temporaire.

## Android Auto

Le code lié à Android Auto reste dans le projet pour une reprise ultérieure. Aucun APK Android Auto n’est fourni dans cette livraison.

## Validation

Contrôles de code ciblés, import des vraies archives 7z/SQLite et tests Android simulés documentés dans `verification/`. L’APK téléphone est un build local, pas une exécution physique. Voir `AVANCEMENT-11.14.248.md` pour la table pondérée complète et les preuves.

🔴 **INTERVENTION OBLIGATOIRE — PLUS TARD** : compiler le projet dans CodeAssist sur Xiaomi et essayer le démarrage, le zoom, les radars et les trois apparences. Si « ROADGUARD ne répond pas » revient, ouvrir **Voir l’historique** et transmettre la pile complète de l’ANR avec l’heure ; cela permettra d’identifier la cause réelle. Android Auto est reporté à une version ultérieure.
