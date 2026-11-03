# Reprise ROADGUARD 11.14.163

Base 11.14.162. Nouvel import ZIP POI-Warner CH sous id indépendant `poi_warner_ch`; filtre rectangulaire régional (peut inclure les pays voisins), whitelist CSV par nom, coordonnées dédupliquées, validation et sauvegarde transactionnelle. La source n’indique pas la licence/date; libellés originaux FR, donc base non vérifiée, exclue de l’affichage et des alertes car `country=CH`. Lufop reste intact.

Analyse de l’archive fournie : 287 points uniques dans le rectangle géographique autour de CH (pas une frontière nationale exacte). Tests statiques, préflight/build selon demande utilisateur (pas de compilation). Version 363.
