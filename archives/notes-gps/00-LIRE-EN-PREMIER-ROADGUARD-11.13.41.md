# ROADGUARD 11.13.41 — restauration transactionnelle

## Réalisé
- Correction du piège EXIT écrasé dans `tools/prepare_france_pmtiles.sh`: l'appel à `rollback` est conservé en cas d'erreur.
- Ajout de `tools/test_rollback_trap_111341.py` et `tools/test_rollback_transaction_111341.py`.
- Test d'injection d'échec pendant la publication des fichiers : les trois fichiers précédents sont restaurés et les fichiers temporaires supprimés.
- Code Android GPS et caméra inchangé.

## Tests
- `bash -n tools/prepare_france_pmtiles.sh` OK.
- 2 tests du piège EXIT OK.
- 1 test de transaction avec panne injectée OK.
- 2 tests de limites d'extraction OK.
- Intégrité ZIP vérifiée à la livraison.

## NON RÉALISÉ / BLOCAGES
- Pas de vraie archive France + Corse produite, pas de publication HTTPS, pas de téléchargement PMTiles réel.
- Pas de compilation APK, pas de test physique Xiaomi, pas de validation hors ligne.
- Le test transactionnel emploie un simulateur PMTiles; il ne démontre pas que les tuiles s'affichent.
- Ancien test `test_prepare_france_atomic.py` attend un faux fichier de 11 octets comme archive valide, donc ne reflète plus les contrôles réels du format.

## Suite prioritaire
1. Obtenir une source PMTiles réelle et extraire la France + Corse.
2. Valider les couches de routes et la lisibilité du style MapLibre, puis cataloguer.
3. Héberger et tester HTTP Range 206, reprise et SHA-256.
4. Compiler APK, installer sur Xiaomi, tester mode avion, GPS et caméra.
