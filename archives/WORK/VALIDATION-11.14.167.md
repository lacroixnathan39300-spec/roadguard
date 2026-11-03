# Validation ROADGUARD 11.14.167 — 16/10/2026

## Vérifications exécutées

- Toutes les sources Java de l’application ont passé le contrôle ECJ 3.39.0, cible Java 8, avec classes Android API 34 et runtime Java disponible. Aucun assemblage APK effectué.
- `BundledRadarCatalogTest` exécuté sur les véritables CSV : FR 7325 + CH 1777 = 9102 points sélectionnés par défaut. Le cache CH est lu et réutilisé. Les points CH sont accessibles à une vraie requête `DangerSpatialIndex`, utilisée par la carte et les alertes.
- Catégories suisses vérifiées : feux rouges, tunnels, limitation variable, vitesse moyenne et 194 lieux mobiles non confirmés.
- Indépendance des réglages : les quatre combinaisons donnent 8908, 9102, 26012 et 26206 points. Le commutateur CH ne modifie pas les lieux temporaires français.
- Validation des fichiers par pays, rejet d’identités répétées et de NaN, cache immutable : vérifications conservées.
- `RadarSourcePolicyTest` : bases CH vérifiées utilisables; ancien POI-Warner FR au territoire ambigu isolé; mêmes règles de filtre et de libellé mobile pour les imports au premier plan et dans le service GPS.
- Les CSV de coordonnées sont strictement inchangés par rapport à 11.14.166. SHA-256 FR `79e3e8e9b18ef1e22c6ed9e74a69884f0cffdd2cd580b993edb9f781208015d0`, CH `6ae499183f4f4b69191384da5060cf0bc1b065124b93f37cd5806b9aea126da1`.
- Syntaxe des trois scripts JavaScript embarqués dans les deux moteurs de carte vérifiée avec Node.
- Préparation des assets : le corps réel de la tâche Gradle `prepareBundledMapLibre` a été exécuté sous Groovy, avec les fichiers locaux. Cela vérifie le correctif de portée `digest`, les empreintes MapLibre et Brotli, les glyphes et les licences; ce n’est pas une exécution du plugin Android Gradle ni un assemblage APK.

## Contrôles Python

| Groupe | Résultat |
|---|---|
| test_embedded_radars.py | OK |
| test_swiss_activation.py | OK |
| test_radar_dataset.py | OK |
| test_radar_storage_current.py | OK |
| test_radar_spatial_index.py | OK |
| test_radar_alert_accuracy.py | OK |
| test_radar_categories.py | OK |
| test_radar_filter_indices.py | OK |
| test_danger_type_policy.py | OK |
| test_swiss_radar_batch.py | OK |
| test_poi_warner_import.py | OK |
| test_map_manual_pan.py | OK |
| test_gps_stability.py | OK |
| test_radar_counters_current.py | OK |
| test_radar_country_downloads.py | OK |
| test_radar_map_visibility.py | OK |
| test_functional_handoff_current.py | OK |
| test_visual_handoff_current.py | OK |
| check_handoff.py | OK |

Les contrôles Python comprennent des assertions statiques. Ils ne simulent pas un téléphone. Les anciens tests qui exigeaient l’exclusion globale de CH ont été actualisés pour l’activation explicitement demandée; la validation de pays et la quarantaine des données ambiguës restent contrôlées.

## Recette Xiaomi restante

1. Importer le ZIP dans CodeAssist et compiler 11.14.167 / 367.
2. Sans téléchargement initial et sans point personnel en doublon : France 7325 chargés; Suisse 1777 chargés; 9102 au total.
3. Désactiver « Lieux mobiles signalés · Suisse » : CH 1583, total 8908. Réactiver : CH 1777, total 9102.
4. Vérifier affichage des marqueurs, filtres, bips et annonces vocales selon les réglages globaux existants et une position GPS fiable. Activer le service et les autorisations Android pour un essai écran verrouillé. Les préférences du service sont relues périodiquement au plus tard après 30 secondes lors d’un fix GPS.
5. Vérifier import/actualisation d’une base CH OSM/MapAtlas/Lufop, suppression et retrait des doublons. L’ancien paquet poi_warner_ch ne doit jamais être assimilé à une base suisse vérifiée.
6. Déplacer la carte : un nouveau fix GPS ne doit pas la recentrer; ⌖ reprend le suivi. Caméra en dernier.

## Limites

Aucun APK/dex/signature ni essai Xiaomi. Les données publiques peuvent être incomplètes ou anciennes; les lieux mobiles ne confirment aucune présence actuelle. Les réglages globaux des alertes, des marqueurs et du service sont conservés. Les pourcentages historiques ne sont ni un audit global ni une mesure de couverture radar.

L’OFROU indique que les avertisseurs de contrôles routiers sont interdits dans un véhicule en Suisse : https://www.astra.admin.ch/fr/appareils-gps. Activation technique ne signifie pas autorisation d’usage.
