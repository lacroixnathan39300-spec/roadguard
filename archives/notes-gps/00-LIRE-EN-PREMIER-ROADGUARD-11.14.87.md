# ROADGUARD 11.14.87 — Diagnostic vectoriel, sans compilation demandée

## Modifications réalisées
- Ajout aux diagnostics WebView du thème actif, des événements de chargement de la source vectorielle et du compteur d’erreurs de tuiles.
- Réinitialisation des compteurs à chaque sélection d’un pack vectoriel.
- Messages d’erreur distinguant la carte vectorielle et le mode raster.
- Numéro de version Android actualisé : 11.14.87 / code 287.
- GPS, alertes, caméra et cache raster inchangés.

## Vérifications et limites
- Syntaxe JavaScript et présence des glyphes français locaux à contrôler statiquement.
- Une source PMTiles vectorielle compatible et légalement utilisable reste indispensable pour voir les couches premium.
- Compteur `vectorTilesSeen` = événements de fin de chargement de source, pas nombre exact de tuiles.
- Compilation Android et test Xiaomi non réalisés : ne pas demander de compilation à ce stade.

## Prochaine étape
- Valider la compatibilité du schéma de couches avec un jeu de données PMTiles autorisé et intégrer un diagnostic de disponibilité du pack côté Android.
