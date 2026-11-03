# ROADGUARD 11.14.243 — SQLite, surcouches et preuve de chargement

**21 octobre 2026 — versionCode 443 — package `fr.roadguard.app`.**

Reprise intégrale de la 11.14.242, sans recréer le projet. Le ZIP 242 de référence a le SHA-256 `f8d1e991a3134c15ff31052279987bc6534c7fac947684d4538f07873d1e238b`. Les sources restent dans un projet Gradle complet pour CodeAssist, désormais sous la racine `ROADGUARD-11.14.243`.

Le problème d’espace signalé lors de la première livraison 242 est résolu : le ZIP 242, sa note WORK et son avancement ont été enregistrés avec succès pendant cette reprise. L’ancienne mention de quota dans WORK 242 reste un état historique, pas le blocage courant.

## Changements réalisés

### Lecture SQLite en lecture seule

`SpeedAlertSqliteImporter` lit le schéma observé `cameras(id, lat, lon, type)`. Il contrôle la signature, une limite de 32 Mo, `quick_check(1)`, la présence d’une table ordinaire, les colonnes obligatoires, le comptage avant lecture (1 à 100 000 points), les types SQLite, les identifiants, les coordonnées et les tailles des textes. Les vues et tables virtuelles sont refusées.

L’ouverture utilise `OPEN_READONLY | NO_LOCALIZED_COLLATORS` et un gestionnaire de corruption sans suppression de la source. Les entrées locales sont copiées dans un fichier de travail interne temporaire, qui est effacé après lecture. Aucune base source n’est ouverte pour écriture et aucune table originale n’est modifiée.

Les colonnes inconnues textuelles ou numériques sont gardées dans les métadonnées ; les colonnes binaires supplémentaires sont refusées plutôt que tronquées. Une colonne supplémentaire nommée speed ne devient pas automatiquement une limite : le schéma étudié n’en définit pas. Vitesse absente = 0/inconnue, direction absente = valeur nulle. Le type brut et le SHA-256 du SQLite sont conservés.

### Modèle et stockage canoniques

`CanonicalRadar` représente pays, identifiant stable, position, type normalisé, vitesse, direction, source, date connue ou vide, fiabilité et métadonnées. Un type non reconnu est conservé avec l’étiquette générique « Radar · type non renseigné », sans inventer de vitesse ou de sous-type. Les types temporaires restent soumis aux filtres historiques existants.

`CanonicalRadarStore` écrit les surcouches dans `files/radar_updates/canonical/<pays>.csv`, séparément de tous les assets. Le fichier complet est validé et synchronisé avant un renommage atomique. Une copie de la surcouche précédente est préparée avant de remplacer le fichier actif ; deux générations de sauvegarde sont conservées. Une annulation ou un rejet avant le commit laisse la surcouche précédente en place. Une surcouche corrompue se replie sur la première sauvegarde valide, puis sur la seconde ; les originaux restent chargés si toutes sont invalides.

Un import remplace la précédente surcouche de ce pays avec sauvegardes. Il ne remplace pas les inventaires embarqués. Les originaux FR/CH, les cinq voisins et l’Europe Lufop restent présents.

### Raccordement au catalogue et aux écrans

Le catalogue commun `UpdatedRadarCatalog.active()` ajoute les points canoniques aux sources existantes. La déduplication type/position conserve les originaux ; un type nouveau non renseigné placé au même endroit qu’un original précis n’ajoute pas une seconde alerte. Deux types distincts, par exemple fixe et feu rouge, restent distincts.

Le bouton d’import avancé accepte maintenant un fichier SQLite compatible, en plus des ZIP ASC et CSV existants. Le pays est capturé au lancement de l’import. Le format est reconnu par sa signature, sans se fier à son extension. Les erreurs ne désactivent pas les bases embarquées.

