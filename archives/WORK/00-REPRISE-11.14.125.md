# Reprise ROADGUARD 11.14.125

Version 11.14.125 / code 325. Source ZIP, sans APK.

## Réalisé
- Harmonisation du seuil GPS des alertes premier plan/arrière-plan : précision disponible et ≤60 m.
- Les fixes sans précision ne déclenchent plus le bandeau/son/voix radar au premier plan.
- Milestones persistants communs, évitant les annonces en double, conservés.
- Test statique `tools/test_radar_alert_accuracy.py`.

## Estimation
Radars 68→69 % (+1); global 66→67 % (+1), non audités.

## À faire
Compiler avec CodeAssist et vérifier sur Xiaomi en déplacement puis écran verrouillé. La compilation locale reste bloquée par l’absence du SDK/Gradle. Caméra à la fin; aucun préchargement.
