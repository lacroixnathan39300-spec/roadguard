# ROADGUARD 11.14.109 — état Accueil et cartographie

- Version source : 11.14.109 / versionCode 309.
- Accueil : badge GPS dynamique distinguant permission requise, position récente (moins de 2 minutes) et attente; tap ouvre la carte.
- Cache : carte d’état affiche les données réelles; aucun pourcentage si le GPS récent ou une source valide manque; détail repliable.
- Fonction de cache toujours non activée sans fournisseur explicitement autorisé. Aucune couverture de 50 km n’est garantie.
- Progression estimée : global 47→48 (+1), interface 67→68 (+1), architecture visuelle 35→37 (+2), Accueil 41→43 (+2). Cache automatique fonctionnel : 0 %.
- Contrôles statiques et tests Python ciblés : `TESTS-ET-BLOCAGES.md`. Aucune compilation ni vérification Xiaomi.
