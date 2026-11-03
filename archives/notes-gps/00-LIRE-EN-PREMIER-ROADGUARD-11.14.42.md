# ROADGUARD 11.14.42 — préchargement anticipé

Base : 11.14.41. Aucun remplacement du moteur de carte ou des tuiles affichées.

## Changements
- Planificateur `planAhead` : proximité immédiate prioritaire puis échantillons à 10, 20, 30, 40 et 50 km dans le cap GPS fiable.
- Sans cap fiable : proximité seulement, aucun corridor arbitraire.
- Déduplication des tuiles et plafond de 360 tuiles planifiées.
- Affichage d'état et audit alignés sur le plan d'anticipation.

## Limites importantes
- Préchargement **désactivé tant qu'aucune source XYZ autorisant explicitement le préchargement n'est configurée**. Ne pas prétendre qu'il fonctionne par défaut.
- Le plan n'est pas une couverture exhaustive de 50 km ; téléchargement limité à 12 tentatives par minute et 128 Mio de cache.
- Le cache auxiliaire n'est pas automatiquement connecté à la carte principale existante ; aucune amélioration de latence réelle démontrée à ce stade.
- Compilation Android/APK et essai Xiaomi non effectués.

## Contrôle
- `EmergencyTilePlanner.java` compilé avec javac et test Java basique passé.
- Vérifier sur appareil les licences fournisseur, la compatibilité du rendu et l'absence de régression.
- Caméra inchangée.
