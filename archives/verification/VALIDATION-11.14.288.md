# Contrôles ciblés du lot 288

Les corrections visent les quatre problèmes des captures transmises : doublons, carte hors ligne vide, icône du contrôle routier et panneau Spotify.

## Exécutions locales

Commande Java : `python3 tools/run_user_fixes_checks.py`. Journal : user-fixes-11.14.288.log. Les composants autonomes sont compilés en cible Java 8. Les frontières Android, les préférences et le stockage canonique utilisent les adaptateurs JVM existants ; ce n’est pas une compilation de MainActivity ou du panneau Spotify.

| Groupe | Résultat |
|---|---|
| TestUserFixes | 24 444 contrôles, dont la conservation de tous les identifiants FR d’origine. Deux vrais emplacements des captures regroupent leur observation Lufop ; limitations, directions, types, statuts, sources indépendantes et correspondances ambiguës restent protégés |
| TestMusicProgress | 15 contrôles sur la vraie politique de temps : lecture, pause/chargement, position inconnue, horloge absente/future, vitesse invalide, durée et formatage |
| TestRasterTileSource | 12 contrôles du vrai cache et de vrais fichiers : lecture hors ligne après redémarrage, repli vers PNG/JPEG importé, MIME, fichier invalide/trop grand, URL interdite, cache non initialisé ; zéro appel réseau en mode hors ligne |
| TestTileCacheStandalone | 17 scénarios sur de vrais fichiers ; transport HTTP injecté |
| TestRadarMerge | 28 contrôles et conservation des observations FR ; métadonnées et conflits |
| TestRadarSupplements | 12 scénarios, 24 442 assertions ; vrais fichiers, conservation des originaux |
| TestCatalogCore | 15 scénarios, 424 104 assertions ; vrais fichiers embarqués et adaptateurs JVM. Ces assertions nombreuses portent essentiellement sur la conservation des données, pas sur autant de scénarios indépendants |

Commande JS : `python3 tools/run_ui_regressions.py`. Journal : ui-regressions-11.14.288.log.

| Groupe | Résultat |
|---|---|
| test_offline_regressions.js | Cadre et zoom conservés au démarrage avec le vrai code d’initialisation Leaflet ; persistance limitée, repli natif quand le stockage DOM échoue, données invalides rejetées ; vrai callback de couverture avec images présentes, parents conservés ou images manquantes |
| test_radar_icons.js | 15 dessins SVG, classification distincte de Contrôle routier, fichiers exportés identiques à la source, marqueurs Leaflet/MapLibre, échelle, visibilité et rechargement de style |
| test_map_startup.js | 13 scénarios des scripts MapLibre complets, dont ancien cadre et maintien du fond raster en cache en mode vectoriel hors ligne |
| test_map_view_state.js | Restauration de l’exploration et retour au suivi conservés |
| test_map_native_network.js | Le mode réseau natif reste prioritaire sur les événements réseau du navigateur |
| test_vector_camera.js | Marges, pause manuelle, reprise, aperçu du trajet et garde de style |
| roadguard_map_syntax_check.js | Scripts intégrés des deux moteurs analysés sans erreur |

Les adaptateurs JavaScript simulent la carte et le DOM ; aucun de ces essais ne rend des pixels dans WebView ou WebGL. Le repli JPEG vérifie la signature et le chemin, pas le décodage Android. Le rapprochement Lufop ne certifie pas l’identité de toute caméra géographiquement proche : un cas ambigu reste séparé.

Analyse syntaxique Java : 13 fichiers modifiés, succès dans syntax-11.14.288.log. Ce contrôle ne résout pas les types Android.

## Tests natifs préparés et non exécutés

- MapModeRuntimeTest : la sélection Hors ligne puis Auto doit conserver la même WebView déjà chargée.
- SpotifyRuntimeTest : deux scénarios ajoutés, sélection d’une session active et capacité de déplacement ; le scénario d’ouverture vérifie aussi l’absence d’un second dialogue et sa fermeture.
- Les anciens tests natifs des lots précédents restent à exécuter lors de la session finale.

Pas de compilation Android complète, d’APK 288, de Robolectric, de rendu Xiaomi, de validation TalkBack, de contrôle d’une vraie session Spotify, de permission système accordée, de son ou de Bluetooth dans cet environnement. Les méthodes de lecture/commande MediaController, l’apparence du dialogue et le cycle réel WebView restent à valider sur téléphone. Aucun test de charge du serveur n’a été répété dans ce lot.

Les 11 CSV/DB/SQLite sont comparés octet par octet à 287 dans assets-11.14.288.json. Les sources originales restent intactes. Les actualisations communautaires restent à deux minutes. La grille historique reste inchangée : ces sous-parties ne ferment pas entièrement A6 ou U4 et ne valent pas validation sur appareil.
