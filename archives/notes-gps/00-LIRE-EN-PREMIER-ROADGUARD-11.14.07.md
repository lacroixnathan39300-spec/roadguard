# ROADGUARD 11.14.07 — Refonte structurelle d’après la maquette validée

Base : 11.14.06. Référence : references/ROADGUARD-MAQUETTE-ORIGINALE.png.

## Réalisé
- Barre supérieure avec menu, logo/titre et sous-titre, badge bêta.
- Barre inférieure cinq rubriques Carte, Trajet, Alertes, Hors ligne, Paramètres, thème bleu nuit/cyan de la maquette.
- Panneau inférieur avec recherche, raccourcis Domicile/Travail/Favoris et cinq boutons colorés.
- Actions connectées aux rubriques existantes; recherche Stations explicitement indisponible (pas de fausses données).
- Compteur vitesse natif repositionné en haut au centre; compteur HTML doublon masqué.
- Contrôles flottants de la carte réduits.
- Version Android 11.14.07.

## Limites importantes
- Reproduction NON encore pixel-perfect : maquette statique de référence, carte réelle variable et dimensions variables.
- Domicile/Travail ouvrent les favoris existants, sans gestion distincte des adresses.
- Pas d’APK compilé ni de test sur Xiaomi. Vérifier Java/Android build et fonctionnement avant installation.
- GPS, cache, PMTiles, radars et caméra : code métier non modifié. Caméra à la fin.
- Vérifier la carte en situation, le panneau et les interactions, ainsi que l’accessibilité.

## Suite
1. Compiler et tester sur Xiaomi; corriger erreurs éventuelles.
2. Aligner précisément la disposition et les pictogrammes avec la maquette.
3. Refaire les écrans secondaires et régler lisibilité de carte sans modifier le moteur/cache.
