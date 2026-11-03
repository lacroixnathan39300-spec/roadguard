# ROADGUARD 11.14.260 — gel pendant l'import des radars

La capture utilisateur montre « ROADGUARD ne répond pas » dans l'écran Installer les radars, après vérification d'une archive Suisse. Le service de surveillance demandait alors un rechargement intégral du catalogue radar directement sur le fil principal Android : lecture des bases, dédoublonnage et construction de l'index spatial. Il faisait également ce travail au démarrage et toutes les 30 secondes pendant le suivi GPS.

Correction : le service construit désormais un catalogue complet sur son exécuteur dédié, conserve les anciens points pendant la construction, puis publie ensemble les points, noms et index sur le fil principal. Les demandes simultanées sont regroupées et une mise à jour en attente est relancée après la publication. Un échec de lecture conserve l'ancien catalogue. L'exécuteur est arrêté avec le service. L'enregistrement de la révision radar utilise `apply()` afin de ne pas attendre une écriture disque sur l'interface.

Les archives France/Suisse intégrées et les cartes sont conservées dans ce projet. L'import par pays et l'écran de gestion restent disponibles. Cet archive contient le projet complet, sans APK distinct.

Vérification locale : analyse syntaxique des 68 sources Java de l'application avec le parseur JDK. Cet environnement ne dispose pas du SDK Android et ne permet donc pas de construire ni d'exécuter l'APK. Le diagnostic provient de la lecture du code et de la capture ; aucun journal ANR de l'appareil n'a été fourni.

🔴 INTERVENTION OBLIGATOIRE — Sur le téléphone : compiler ce ZIP dans CodeAssist, installer la version 11.14.260, ouvrir Installer les radars, appuyer sur Importer pour Suisse puis France, attendre le message « points importés », revenir à la carte et vérifier que l'écran reste réactif. Si « ne répond pas » revient, transmettre l'historique ou le journal Android de l'incident, avec l'heure précise.
