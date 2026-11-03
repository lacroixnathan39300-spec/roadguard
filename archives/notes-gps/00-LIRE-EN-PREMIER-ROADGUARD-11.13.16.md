# ROADGUARD 11.13.16 — état réel

## Modifications réalisées
- Le statut « France installée » vérifie désormais aussi les PMTiles téléchargés dans le stockage interne (en-tête PMTiles v3, vectoriel, couverture géographique), plutôt que seulement les assets de l'APK.
- Le style MapLibre prend en compte les noms de couches `roads` et `places` de Protomaps en complément des couches OpenMapTiles. Le point GPS bleu n'a pas été modifié.
- Source historique `map.comptoir.net` toujours neutralisée dans le bouton France, car indisponible.

## Limites bloquantes
- AUCUN fichier France+Corse complet n'est inclus dans ce ZIP.
- Aucun URL pérenne et vérifié de pack France complet n'a été configuré. Le bouton de téléchargement France n'est donc PAS opérationnel.
- Aucun APK compilé ni essai sur Xiaomi. Le support du schéma Protomaps est partiel et doit être testé avec une vraie archive.
- L'extraction `--bbox` peut inclure des territoires voisins : contrôler les données, les droits et les tailles.
- Ne jamais annoncer une carte 100% hors ligne avant validation des glyphes, des styles et d'un test en mode avion.

## Étapes suivantes
1. Produire une archive PMTiles France + Corse depuis une source autorisée, `pmtiles verify`, contrôler métadonnées et rendu.
2. Héberger avec HTTPS et téléchargements reprenables, publier URL, taille et SHA-256 dans catalogue signé/fiable.
3. Tester téléchargement, pause/reprise, validation SHA-256, installation atomique et stockage persistant.
4. Compiler APK avec MapLibre et ses glyphes embarqués, tester hors ligne sur Xiaomi (routes, noms, GPS, caméra).
5. Tester le schéma Protomaps sur échantillons et adapter les styles au besoin.
