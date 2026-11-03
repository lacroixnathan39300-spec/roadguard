# ROADGUARD 11.13.63 — audit de source cartographique

## Fait et vérifié
- Confirmé dans `app/src/main/assets/map_maplibre.html` : l'affichage en ligne utilise `https://tile.openstreetmap.org/{z}/{x}/{y}.png`.
- Ajouté `tools/audit_offline_tile_policy.py` pour signaler une connexion directe de cette source à un mécanisme de téléchargement permanent.
- Exécuté l'audit statique avec succès; version du projet passée à 11.13.63.
- Aucun changement du GPS bleu, de la caméra, de la carte en ligne, du moteur PMTiles ni du téléchargeur.

## Ce qui ne fonctionne PAS encore
- Aucune source de tuiles autorisant explicitement un téléchargement automatique cumulatif sur 50 km n'est configurée.
- Le cache WebView/MapLibre existant ne constitue pas une bibliothèque hors ligne permanente.
- Le moteur de synchronisation de packs est encore dépourvu de catalogue de cartes réel.
- Aucune nouvelle APK compilée ni testée sur Xiaomi.

## Prochaine tâche prioritaire
1. Configurer un serveur / une source de tuiles avec droits de préchargement et conservation permanente.
2. Relier les tuiles téléchargées à un stockage indexé local et au rendu MapLibre, sans casser le GPS.
3. Tester sur une APK réelle : réseau puis mode avion puis redémarrage.

## Avancement
Global historique estimé : 42 %, inchangé. Source cartographique compatible avec bibliothèque permanente : BLOQUÉ (pas de fournisseur/serveur opérationnel).

## Reprise
Reprendre depuis ce ZIP, lire `SUIVI-AVANCEMENT.md`, `TACHES-RESTANTES.md` et le présent fichier. Ne jamais considérer un cache HTTP classique comme stockage permanent. Ne jamais télécharger en masse depuis `tile.openstreetmap.org`.
