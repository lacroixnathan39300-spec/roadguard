# ROADGUARD 11.13.26 — préparation du catalogue France

## État vérifié
- Source 11.13.25 conservée. Aucune modification du GPS bleu.
- Ajout `tools/build_france_pmtiles_catalog.py` : génère un vrai `catalog.json` pour un PMTiles France existant, à partir de sa taille et de son SHA-256. Vérifie signature PMTiles v3, enveloppe géographique approximative, URL HTTPS.
- AUCUNE archive France n'a été produite ou hébergée ici. Le bouton France reste bloqué tant qu'un pack n'est pas disponible.
- AUCUNE APK compilée/testée sur Xiaomi.

## Ordre des prochaines étapes
1. Obtenir une source Protomaps légalement utilisable (voir https://docs.protomaps.com/guide/getting-started).
2. Exécuter `pmtiles extract SOURCE france.pmtiles --bbox=-5.6,41.2,9.9,51.2 --maxzoom=14` sur un environnement doté du réseau et de l'espace disque nécessaire. La BBOX inclut des territoires limitrophes; elle ne garantit pas l'intégrité.
3. `pmtiles verify france.pmtiles`; vérifier contenu, schéma de couches, détails et licence.
4. Héberger le fichier sur HTTPS stable avec prise en charge des téléchargements et Range.
5. `python3 tools/build_france_pmtiles_catalog.py france.pmtiles --https-url https://VOTRE-HEBERGEUR/france.pmtiles --out catalog.json`.
6. Héberger catalog.json, configurer ROADGUARD, tester téléchargement et carte en mode avion.
7. Compiler et tester sur Xiaomi sans effacer les données; contrôler GPS, caméra, radars, itinéraires.

## Intervention utilisateur
Pas d'action immédiate requise pour lire cette passation. L'hébergement réel et le test Xiaomi exigeront un accès/une intervention si l'assistant ne dispose pas des outils ou autorisations nécessaires.

## Avancement
42% global = ancienne estimation non auditée, pas un résultat de test. Le catalogue est prêt à être généré, mais la carte France reste absente.
