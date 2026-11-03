# ROADGUARD 11.13.50 — Planificateur 50 km autour du GPS

## Réalisé
- Planification locale de secteurs de ~5 km autour de la position GPS, classés par proximité jusqu'à 50 km.
- Rafraîchissement limité à un changement de secteur ou 5 minutes.
- Préférence utilisateur pour activer/désactiver la préparation de zone.
- Persistance de la file dans les préférences Android, sans transfert réseau.
- GPS, caméra et logique d'affichage non modifiés hors appel au planificateur.

## Limites essentielles
- PAS de téléchargement automatique effectif, ni de travail en arrière-plan une fois l'app fermée.
- PAS de fournisseur de tuiles autorisant l'aspiration hors ligne configuré.
- PAS d'APK compilée ni de test Xiaomi.
- La grille est une approximation géographique, pas un découpage exact de 5 km à toutes les latitudes.
- Prochaine étape : connecter un service cartographique avec droit de cache hors ligne, implémenter un Worker Android avec limites de batterie/réseau, persistance, quotas, et affichage MapLibre des tuiles locales.
