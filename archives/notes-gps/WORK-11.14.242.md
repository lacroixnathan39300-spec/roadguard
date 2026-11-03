# ROADGUARD 11.14.242 — reprise complète et protection des catalogues

**21 octobre 2026 — versionCode 442 — applicationId `fr.roadguard.app`.**

Ce lot reprend le vrai projet, sans recréation : source complète 11.14.240 puis superposition des 46 fichiers du checkpoint 11.14.241 envoyé par lien. Le checkpoint est une sauvegarde partielle, pas un projet CodeAssist autonome. Le présent ZIP contient le projet Gradle complet sous une racine `ROADGUARD-11.14.242`.

## Références de reprise

- Source 240 : `ROADGUARD-11.14.240-SUIVI-CENTRE-BANNIERE.zip`, SHA-256 `c987f78d1d73a170993d7b41007fc50e74f85760a37eed36ac4f102033b26b24`.
- Le SHA-256 du checkpoint 241 et le début de session sont conservés dans `verification/session-11.14.242.json`.
- Les correctifs 241 de centrage, exploration manuelle, distance commune voix/texte, fraîcheur du GPS et versioning sont conservés. Les notes et preuves historiques restent présentes avec leur date ; elles ne sont pas des résultats Android de la 242.

## Modifications de ce lot

### 1. Import Lufop réellement additif

Le code précédent choisissait un CSV local valide à la place de la ressource Europe embarquée. Un import partiel pouvait donc retirer des points du catalogue actif alors que les fichiers de l’APK restaient intacts.

`LufopEuropeCatalog` charge maintenant l’inventaire embarqué puis ajoute la première surcouche locale valide, en essayant ensuite les deux sauvegardes en cas de corruption. Les originaux restent chargés, les doublons de même type/position sont ignorés et les types différents sont préservés. Les compteurs sont recalculés sur ce catalogue. La révision du cache correspond au début de la lecture : un fichier changé pendant le chargement sera relu à la demande suivante.

Une reproduction exécutée contre le code précédent échoue sur la disparition d’un point original. Le même scénario réussit après correction : `verification/lufop-regression-11.14.242.log`.

### 2. Téléchargements radar contrôlés et annulables

- `RadarArchiveDownloader` centralise le transfert et la fermeture des connexions, y compris sur HTTP 404, redirection refusée, timeout et taille incorrecte.
- La limite du flux est la taille exacte de la release épinglée, au lieu d’accepter jusqu’à 100 Mo avant comparaison finale. Un serveur sans Content-Length reste contrôlé par cette limite et le SHA-256.
- L’annulation déconnecte aussi une requête en attente de réponse. Une annulation avant le remplacement final laisse l’archive précédente intacte ; une archive déjà installée n’est pas effacée par une annulation tardive.
- Le fichier provisoire est propre au transfert. La validation précède un remplacement atomique dans le même dossier ; aucun effacement préalable de l’ancienne archive.
- `RadarArchivePolicy` contrôle signature, version, CRC du Start Header, bornes des métadonnées, entiers sans débordement et CRC du Next Header. **Ce sont des contrôles structurels, pas une extraction 7z.** Le SHA-256 épinglé contrôle aussi le contenu compressé.
- `RadarCountryBatch` garantit un seul pays en cours et conserve les résultats par pays. Le bouton Annuler fonctionne sur un téléchargement individuel comme sur la file groupée ; un échec de pays ne bloque pas le suivant.
- Les contrôles d’archives et les comptages s’exécutent hors du fil d’interface. Une réponse d’inventaire périmée ne remplace plus un relevé plus récent.
- L’écran distingue toujours une archive vérifiée et des radars activés. Les sept sources épinglées de la 241 sont conservées ; aucun nouveau contrôle HTTP public n’est annoncé pour cette session.

### 3. Rechargement de la carte après modification des bases

`RadarCatalogRevision` détecte les modifications d’imports et de visibilité. Au retour dans l’activité principale, `requestRadarCatalogReload()` prépare le catalogue en arrière-plan et remplace ses points en une publication sur le fil UI. Les dangers personnels, communautaires et les autres sources sont conservés. Un résultat devenu périmé est rejeté, les compteurs et l’index spatial sont remis à jour et l’ancienne alerte visuelle est nettoyée. Un simple retour avec une révision inchangée ne déclenche pas de relecture complète.

