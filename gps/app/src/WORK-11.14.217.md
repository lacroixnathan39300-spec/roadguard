# ROADGUARD 11.14.217 — Activation et inventaire

## Modifications
- Corrige le sélecteur de pays : toute modification de la case Activer/Désactiver demande maintenant un rechargement du service d’alertes, y compris après changement de pays.
- Ajoute un inventaire par pays mis en cache : l’écran n’alloue plus une liste de tous les points d’un pays uniquement pour en afficher le nombre.
- Le cache d’inventaire est invalidé avec le cache Lufop lors d’une mise à jour.

## Données et sécurité
- Aucune ressource radar CSV/ASC modifiée.
- Aucune URL RadarDroid ou accès serveur protégé ajouté.

## Tests
- Vérification statique des remplacements et de la conservation des données.
- Compilation Android et test sur Xiaomi non effectués.

## À faire
- Télécharger des ZIP par pays depuis des sources autorisées et vérifiées.
- Tester rechargement en navigation, Android Auto et import ASC sur téléphone.
- Conserver les sept bases d’origine comme secours.
