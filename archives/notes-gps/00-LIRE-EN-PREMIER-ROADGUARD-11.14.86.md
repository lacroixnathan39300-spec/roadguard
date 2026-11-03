# ROADGUARD 11.14.86 — LOT VECTORIEL, SANS COMPILATION DEMANDÉE

## Réalisé
- Carte MapLibre : correction de la superposition des voies ferrées avec les routes classiques dans la couche OpenMapTiles `transportation`.
- Autoroutes OpenMapTiles séparées avec accent rouge, routes ordinaires gris clair.
- Thèmes jour/nuit : bascule de palette à chaud quand le style vectoriel est déjà chargé, sans recharger le pack ni réinitialiser les couches GPS/itinéraires.
- VersionCode 286, versionName 11.14.86.
- Carte raster et fonctions GPS/alertes/caméra laissées inchangées.

## Limites / risques
- La source vectorielle PMTiles compatible n'est PAS fournie : il faut une archive autorisée et compatible avec les noms de couches du style.
- Certaines polices/glyphes ne sont pas encore embarqués; les noms peuvent manquer suivant la configuration.
- Le rendu final sur Xiaomi et la compilation Gradle n'ont PAS été testés.
- Les couches Protomaps et OpenMapTiles varient selon le schéma réel des tuiles; vérification sur appareil encore requise.

## Prochain lot (avant demande de compilation)
1. Vérifier la disponibilité des glyphes locaux et proposer une gestion robuste des noms de routes.
2. Ajouter un diagnostic de source PMTiles et un retour clair si le pack est absent.
3. Évaluer le rendu jour/nuit avec une source vectorielle autorisée.
4. Demander un test Xiaomi uniquement quand le lot sera suffisamment complet.

## Vérifications hors compilation
- Contrôle de syntaxe JavaScript via node --check.
- Contrôle du ZIP via zipfile.testzip().
- Contrôle des fichiers Gradle/Java/HTML présents.
