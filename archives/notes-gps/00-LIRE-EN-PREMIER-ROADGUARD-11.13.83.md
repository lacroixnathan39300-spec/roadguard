# ROADGUARD 11.13.83 — garde-fous fournisseur de tuiles

## Réalisé
- Validation de l’URL XYZ HTTPS avant d’activer la source MapLibre.
- Rejet explicite du serveur public OpenStreetMap et de ses sous-domaines comme fournisseur de cache de secours.
- Vérification des coordonnées XYZ avant lecture/téléchargement.
- Tests JVM de validation du fournisseur et des coordonnées.

## Limitations
- L’URL d’un fournisseur valide ne prouve pas les droits de préchargement : autorisation contractuelle nécessaire.
- Aucun préchargement automatique de 50 km n’a été activé; pas de licence fournisseur configurée.
- Pas de compilation APK ni de validation sur Xiaomi. Aucun pourcentage global fiable.

## Prochaines tâches
1. Obtenir une source raster XYZ HTTPS autorisant explicitement la mise en cache et le préchargement.
2. Ajouter une configuration sécurisée, un budget de tuiles et une file de préchargement progressive.
3. Intégrer le préchargement aux mises à jour GPS, mesurer la couverture effective.
4. Compiler l’APK et tester en ligne/hors réseau sur Xiaomi.
