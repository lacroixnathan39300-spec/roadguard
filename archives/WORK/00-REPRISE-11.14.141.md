# Reprise ROADGUARD 11.14.141

## Demande traitée
Télécharger des bases radar depuis l’application sans clé Lufop.

## Réalisé
- Ajout d’un téléchargement direct OpenStreetMap via Overpass par code pays, sans compte ni clé.
- Conversion des nœuds highway=speed_camera en CSV local ROADGUARD avec contrôle des coordonnées, limite de volume et remplacement protégé par sauvegarde.
- Annulation possible; erreurs réseau signalées. Attribution et licence ODbL enregistrées et présentées dans l’écran de la base installée.
- Lufop demeure optionnel pour les utilisateurs ayant une clé; la Suisse reste exclue de l’affichage et des alertes.

## Limites et vérifications restantes
- OSM est contributif : couverture inégale, données de radars fixes cartographiés uniquement, pas de garantie d’exhaustivité ni de radars mobiles. Overpass public peut limiter les requêtes.
- À tester dans CodeAssist avec un téléchargement réel (ex. Belgique) et vérification CH stockée mais inactive.
- Compilation Android et test Xiaomi non effectués ici.
- Version source 11.14.141 / code 341. Caméra à la fin; aucun préchargement de carte.
