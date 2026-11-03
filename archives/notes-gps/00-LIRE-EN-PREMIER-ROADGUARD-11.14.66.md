# ROADGUARD 11.14.66 — cockpit adaptatif

Base : 11.14.65.

## Modifications réelles
- Position du compteur GPS calculée à partir de la hauteur mesurée du panneau inférieur (supprime la translation fixe).
- Alerte radar/danger repositionnée dynamiquement au-dessus du compteur, avec limite pour écrans courts.
- Correction de la détection tactile des glissements verticaux sur la poignée du panneau ; tap pour cycler entre 3 états.
- Réduction des recalculs inutiles du panneau à chaque relevé GPS ; fermeture automatique uniquement en mode conduite et si l'utilisateur n'a pas forcé le panneau.
- Carte, calcul d'itinéraire, GPS, alertes, cache, caméra conservés.

## Validation
- Vérification statique du code source et archive ZIP.
- Compilation Android et tests Xiaomi non effectués ; ne pas déclarer l'APK validé.

## Suite
- Tester le positionnement réel sur petit et grand écran, puis la visibilité des routes et des libellés cartographiques.
- Ne pas travailler sur la caméra avant la fin du projet.
