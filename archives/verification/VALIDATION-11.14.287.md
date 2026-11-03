# Contrôles ciblés du lot 287

Commande : `python3 tools/run_route_recovery_checks.py`. Journal : route-recovery-11.14.287.log.

| Exécution locale | Résultat |
|---|---|
| Compilation Java 8 | Vrais composants autonomes RouteRequestPolicy, NavigationProgress, RadarApproachFilter et LatestRouteHttp compilés, sans SDK ni adaptateurs Android |
| TestRouteDirection | 27 contrôles : bon sens, sens opposé, trajet perpendiculaire, entrée/sortie de virage, seuil de proximité du virage, cap absent/invalide, précision, éloignement, option non directionnelle, passage 359°/0°, antiméridien, sommets répétés et route de longueur nulle |
| TestLatestRouteHttp | 24 contrôles avec frontière HTTP injectée : limite annoncée/réelle, redirection refusée, jeton périmé, interruption, annulation pendant l’attente des en-têtes, nouveau calcul après remplacement, hôte/protocole/port/identifiants/fragment/chemin refusés et HTML refusé avant lecture du corps |
| TestRouteContinuity | 22 contrôles : boucle avec branche future parallèle, limites de continuité, anciennes mesures, reprise après interruption, corridor et antiméridien |
| RadarApproachFilterProbe | Avant/arrière, route parallèle hors corridor, diagonales à trois latitudes, antiméridien et projections invalides : réussi |
| Analyse syntaxique Java | Résultat et nombre de fichiers modifiés : syntax-11.14.287.log ; pas de vérification des types Android |
| Données | Bilan et empreintes dans assets-11.14.287.json ; les 11 CSV/DB/SQLite restent identiques à 286 |

Les transitions réelles du callback réseau Android et du sélecteur de mode ont été raccordées et relues, mais n’ont pas été exécutées dans Android. Le test d’annulation HTTP exerce le vrai client et une connexion injectée, pas un serveur OSRM distant. Il ne prouve pas les délais réels de fermeture d’une socket sur Xiaomi. Les tests géométriques ne prouvent pas la discrimination de deux chaussées très proches.

Pas de compilation Android complète, de vérification des types Android par SDK, de Robolectric, d’APK 287, de GPS réel, de requête distante, de Bluetooth ou de DHU. Les tests audio du lot 286 restent des résultats historiques de 286 ; ils ne sont pas présentés comme réexécutés pour ce lot. Android Auto reste reporté.

Aucun nouveau crédit n’est attribué à une tâche globale simplement parce que ces sous-parties passent leurs contrôles JVM. Grille historique conservée et deltas +0,0000 point.
