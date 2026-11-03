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
