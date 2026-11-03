# Validation ROADGUARD 11.14.180

- 87 cas Android simulés réussis, 0 échec/erreur, sur API26/28/34. Suites et détails dans android-runtime-tests-11.14.180.json et les rapports JUnit XML de WORK.
- Tests réels de composants Java Android et AndroidX Car App Testing; l’hôte automobile est un hôte de test, sans véhicule ni DHU.
- Compilation Gradle 8.2.1 / AGP8.2.2 : assembleDebug réussi, code380/version11.14.180. Signature v2/v3 et alignement contrôlés après génération du paquet final; certificat de vérification conservé depuis les 178/179.
- 35 contrôles de régression Python/JavaScript réussis lors du contrôle final; résultat consigné dans regressions-11.14.180.json.
- Test Java des mesures GPS et du catalogue complet conservé de la 179; aucun fichier radar FR/CH modifié par la 180.
- Émulateur Android28 sans accélération matérielle : une APK intermédiaire a été installée (Success) et son activité lancée (Status ok). Le système a affiché une ANR de System UI puis le démarrage ROADGUARD est resté en attente. Carte et fluidité non validées. La version finale comportant le chargement asynchrone a été reconstruite et soumise aux 87 tests, sans nouvelle exécution complète sur cet émulateur.
- Aucun essai de conduite sur Xiaomi, aucun DHU/véhicule et aucune publication Play.

Les lectures initiales des données sont désormais exécutées en arrière-plan. Un test vérifie aussi que la réception de nouveaux signalements ne relit pas la base radar et ne recrée pas l’abonnement GPS. Ces corrections sont validées logiciellement; leur gain de fluidité réel reste à mesurer.
