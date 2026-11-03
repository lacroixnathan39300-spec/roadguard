# ROADGUARD 11.13.67 — Reprise

Global historique : 42 % (estimation non auditée). Dernier ZIP source : 11.13.67.

## Réalisé
- `tools/execute_versatiles_plan.py` : validation du plan, extraction VersaTiles via commande externe uniquement avec `--execute`, sortie temporaire, contrôle superficiel signature PMTiles, SHA-256, rapport, refus de réécraser un fichier existant.
- `tests/test_execute_versatiles_plan.py` : quatre tests automatisés avec binaire factice (pas de téléchargement réel).
- Documentation officielle : https://docs.versatiles.org/guides/download_tiles.html et https://docs.versatiles.org/guides/converter.html

## Non réalisé / blocages
- Aucune extraction réelle France/Suisse : environnement sans accès DNS à download.versatiles.org et exécutable VersaTiles non fourni.
- Vérification PMTiles superficielle (signature/taille uniquement), pas de validation structurelle approfondie.
- Aucun serveur HTTPS ROADGUARD ni catalogue de packs réels.
- Aucun affichage hors ligne Android vérifié, aucune APK compilée/testée.
- Ne pas connecter le serveur public de tuiles OSM au téléchargement massif.

## Prochaines tâches (ordre)
1. Disposer de l'exécutable VersaTiles sur un environnement réseau et extraire un pack pilote réel (Jura), contrôler PMTiles structurellement.
2. Héberger le pack sur un stockage HTTPS stable et publier un catalogue JSON avec SHA-256, bbox, version et attribution.
3. Connecter ce catalogue à OfflinePackSync sur Android, puis MapLibre à la lecture des packs locaux.
4. Tester le GPS 50 km, l'anti-doublon, les redémarrages et la déconnexion sur Xiaomi.

## Commande
`python tools/execute_versatiles_plan.py chemin/vers/plan.json --out-dir prepared-regions` (simulation)
`python tools/execute_versatiles_plan.py chemin/vers/plan.json --out-dir prepared-regions --execute` (réel, nécessite VersaTiles + réseau)

## Pourcentages (historiques, non audités)
GPS 100 ; carte en ligne 85 ; planification GPS 70 ; mémoire 55 ; téléchargement réel 20 ; arrière-plan 5 ; hors ligne 30 ; tests Xiaomi 10. Global 42.
