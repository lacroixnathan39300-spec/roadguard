# ROADGUARD 11.14.81 — Cockpit compact selon la maquette de référence

Base : ROADGUARD 11.14.80 validée au lancement sur Xiaomi.

## Réalisé
- En-tête moins haut et bouton accueil compact.
- Cadran de vitesse de 108 à 96 dp, texte adapté et bordure circulaire.
- Rail de commandes ramené à 43–45 dp avec coins arrondis plus proches du cercle.
- Navigation inférieure plus basse et coins affinés.
- Correction du libellé de vitesse : reste « km/h » même si le GPS fournit un cap.
- Version Android 11.14.81 (code 281).

## Préservé
GPS, moteur de carte, cache, itinéraires, alertes, signalements, caméra et écrans secondaires. Aucune limitation de vitesse n'est inventée. Le panneau de guidage ne s'affiche que si un trajet existe.

## Vérifications et limites
Vérification statique du code et de l'archive ZIP. Compilation APK Android et test Xiaomi NON effectués dans cet environnement. Importer le ZIP comme nouveau projet dans CodeAssist, compiler, puis vérifier visuellement.

## Priorités suivantes
Comparer une capture réelle à design/reference-validee/ROADGUARD-MAQUETTE-PREMIUM-10-ECRANS.png ; harmoniser le guidage réel et les pictogrammes, puis les écrans secondaires. Caméra en dernier.
