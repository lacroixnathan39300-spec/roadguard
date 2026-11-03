# Validation 11.14.166 — 16 octobre 2026

## Vérifications réussies

- Toutes les sources Java de `fr.roadguard.app` contrôlées avec ECJ 3.39.0, cible source Java 8, classes Android API 34 et runtime Java disponible. Code de sortie 0. Aucun APK assemblé.
- Véritable lecteur Java `BundledRadarCatalog` exécuté : FR=24429, inventaire=7325, historiques=17104, CH=1777. Rejet des pays incorrects/mélangés, identités répétées et NaN; cache immutable réutilisé.
- Reconstruction hors réseau : les deux CSV et le manifeste ont les mêmes SHA-256 avant/après reconstruction.
- Scripts JavaScript embarqués des cartes Leaflet/MapLibre : analyse de syntaxe Node réussie (3 scripts).
- Le téléchargement officiel français de décembre 2025 récupéré pendant cette recherche correspond aux 3309 IDs, types et coordonnées du socle embarqué.

## Contrôles Python exécutés

| Groupe | Résultat |
|---|---|
| test_embedded_radars.py | OK |
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

Les contrôles Python existants incluent des vérifications statiques; ils ne représentent pas des tests d’interface sur un téléphone. Les assertions historiques liées au numéro de version, au titre du compteur et à l’ancienne méthode Radardroid retirée ont été mises à jour pour la livraison actuelle.

## Non vérifié

Assemblage Gradle, génération/dex/signature d’un APK, installation et rendu sur Xiaomi, conduite réelle, comportement du cache de carte sous coupure réseau. Aucune assurance de couverture exhaustive ou d’activité actuelle des points. Les pourcentages restent des estimations historiques, sans hausse pour ces travaux.

## Recette à effectuer sur Xiaomi

1. Importer le ZIP dans CodeAssist et compiler 11.14.166 / 366.
2. Premier démarrage sans réseau : ouvrir Alertes et vérifier FR 24429 inclus / 7325 chargés en l’absence de points personnels en doublon; CH 1777 inclus, sans affichage/alertes CH.
3. Activer les lieux temporaires : FR jusqu’à 24429 chargés, libellés de présence non confirmée; désactiver et vérifier retour à 7325 points d’inventaire.
4. Vérifier les filtres fixes, feux, tronçons, PN, zones et chantiers; le choix doit aussi atteindre le service GPS après son rafraîchissement périodique.
5. Explorer la carte puis recevoir un fix GPS : la carte doit rester libre; ⌖ reprend le suivi. Ne terminer la caméra qu’en fin de projet.