Les tests Android du retour depuis le gestionnaire, de l’absence de doublons et de la conservation de l’abonnement GPS ont été ajoutés dans `RadarCatalogReloadRuntimeTest`. **Ils n’ont pas été exécutés ici : ce raccordement Android reste à valider.** Le service conserve sa demande de rechargement existante ; la fusion directionnelle complète reste inachevée.

### 4. Livraison et version

Gradle est aligné sur 11.14.242 / 442 ; SDK 36, minSdk 26 et applicationId restent identiques. Le suivi affiché dans l’application lit le nouveau ledger versionné. Le conditionnement déduit désormais sa racine du versionName.

**Android Auto reste activé dans `gradle.properties` (`roadguardAndroidAuto=true`), comme dans la source reçue.** Le variant téléphone peut être isolé en mettant cette propriété à `false`, ou avec `-ProadguardAndroidAuto=false`. Les deux variantes ne sont pas déclarées compilées dans cette session.

## Vérifications effectivement exécutées

| Contrôle | Résultat et portée |
|---|---|
| Compilation Java 8 du cœur ciblé | Réussie pour 18 classes métier et les tests, avec des adaptateurs JVM explicites pour Context, préférences, assets, Location et Log. Ce n’est pas une compilation de l’application Android. |
| Téléchargements / file | 27 scénarios réussis : HTTP simulé, redirections, 404, lecture interrompue/timeout, longueur absente ou fausse, flux tronqué/trop long, mauvais SHA-256, CRC et bornes invalides, annulations avant/pendant/après transfert, simultanéité et bilan. Fichiers temporaires et renommage atomique réellement utilisés. |
| Catalogues / règles | 14 scénarios réussis sur les vraies ressources : inventaires FR/CH, import Europe partiel, types co-localisés, doublons, deux sauvegardes, corruption, révisions, visibilité, fraîcheur GPS et paliers concurrents. Les frontières Android utilisent des adaptateurs de test. |
| Inventaires lus par le test | Europe embarquée : 80 021 lignes ; FR : 24 429 ; CH : 1 777 ; sélection fusionnée avec historiques autorisés : 104 838 points. Ces nombres de test ne sont pas le comptage d’un téléphone ni le nombre de radars confirmés actuellement présents. |
| Caméra de carte | 405 géométries projetées réussies, ainsi que pause manuelle, rotation, dégagement du panneau et zoom adaptatif. Tests JavaScript hors navigateur, pas captures Xiaomi. |
| Autres politiques | Tests voix/texte, types sans fausse vitesse, mouvement GPS, compensation et projection réussis. |
| Syntaxe Java | 78 fichiers dans `app/src` analysés par le parseur JDK. Les types Android ne sont pas résolus par ce contrôle. |
| Version et manifestes | Package/SDK/version vérifiés et deux manifestes XML lus ; aucun crédit de build Android attribué. |
| Bases d’origine | 59/59 CSV/ASC/JSON originaux identiques à la référence SHA-256 240, y compris les bases FR/CH, les cinq voisins et l’Europe. Contrôle répété dans le ZIP livré. |
| ZIP complet | CRC de toutes les entrées et empreintes d’origine vérifiés lors du conditionnement. |

Les 430 302 assertions du test de catalogues comprennent des vérifications répétées de conservation des identifiants ; elles correspondent à **14 scénarios**, pas à 430 302 essais matériels.

Reproduction locale : `python3 tools/run_targeted_checks.py`, puis `python3 tools/update_progress_11_14_242.py` et `python3 tools/render_progress_report.py`. Les tests utilisent le module compilateur du JDK si l’exécutable `javac` n’est pas exposé. Résultats : `verification/targeted-checks-11.14.242.log`, `targeted-results-11.14.242.json` et `release-contract-11.14.242.json`.

## Compilation, tests non réalisés et blocages

