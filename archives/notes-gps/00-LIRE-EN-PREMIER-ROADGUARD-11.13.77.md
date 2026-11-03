# ROADGUARD 11.13.77 — Planificateur de cache de secours 50 km

## Réalisé
- Nouveau `EmergencyTilePlanner.java`: calcule les tuiles Web Mercator couvrant un rayon GPS, ordre des zooms bas vers les zooms hauts, proximité au sein de chaque zoom, plafond configurable pour contrôler données et batterie, longitude bouclée à la ligne de changement de date.
- Test Java `tests/EmergencyTilePlannerTest.java`: validité, doublons, plafond, ordre, reproductibilité, coordonnées invalides.
- Aucun changement au GPS bleu, à la caméra ni au moteur cartographique existant.

## Limites / blocages
- **Planification uniquement**: AUCUNE requête de préchargement n'est encore branchée, donc le tampon de 50 km n'est PAS opérationnel.
- La source `tile.openstreetmap.org` utilisée actuellement ne permet pas le préchargement massif. Ne pas l'utiliser pour exécuter ce plan. Il faut un fournisseur qui autorise explicitement la mise en cache anticipée, puis un cache persistant pendant la coupure et la liaison WebView.
- Aucun APK compilé ni essai sur Xiaomi.
- Ancienne bibliothèque PMTiles conservée dans le code pour compatibilité, mais elle n'est plus dans les objectifs prioritaires de l'utilisateur.

## Prochaines actions
1. Choisir/configurer une source cartographique autorisant le préchargement temporaire dans un rayon de 50 km.
2. Connecter le planificateur à un téléchargeur borné, asynchrone et sensible au réseau, puis au cache WebView/MapLibre.
3. Tester la coupure réseau réelle et le retour réseau, compiler APK, vérifier sur Xiaomi.

## Avancement estimé, non testé sur téléphone
GPS 100%; carte en ligne 85%; détection/bascule réseau 50%; planification du tampon 50 km 35% (+25 points, **planification seulement**); tampon réellement préchargé 0%; affichage pendant coupure 25%; reprise 35%; validation APK Xiaomi 0%. Global: non recalculé, ne pas réutiliser 42% de l'ancien périmètre.
