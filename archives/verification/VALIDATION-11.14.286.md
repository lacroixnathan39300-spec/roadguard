# Contrôles ciblés du lot 286

Commande : `python3 tools/run_guidance_checks.py`. Journal : guidance-11.14.286.log.

| Contrôle exécuté | Résultat |
|---|---|
| Compilation Java 8 des politiques, du suivi vocal et de son adaptateur | Réussie avec frontières Android/TTS/préférences injectées ; ce n’est pas une compilation Android |
| TestGuidanceAudio | 21 contrôles : priorité, préemption, jeton révoqué, refus/perte de focus, délai de reprise, callback défaillant et perte synchrone |
| TestSpeechPriority | 28 contrôles : vrai tracker, arbitre, adaptateur et registre ; radar interrompant guidage, arrêt TTS synchrone, retours périmés, restitution et nouvelle tentative |
| TestSpeechMilestones | 31 contrôles du registre et des annonces, dont erreurs et expiration |
| TestRerouteConfirmation | 26 contrôles : positions distinctes, temps de confirmation, hystérésis, délai, vitesse/précision, mesures périmées et unités |
| TestBubbleTransitions | 702 assertions : transitions des activités avec temporisation injectée, 32 combinaisons visibilité/verrouillage/permission, géométrie et 312 placements du bouton |
| TestMotion | Filtrage GNSS/réseau, arrêt, qualité/péremption, cap, conversion m/s et intentions/projection : réussi |
| TestRouteContinuity | 22 contrôles de projection, continuité et corridor : réussis |
| Analyse syntaxique Java | Sources modifiées analysées ; nombre et résultat dans syntax-11.14.286.log. Aucun contrôle des types Android par SDK |
| Ressources embarquées | Bilan et empreintes dans assets-11.14.286.json ; fichiers CSV/DB/SQLite identiques à 285 |

L’adaptateur Android AudioManager, les fenêtres du gestionnaire Android et les générateurs de son sont relus et analysés syntaxiquement, sans exécution Android. Les fonctions autonomes utilisent leurs vraies implémentations ; les horloges, la frontière TTS et les préférences sont injectées. Les tests de géométrie ne sont pas des captures de l’écran Android. Les tests natifs ajoutés aux lots précédents restent à exécuter.

Pas de compilation Android complète, d’APK 286, d’installation, de Robolectric, de son réel, de Bluetooth, d’appel réel, de fenêtre flottante Android, de TalkBack, de GPS réel, de requête distante de recalcul ou de DHU. Le SDK Android et une distribution Gradle utilisables ne sont pas présents ici. Le comportement de focus selon la version Android et l’hôte Car devra être confirmé pendant la session finale.

Les tâches globales de direction radar, d’accessibilité et de transitions Android Auto restent ouvertes ; les pourcentages historiques ne sont pas augmentés par ce lot.
