# ROADGUARD 11.13.42 — Retrouver une archive locale

## Réalisé
- Bouton de la fenêtre « Carte France : source nécessaire » redirigé vers le sélecteur Android de fichiers locaux, et non vers un catalogue HTTPS vide.
- Sélecteur Android sans filtre MIME restrictif : certains fournisseurs Android déclarent les `.pmtiles` comme type inconnu.
- Conservation de la validation PMTiles lors de l'import et du GPS existant.
- 3 tests unitaires ciblés réussis.

## Important
- ZIP source uniquement. Aucune APK compilée ni test Xiaomi effectué.
- L'application BÊTA 11.13.10 installée sur le téléphone ne change pas avec ce ZIP.
- Les 2,76 Go évoqués par l'utilisateur ne sont pas accessibles depuis cette session : existence, format, emplacement et compatibilité non vérifiés.
- L'import actuel copie l'archive dans le stockage privé de l'application : prévoir suffisamment d'espace libre ; une importation sans copie via SAF serait une évolution distincte.
- Aucun pack France + Corse réel hébergé, aucune URL de catalogue vérifiée.
- Pas de garantie de couverture cartographique par simple bounding box.

## Priorités
1. Localiser/réutiliser les données déjà téléchargées sans imposer de nouvelle copie si possible (SAF ou répertoire connu selon permissions Android).
2. Obtenir un PMTiles France + Corse réel et vérifier les couches MapLibre et les routes hors ligne.
3. Compiler APK, installer sur Xiaomi et tester en mode avion.
4. Fournir un catalogue HTTPS réel uniquement après hébergement et validation.
