# ROADGUARD 11.13.98 — Reprise réseau / tuiles visibles

Base : 11.13.97. Modification unique : `app/src/main/assets/map.html`.

- Lors d’une transition hors ligne → en ligne, temporisation de 800 ms.
- Si des erreurs de tuiles ont été constatées, relance des tuiles visibles de la couche réseau par `redraw()`.
- Pas de préchargement de tuiles hors écran, ni changement du tampon x10 local (720/360) ou OSM public (9).
- Aucun changement de la caméra, du GPS, des alertes radar ou du code Java.

Limites : `redraw()` peut provoquer un bref rafraîchissement visuel. La reprise dépend de la détection réseau Android actuelle (notamment `pushPosition()` et `onResume()`). Les tuiles non présentes dans le cache HTTP ne sont pas garanties hors ligne. Pas de compilation APK ni de test sur Xiaomi.

Tests : vérification syntaxique JS avec Node, assertions statiques et intégrité ZIP.
Avancement global estimé : 63 % (inchangé sans recette appareil).
