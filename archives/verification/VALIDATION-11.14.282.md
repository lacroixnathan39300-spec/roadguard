# Contrôles ciblés du lot 282

| Contrôle réellement exécuté | Résultat |
|---|---|
| TestSpeechMilestones, classes réelles AlertSpeechTracker et AlertMilestones, frontières Android injectées | 21 vérifications réussies : réservation, refus immédiat, erreur asynchrone, interruption, achèvement, changement de propriétaire, rollback protégé, réarmement et valeurs invalides. Aucune voix physique synthétisée. |
| TestAlertPolicies | Distances vocales/visuelles, types et limites, valeurs invalides et étapes réussis. |
| RadarApproachFilterProbe, classes réelles | Avant/arrière, route parallèle, segments diagonaux à trois latitudes, antiméridien et projections invalides : réussi. |
| test_map_startup.js | 11 scénarios réussis avec adaptateur de moteur, dont restauration d'exploration, nouvelle position GPS et recentrage volontaire. Pas de WebGL réel. |
| test_map_view_state.js | Restauration Leaflet, état invalide, recentrage et ordre des scripts : réussi. |
| test_vector_camera.js | Contrôles de suivi, pause et recentrage réussis avec adaptateur. |
| Analyse syntaxique JDK | MainActivity, BackgroundAlertService, AlertSpeechTracker, AlertMilestones et RadarApproachFilter analysés sans erreur syntaxique. |

Les classes du registre vocal et de géométrie ont été compilées avec la cible Java 8. Les frontières de préférences et de synthèse vocale sont des adaptateurs explicites dans tools/speech-tests, non des classes embarquées dans l'application. Les deux grands propriétaires Android n'ont pas été compilés ni exécutés ici.

Pas de nouveau grand audit de l'ensemble de l'application. Les données radar et les sept bases embarquées n'ont pas été modifiées dans ce lot. Les parcours nouveaux nécessitent la session finale Android demandée par l'utilisateur.
