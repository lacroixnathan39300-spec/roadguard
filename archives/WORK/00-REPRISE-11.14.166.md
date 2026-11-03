# ROADGUARD 11.14.166 — radars France/Suisse embarqués

Base : 11.14.165. Android versionCode 366. Livraison de sources, aucun APK assemblé.

France : **24429 points**, dont **7325 d’inventaire** et **17104 lieux temporaires/chantiers non confirmés**. Suisse : **1777 points**, dont **1583 d’inventaire** et **194 lieux mobiles**. Comptage après dédoublonnage décrit dans SOURCES-RADARS-11.14.166.md; aucune garantie d’exhaustivité ou d’activité réelle.

Les deux CSV sont livrés dans l’APK, sans téléchargement initial. La France est chargée par le même lecteur au premier plan et dans le service GPS. Les lieux historiques sont désactivés par défaut; option dans Alertes → Bases déjà incluses. Les données CH restent exclues de l’affichage et des alertes comme dans 11.14.165.

Le déplacement manuel de la carte et son bouton de recentrage restent dans les fichiers de 11.14.165; leurs tests de régression passent.

## Vérifications

Lecteur Java réel testé sur les deux CSV. Contrôle des empreintes, catégories, provenance, 3309 identités officielles, pays, récursion OSM et dédoublonnage. Toutes les sources Java ont passé le contrôle de compilation ECJ avec les classes Android API 34 et le runtime Java disponible. Cela ne remplace pas un assemblage Gradle/dex/ressources ni l’essai sur Xiaomi.

## Intervention finale

Importer ce projet dans CodeAssist et compiler. Tester le premier démarrage sans réseau, les compteurs Alertes, le commutateur de lieux historiques, le service GPS, puis vérifier que l’exploration manuelle reste libre. Caméra à terminer en dernier.

## Documents

- SOURCES-RADARS-11.14.166.md : sources, dates, couverture et rejets.
- SUIVI-AVANCEMENT.md : tableau complet, global estimé 94 %, variations +0 pt.
- WORK/00-REPRISE-11.14.166.md : travaux, vérifications et reste à faire.
- radar_sources/2026-10-16 : copies des données et script reproductible tools/build_embedded_radars.py.

## Travaux réalisés

- Bases France/Suisse directement dans les assets APK, sans téléchargement initial.
- France : 24429 points dédoublonnés; Suisse : 1777 points dédoublonnés.
- Sources officielles françaises, Lufop, MapAtlas, OSM/SpeedCams.world et extraction Overpass suisse recoupées.
- 31 fichiers exportés identiques après tri ignorés; les 20592 enregistrements CoPilot FR sont corroborés par Lufop et n’ajoutent pas de points.
- Polygone swisstopo utilisé pour isoler les erreurs de pays; points ambigus conservés dans un audit, exclus du catalogue actif.
- Référence SITG Genève de 41 socles séparée, sans assimilation à des radars actifs.
- Lecteur immutable partagé et mis en cache; même choix de lieux historiques au premier plan et en service GPS.
- Sources, dates, identifiants, vitesses divergentes, empreintes SHA-256 et script de reconstruction inclus.
- Compilation Java contre les classes Android API 34 réussie; test du véritable lecteur des CSV et contrôles de régression exécutés.

## Blocages et reste à faire

- Aucun APK assemblé ni test Xiaomi de cette version.
- Aucune source ne garantit tous les radars ni leur présence/activité actuelle; MapAtlas est daté de 2024 et le snapshot SpeedCams.world n’a pas de date fiable.
- Les données CH restent exclues de la carte et des alertes comme en 11.14.165.
- Les positions mobiles, temporaires et chantiers sont des lieux historiques opt-in, pas des signalements en direct.
- Caméra reportée à la dernière étape.
