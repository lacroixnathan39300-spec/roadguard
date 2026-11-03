# ROADGUARD 11.13.32 — Contrôle d'installation France

## Changement réel
Dans `app/src/main/java/fr/roadguard/app/MainActivity.java`, `downloadPmtilesPack()` vérifie désormais que toute entrée de catalogue déclarée `country=france`, `scope=country` est une archive PMTiles vectorielle et que ses bornes incluent la France métropolitaine et la Corse. Ce contrôle est effectué **avant** le remplacement d'une carte installée. Il s'applique aussi aux sources publiques, comme auparavant.

## Limites et blocages
- Aucun pack France + Corse n'a été produit ni hébergé.
- L'URL historique `map.comptoir.net` reste obsolète, ne pas la présenter comme une source fonctionnelle.
- Pas de compilation Android ni de test physique Xiaomi lors de cette étape.
- La couverture des bornes n'est pas une preuve que toutes les tuiles routières sont présentes.
- GPS bleu inchangé. Pas de suppression des cartes existantes.

## Priorités
1. Produire un pack France+Corse légal et complet depuis un jeu de données approprié.
2. Héberger avec HTTPS, support Range, taille et SHA-256 publiés dans un catalogue JSON.
3. Implémenter la reprise fiable des téléchargements interrompus.
4. Compiler et tester en mode avion sur Xiaomi, sans toucher au GPS.

## Suivi
42 % est une estimation historique non auditée, pas une mesure certifiée. Pastille rouge uniquement si une intervention utilisateur devient indispensable.
