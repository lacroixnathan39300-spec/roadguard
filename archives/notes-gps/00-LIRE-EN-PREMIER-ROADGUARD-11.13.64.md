# ROADGUARD 11.13.64 — INDEX MONDIAL DE PLANIFICATION

## État réel
- Base: ROADGUARD 11.13.63; versionName 11.13.64.
- Ajout: tools/generate_world_region_index.py; tests/test_world_region_index.py.
- Source mondiale identifiée: https://download.versatiles.org/osm.versatiles (VersaTiles v02, **pas** PMTiles). Le logiciel `versatiles convert` est nécessaire pour extraire et convertir vers PMTiles.
- Le service officiel distribue les conteneurs; ne pas présumer qu'il héberge nos packs GPS, ni que le service démo autorise un préchargement automatique massif.
- Le générateur crée un index de PLANIFICATION à partir de frontières GeoJSON fournies séparément; il ne crée pas de packs et ne fabrique aucune URL ni somme SHA256.
- Tests exécutés: `python -m unittest discover -s tests -p test_world_region_index.py -v` : 3/3 réussis.
- Aucun téléchargement de cartes réelles, aucun serveur ROADGUARD, aucune compilation APK, aucun test Xiaomi.
- GPS bleu/caméra non modifiés.

## Prochaines tâches (priorité)
1. Obtenir un GeoJSON de frontières nationales et régionales sous licence adaptée, avec attribution et contrôles d'antiméridien; créer l'index complet.
2. Installer le CLI VersaTiles, extraire une région de test depuis la source mondiale, convertir en PMTiles, contrôler licence/attribution et hash.
3. Héberger nos packs sur une URL HTTPS stable et publier un catalogue TSV validé par OfflinePackCatalog (URL, SHA256, limites, emprise).
4. Brancher la sélection GPS aux packs réels et enchaîner les téléchargements en arrière-plan avec reprise réseau.
5. Raccorder les PMTiles téléchargés au style MapLibre (routes, noms, glyphes, sprites) en mode hors ligne.
6. Compiler l'APK et vérifier sur Xiaomi (GPS bleu, caméra, réseau coupé, redémarrage).

## Pourcentages historiques, NON audités
Global 42%; GPS 100%; carte en ligne 85%; planification GPS 70%; mémoire 55%; téléchargement réel 20%; arrière-plan 5%; affichage hors ligne 30%; APK Xiaomi 10%.
Ne pas augmenter sur la seule base d'un générateur d'index.
