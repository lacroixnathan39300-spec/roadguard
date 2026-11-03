# ROADGUARD 11.13.84 — tampon 20 km expérimental

## Réalisé
- Ajout d’un ordonnanceur de préchargement autour du GPS, rayon 20 km, zooms 9 à 11.
- Maximum 12 tentatives réseau par lot, un lot au plus par minute, sur exécuteur distinct du fil UI.
- Réutilisation du cache disque existant, augmenté à 128 Mo pour la nouvelle fonctionnalité.
- Reprise à la position courante au retour du réseau, sans recharger les tuiles déjà présentes.
- Aucun téléchargement automatique par défaut : activer **explicitement** `licensed_emergency_prefetch_allowed=true` ET renseigner `licensed_emergency_xyz_template` avec un fournisseur HTTPS XYZ autorisant juridiquement le préchargement et la conservation locale. Ces préférences n’ont pas encore d’écran de configuration.
- Le serveur public OSM est refusé comme fournisseur de préchargement.

## Limitations et vérité terrain
- Sans fournisseur licencié et sans opt-in, **aucun préchargement ne démarre**.
- 20 km = rayon de planification ; le lot limité et le zoom plafonné ne garantissent **pas** la couverture complète ni les détails de rues.
- Les données déjà en cache expirent après 24 h; la disponibilité hors réseau dépend de la couverture effectivement téléchargée.
- Pas de compilation APK ni de validation Xiaomi réalisée dans cette livraison.
- Prochaine étape : interface de configuration fournisseur + consentement, couverture progressive auditable, test réel carte hors réseau.
