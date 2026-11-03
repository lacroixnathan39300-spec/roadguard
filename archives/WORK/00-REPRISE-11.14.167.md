# ROADGUARD 11.14.167 — radars Suisse activés

Base : 11.14.166. Android versionCode 367. Projet source à importer dans CodeAssist; aucun APK assemblé.

## Résultat

- Les fichiers France et Suisse sont chargés sans téléchargement initial pour la carte et les alertes GPS.
- Réglages par défaut : **7325 points FR + 1777 points CH = 9102 points**, avant rapprochement des éventuels points personnels/importés.
- Suisse : **1583 points d’inventaire + 194 lieux mobiles signalés**. Leur présence actuelle reste non confirmée. Le commutateur « Lieux mobiles signalés · Suisse » est indépendant de celui de la France.
- Le même sélecteur est utilisé au premier plan et dans le service GPS. Les bases suisses installées MapAtlas, OSM et Lufop ne sont plus exclues par leur pays.
- L’ancien import POI-Warner aux libellés FR et au territoire ambigu reste isolé; les 41 socles SITG restent une référence d’infrastructure.
- Compteurs par pays, textes des imports et actualisation Overpass CH harmonisés. Les réglages globaux des sons, voix, marqueurs et du service GPS sont conservés.

## Compilation et recette

Importer ce ZIP dans CodeAssist, ouvrir le projet Android à sa racine puis compiler. Tester sur Xiaomi le premier démarrage sans réseau, les compteurs chargés, les filtres et les bips/voix. Pour l’écran verrouillé, activer le service dans Alertes et accorder les autorisations GPS Android demandées.

Avec les réglages par défaut, CH affiche 1777 points chargés. Désactiver les lieux mobiles CH doit donner 1583, soit un total FR/CH de 8908. Réactiver redonne 1777 / 9102. Activer aussi les lieux temporaires français porte le catalogue chargé à 26206 points, en l’absence de doublons personnels/importés.

La carte doit rester libre après un déplacement manuel; ⌖ reprend le suivi. Caméra à terminer en dernier.

## Validation et limites

Les tests et la compilation Java contre les classes API 34 sont détaillés dans WORK/VALIDATION-11.14.167.md. Aucun APK/dex/signature ni essai Xiaomi de cette version. Les pourcentages restent des estimations historiques non auditées, global 94 %, +0 pt.

L’OFROU indique que les avertisseurs de contrôles routiers sont interdits dans un véhicule en Suisse : https://www.astra.admin.ch/fr/appareils-gps. L’activation technique ne constitue pas une autorisation d’usage.

Détail des sources et dates : SOURCES-RADARS-11.14.167.md. Les coordonnées et comptes des CSV restent ceux de 11.14.166; seule leur utilisation dans l’application change.

## Travaux réalisés

- Lecture des deux fichiers embarqués FR/CH et sélection commune à l’activité et au service GPS.
- 1777 points suisses activés par défaut, dont 194 lieux mobiles signalés et non confirmés; réglage CH indépendant de la France.
- Suppression des exclusions globales CH pour les bases installées MapAtlas, OpenStreetMap, Lufop et catalogues vérifiés.
- Quarantaine conservée uniquement pour l’ancien paquet poi_warner_ch aux libellés FR et au pays ambigu.
- Affichage des compteurs chargés par pays, textes des téléchargements/imports et détail des catégories corrigés.
- Serveur Overpass Suisse ajouté en premier pour actualiser les points CH.
- Contrôles Java du sélecteur et des données; compilation Java complète et tests de régression à consigner dans la note WORK.

## Reste à faire et blocages

- Aucun APK assemblé ni essai Xiaomi pour cette version.
- Les lieux mobiles sont des emplacements signalés, jamais une confirmation de présence actuelle.
- Les bases publiques peuvent être anciennes, incomplètes ou contenir des doublons résiduels.
- En Suisse, les avertisseurs de contrôles routiers sont interdits dans un véhicule selon l’OFROU; activation technique ne signifie pas autorisation d’usage.
- Caméra reportée à la fin du projet.

## Correctif de préparation Gradle

La fermeture digest était déclarée dans la boucle assets.each, puis appelée en dehors pour vérifier Brotli. Elle est désormais définie dans la portée commune doLast. Les fichiers MapLibre/Brotli/glyphes et licences sont inclus; vérification du corps de tâche exécutée sans assemblage APK.
