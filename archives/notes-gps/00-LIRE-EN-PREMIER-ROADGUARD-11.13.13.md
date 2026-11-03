# ROADGUARD 11.13.13 — Passation honnête

Source de départ : 11.13.12. Correction limitée à la gestion du bouton France.

## Réalisé
- Le bouton France cherche d'abord une archive PMTiles France + Corse vectorielle déjà installée, et la réutilise si valide.
- Si aucun catalogue vérifié ne contient de pack France, l'application ne tente plus silencieusement le serveur map.comptoir.net qui a échoué chez l'utilisateur. Elle affiche un message explicite.
- Le code du marqueur GPS 11.13.12 est conservé sans modification.

## NON réalisé / BLOQUÉ
- Aucune URL HTTPS de pack France + Corse complet, fiable, disponible et vérifié (taille + SHA-256) n'a été établie. Le téléchargement en un clic n'est donc PAS fonctionnel.
- Aucun PMTiles France de plusieurs Go n'est inclus dans ce ZIP.
- APK non compilée ; test Xiaomi et mode avion non effectués.

## Priorité Work
1. Trouver ou héberger légalement un vrai PMTiles vectoriel France + Corse et vérifier sa disponibilité, son contenu et sa somme SHA-256.
2. Ajouter une entrée catalogue France vérifiée (HTTPS, taille, SHA-256) pour le téléchargement direct, reprise HTTP Range, atomicité et validation après transfert.
3. Compiler APK ; installer en mise à jour sans effacer les données ; tester sur Xiaomi avec Wi-Fi puis mode avion : rues, villes, GPS bleu.
4. Tester téléchargements interrompus, manque d'espace, corruption, redémarrage, mise à jour, puis autres pays.
5. Ne pas déclarer la fonctionnalité terminée avant preuve de test.