`RadarActivationState` ne donne un reçu de chargement qu’au consommateur du catalogue. Le moteur téléphone l’écrit après avoir chargé les points ; ni le téléchargement, ni le bouton d’import n’écrivent ce reçu. Le gestionnaire affiche un import en attente jusqu’au reçu correspondant exactement à la révision actuelle. Un reçu ancien est invalidé par une modification de fichier, de visibilité ou de révision. Les points filtrés ou déjà couverts peuvent donner zéro ajout au moteur ; cet état reste distinct d’une activation réussie.

Le catalogue commun alimente également le service et les données voiture. La confirmation explicite de chargement est raccordée au téléphone dans ce lot ; les confirmations spécifiques service/voiture, les transitions de propriété des alertes et la fusion avec sens/fiabilité restent à finir.

Un fichier SQLite local est indiqué comme import local à provenance non vérifiée. Le fait de reconnaître son schéma ne le présente pas comme une archive officielle SpeedAlert téléchargée et vérifiée.

## Ce qui reste bloqué pour le 7z

**L’extraction 7z n’est pas intégrée et aucune archive 7z n’est activée automatiquement dans ce lot.** Le téléchargement reste une réception contrôlée, pas une activation. Les cinq nouveaux JAR ne sont pas ajoutés comme dépendances absentes ou fictives au projet.

L’environnement ne parvient toujours pas à joindre Maven (`Proxy CONNECT aborted due to timeout`). Les deux vraies archives FR/CH et les cinq JAR nécessaires ne sont ni dans les sources ni dans les fichiers retrouvés. Les métadonnées officielles de la release `europe-202606` ont été relues et les tailles/SHA-256 FR/CH correspondent aux valeurs épinglées ; cela ne remplace pas un téléchargement ni un essai d’extraction. Preuve : `verification/speedalert-release-metadata-11.14.243.json`.

**🔴 INTERVENTION OBLIGATOIRE — MAINTENANT POUR FINIR LE 7z :** joindre les sept fichiers listés et liés dans `FICHIERS-POUR-IMPORT-7Z.md` : `fr-camera.sqlite.7z`, `ch-camera.sqlite.7z`, `commons-compress-1.28.0.jar`, `commons-io-2.20.0.jar`, `commons-lang3-3.18.0.jar`, `commons-codec-1.19.0.jar`, `xz-1.12.jar`. Ils peuvent être réunis dans un seul ZIP. Le projet complet et les bases d’origine sont déjà disponibles.

À réception : contrôler les JAR/licences/empreintes, intégrer une extraction bornée en taille, mémoire et nombre d’entrées, inspecter les vrais types FR/CH, raccorder l’import automatique au téléchargement, puis éprouver annulation, échec et activation. Le modèle et le stockage de ce lot préparent cette étape sans la déclarer achevée.

## Vérifications réellement exécutées

| Contrôle | Résultat et portée |
|---|---|
| Java ciblé | Les 23 classes métier ciblées et les tests compilent en Java 8. Les frontières Android utilisent des adaptateurs JVM explicites, hors sources de l’APK. Ce n’est pas une compilation complète Android. |
| SQLite / canonique | 35 scénarios réussis avec 14 vraies bases SQLite synthétiques, ouvertes en lecture seule par SQLite natif Python derrière les adaptateurs JVM. Le code métier Java de l’importeur et du stockage est celui de l’application. |
| Scénarios négatifs | Signature invalide, table absente, colonnes absentes, vue, base vide/trop grande, coordonnées invalides, doublons d’ID, coordonnées textuelles, type nul/trop long et métadonnée binaire refusés ; le fichier source garde son empreinte. |
| Sauvegardes / annulation | Fichiers, écritures, synchronisations et renommages atomiques réels ; doublon, mauvais pays, annulation avant commit et pendant sauvegarde, corruption et deux niveaux de repli contrôlés. |
| Activation | Import absent du statut actif avant reçu ; reçu correspondant reconnu ; reçu périmé refusé ; visibilité et conservation des originaux vérifiées. Type inconnu co-localisé avec un original précis dédupliqué. |
| Régression téléchargements | 27 scénarios de la 242 réexécutés : HTTP simulé, erreurs, annulations et file séquentielle ; aucune extraction 7z dans ce test. |
| Régression catalogues / règles | 14 scénarios de catalogues réexécutés, plus politiques d’alertes et mouvement GPS ; les vrais assets restent la référence. |
| Syntaxe | 84 fichiers Java dans `app/src` analysés ; aucun diagnostic syntaxique. Les types Android de toute l’application ne sont pas résolus par cette vérification. |
| Android natif | `CanonicalSqliteRuntimeTest` ajouté pour API 26/36 ; non exécuté. Les tests de rechargement Android précédents restent également à exécuter. |
| Données d’origine | 59/59 CSV/ASC/JSON de la référence 240 inchangés ; vérification répétée dans le ZIP livré et CRC de toutes les entrées contrôlé au conditionnement. |

