# Reprise ROADGUARD 11.14.164

À partir de 11.14.163. Corrigé la perte de déplacement manuel de la carte.

- MapLibre: `manualCameraPaused` est distinct de `followOn`; `updateGPS` ne peut donc plus réactiver le recentrage après glissement.
- Leaflet: `dragstart` suspend toujours le suivi sans dépendre de `originalEvent`.
- Le point GPS et les couches continuent à suivre les nouvelles positions; ⌖ efface la pause et recentre.
- Test statique de régression ajouté pour les deux moteurs.
- Version Gradle 364 / 11.14.164.
- Pas de compilation Android, conformément à la consigne antérieure.
