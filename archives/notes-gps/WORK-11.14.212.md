# ROADGUARD 11.14.212 — Validation des entrées ZIP

## Fait
- Rejet des noms de fichiers ZIP suspects (chemins absolus, `..`, caractères de contrôle, noms trop longs) dans le convertisseur ASC et dans l'importateur CSV.
- Préservation de l'overlay installé en cas de refus de l'archive (le rejet intervient avant la phase d'installation).
- Compilation Java isolée réussie de `LufopAscZipImporter.java`.

## Non fait / blocages
- Pas de téléchargement RadarDroid direct : accès serveur et autorisations non établis.
- Pas de compilation Android complète ni de test Xiaomi.
- Pas de validation end-to-end du gestionnaire d'import Android.

## Suite
- Tests d'import ZIP sur Android, sauvegarde/restauration et rechargement des alertes.
- Source de téléchargement autorisée, sélection pays et progression réelle.

## Données
- Aucun CSV embarqué n'a été modifié.
