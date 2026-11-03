# ROADGUARD 11.13.17 — passation vérifiable

Base : archive 11.13.16 reçue dans cette conversation. GPS bleu laissé intact.

## Réalisé
- Ajout `tools/prepare_verified_france_catalog.py` : contrôle structurel PMTiles v3, emprise France + Corse, type MVT, SHA-256 en flux et génération d'une entrée catalogue pour une URL HTTPS hébergée par l'exploitant.
- Aucune carte volumineuse téléchargée, aucun hébergement créé, aucune APK compilée.

## Blocage réel
- Aucun fichier PMTiles France + Corse vérifié ni URL pérenne de ce fichier ne sont disponibles dans ce ZIP.
- Le bouton France reste bloqué tant que le catalogue ne contient pas un pack hébergé et validé. Ne pas rétablir `map.comptoir.net`.
- La présence d'un en-tête PMTiles correct ne garantit pas le rendu : tester les couches, les glyphes et les tuiles dans la WebView sur Xiaomi en mode avion.

## Suite prioritaire
1. Obtenir un extrait France + Corse légalement redistribuable, et vérifier son schéma de couches.
2. Héberger le PMTiles sur HTTPS fiable avec téléchargements complets et reprise HTTP Range.
3. Exécuter `python3 tools/prepare_verified_france_catalog.py /chemin/france.pmtiles --url https://hebergement.example/france.pmtiles`.
4. Intégrer l'entrée dans le catalogue ROADGUARD et vérifier que le JSON attendu par l'application est compatible.
5. Compiler, installer sans effacer les données, télécharger puis tester en mode avion ; vérifier GPS, noms de villes, routes, reprise, stockage.

État : code outil ajouté et syntaxe vérifiée, aucune validation terrain. Ne pas déclarer la carte hors ligne fonctionnelle.
