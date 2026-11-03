# ROADGUARD 11.13.28 — passation

## Travail réalisé
- Ajout `tools/verify_map_host.py`: vérification HTTPS, réponse HTTP 206, Content-Range `bytes 0-126/N`, signature PMTiles v3 sans téléchargement complet.
- Ajout `tools/test_verify_map_host.py`: quatre tests unitaires réussis.
- Aucun changement de GPS, interface, MapLibre ou APK.

## Utilisation
`python tools/verify_map_host.py https://HEBERGEUR/CHEMIN/france.pmtiles`

## Limites
- Pas d'archive France + Corse générée, ni d'URL HTTPS réelle vérifiée.
- Pas de catalogue déployé, ni de téléchargement opérationnel dans l'APK.
- Ce contrôle ne vérifie pas la licence, les couches, la couverture réelle ni le rendu hors ligne.
- APK non compilée, aucun test Xiaomi.

## Suite prioritaire
1. Obtenir/produire une archive France + Corse PMTiles réelle avec licence vérifiée.
2. Héberger l'archive sur un HTTPS fiable et exécuter verify_map_host.py.
3. Générer le catalogue JSON avec build_france_pmtiles_catalog.py, publier le catalogue.
4. Intégrer l'URL dans ROADGUARD, vérifier téléchargement/reprise, MapLibre hors ligne, GPS et APK Xiaomi.

## Suivi
- Progression historique globale provisoire : 42 %, inchangée (aucune fonctionnalité utilisateur finalisée).
- Pastille rouge uniquement pour une action utilisateur indispensable.
