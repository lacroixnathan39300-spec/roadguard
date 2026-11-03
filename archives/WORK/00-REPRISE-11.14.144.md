# Reprise ROADGUARD 11.14.144

## Bases radar
- MapAtlas Camera Index se télécharge par pays directement depuis l’application, sans clé, depuis son catalogue de 106 régions.
- Quatre classes converties en CSV local : `spot`, `signal`, `corridor`, `adaptive`.
- Chaque base garde son identifiant MapAtlas distinct des données OSM et Lufop. Mise à jour/suppression possible; la base précédente est préservée si un téléchargement échoue.
- Attribution sauvegardée : MapAtlas, CC BY 4.0. Jeu de données contributif, non officiel; snapshot 2024-05-21.
- Pays suisses stockés mais toujours exclus de la carte et des alertes ROADGUARD.

## Suite
1. Compiler avec CodeAssist (version 11.14.144, code 344).
2. Tester le téléchargement MapAtlas France et un pays supplémentaire, puis vérifier affichage carte, filtres/alertes, fonctionnement hors ligne, mise à jour et suppression.
3. Pas de téléchargement en arrière-plan ni de préchargement. Caméra à la toute fin.
