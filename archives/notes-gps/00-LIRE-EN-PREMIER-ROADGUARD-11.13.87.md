# ROADGUARD 11.13.87 — Audit du tampon cartographique 20 km

## Modifications effectives
- Nouvelle classe Java pure `EmergencyBufferAudit` : mesure les tuiles présentes et non expirées dans le plan autour de la dernière position GPS, sans déclencher de téléchargement ni lire les fichiers entiers.
- `MainActivity.emergencyBufferStatus()` affiche les tuiles présentes / prévues et un pourcentage local de couverture du plan limité (zooms 9–11, 240 tuiles maximum).
- Nouveau test `EmergencyBufferAuditTest` : cache vide, une tuile, expiration.
- Quatre tests Java exécutés avec succès : planificateur, disque, passerelle et audit.

## Limites / vérifications indispensables
- Pourcentage = couverture du plan limité autour du dernier GPS, **pas** couverture de toutes les routes ou niveaux de zoom dans un disque de 20 km.
- Aucun fournisseur de tuiles autorisant le préchargement configuré par défaut : pas de préchargement automatique effectif à l'installation.
- Aucune compilation APK ni test sur Xiaomi : SDK Android non disponible dans cet environnement.
- GPS bleu et caméra non modifiés dans cette version, mais pas revalidés sur appareil.
- Cache limité à 128 Mio et TTL glissant de 24 heures. Vérifier la compatibilité avec les conditions de licence du fournisseur avant usage.

## Suite prioritaire
1. Configurer une source autorisant explicitement le préchargement et le stockage temporaire.
2. Construire l'APK et tester l'affichage de carte après coupure réseau sur Xiaomi.
3. Ajouter un audit de zoom plus fin et stratégie de trajet sans saturer le réseau.
