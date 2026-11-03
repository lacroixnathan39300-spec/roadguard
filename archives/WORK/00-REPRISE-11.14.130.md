# Reprise ROADGUARD 11.14.130

Version 11.14.130 / code 330. Source ZIP, sans APK.

## Réalisé
- Recherche d’adresse et coordonnées dans le panneau Carte, sans navigation vers l’écran Trajet (11.14.129).
- Catalogue radar par pays configurable; URL HTTPS, taille, SHA-256, format CSV et licence contrôlés.
- Téléchargement atomique dans le stockage privé; chargement partagé entre alertes premier plan et service GPS en arrière-plan.
- Suppression d’une base pays et mise à jour des alertes locales.
- Tests statiques ciblés : `tools/test_radar_country_downloads.py`.

## Estimation
Radars 72→76 % (+4); global 70→71 % (+1); non audités. Navigation 48 % inchangée. L’APK n’est pas compilée.

## À faire
Publier/configurer un catalogue radar de pays dont les sources permettent la redistribution. Compiler dans CodeAssist et tester recherche, téléchargements et alertes hors connexion sur Xiaomi. Caméra à la fin; aucun préchargement.