Les fixtures SQL sont des données synthétiques de test, générées hors de l’application ; elles ne sont pas ajoutées comme inventaire radar de production. Les nombreuses assertions de conservation d’identifiants ne sont pas des essais matériels.

Résultats : `verification/targeted-checks-11.14.243.log`, `targeted-results-11.14.243.json`, `data-integrity-11.14.243.json`, `release-contract-11.14.243.json`. Reproduction : `python3 tools/run_targeted_checks.py`, puis `python3 tools/update_progress_11_14_243.py` et `python3 tools/render_progress_report.py`.

## Build, versions et limites

- Gradle : 11.14.243 / 443 ; applicationId `fr.roadguard.app`, compile/target SDK 36 et minSdk 26 conservés.
- Android Auto reste configuré comme dans la source reçue (`roadguardAndroidAuto=true`). `false` isole le téléphone sans bibliothèque Car.
- **Aucun APK 243 produit, aucune suite Robolectric exécutée, aucun essai Xiaomi/autoradio réalisé.**
- La distribution Gradle et le SDK Android ne sont pas disponibles. Le blocage réseau précédent est conservé dans `gradle-phone-11.14.242.log` et l’environnement de ce lot dans `environment-11.14.243.json`. Une nouvelle tentative identique de build n’a pas été lancée, l’accès réseau restant défaillant.
- L’APK 197 dans `LIVRAISON/` reste une référence historique, pas une version compilée de ce lot.
- La caméra, le thème graphite/rouge, les maquettes et le cache cartographique restent inchangés. Les preuves caméra 242 sont conservées sans relancer un audit caméra. Aucun pack cartographique permanent ou téléchargement intégral France n’est réactivé.

## Avancement et suite

Backlog conservé : 39 tâches, 100 points et poids de modules inchangés. D6 (SQLite vers modèle canonique) est codé ; D5 (extraction 7z) et D7 (fusion directionnelle/fiabilité/rechargement complet) restent inachevés.

- Développement : **78,0000 %**, soit **+3,0000 points** contre la 242.
- Validation : **27,5000 %**, soit **+2,5000 points** : contrôle automatique D6 +2,0000, sauvegardes/repli S2 +0,5000. Aucun point Android natif, appareil ou 7z ajouté.
- Les 48,0000 % historiques restent une estimation non auditée séparée. Les contrôles hérités 241/242 restent datés et ne sont pas tous réexécutés.

Tableau complet par module et preuves : `AVANCEMENT-11.14.243.md`. Ledger affiché dans l’application : `app/src/main/assets/roadguard_progress_11_14_243.json`.

**Chantier actif :** vraie chaîne 7z → SQLite → import automatique → lecture effective par le moteur, en attente des fichiers demandés. Les essais de ce lot sont ciblés après modifications ; aucun audit global n’est relancé à chaque micro-correction.

**🔴 INTERVENTION OBLIGATOIRE — PLUS TARD :** en fin de lot 7z, compiler une seule fois dans CodeAssist avec la signature habituelle, installer en conservant les données, puis vérifier import, compteurs, retour à la carte et alertes sur Xiaomi ; Android Auto sera essayé séparément. Le besoin immédiat est la fourniture des fichiers, pas une compilation à chaque étape.
