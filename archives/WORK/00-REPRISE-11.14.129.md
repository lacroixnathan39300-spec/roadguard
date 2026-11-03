# Reprise ROADGUARD 11.14.129

Version 11.14.129 / code 329. Source ZIP, sans APK.

## Réalisé
- Le champ « Où va-t-on ? » de Carte est maintenant une saisie directe.
- Recherche via le géocodeur Android; résultats dans le bottom sheet, sélection sans changer d’écran.
- Saisie directe de coordonnées prise en charge; calcul de route lancé après sélection si le GPS est connu.
- Test statique `tools/test_drive_search_inline.py`.

## Estimation
Navigation 45→48 % (+3); global 69→70 % (+1), non audités.

## À faire
Compiler dans CodeAssist et essayer sur Xiaomi avec une adresse puis des coordonnées. Dépend du géocodeur du téléphone, du réseau et du serveur OSRM. Caméra à la fin; aucun préchargement.
