# ROADGUARD 11.14.69 — REFONTE VISUELLE GRAPHITE / ROUGE

Base: sources complets 11.14.68. Référence visuelle: `design/reference-validee/ROADGUARD-MAQUETTE-PREMIUM-10-ECRANS.png`.

## Modifications effectivement codées
- GPS status pill
- GPS status typography
- maneuver spacing
- maneuver styling
- speed outline
- speed typography
- rail spacing
- rail shell
- rail zoom discoverability
- compact zoom in
- compact zoom out
- bottom sheet shell
- bottom handle hierarchy
- search pill
- bottom action palette
- action icon contrast
- bottom collapsed copy
- accurate local signals label
- secondary screen spacing
- collapsible panel styling
- report tiles
- report icon contrast
- radar filter tabs
- theme selector
- settings summary

## Comportement / usage
- Rail droit compact par défaut (radars, signalement, recentrage). Appui long sur Radars pour révéler les deux zooms; accessible aussi via Paramètres > Commandes de la carte.
- Recherche destination dans le tiroir inférieur, à ouvrir par la poignée; glissement pour développer/replier.
- Tous les changements sont dans le code Java natif. GPS, sources cartographiques, cache, alertes et caméra inchangés.

## Tests et limites
- Vérification statique du fichier Java et des changements, ZIP intégral et contrôle CRC.
- Pas de compilation Android / installation / test sur Xiaomi: rendu visuel à confirmer.
- Il s'agit d'une étape de convergence vers la maquette, pas de la reproduction pixel-perfect des 10 écrans.
- Aucune connexion à une communauté de signalements ni à une base radars distante.

## Prochaines tâches
1. Installer l'APK après compilation et comparer côte à côte avec la maquette sur le Xiaomi.
2. Ajuster tailles, safe areas et densités d'après captures réelles.
3. Reprendre les détails visuels des pages Trajet, Radars, Signalements et Accueil.
4. Caméra en dernier.
