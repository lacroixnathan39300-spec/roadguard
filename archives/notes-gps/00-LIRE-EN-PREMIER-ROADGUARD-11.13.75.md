# ROADGUARD 11.13.75 — Changement de périmètre, cache de secours

## Demande actuelle
- Carte connectée avec préchargement *temporaire* visé sur 50 km autour du point GPS bleu.
- Lors d'une coupure, garder la carte déjà chargée quelques kilomètres, puis reprendre en ligne.
- Abandon de la bibliothèque permanente et des téléchargements PMTiles par pays.

## Réalisé dans cette version
- WebView bascule automatiquement vers `LOAD_CACHE_ELSE_NETWORK` en absence de réseau, et revient à `LOAD_DEFAULT` lorsque le réseau revient, à chaque position GPS acceptée.
- Mode de cache initialisé avant l'ouverture de la carte.
- Déclenchement GPS des anciens téléchargements permanents désactivé. Les anciens fichiers restent dans le ZIP à des fins de compatibilité, mais ne sont plus une priorité.
- GPS bleu et caméra non modifiés.

## Limites et blocages réels
- **Le préchargement de 50 km n'est PAS implémenté ni validé** : le cache WebView n'enregistre que les ressources effectivement demandées et peut les expulser. Il ne garantit ni la couverture, ni la persistance pendant une coupure.
- La source de carte en ligne utilise `tile.openstreetmap.org` ; sa politique interdit le téléchargement systématique massif et le préchargement hors ligne. Il faut sélectionner une source autorisant explicitement le préchargement avant de coder le tampon 50 km.
- Aucune APK compilée, aucun test Xiaomi ni essai réel sans réseau.
- La détection de réseau n'est rafraîchie que sur les mises à jour GPS; sans GPS, pas de bascule immédiate.

## Prochaines priorités
1. Choisir/configurer une source de tuiles autorisant explicitement le cache et le préchargement de 50 km (avec quota et coûts acceptables).
2. Implémenter un cache temporaire borné, indexé par tuiles, avec stratégie de préchargement progressive et priorité aux routes proches.
3. Gérer les bascules réseau via callback Android et les requêtes MapLibre via un serveur local de tuiles/cache.
4. Compiler une APK et tester sur Xiaomi en mode avion.

## Avancement
Ancien 42 % : **obsolète**, périmètre changé. Pas de nouveau pourcentage global fiable avant audit et essai Android. Les estimations historiques par fonctionnalité ne doivent pas être interprétées comme une validation.
