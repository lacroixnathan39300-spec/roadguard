# ROADGUARD — DOSSIER DE TRANSMISSION COMPLET À CHATGPT WORK
**État de référence : 20 octobre 2026 — source la plus récente : 11.14.240**

> **Instruction principale à Work :** reprendre le projet Android existant, inspecter réellement les fichiers, compiler si l'environnement le permet, corriger les anomalies, continuer par étapes et produire un nouveau ZIP CodeAssist avec une note WORK. Ne jamais annoncer comme testé ce qui ne l'a pas été. Ne jamais modifier ni supprimer les bases radar d'origine.

## 1. Livrables, vérité sur les sources et méthode de reprise
- Le ZIP source `ROADGUARD-11.14.240-SUIVI-CENTRE-BANNIERE.zip` est inclus dans le paquet de transmission. **C'est la source de travail prioritaire**. Il contient le projet Gradle, les Java, ressources, tests et notes historiques. La racine interne peut encore s'appeler `ROADGUARD-11.14.224` : ne pas en déduire que le code est ancien ; lire les notes `WORK-11.14.240.md` et les fichiers réels.
- Les APK RadarDroid, Waze, Sygic et ROADGUARD historique ont été fournis pour **analyse technique / comparaison**, pas comme dépôts de code source. Un APK/XAPK contient du code compilé et des ressources, **pas le code source original complet**. Ne pas prétendre posséder les sources propriétaires, ne pas recopier des composants propriétaires, contourner des services restreints ou utiliser des API privées sans autorisation.
- Les fichiers APK/XAPK tiers ne sont pas dupliqués dans ce paquet pour limiter son volume. Leurs noms exacts et empreintes SHA-256 figurent ci-dessous ; demander à l'utilisateur de les joindre à Work si leur inspection binaire devient indispensable. Le projet ROADGUARD source, lui, est inclus.
- **Aucune action sur les APK tiers ne doit être nécessaire pour commencer** : poursuivre d'abord le code ROADGUARD.
- CodeAssist sur Android est l'environnement de compilation réel de l'utilisateur ; il travaille principalement sur un téléphone Xiaomi, sans PC.
- Vérifier `app/build.gradle` : dans la source inspectée, `compileSdk 36`, `targetSdk 36`, `minSdk 26`, `applicationId fr.roadguard.app`, `versionName` Gradle **11.14.202** malgré l'étiquette de livraison **11.14.240**. **Corriger cette incohérence de version dans une prochaine livraison** sans casser les migrations. Le module Android Auto est conditionnel à `-ProadguardAndroidAuto=true`.
- La source inspectée contient environ 81 fichiers Java (tests compris), un manifeste principal et un manifeste `src/auto/`, MapLibre et un mécanisme de préchargement/cache de tuiles.

