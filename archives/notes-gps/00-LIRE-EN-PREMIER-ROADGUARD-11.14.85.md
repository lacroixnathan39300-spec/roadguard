# ROADGUARD 11.14.85 — sélection des packs vectoriels

## Réalisé
- Deux commandes Nuit / Jour sur chaque PMTiles vectoriel installé, depuis le menu des cartes locales.
- Le thème choisi est sauvegardé et transmis à MapLibre avant l’activation du style vectoriel.
- Conservation de la carte raster actuelle tant qu’aucun pack vectoriel compatible n’est sélectionné.
- Aucune modification du GPS, de la vitesse, des alertes, du cache ni de la caméra.

## Limitations
- Aucune source vectorielle sous licence n’est incluse. Il faut importer un PMTiles vectoriel compatible OpenMapTiles.
- Les deux fichiers JSON de palettes ne constituent pas à eux seuls une carte vectorielle.
- Aucun APK compilé ou test Xiaomi effectué dans cet environnement.

## Tests à effectuer sur Xiaomi
1. Importer le projet dans CodeAssist et compiler.
2. Vérifier que la carte actuelle fonctionne sans PMTiles vectoriel.
3. Avec un PMTiles vectoriel compatible installé, sélectionner Premium Nuit, puis Premium Jour.
4. Vérifier la lisibilité des routes, du GPS et des labels, puis le retour à la carte en ligne.

## Prochaines tâches
- Valider le schéma réel des couches vectorielles avec une source licenciée.
- Ajouter un fournisseur vectoriel en ligne autorisé avec cache temporaire conforme.
- Vérifier le suivi GPS et les changements de style sur Xiaomi.
- Reprendre la fidélité aux dix maquettes; caméra en dernier.
