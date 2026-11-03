# Reprise ROADGUARD 11.14.131

Version 11.14.131 / code 331. Source ZIP, sans APK.

## Réalisé
- Outil pour convertir des fichiers radar source selon une correspondance de colonnes et créer le format CSV accepté par l’app.
- Le catalogue est généré avec URL HTTPS, pays, licence, attribution, taille et SHA-256.
- L’outil ignore les lignes non radar et coordonnées hors limites; tests exécutés sur données entièrement synthétiques.
- Téléchargement sécurisé et intégration aux alertes carte/arrière-plan de 11.14.130 conservés.
- Recherche de destination inline Carte conservée.

## Estimation
Radars 76→77 % (+1); global inchangé 71 %; non audités. Aucun fournisseur réel configuré. APK pas compilée.

## À faire
Trouver des sources autorisées, compléter un manifeste, générer et publier le catalogue sur HTTPS. Compiler dans CodeAssist et tester sur Xiaomi. Caméra à la fin; aucun préchargement.
