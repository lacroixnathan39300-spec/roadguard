# Contrôles ciblés du lot 285

| Exécution locale | Résultat |
|---|---|
| Compilation des nouvelles politiques et composants autonomes, cible Java 8 | Réussie, sans stubs Android pour ces classes |
| TestRouteContinuity | 22 contrôles : boucle avec branche future parallèle, saut hors trajet, retour sur trajet, reprise après coupure, ancienne mesure, même instant, curseur restauré, distance routière, corridor, arrivée et antiméridien |
| TestMapInteraction | 400 assertions, dont 150 configurations géométriques et 124 placements flottants ; coordonnées françaises et valeurs invalides ; aucune mesure Android |
| TestLatestSearchHttp | 16 contrôles : taille annoncée/réelle, réponse exacte à la limite, redirection refusée, token périmé, interruption et annulation pendant l'attente des en-têtes ; frontière HTTP injectée |
| TestTileArchiveInstaller | 28 contrôles : vrais ZIP et fichiers, coordonnées, signatures de fixture, doublons, image trop grande, échec de renommage injecté, restauration, récupération, import remplacé, suppression pendant import, normalisation et nettoyage protégeant un import actif |
| RadarApproachFilterProbe | Avant/arrière, corridor, diagonales à trois latitudes, antiméridien et projections invalides réussis |
| test_camera_top_inset.js | 135 projections des fonctions Leaflet/MapLibre avec panneaux haut et bas, réussies ; aucun WebGL |
| test_camera_anchor.js | 405 cas projetés et gel de l'orientation en exploration, réussis |
| test_vector_camera.js | Suivi, reprise, panneau, aperçu du trajet et chargement de style, réussis avec adaptateur de carte |
| test_map_startup.js | 12 scénarios des scripts complets, réussis avec moteur simulé |
| roadguard_map_syntax_check.js | Scripts intégrés des deux cartes analysés sans erreur |
| ParseJava sur les sources Java modifiées | 13 fichiers analysés sans erreur de syntaxe ; pas de vérification des types Android |
| Données embarquées comparées à 284 | 11 CSV/DB/SQLite identiques ; 141 ressources de carte inchangées au total |

Commande des contrôles autonomes : `python3 tools/run_interaction_checks.py`. Journal : interaction-11.14.285.log. Le script d'import ancien test_import_tile_zip_zoom.py délègue maintenant aux tests comportementaux du composant au lieu de vérifier la forme d'une ligne de code.

Le premier scénario de saut GPS a révélé qu'une borne fondée seulement sur le déplacement observé était trop permissive. La borne a été corrigée pour limiter aussi l'avancement par le temps et la vitesse, puis le groupe ciblé a été exécuté avec succès. Les exécutions ont été élargies seulement après les changements du même lot ; aucun nouvel audit global de tous les modules n'est revendiqué.

MapOverlayRuntimeTest ajoute quatre tests natifs : portrait, paysage, gros caractères et sauvegarde de l'état de rotation. StartupRuntimeTest a été adapté à l'absence de bandeau de chargement radar dans la vue de carte compacte. Ces tests Android/Robolectric n'ont pas été exécutés ici. Les trois tests natifs ajoutés au lot 284 restent également à exécuter.

Pas de compilation Android complète, de vérification des types Android par le SDK, d'APK installé, d'essai Xiaomi/TalkBack, de rendu WebGL matériel, de réseau distant réel, de son ou de Bluetooth. Les fixtures PNG/JPEG valident les signatures et le cycle d'installation, pas le décodage complet d'une image Android. La limite globale de 100 Mio et le plafond de 25 000 entrées ne sont pas présentés comme testés en charge. Les interruptions de processus et la prise en charge des renommages atomiques restent à confirmer sur le stockage Android.

Le bilan des ressources et les fichiers modifiés sont listés dans assets-11.14.285.json et CHANGEMENTS-11.14.285.txt. Les chiffres de progression historiques restent inchangés.
