# ROADGUARD — CAHIER DES CHARGES ACTIF (11.14.99)

## Décision utilisateur : suppression du téléchargement France et des cartes permanentes
- Plus de téléchargement de la France entière ni de pays/régions.
- Plus de bibliothèque de cartes permanentes dans le périmètre fonctionnel.
- Les anciens points de suivi France, bibliothèque PMTiles et téléchargement manuel sont **ANNULÉS**, pas terminés.
- Carte en ligne en priorité; cache temporaire automatique autour du GPS (~50 km) lors d'une perte de réseau, seulement via un fournisseur autorisant ce préchargement.
- Aucune donnée déjà installée sur le téléphone n'est effacée par cette mise à jour.
- L'ancien code de téléchargement est neutralisé aux points d'entrée mais reste physiquement présent pour ne pas casser les dépendances. Un nettoyage ultérieur requiert une compilation et des tests de non-régression.

## Priorité exclusive actuelle
Architecture visuelle : lisibilité carte, panneaux, boutons, proportions, jour/nuit, écrans Xiaomi. Caméra en dernier.

## Suivi
Avancement global historique 42 % : NON RE-CALCULÉ depuis le changement de périmètre.
Progression de l'architecture visuelle : NON AUDITÉE. Aucun gain validé par test Xiaomi.
Le module cache 50 km n'est pas entièrement opérationnel; source licenciée et validation en conditions réelles manquantes.

## Tests
Pas de compilation Android ni test Xiaomi pour cette version; archive vérifiée uniquement.
