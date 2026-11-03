# ROADGUARD 11.14.112 — cockpit navigation revu

- Version source : 11.14.112 / versionCode 312.
- Cette reprise répond aux nouvelles captures : barre de destination visible même en panneau compact; raccourcis domicile/travail/favoris à l’ouverture; rail à droite pour orientation, signalement rouge et recentrage; vitesse en pastille à gauche.
- La route de navigation est rouge ROADGUARD dans les moteurs Leaflet, MapLibre et hors ligne; le point GPS bleu est conservé.
- Le bandeau replié s’ouvre/ferme en deux états. Zooms présents en commandes avancées. La logique réseau/GPS, la source des tuiles et les fonctions métier restent inchangées.
- Contrôles : 25 contrôles statiques, syntaxe Node des trois cartes, 2 suites Python (dont 9 contrôles GPS/carte).
- Progression estimée du code : global 50→51 (+1), interface 71→74 (+3), architecture visuelle 41→44 (+3). Ces chiffres ne sont pas un test de rendu.
- Aucun SDK Android/Gradle disponible pour compiler ici. L’APK 11.14.112 n’est pas fournie : le projet est préparé pour la prochaine compilation utilisateur, puis retour visuel Xiaomi.

## Modules

| Module | Avancement |
|---|---:|
| GPS bleu | 100 % |
| Carte en ligne MapLibre | 85 % |
| Planification GPS 50 km | 71 % |
| Affichage pendant coupure | 30 % |
| APK et essais Xiaomi | 10 % |
| Navigation | 35 % |
| Radars | 30 % |
| Caméra | 40 % |
| Interface | 74 % |
| Architecture visuelle premium | 44 % |
| Cache temporaire automatique GPS | 0 % |
| **Global** | **51 % estimé** |

## Écrans

| Écran | Avancement | Variation 11.14.111→112 |
|---|---:|---:|
| Navigation jour/nuit | 52 % | +4 |
| Accueil | 43 % | +0 |
| Radars et alertes | 48 % | +0 |
| Signalements | 52 % | +0 |
| Itinéraires | 49 % | +1 |
| Paramètres et apparence | 50 % | +0 |
