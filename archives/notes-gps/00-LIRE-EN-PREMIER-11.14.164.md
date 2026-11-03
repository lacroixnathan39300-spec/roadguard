# ROADGUARD 11.14.164 — carte libre sans retour GPS

Version source à importer dans CodeAssist; aucune compilation Android n’a été lancée.

## Correctif

Le GPS continue à mettre à jour le point bleu, mais ne recentre plus la carte après que tu as déplacé la vue. Cela fonctionne jusqu’à l’appui volontaire sur ⌖.

La cause était double : MapLibre réactivait le suivi caméra sur chaque mise à jour GPS reçue de l’application, et la détection de glissement Leaflet dépendait d’un événement tactile (`originalEvent`) qui peut manquer dans WebView.

## Avancement

- Global : 94 % estimé (+1).
- Carte en ligne : 89 % (+2); deux moteurs corrigés, validation sur Xiaomi encore nécessaire.
- Navigation : 100 % côté code; compilation et essai appareil à faire.
- Radars : 99 %.
- Caméra : 40 %, reportée à la fin.

Les pourcentages décrivent l’implémentation source et ne signifient pas que cette version a été compilée ou testée sur téléphone.
