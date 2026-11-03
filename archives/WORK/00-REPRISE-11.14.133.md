# Reprise ROADGUARD 11.14.133

Projet Android source / code 333, sans APK.

## Fait
- Le téléchargement d’une base radar affiche sa progression et peut être annulé. Le fichier partiel est supprimé et la base installée reste en place.
- Les téléchargements simultanés sont refusés.
- Le catalogue ignore les réponses obsolètes et la fiche du paquet est comparée au catalogue avant l’installation.
- Les contrôles de taille, SHA-256, CSV et catégorie radar restent appliqués.
- Recherche de destination sur la carte conservée. Aucun préchargement.

## Estimations
Radars 78→79 %; global 71→72 %. Estimations non auditées. Six écrans à 100 % côté code, rendu Xiaomi non vérifié.

## Suite
Configurer un catalogue HTTPS avec sources redistribuables, compiler via CodeAssist, tester les téléchargements et la navigation sur Xiaomi. Caméra en dernier.
