# ROADGUARD 11.13.46 — imports régionaux pour test du rendu

## Changements réellement effectués
- Import PMTiles valide hors France accepté comme `region_local.pmtiles`, sans le faire passer pour une archive France+Corse.
- Import couvrant géographiquement France+Corse conservé sous `france_local.pmtiles`.
- Activation du pack vectoriel importé dans les préférences; préservation de la sélection régionale au prochain lancement.
- Contrôle signature/en-tête et protection de restauration inchangés; GPS et caméra non modifiés.
- versionName 11.13.46, versionCode 176.

## Tests
- Quatre assertions statiques du code réussies; contrôle ZIP réussi.
- Aucune compilation Android ni essai sur Xiaomi, aucune carte France réelle téléchargée.

## Blocages réels
- Pas de serveur de tuiles/cartes autorisé configuré; pas de basemap en ligne validée; pas de moteur de téléchargement par zone indépendant du catalogue.
- Le sélecteur de fichiers est encore une fonction de diagnostic, pas l'expérience Google Maps demandée.
- Pas de Gradle/SDK Android disponible dans cet environnement (état antérieur, non réévalué exhaustivement).

## Prochaines priorités
1. Choisir une source cartographique hébergée et autorisée pour le rendu réseau et les archives régionales, avec attribution OSM.
2. Connecter l'affichage MapLibre à cette source et créer un véritable téléchargement intégré par zone, sans URL saisie par l'utilisateur.
3. Compiler APK, vérifier sur Xiaomi le GPS bleu, rendu en ligne/hors ligne, les glyphes, et l'import Florence.
4. Éviter toute suppression de données utilisateur existantes.
