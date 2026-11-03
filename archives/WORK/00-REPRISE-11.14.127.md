# Reprise ROADGUARD 11.14.127

Version 11.14.127 / code 327. Source ZIP, sans APK.

## Réalisé
- `DangerTypePolicy` partagé par `MainActivity` et `BackgroundAlertService`.
- Une même logique reconnaît les types radar, filtre fixe/mobile et valide les libellés.
- Les types personnalisés manquants/invalides sont écartés en arrière-plan, pas transformés en radar fixe.
- Tests statiques : `tools/test_danger_type_policy.py`, classification mobile et stockage radar.

## Estimation
Radars 71→72 % (+1); global 68→69 % (+1), non audités.

## À faire
Compiler dans CodeAssist et tester les points fixes/mobiles sur Xiaomi, au premier plan et écran verrouillé. APK non compilée ici. Caméra à la fin; aucun préchargement.
