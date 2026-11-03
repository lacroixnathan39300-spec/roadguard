# Validation ROADGUARD 11.14.168

## Vérifications exécutées

- 14 SVG valides, rendus avec un moteur SVG et inspectés visuellement à 70 et 42 px. Export SVG identique à celui de la bibliothèque partagée.
- Test Node `tools/test_radar_icons.js` : classification des 14 variantes; absence d’émojis, de scripts et de ressources distantes dans les dessins; marqueurs Leaflet limités au viewport, popup conservée, taille et visibilité; propriétés des symboles MapLibre, enregistrement des 14 images à densité double, filtre de visibilité, changement de taille, réenregistrement après changement de style et absence de décodage répété.
- Syntaxe du nouveau JS et des trois scripts inline des cartes Leaflet/MapLibre vérifiée avec Node. Le script inline de la carte locale est également analysé avant livraison.
- Les règles de dessin CSS sont limitées au SVG racine; les bandes internes du matériel gardent leurs dimensions.
- Aucun changement fonctionnel Java; seul le numéro affiché passe à 11.14.168. Le contrôle Java/API 34 et la préparation des assets de 11.14.167 restent applicables au code natif inchangé, sans être présentés comme un nouvel assemblage APK.

## Contrôles Python

| Groupe | Résultat |
|---|---|
| test_radar_map_render.py | OK |
| test_map_marker_size.py | OK |
| test_radar_map_visibility.py | OK |
| test_radar_categories.py | OK |
| test_map_manual_pan.py | OK |
| test_gps_stability.py | OK |
| test_embedded_radars.py | OK |
| test_swiss_activation.py | OK |
| test_radar_country_downloads.py | OK |
| test_radar_filter_indices.py | OK |
| test_radar_counters_current.py | OK |
| test_functional_handoff_current.py | OK |
| test_visual_handoff_current.py | OK |

Les contrôles de régression Python comprennent des assertions statiques. Les adaptateurs Node exécutent les vraies fonctions de rendu, mais ne reproduisent pas le moteur WebView ou le GPU du téléphone.

## Non vérifié

Assemblage Gradle/dex/ressources/signature d’un APK 11.14.168, installation et rendu sur Xiaomi. Vérifier les trois tailles, le jour/nuit, les fiches, le masquage des marqueurs et l’exploration libre après compilation dans CodeAssist.

## Conservation des données

Les CSV FR et CH et leurs SHA-256 restent ceux de 11.14.167. La sélection par défaut reste 7325 FR + 1777 CH = 9102 points. Les lieux mobiles restent non confirmés; aucune nouvelle donnée ni modification du guidage ou des alertes dans cette livraison.
