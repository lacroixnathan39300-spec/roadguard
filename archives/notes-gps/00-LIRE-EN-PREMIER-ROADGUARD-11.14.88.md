# ROADGUARD 11.14.88 — passation

## Réalisé
- Correction du diagnostic MapLibre : `sourcedata.isSourceLoaded` mesure la fin de chargement de la source, PAS le nombre de tuiles.
- `vectorSourceEvents` compte ces événements ; `vectorTilesSeen` est désormais `null` (non instrumenté), plutôt qu’un nombre trompeur.
- Réinitialisation des compteurs lors du changement de pack.
- Vérification statique de la syntaxe JavaScript via Node.js.
- Vérification des ressources glyphes Noto Sans Regular et du point de service WebView dans MainActivity.

## Restant
- Trouver une source PMTiles vectorielle autorisée et compatible avec le schéma utilisé, puis valider sa lecture réelle.
- Tester les glyphes, les couches et le basculement des thèmes sur un appareil.
- Aucun changement caméra.

## Tests et limites
- Syntaxe JS validée. ZIP contrôlé. Pas de compilation APK ni test Xiaomi.
- Les compteurs ne prouvent pas que des tuiles ont été affichées.
- Aucune compilation demandée à l’utilisateur pour ce lot intermédiaire.
