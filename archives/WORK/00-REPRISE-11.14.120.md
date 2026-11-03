# Reprise ROADGUARD 11.14.120

Source de cette passe : archive ROADGUARD mise à jour, version Android 11.14.120 / code 320.

## Réalisé
- Contrôles de CSV radars : rejette NaN/infini, positions hors limites et noms vides ou excessivement longs.
- Radars expirés exclus au chargement, supprimés quand Alertes s’ouvre même sans fix GPS, et filtrés dans le service arrière-plan.
- Test statique spécifique ajouté et tableau complet de progression révisé.
- Aucun changement au GPS bleu, au graphisme des six écrans ni à la caméra.
- Préchargement désactivé; cache normal des tuiles consultées seulement.

## Progression estimée, non auditée
Radars 35→40 % (+5); global 58→59 % (+1). Les six modules d’écran sont maintenus à 100 % d’implémentation côté source, pas encore vérifiés dans l’APK.

## Vérifications et blocages
Les contrôles de passation, visuels, fonctionnels, radars locaux, cache sans préchargement, scripts de carte et tests GPS/map ciblés passent. Android SDK platform 34, Gradle et wrapper absents : pas de compilation ni de test Xiaomi. Deux anciens tests figés sur l’interface/version d’avant ne reflètent plus le code actuel (`test_online_mode_111349.py`, `test_map_viewport_11113.py`).

## Suite
Compiler via CodeAssist, valider radars mobiles/expiration et alertes premier plan/arrière-plan sur Xiaomi, puis continuer la navigation et la caméra en dernier.
