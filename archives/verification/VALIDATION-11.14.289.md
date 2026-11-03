# Validation 11.14.289

Résultats exécutés :
- offline-packs-11.14.289.log : compilation Java réelle des classes de registre, découverte, résolution et lecteur ; archive PMTiles synthétique et octets vectoriels ; contrôles SHA, identifiants et fichiers terminés.
- ui-regressions-11.14.289.log : 12 scénarios du filtre de marqueurs ; vrai callback de couverture partielle (une tuile chargée et une manquante) ; régressions démarrage, caméra et scripts. DOM, projection et renderer simulés, aucun rendu WebView.
- user-fixes-11.14.289.log : catalogue original, rapprochements Lufop, cache natif, source raster et politiques. Tests JVM avec adaptateurs Android.
- syntax-11.14.289.log : syntaxe des quatre fichiers Java modifiés ; ce contrôle n’est pas un typage Android.

Non exécutés : build APK/SDK Android, Robolectric API26/36, rendu WebView matériel, mode avion sur Xiaomi et tests en conduite. Les scénarios MapModeRuntimeTest du lot précédent couvrent seulement le cas sans pack local ; ils ne valident pas la nouvelle sélection avec pack.

Les 14 fichiers CSV/SQLite/DB ont été comparés octet par octet à 288. Les sept inventaires sources restent inchangés. Filtre d’affichage uniquement ; ne pas interpréter ce lot comme une déduplication complète des alertes natives.

Pourcentages historiques inchangés : développement global 85,0000 %, validation globale 44,0000 %, deltas +0,0000 point. Voir AVANCEMENT-11.14.289.md.