La tentative réelle `sh gradlew --offline :app:assembleDebug -ProadguardAndroidAuto=false` échoue **avant la compilation** lors de l’installation de Gradle 8.11.1 : `java.net.SocketException: Network is unreachable`. Le SDK Android 36 n’est pas disponible non plus dans cet environnement. Journal exact : `verification/gradle-phone-11.14.242.log`.

**Aucun APK 11.14.242 produit, aucune suite Robolectric exécutée, aucun essai Xiaomi ou autoradio réalisé.** L’APK 11.14.197 conservé dans `LIVRAISON/` est historique et ne contient pas ces modifications. Les anciens journaux Android 241 décrivent une autre étape et ne remplacent pas une compilation de ce lot.

Les nouvelles bibliothèques 7z n’ont pas pu être récupérées et leur intégration Android n’a pas été ajoutée sans essai. L’extraction Android 7z → SQLite → conversion canonique → activation reste **à faire**. Les contrôles d’en-têtes de cette version ne rendent donc pas les archives France/Suisse actives dans le moteur.

## Avancement pondéré et travaux restants

Le backlog de 39 tâches sur 100 points est conservé. Référence : checkpoint 241, développement 75,0000 %, validation 18,0000 %. Le développement reste **75,0000 % (+0,0000 point)** : ce lot corrige des tâches déjà comptées comme codées et n’achève pas artificiellement les chantiers 7z, SQLite ou direction. La validation atteint **25,0000 % (+7,0000 points)** : D1 (+4,0000), D8 (+1,0000), Q1 (+2,0000). Les essais matériels gardent leur poids non acquis. Les 48,0000 % historiques restent une estimation séparée, non auditée.

Tableau complet par module, deltas et preuves par tâche : `AVANCEMENT-11.14.242.md`. Le ledger `app/src/main/assets/roadguard_progress_11_14_242.json` est la référence de l’application. Les validations héritées de la 241 restent explicitement datées ; seules les familles signalées comme réexécutées le sont pour ce lot.

**Chantier actif suivant :** import Android 7z borné → SQLite en lecture seule → modèle avec provenance et champs inconnus → surcouche atomique → comptage/activation réellement lus par les moteurs. Il faut aussi finaliser les sens et routes parallèles, la fusion de sources, les transitions Android Auto et la revue paysage/accessibilité. Le rechargement principal ajouté doit passer ses tests d’intégration Android.

Le thème graphite/rouge, les ressources visuelles, le cache temporaire automatique et le module vidéo sont conservés. Aucun téléchargement manuel de carte France ou pack permanent n’est réactivé. Les reprises de tests après correction sont ciblées ; aucun nouvel audit global répété à moins d’une heure n’est lancé pendant ce lot.

## Intervention utilisateur

**🔴 INTERVENTION OBLIGATOIRE — MAINTENANT, conservation de la livraison :** au moment de la livraison, l’enregistrement durable est refusé car l’espace de fichiers du compte a atteint sa limite. Télécharger le ZIP fourni pour le conserver. Libérer de la place permettra un enregistrement ultérieur ; aucune ancienne version n’a été supprimée pour contourner cette limite.

**🔴 INTERVENTION OBLIGATOIRE — PLUS TARD, pour valider ce lot sur ton appareil :**

1. Importer le ZIP complet 11.14.242 dans CodeAssist et conserver la signature habituelle de ROADGUARD. Ne pas désinstaller l’application ni effacer ses données.
2. Compiler une seule fois à la fin du lot. Si AndroidX Car empêche cette compilation, mettre `roadguardAndroidAuto=false` pour isoler le téléphone et transmettre l’erreur exacte du variant voiture.
3. Installer la mise à jour, vérifier le centrage/exploration manuelle et les alertes, puis changer une visibilité Lufop dans Installer les radars et revenir à la carte : compteur et marqueurs doivent suivre sans redémarrage, les originaux FR/CH doivent rester présents.
4. À l’arrêt, essayer un téléchargement individuel puis la file groupée et Annuler : l’état doit rester « archive vérifiée », sans activation fictive. Tester paysage et Android Auto séparément si le matériel est disponible.

Cette intervention est nécessaire pour les validations matérielles, pas pour poursuivre le code. Le blocage actuel du build dans cet environnement reste signalé ci-dessus ; aucune validation Android n’est prétendue en son absence.
