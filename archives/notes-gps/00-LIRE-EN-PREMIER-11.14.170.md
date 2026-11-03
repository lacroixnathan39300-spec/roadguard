# ROADGUARD 11.14.170 — caméra des trois cartes

Base 11.14.169, versionCode 370. Code source Android pour CodeAssist; aucun APK 11.14.170 assemblé ici.

Les commandes de zoom de l’application suspendent désormais le suivi GPS dans Leaflet, MapLibre et la carte locale Canvas. Les gestes de zoom sur MapLibre et la carte locale le suspendent également. La carte locale ne se recentre plus après un déplacement manuel quand un nouveau point GPS ou des données cartographiques arrivent; ⌖ reprend à la dernière position. Son cadrage tient compte du panneau de trajet et elle dispose d’un aperçu d’itinéraire. Les améliorations de MapLibre en 11.14.169 (panneau et changement de style) sont conservées.

Les radars France/Suisse, leurs réglages, les pictogrammes, la navigation et le fonctionnement du cache courant ne changent pas. Les 194 lieux mobiles CH par défaut signalent des emplacements historiques, pas une présence actuelle.

Les contrôles sont décrits dans WORK/VALIDATION-11.14.170.md. Le retour utilisateur indique que la version précédente fonctionne, mais n’est pas une validation du nouveau code sur appareil.

Le chantier actif est l’essai du suivi caméra réel sur Xiaomi. Les estimations historiques passent à 95 % global (+1), caméra 70 % (+30), MapLibre 90 % (+1); il reste la compilation APK et la mesure du comportement en conduite.

**INTERVENTION OBLIGATOIRE — À LA FIN DU TRAVAIL SOURCE :** importer le ZIP 11.14.170 dans CodeAssist et compiler. Sur Xiaomi, déplacer puis zoomer dans chaque carte, constater que le suivi reste en pause, toucher ⌖, ouvrir le panneau de trajet et vérifier que le point bleu reste visible. Tester l’aperçu d’itinéraire et la fluidité pendant un déplacement réel.
