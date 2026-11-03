# ROADGUARD 11.14.218 — Redirections HTTPS et actualisation des alertes

## Modifications
- AuthorizedRadarImporter: suit au plus cinq redirections HTTP 301/302/303/307/308, uniquement vers HTTPS sans identifiants intégrés ni fragment.
- SHA-256 et validation CSV restent obligatoires. Aucune tentative de contournement d'accès RadarDroid/Lufop.
- RadarDownloadActivity: demande le rechargement du service d'alertes après téléchargement CSV vérifié.

## Préservation
- Aucun fichier de données radar embarquées modifié; seules deux classes Java et cette note ont été changées.

## À faire
- Catalogue de sources autorisées par pays, sans saisie d'URL ni SHA-256.
- Compilation Android intégrale et test sur Xiaomi en fin de chantier.
- Vérifier le rechargement effectif pendant la navigation et Android Auto.

## Validation
- Vérification statique du patch. Pas de compilation Android ni de test réseau ou appareil.
