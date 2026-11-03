# ROADGUARD 11.13.48 — carte en ligne MapLibre

## Changement réel
- Le bouton « Afficher les routes avec Internet (MapLibre) » active explicitement l'aperçu MapLibre plutôt que la carte HTML historique.
- Le style de démarrage MapLibre charge les tuiles raster OpenStreetMap pour une consultation interactive avec connexion.
- Attribution © OpenStreetMap contributors intégrée au style.
- Le marqueur GPS bleu et l'importation PMTiles locale restent inchangés.

## Limites impératives
- Les tuiles `tile.openstreetmap.org` sont réservées à l'affichage interactif, PAS au préchargement massif ni à la sauvegarde hors ligne. Ne jamais réutiliser cette URL pour le moteur de téléchargement.
- La source publique peut être soumise à des limites de débit. Prévoir un fournisseur contractuel/auto-hébergé pour production.
- Aucun téléchargement hors ligne intégré n'est opérationnel ; aucun APK compilé ni essai sur Xiaomi.
- Le code actuel conserve des limitations de rendu vectoriel et de polices.
- Conserver les fichiers existants de l'utilisateur et ne pas désinstaller l'ancienne APK.

## Vérifications
- Syntaxe JavaScript `node --check` OK.
- Contrôles statiques du style et du GPS OK.
- ZIP vérifié après création.

## Suite prioritaire
1. Compiler l'APK et tester la carte en ligne avec GPS sur Xiaomi.
2. Intégrer une source autorisée de packs PMTiles régionaux et leur catalogue intégré, sans saisie d'URL.
3. Tester la conservation des cartes téléchargées et le mode avion.
