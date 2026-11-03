# ROADGUARD 11.13.21 — passation

## Travail effectivement réalisé
- Tableau de bord : barre globale et reste à faire, statut intervention rouge seulement si intervention_obligatoire=true.
- Suivi JSON actualisé et libellés trompeurs du téléchargement France corrigés.
- GPS non modifié.

## État réel
- 42 % : estimation prudente et non auditée. L’ancien 65 % n’était pas étayé par une recette.
- Aucun pack France+Corse vérifié, pas de téléchargement opérationnel, pas d’APK compilée ou testée.
- Aucune intervention utilisateur obligatoire pour ces changements source.

## Priorités
1. Obtenir légalement une archive France+Corse complète et la vérifier (format, couches, couverture, SHA256).
2. Héberger un catalogue HTTPS et l’archive, puis vérifier téléchargement avec reprise et intégrité.
3. Valider le rendu PMTiles dans WebView MapLibre hors réseau.
4. Compiler l’APK et tester sur Xiaomi en mode avion, GPS compris.
5. Auditer l’ensemble des autres fonctions du cahier des charges.

## Tests
- Vérification statique des sources et validité JSON seulement. Aucune compilation ni essai réel.
