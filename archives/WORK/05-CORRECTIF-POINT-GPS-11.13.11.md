# ROADGUARD 11.13.11 — Correctif du point GPS MapLibre

## Symptôme
Sur le Xiaomi, la barre affiche « GPS ACTIF », les tuiles PMTiles sont visibles, mais le point de position n'apparaît pas.

## Correctif source
- Les sources et couches GPS, destination et itinéraire sont recréées si nécessaire après chaque `style.load`; le callback ne quitte plus prématurément lorsqu'une source a survécu au changement de style.
- Après l'activation du style PMTiles, la position reçue est resynchronisée, la caméra est recentrée sur le dernier fix et les couches du point GPS sont déplacées au-dessus des bâtiments et des routes.
- Le marqueur et son halo sont agrandis et renforcés pour rester lisibles.
- Les données PMTiles existantes et le flux GPS Android sont conservés.

## Recette à faire sur le Xiaomi
1. Compiler et installer l'APK depuis ce projet en mise à jour de l'application existante.
2. Ouvrir Carte et sélectionner le pack France déjà présent; ne pas le télécharger à nouveau.
3. Vérifier le point cyan et son halo après le chargement des tuiles, puis déplacer la carte et toucher « Centrer ».
4. Refaire l'essai en mode avion pour les tuiles et avec le GPS actif.

## État
- Vérifié ici : archive ZIP intègre, JavaScript de la carte syntaxiquement valide.
- Non vérifié : compilation Android, rendu réel avec les 2,76 Go PMTiles, APK et test GPS sur Xiaomi. Ces essais exigent l'environnement Android et le téléphone.
