# Contrôles ciblés du lot 284

| Contrôle exécuté | Résultat |
|---|---|
| Compilation des classes de catalogue ciblées avec javac via Java, cible Java 8 | Réussie avec les frontières Android de tools/jvm-support ; hors activités/services Android et org.json |
| TestRadarMerge | 28 contrôles réussis, plus conservation de tous les identifiants FR : enrichissement, conflits, idempotence, type, pays, snapshot immuable, révisions des anciens fichiers, catalogue réel et sauvegarde d'un pays corrompu |
| TestRadarSupplements | 12 scénarios / 24 442 assertions réussis : originaux conservés, types voisins distincts, frontière entre pays, quarantaine, corruption, interruption et limite des doublons |
| TestCatalogCore | 15 scénarios / 430 305 assertions réussis avec vraies ressources : FR 24 429, CH 1 777, Europe 80 021 ; 104 838 points dans la sélection du scénario initial |
| Syntaxe Java des fichiers de production modifiés et du nouveau test Robolectric | Réussie ; analyse syntaxique seulement pour les classes Android |
| Comparaison binaire des CSV / DB / SQLite embarqués avec 283 | 11 fichiers identiques ; aucun nouveau radar ajouté aux bases embarquées |

Commande reproductible des contrôles de catalogue : `python3 tools/run_radar_merge_checks.py`. Journal : radar-merge-11.14.284.log.

Une ancienne fixture de doublon utilisait le premier point FR quel que soit son type ; elle a été précisée pour viser réellement un radar fixe, et un scénario distinct contrôle la conservation d'un autre type au même endroit. Le premier échec de cette fixture a été corrigé avant la nouvelle exécution réussie.

InstalledRadarCatalogRuntimeTest contient trois tests Robolectric supplémentaires, non exécutés ici. Aucune compilation Android complète, aucune installation APK, aucun essai de callbacks SharedPreferences/Service sur Android, aucune simulation DHU, aucun test sonore ou réseau matériel. Le plafond global de 300 000 points n'a pas fait l'objet d'un test de charge. Les contrôles antérieurs ne sont pas présentés comme réexécutés dans ce lot.

Les validations Android restent regroupées pour la session finale. Les pourcentages historiques n'ont pas augmenté.
