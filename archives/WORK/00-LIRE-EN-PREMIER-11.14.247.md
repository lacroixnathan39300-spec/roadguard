# ROADGUARD 11.14.247 — dossier complet

Ce dossier est le projet Android complet à importer dans CodeAssist : `app`, sources, ressources, bibliothèques, données radars originales, tests, scripts, preuves, documentation et les deux APK debug dans `LIVRAISON`. Le ZIP parent contient ce seul dossier.

## Correctif du diagnostic reçu

CodeAssist a signalé `The import fr.roadguard.vendor cannot be resolved` puis `SevenZFile cannot be resolved`. Son import Gradle peut ne pas mettre les JAR locaux sur le classpath de son compilateur Java. `RadarSevenZipExtractor` ne référence donc plus statiquement ces classes. Il résout la bibliothèque 7z isolée au moment d'un import, avec exactement les contrôles de mémoire (32 MiB), codecs, taille, CRC, nom, nombre d'entrées, signature SQLite et nettoyage temporaire précédents. Si la bibliothèque manque aussi dans l'APK créé par CodeAssist, l'import échoue explicitement avec « Bibliothèque radar 7z absente de l’application » ; aucune base active n'est écrasée. Le fichier `app/libs/roadguard-radar-runtime-1.jar` reste requis et est bien intégré aux deux APK Gradle livrés.

L'avertissement `targetSdk 36 enables edge-to-edge` n'est pas une erreur de compilation. `MainActivity` et `RadarDownloadActivity` appliquent déjà les marges `WindowInsets` des barres système ; la cible 36 est conservée.

## Contrôles et limites

`tools/test_codeassist_7z_classpath.py` compile le véritable extracteur sans bibliothèque fournisseur et vérifie le message d'échec propre en cas d'absence du runtime. `tools/run_targeted_checks.py` réexécute 25 scénarios d'import 7z/SQLite avec les vrais fichiers FR/CH et les contrôles de cache/pack de carte. Les APK et les tests Android simulés sont documentés sous `verification`. Un build CodeAssist **sur ce Xiaomi** n'est pas exécuté ici : le correctif du message précis est vérifié localement, mais son empaquetage 7z par l'IDE reste à constater sur l'appareil.

Le « pack texture » est le pack local d'apparences **Graphite / Lisibilité / Couleurs OSM**, inclus sous `app/src/main/assets/map_packs`. Il modifie l'aspect de la carte, pas les données géographiques. Les tuiles restent chargées en ligne et conservées dans un cache temporaire borné. Le rendu visuel sur le téléphone et la récupération réelle de nouvelles tuiles nécessitent un essai utilisateur.

WazeAPI et OpenWeb Ninja sont deux fournisseurs **facultatifs** de signalements externes dans « Contrôles mémorisés ». Ils demandent leur propre clé API et ne fournissent ni les radars fixes nationaux ni les tuiles de carte. Ils restent désactivés par défaut et n'interviennent pas dans la compilation ou le pack graphique.

🔴 INTERVENTION OBLIGATOIRE — PLUS TARD : extraire le ZIP, importer le dossier `ROADGUARD-11.14.247` dans CodeAssist et compiler. Si l'import des bases FR/CH sur le Xiaomi affiche « Bibliothèque radar 7z absente », transmettre le log complet de construction CodeAssist : le compilateur Java serait corrigé, mais son empaqueteur aurait ignoré le JAR. Tester également les trois apparences et le retour de tuiles après coupure réseau. Ne désinstaller aucune version existante uniquement pour ce contrôle.
