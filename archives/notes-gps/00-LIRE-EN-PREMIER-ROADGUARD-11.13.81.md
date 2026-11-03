# ROADGUARD 11.13.81 — passation

## Objectif actuel
Carte connectée, cache temporaire pour continuer quelques kilomètres après une coupure réseau. Pas de bibliothèque permanente par pays. Rayon cible 50 km, NON réalisé.

## Réalisé et testé
- `EmergencyTileGateway.java`: passerelle à la demande entre source HTTPS PNG explicitement configurée et `EmergencyTileDiskCache`.
- `MainActivity.java`: interception de `https://roadguard.local/emergency/{z}/{x}/{y}.png`, cache 64 Mio, expiration 24 h; réponses cache hors réseau.
- Ne déclenche jamais de téléchargement si aucune source autorisée n'est configurée.
- Tests `EmergencyTileGatewayTest` et `EmergencyTileDiskCacheTest` : réussis avec javac/java.

## LIMITES / BLOQUÉ
- La carte MapLibre pointe toujours vers les tuiles OSM publiques; elle n'utilise pas encore l'URL /emergency/. Donc aucune amélioration visible garantie sur Xiaomi.
- Il manque une source HTTPS de tuiles autorisant explicitement stockage et préchargement; aucune source par défaut n'a été inventée. Ne jamais précharger massivement tile.openstreetmap.org.
- Pas de téléchargement 50 km ni de couverture multi-zoom garantie. Le cache fonctionne seulement pour des tuiles demandées via la passerelle.
- APK non compilé, test Android et GPS/caméra non effectués.
- Attention: `licensed_emergency_xyz_template` est une préférence interne, pas encore une interface de configuration.
- Le cache existant rafraîchit la date de dernier accès; la durée de vie est glissante.

## Prochaines tâches prioritaires
1. Choisir une source cartographique licenciée avec droit de mise en cache / préchargement, ou héberger soi-même des tuiles.
2. Exposer une configuration sécurisée et activer dans MapLibre le point d'entrée `/emergency/` seulement si une source valable est configurée.
3. Raccorder un préchargeur limité, conscient du réseau et de la batterie, puis valider la couverture réelle; réduire les zooms ou le rayon si la taille est excessive.
4. Compiler un APK, tester GPS bleu, caméra, carte en ligne, coupure réseau et retour du réseau sur Xiaomi.
5. Auditer les anciens modules PMTiles non prioritaires et les pourcentages hérités.

## Estimations (non vérifiées sur appareil)
GPS 100% historique non revalidé; carte en ligne 85% historique; bascule réseau 50%; planificateur 45%; cache disque 50%; passerelle à la demande 45%; préchargement réel 0%; tests APK Xiaomi 0%. Avancement global non calculé.
