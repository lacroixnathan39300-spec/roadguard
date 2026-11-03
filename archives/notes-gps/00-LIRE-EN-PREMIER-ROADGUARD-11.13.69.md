# ROADGUARD 11.13.69 — Passation

## Fait
- Correction du nom temporaire utilisé par `tools/execute_versatiles_plan.py` : `*.incomplete.pmtiles` au lieu de `*.pmtiles.partial`, afin que VersaTiles identifie le format de sortie PMTiles.
- Renommage atomique vers le fichier final uniquement après contrôle de l'en-tête et calcul SHA-256.
- Nouveau test `tools/test_execute_versatiles_atomic.py` : succès, non-réextraction et échec nettoyé (2 tests OK, simulateur local).

## Non fait / blocages
- Aucune extraction de vraies données Jura, France ou Suisse lors de cette livraison.
- Pas d'hébergement HTTPS de packs ni de catalogue réel, pas de téléchargement Android fonctionnel, pas de rendu hors ligne validé, pas d'APK compilée ni de test Xiaomi.
- Les tests utilisent un faux exécutable VersaTiles et ne valident pas les tuiles géographiques.

## Prochaines actions
1. Installer l'exécutable officiel VersaTiles sur une machine disposant d'un accès réseau autorisé.
2. Exécuter un petit extrait Jura en PMTiles et valider réellement le contenu avec `versatiles probe` et un affichage MapLibre.
3. Héberger les packs et produire le catalogue HTTPS avec SHA-256, puis intégrer à Android.
4. Tester téléchargement, persistance, rendu hors ligne, GPS et caméra sur Xiaomi.

## Avancement
Global historique estimé 42 %, inchangé. Les autres pourcentages historiques sont dans SUIVI-AVANCEMENT.md. Ne pas les interpréter comme des validations sur appareil.
