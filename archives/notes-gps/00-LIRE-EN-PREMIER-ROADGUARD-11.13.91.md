# ROADGUARD 11.13.91 — affichage réseau du mode cache

## Travaux réellement effectués
- `app/src/main/assets/map.html`: ajout de `setRoadguardNetworkState(online)` pour la carte Leaflet (le moteur MapLibre disposait déjà de cette fonction).
- Indication explicite lors d'une coupure : seules les tuiles déjà en cache peuvent apparaître. Une tuile chargée depuis le cache ne fait pas disparaître l'avertissement hors réseau.
- `MainActivity.java`: rafraîchissement de l'état réseau après chargement WebView et dans `onResume`, sans attendre un nouveau point GPS.
- Préchargement automatique 20 km toujours désactivé (depuis 11.13.90). Aucun nouveau téléchargement spéculatif.

## Vérifications
- Six tests Java autonomes `Emergency*Test`: PASS.
- Syntaxe du JavaScript intégré dans `map.html`: `node --check` PASS.
- Vérification statique des appels et des textes PASS.
- Archive ZIP: test d'intégrité PASS.

## Limites honnêtes
- Cache WebView dépend des réponses HTTP, du stockage Android et de l'éviction. Il ne garantit pas l'affichage hors réseau de chaque zone consultée.
- Aucune compilation APK Android ni validation sur Xiaomi : SDK/Gradle non disponibles ici.
- Le suivi réseau n'est pas un écouteur de connectivité en temps réel : actualisation au retour dans l'application et à chaque mise à jour GPS.
- GPS, radar et caméra n'ont pas été modifiés ni revalidés sur téléphone.

## Priorités
1. Tester l'APK sur Xiaomi avec carte consultée en ligne puis mode avion.
2. Confirmer la persistance des tuiles dans WebView et l'état du point GPS.
3. Mettre en place un écouteur de connectivité Android si nécessaire.
4. Valider radar et caméra après régression.
