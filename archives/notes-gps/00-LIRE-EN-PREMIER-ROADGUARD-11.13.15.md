# ROADGUARD 11.13.15 — préparation vérifiable des cartes France

## Réalisé
- Ajout de `tools/prepare_france_pmtiles.sh` : extraction France métropolitaine + Corse depuis une archive planète PMTiles fournie par l'opérateur, vérification PMTiles, somme SHA-256 et métadonnées.
- Conservation du GPS bleu existant, sans modification de `MainActivity.java` ni `map_maplibre.html`.
- Vérification du code existant : le bouton France ne possède pas encore de catalogue HTTPS signé/validé opérationnel ; l'URL map.comptoir.net n'est pas une source fiable.

## Non réalisé / BLOQUÉ
- Aucune archive France de plusieurs Go téléchargée, aucun hébergement permanent mis en place, aucune APK compilée ou testée sur Xiaomi.
- Un pack Protomaps utilise un schéma de couches différent d'OpenMapTiles. Le style `map_maplibre.html` doit être adapté à ce schéma AVANT de déclarer la carte utilisable.
- Les noms de villes exigent aussi des glyphes locaux (l'URL `/fonts/` doit réellement fournir des PBF). Contrôler la licence et l'attribution des données.

## Suite prioritaire
1. Choisir un build Protomaps daté existant (voir https://maps.protomaps.com/builds), installer CLI pmtiles, exécuter `bash tools/prepare_france_pmtiles.sh URL output/france.pmtiles`.
2. Examiner les métadonnées du pack et implémenter un style conforme à ses `vector_layers`, avec glyphes hors ligne.
3. Héberger l'archive finale sur un HTTPS stable acceptant Range et établir le catalogue avec taille et SHA-256.
4. Brancher téléchargement avec reprise, contrôle SHA-256, installation atomique et stockage privé persistant.
5. Compiler, tester en mode avion sur Xiaomi (France + Corse), vérifier le GPS bleu et les mises à jour sans perte de données.

Sources : https://docs.protomaps.com/pmtiles/cli ; https://maplibre.org/maplibre-gl-js/docs/examples/pmtiles/
