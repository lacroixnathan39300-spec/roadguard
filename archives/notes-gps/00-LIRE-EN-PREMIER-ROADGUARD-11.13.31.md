# ROADGUARD 11.13.31 — contrôle de carte installée

## Correction effective
Dans `MainActivity.downloadFranceOneTap`, une archive simplement présente sur le disque ne suffit plus pour afficher « Carte France déjà présente ». Le contrôle vérifie désormais l'en-tête PMTiles v3, le type vectoriel et les bornes France + Corse. Une archive corrompue ou inadaptée ne bloque plus la recherche du catalogue.

## Conservation
Aucune suppression des cartes ou préférences. Code GPS bleu inchangé. Ancien serveur public non réactivé.

## Validation
Test statique de présence des vérifications ; aucune compilation APK ni test Xiaomi. Aucun pack France créé, téléchargé ou hébergé. Le téléchargement reste bloqué sans source réelle.

## Suite prioritaire
1. Fournir une véritable archive France + Corse sous licence compatible et un hébergement HTTPS Range.
2. Intégrer un catalogue vérifié et tester le téléchargement avec reprise.
3. Compiler APK et valider la carte en mode avion sur Xiaomi.

## Suivi
Progression globale historique 42 % (non auditée). Intervention utilisateur : aucune pour cette correction. Ne pas considérer la carte hors ligne comme fonctionnelle.