## 2. Objectif produit
ROADGUARD est un GPS Android privé de navigation et d'alertes radar, à interface premium graphite/noir et rouge. Il doit être utilisable sur téléphone et, si techniquement possible et autorisé, sur Android Auto. L'utilisateur exige :
- Carte en ligne fluide (MapLibre) avec cache temporaire automatique autour du GPS, y compris quelques kilomètres sans réseau, reprise transparente au retour du réseau.
- **Ne pas réintroduire le téléchargement de cartes France intégrales ni une bibliothèque de packs cartographiques permanents** ; l'ancien code peut subsister mais ne doit pas être réactivé comme fonctionnalité prioritaire.
- Position GPS stable, flèche centrée horizontalement, légèrement sous le centre vertical pendant la navigation, orientation et zoom fluides, retour volontaire au suivi après déplacement manuel de la carte (ne pas forcer le recentrage pendant l'exploration manuelle).
- Alertes radar fiables et lisibles : feu rouge ≠ vitesse, radar chantier = présence non confirmée si donnée incertaine, distance cohérente entre bandeau et voix, pas de double notification, bandeau compact/masqué en l'absence de radar proche.
- Gestion de bases radar par pays avec drapeaux, tri alphabétique, recherche, bouton rouge Télécharger, vert seulement si état clairement défini, gestion des mises à jour et conservation de toutes les bases existantes.
- Design calqué sur les maquettes validées : sombre graphite, accents rouges, commandes arrondies, carte dominante, disposition propre au Xiaomi, adaptation aux barres système Android 15/16.
- Les changements caméra (module caméra distinct) doivent rester **en toute fin de projet** ; ne pas toucher au module caméra avant le reste.
- L'application est destinée à l'usage personnel de l'utilisateur ; ne pas présumer qu'une distribution Play Store est souhaitée.

## 3. Retour réel utilisateur : ce qui a été vu et ce qui ne l'a pas été
### Confirmé par captures/retours
- Le projet fonctionne sur Xiaomi, affiche la carte et le suivi GPS.
- Le nouvel écran « Installer les radars » avec drapeaux, recherche, boutons et états fonctionne depuis le raccordement correct dans `MainActivity.openRadarCountryPicker()` (11.14.227).
- Capture à 16:24 : Suisse « Archive 7z contrôlée • radars non activés » ; France téléchargée (`fr-camera.sqlite.7z`) mais conversion non intégrée ; plusieurs pays affichent « Radars intégrés • N points Lufop ». L'état affiché ne prouve pas que les 7z alimentent le moteur d'alertes.
- Captures de navigation à 16:23 : carte, radar chantier, vitesse et bandeau radar visibles ; flèche parfois décentrée, bandeau trop présent. La 11.14.240 a reçu des corrections ciblées mais **aucun retour de test de cette version n'est encore disponible**.
### À NE PAS annoncer comme validé
- Extraction des 7z, lecture SQLite et injection des nouveaux points radar : non implémentées complètement à la date de transmission.
- Tous les liens pays : non vérifiés un par un, les réponses HTTP et licences doivent être auditées.
- Android Auto natif : non démontré sur véhicule ; anciennes difficultés de dépendances AndroidX Car dans CodeAssist.
- Compilation complète de la 11.14.240 et tests GPS en conduite : non démontrés.
- Synchronisation exacte voix/bandeau, dédoublonnage des sources et affichage radar dans toutes les conditions : non démontrés.

## 4. Code à ouvrir en priorité
**Racine :** `app/src/main/java/fr/roadguard/app/`
1. `MainActivity.java` : UI principale, carte, suivi caméra, navigation, bandeaux, déclencheurs d'écrans et boutons. Chercher `openRadarCountryPicker`, gestion de la caméra et du bandeau radar.
2. `RadarDownloadActivity.java` : liste des pays, états, recherche, téléchargement, file groupée, annulation, import avancé, WindowInsets.
3. `BackgroundAlertService.java` : GPS arrière-plan, annonces vocales, coordination foreground/background, rechargement des bases.
4. `AlertMilestones.java`, `RadarAlertPolicy.java`, `DangerTypePolicy.java`, `DangerSpatialIndex.java`, `RadarAlertAudio.java` : règles d'alertes, validation des distances/coordonnées, types de radars, audio et seuils.
5. `BundledRadarCatalog.java`, `LufopEuropeCatalog.java`, `UpdatedRadarCatalog.java`, `NeighborRadarCatalog.java` : bases d'origine, Lufop, surcouches téléchargées et fusion. **Ne pas écraser les données originales.**
6. `AuthorizedRadarImporter.java`, `LufopAscZipImporter.java`, `RadarSourcePolicy.java` : import CSV/ASC, contrôles de sécurité, téléchargements HTTPS et SHA-256.
7. `VisibleTileCache.java`, `OfflineCellStore.java`, `NearbyOfflinePlanner.java`, `GpsMotion.java` : carte en ligne, cache et mouvement.
8. `CarLocationService.java`, `DriveAutoUi.java`, `CarProjection.java`, `src/auto/java/...`, `app/src/auto/AndroidManifest.xml` : architecture automobile et Android Auto optionnel.
9. `app/build.gradle`, `app/src/main/AndroidManifest.xml` : configuration, permissions, targetSdk, dépendances et version.
10. Notes `WORK-11.14.198` jusqu'à `WORK-11.14.240` : historique utile mais **vérifier chaque affirmation contre le code**.

## 5. Travaux prioritaires (dans cet ordre)
### P0 — stabiliser et mesurer (avant toute régression)
- Importer le ZIP, identifier la racine réelle, inventorier les sources et construire une base de hashes SHA-256 des ressources radar d'origine. Comparer à chaque livraison.
- Compiler le variant téléphone si environnement compatible ; sinon faire au minimum un contrôle syntaxique ciblé et marquer la compilation « non effectuée ».
- Relever les problèmes de versioning, doublons, TODO et erreurs de branches dans les fichiers Java ; ne pas inventer de résultats.
- Ne pas faire une **grosse vérification plus souvent qu'environ une fois par heure** ; entre-temps, développement ciblé autorisé.
### P1 — carte et alertes (retour utilisateur le plus récent)
- Corriger définitivement le suivi de la caméra : centre horizontal de la flèche, ancrage vertical configurable, lissage, orientation, zoom ; respecter le mode exploration manuelle et le bouton recentrer.
- Transformer le bandeau radar en composant contextuel : réduit/masqué si pas de danger pertinent, visible avec animation mesurée à l'approche, ne jamais masquer les commandes de navigation. Vérifier l'accessibilité et l'affichage en paysage.
- Une source de vérité pour la distance radar : GPS timestamp, filtrage, arrondi commun voix/texte, anti-doublon par ID/type/position, anti-répétition. Ne pas présenter une mesure périmée comme actuelle.
- Feu rouge/passage à niveau sans limite de vitesse artificielle ; radar chantier = présence à confirmer selon la provenance ; éviter les alertes erronées sur route parallèle / sens opposé quand données directionnelles disponibles.
- Tests de logique sur mesures GPS simulées, invalides, changements rapides, hors route, faible précision, arrière-plan/avant-plan.
### P2 — téléchargements radar réellement opérationnels
- Inventorier les bases **France et Suisse** existantes, et les données Lufop Europe ; afficher clairement « intégré », « archive reçue », « importé », « actif » et « erreur ». Le bouton vert « Actif » ne doit apparaître qu'après activation réelle et lecture par le moteur.
- Auditer chaque URL pays, licence, taille, format, HTTPS, redirections, limites, hash lorsque publié, signature 7z, CRC, et protection contre zip bombs / fichiers malveillants. Ne pas utiliser les endpoints protégés de RadarDroid.
- Implémenter une **vraie extraction 7z** compatible Android/CodeAssist (bibliothèque disponible, licence et poids vérifiés), puis lecture SQLite (`SQLiteDatabase` en lecture seule ou extraction contrôlée), inspection du schéma réel de SpeedAlert avant de coder le mapping.
- Normaliser vers un modèle canonique (`id`, pays, lat, lon, type, sens, vitesse, source, date, fiabilité) ; conserver les champs inconnus en métadonnées si utile.
- Dédupliquer avec les bases d'origine et Lufop (distance, type, source et direction), ne pas supprimer les données d'origine ; stocker les surcouches dans un dossier séparé avec remplacement atomique et rollback.
- Recharger le moteur d'alertes sans redémarrage si possible ; mesurer réellement le nombre de points actifs par pays et afficher les sources.
- Finaliser « Tout mettre à jour » : queue séquentielle, annulation sûre, reprise/échec, statut par pays, journaux et bilan, aucune connexion réseau au volant demandée.
- Pour pays sans source autorisée : **ne pas les supprimer automatiquement** s'ils ont des radars intégrés ; afficher « données intégrées / mise à jour indisponible ». N'afficher « Télécharger » que si une source exploitable a été validée.
### P3 — Android Auto
- Étudier les contraintes Android Auto officielles et les bibliothèques `androidx.car.app`, catégorie navigation, services, templates et restrictions de distribution/test. Ne pas promettre un affichage de WebView arbitraire sur Android Auto.
- Conserver le variant téléphone qui compile sans `androidx.car` ; isoler le variant automobile ; tester réellement sur DHU ou autoradio si possible.
- Si l'objectif Android Auto natif est bloqué par politique/SDK, expliquer le blocage exact et proposer un parcours techniquement et légalement viable ; ne pas inventer de permission magique.
### P4 — finition
- Harmoniser les dix maquettes de référence et le thème graphite/rouge ; remplacer les icônes provisoires par de vraies ressources utilisables, pas des caractères emoji partout.
- Corriger les insets, marges de sécurité, navigation gesture, clavier, paysage, petites/grandes résolutions.
- Reporter toute refonte de la caméra jusqu'à la fin.

## 6. Données d'origine : exigence non négociable
- Le projet contient des ressources CSV/ASC/JSON ; 59 fichiers de ces extensions ont été comparés entre plusieurs versions récentes sans différence selon les vérifications ciblées. **Ne pas confondre 59 fichiers de ressources avec 59 bases radar actives.**
- Les 7 bases radar d'origine évoquées par l'utilisateur sont un sous-ensemble important ; **les préserver toutes** ainsi que les autres ressources et les bases France/Suisse.
- Le ZIP `Lufop-Zones-de-danger-EU-ASC (1).zip` a servi à construire une surcouche Europe (environ 80 000 points sur 31 pays selon les versions). Le comptage exact, les doublons et l'activation doivent être recalculés sur le code réel.
- Ne pas fusionner de manière destructive. Privilégier `assets` immuables + stockage interne pour téléchargements + métadonnées de provenance + rollback.
- Vérifier que les bases sont chargées par **le moteur d'alertes**, pas seulement visibles dans le gestionnaire.

## 7. Références des applications fournies
Les fichiers suivants ont été fournis dans l'espace de travail de la conversation. Pour réanalyse binaire dans Work, les joindre séparément si nécessaire. Ne pas les redistribuer publiquement.

| Nature | Nom exact du fichier | Taille | SHA-256 |
|---|---|---:|---|
| Projet source complet ROADGUARD 11.14.240 | `ROADGUARD-11.14.240-SUIVI-CENTRE-BANNIERE.zip` | 29.13 Mo | `c987f78d1d73a170993d7b41007fc50e74f85760a37eed36ac4f102033b26b24` |
| APK ROADGUARD historique | `ROADGUARD-11.14.197.apk` | 7.62 Mo | `6b04b404b2a7671e88c2742b080248e87735c8eede1440dbd8f4ebe81386a6e2` |
| RadarDroid authentique (APK) | `com-ventel-android-radardroid2-375-57480329-e3c6e52b199870597f6759bafa453a98 (1).apk` | 11.63 Mo | `750ef56cd0890fcab8be11f7c0bfb0656ec20c1314e8399e6f53fccc227ea4de` |
| APK nommé radardroid (en réalité Aptoide selon audit précédent) | `radardroid (1).apk` | 19.14 Mo | `188bbdb899be2da401ac0471ba4d10693612fa1067f2ea0987cc6e1afcfd4374` |
| Waze (APK) | `waze-5-22-90-123.apk` | 124.94 Mo | `a45040acce4dbf79c1445a76415b8e3457dd8a720d373f81c8a98c5529f08c80` |
| Sygic (XAPK) | `com.sygic.aura-26.6.2-121046.xapk` | 160.66 Mo | `465ae8e8adafaeb40c01909b5cae5185a8cccca584147026cc6c6f3fb1d2344b` |
| Lufop Europe (ASC ZIP) | `Lufop-Zones-de-danger-EU-ASC (1).zip` | 0.70 Mo | `cc2991cbe3796915ecf260696b3b7867bb874f6ba4cd1e0c8f9363f1ec7c8e7a` |

### Observations précédentes à revérifier
- APK RadarDroid authentique : chaînes/éléments d'interface identifiés `downloadDb`, `downloadAndUnzip`, `DBProviderPickerDialogFragment`, `EXTRA_DOWNLOAD_PROGRESS`, `providerlist_new.zip` ; **ce n'est pas un protocole de téléchargement public intégré**. Ne pas décompiler/coller du code propriétaire dans ROADGUARD.
- `radardroid (1).apk` était identifié comme Aptoide lors d'un audit antérieur : vérifier le package réel avant toute analyse.
- Waze et Sygic sont des références UX/fonctionnelles ; leur code n'est pas fourni en sources. Ne pas tenter de réimplémenter leurs services privés ou intégrer une WebView en la présentant comme Android Auto natif.
- Lufop ASC est une archive de données, pas un programme.

## 8. Sources publiques à examiner (pas de garantie de disponibilité)
- SpeedAlert : `https://github.com/hvdwolf/SpeedAlert_Databases/releases` — archives par pays au format `.sqlite.7z` (ex. `fr-camera.sqlite.7z`, `ch-camera.sqlite.7z`). Vérifier la release réellement publiée, le lien HTTP, la licence et le schéma avant de s'appuyer dessus.
- OpenStreetMap : `https://github.com/Ciriak/speed-cameras` — JSON/GeoJSON, attribution/licence ODbL à respecter.
- Open-GATSO-POI : `https://github.com/1e1/Open-GATSO-POI` — formats POI divers ; vérifier fraîcheur, couverture, licence.
- Ne pas fabriquer de fichiers ou de points radar. Respecter la réglementation locale des avertisseurs de radars (notamment France et Suisse) et prévoir des modes d'affichage compatibles avec les restrictions applicables ; demander validation juridique si nécessaire.

## 9. Critères de validation avant de dire « terminé »
1. Projet Gradle importe et compile sur CodeAssist sans erreur (ou fournir le journal exact et corriger).
2. Carte démarre et reste fluide ; cache temporaire fonctionne sur coupure réseau et reprise.
3. Flèche centrée selon ancrage voulu, pas de recentrage intempestif en exploration manuelle.
4. Bandeau replié hors alerte ; visible au bon moment ; pas de notification redondante.
5. Types de radars, distances, annonces vocales et seuils cohérents.
6. France/Suisse et toutes bases originales intègres (hashs inchangés).
7. Un pays test : téléchargement réel → extraction 7z → lecture SQLite → conversion → activation → comptage → alerte simulée ; rollback si erreur.
8. « Tout mettre à jour » : cas succès, HTTP 404, timeout, téléchargement partiel, annulation, absence de réseau.
9. Interface pays avec drapeaux, tri, recherche et états vrais.
10. Android Auto testé séparément, ne pas bloquer le téléphone pour le faire fonctionner.
11. Aucune régression des fonctionnalités antérieures.
12. Note de version et tableaux de progression cohérents.

## 10. Méthode de progression chiffrée
- **48,0000 %** est une **estimation historique non auditée** ; ne pas l'appeler « pourcentage calculé ».
- Établir à la prochaine grande vérification un backlog pondéré de **100 points** : carte/GPS 20, alertes 20, sources/activation radar 25, UI/ergonomie 10, Android Auto 10, qualité/tests/compilation 10, sécurité/données 5.
- Pour chaque tâche : poids, preuve code, preuve test, statut (à faire / codé / testé / validé), date/version. Le pourcentage **développement** compte la partie codée, le pourcentage **validation** uniquement les tests réalisés. Éviter les variations fictives ; afficher exactement quatre décimales, y compris les deltas.
- À chaque livraison, fournir **le tableau complet par module, total, delta, chantier actif**, préciser le caractère audité ou estimé.
- Ne pas lancer de gros audits à moins d'environ une heure d'intervalle. Le travail de développement peut continuer entre les audits.

## 11. Protocole de livraison Work
Pour chaque nouveau ZIP :
1. Partir de la dernière source, préserver les fichiers de ressources et les notes.
2. Faire une modification substantielle avec tests unitaires/ciblés ; vérifier syntaxe, imports, classes, manifest et compatibilité CodeAssist.
3. Générer un ZIP de projet Gradle complet, **pas un patch isolé**.
4. Ajouter `WORK-<version>.md` : modifications, terminées, TODO, build, tests, versions, blocages, hashes/contrôles, intervention demandée.
5. Vérifier l'intégrité du ZIP, lister les fichiers, comparer les SHA-256 des ressources radar et conserver les preuves.
6. Donner le lien du ZIP et les pourcentages exacts à 4 décimales ; distinguer compilation/test réels de simple vérification statique.
7. Si une action utilisateur est requise, afficher **« INTERVENTION OBLIGATOIRE » en rouge**, avec étapes exactes et préciser **maintenant** ou **plus tard**. Ne pas prétendre qu'aucune action n'est requise si un blocage réel existe.
8. L'utilisateur préfère compiler **à la fin de lots substantiels**, pas à chaque micro-version.

## 12. Première mission concrète à exécuter dans Work
1. Ouvrir le ZIP 11.14.240 inclus et inventorier le vrai code. Ne pas se fier uniquement aux notes.
2. Construire la table de progression pondérée et enregistrer un état de référence auditable.
3. Rechercher les routines de caméra et de bandeau dans `MainActivity.java` ; écrire un correctif robuste et des tests sur le centrage/visibilité.
4. Inspecter `RadarDownloadActivity` et le chargement des surcouches : établir un plan d'import réel `.7z` → SQLite → catalogue sans toucher aux bases.
5. Préparer une version suivante, avec note WORK, contrôle ZIP et protection des données.
6. N'interrompre l'utilisateur que si un fichier indispensable manque ou pour un vrai test Xiaomi. Il a demandé de **continuer activement** plutôt que de multiplier les demandes de compilation.

**Important :** l'objectif est un ROADGUARD réellement fonctionnel, pas une imitation superficielle des captures ni un compteur de progression artificiel.
