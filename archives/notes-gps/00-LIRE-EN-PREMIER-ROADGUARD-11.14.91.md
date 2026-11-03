# ROADGUARD 11.14.91 — Fiabilisation du changement de thème vectoriel

## Réalisé
- Réécriture de l’application du thème différé : ne modifie plus artificiellement la variable de thème pour forcer une mise à jour.
- Applique aussi les halos des noms de rues/villes après le chargement.
- Réduit le risque de perte d’état des couches GPS pendant le changement de style.
- Diagnostic : la présence des polices embarquées est désormais « inconnue » plutôt qu’affirmée sans vérification.
- Conservation de la carte raster et des autres fonctions Android.

## Limites / reste à faire
- Pas de source de tuiles vectorielles distante ou pack PMTiles compatible intégré ; pas de rendu vectoriel garanti.
- Polices MapLibre à vérifier dans le pipeline de compilation.
- Compilation et test Xiaomi NON effectués.
- Caméra à traiter en dernier.
- Ne demander de compilation qu’après un lot substantiel.
