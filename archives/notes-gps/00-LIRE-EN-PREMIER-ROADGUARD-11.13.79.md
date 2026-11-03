# ROADGUARD 11.13.79 — Couverture vérifiable du tampon 50 km

## Travaux réels
- Le planificateur élimine les doublons de tuiles dus au bouclage des longitudes (notamment aux faibles zooms).
- Ajout de `EmergencyTilePlanner.coverage()` : indique le zoom minimal/maximal réellement planifié, le nombre de tuiles et si le zoom maximal demandé est atteint.
- Test ajouté pour les zooms 0–3, la traversée de la ligne de changement de date et le signalement d'une couverture incomplète.

## État / blocages
- La source actuelle `tile.openstreetmap.org` n'autorise pas le préchargement massif. Aucun téléchargement anticipé de 50 km n'a été activé.
- Pas de cache 50 km fonctionnel démontré, ni d'APK compilée, ni de test Xiaomi.
- GPS bleu, caméra et carte existante inchangés.
- Pas de bibliothèque permanente hors ligne, conformément au nouveau cahier des charges.

## Priorités suivantes
1. Choisir/configurer une source avec licence autorisant explicitement le préchargement de 50 km.
2. Brancher le téléchargement progressif et un cache temporaire borné (quotas, Wi-Fi/données, réseau interrompu).
3. Tester le parcours carte en ligne → coupure → retour réseau sur APK Xiaomi.

## Pourcentages estimés
GPS 100 %, carte en ligne 85 %, détection/bascule 50 %, planification 45 % (+5), préchargement réel 0 %, affichage coupure 25 %, reprise 35 %, APK Xiaomi 0 %. Global non recalculé.
