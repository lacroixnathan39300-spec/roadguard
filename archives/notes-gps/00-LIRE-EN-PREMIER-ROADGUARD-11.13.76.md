# ROADGUARD 11.13.76 — Cache de secours strict et diagnostic reseau

## Effectue
- En absence de connexion, la WebView passe a `LOAD_CACHE_ONLY` et ne tente plus de charger les ressources depuis le reseau (precedemment `LOAD_CACHE_ELSE_NETWORK`).
- Ajout de `setRoadguardNetworkState` dans la carte MapLibre et message visible explicitant que seules les tuiles DEJA presentes peuvent apparaitre.
- Le diagnostic JavaScript expose `networkOnline`.
- Le GPS bleu, la camera et les autres fonctions ne sont pas modifies.

## Important / limites
- Aucun prechargement reel de 50 km : source `tile.openstreetmap.org` inappropriee au telechargement massif.
- Cache WebView non garanti et non controle au niveau des tuiles ; les tuiles absentes restent absentes hors connexion.
- Aucune APK compilee ou testee sur Xiaomi.
- Pas de carte hors ligne permanente, conformement au nouveau besoin.

## Suite prioritaire
1. Choisir une source cartographique autorisant le prechargement temporaire massif.
2. Implementer un cache explicite avec budget et politique d'expiration, puis precharger progressivement le disque de 50 km autour du GPS.
3. Tester coupure/reprise du reseau sur APK Xiaomi et mesurer couverture effective.

## Avancement estime (non valide sur appareil)
GPS 100 % ; carte en ligne 85 % ; detection/bascule reseau 50 % (+5) ; prechargement 50 km 10 % ; affichage en coupure 25 % (+5) ; reprise 35 % ; tests APK Xiaomi 0 %. Global : non recalcule (ancien 42 % obsolet).
