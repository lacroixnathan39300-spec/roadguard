# Reprise ROADGUARD 11.14.142

## Retour traité
Le téléchargement radar sans clé échouait sur HTTP 502 Overpass.

## Réalisé
- Après erreur Overpass, le téléchargement essaie en série jusqu’à trois serveurs publics.
- Pas de requêtes concurrentes; contrôles, annulation et conservation de la base précédente maintenus.
- Attribution © OpenStreetMap contributors / ODbL 1.0 conservée.

## Vérifications restantes
- Importer dans CodeAssist, compiler et tester un téléchargement réel sur Xiaomi.
- Vérifier que le serveur secondaire est tenté après échec temporaire du premier. Couverture OSM contributive et non garantie.
- Caméra en dernier; aucun préchargement de carte.
