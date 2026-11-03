# ROADGUARD 11.13.35 — Passation

## Réalisé
- `tools/prepare_france_pmtiles.sh` : extraction atomique avec fichier temporaire unique, nettoyage sur erreur, préservation du fichier précédent si `pmtiles extract` ou `pmtiles verify` échoue, SHA-256 lié au nom final.
- `tools/test_prepare_france_atomic.py` : deux tests isolés, exécutés avec succès (échec et succès simulés).
- Aucun changement au GPS, à l'interface ou à l'APK.

## Blocage principal
Aucun pack France+Corse PMTiles réel n'a été extrait, hébergé ou validé. Pas de téléchargement opérationnel dans l'APK. La compilation Android et le test Xiaomi ne sont pas réalisés.

## Prochaines tâches prioritaires
1. Obtenir une source PMTiles mondiale autorisée, accessible en HTTPS avec Range et couverture France.
2. Exécuter l'extraction réelle, vérifier schéma vectoriel, tuiles et couverture géographique.
3. Héberger le fichier et le catalogue HTTPS, tester Range 206 et reprise de téléchargement.
4. Brancher le catalogue par défaut dans l'APK et compiler.
5. Tester routes, noms de villes et GPS en mode avion sur Xiaomi.

## Progression
42 % global : estimation historique non auditée ; ne pas augmenter sans preuve de fonctionnement. Barres par chantier à conserver. Pastille rouge uniquement si intervention utilisateur strictement nécessaire.
