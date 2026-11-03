# ROADGUARD 11.14.64 — Paramètres et apparence premium

Base: 11.14.63.

## Implémenté dans MainActivity.java
- Résumé de configuration courant en haut des écrans Paramètres et Apparence.
- Deux sélecteurs de thème jour/nuit avec sélection visible, descriptions, accessibilité et persistance SharedPreferences.
- Suppression de la commande jour/nuit redondante.
- Conservation des autres réglages, du GPS, des radars, du cache et de la caméra.

## Validation
- Vérification statique des sources et de l'archive ZIP uniquement.
- Compilation Android et test Xiaomi non réalisés.
- Progression estimée Paramètres/Apparence : 47 % → 50 % (code implémenté, pas validé sur appareil).

## Priorités
- Compiler puis contrôler la sélection des thèmes et les écrans sur Xiaomi.
- Continuer l'alignement de la carte et des alertes sur la maquette validée.
