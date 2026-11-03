# ROADGUARD 11.14.243 — projet CodeAssist complet

Reprise du projet 242, avec lecture SQLite, surcouche canonique séparée, sauvegardes et reçu de chargement par le moteur téléphone.

- Note complète : `WORK-11.14.243.md`.
- Tableau pondéré : `AVANCEMENT-11.14.243.md`.
- Fichiers demandés pour la suite : `FICHIERS-POUR-IMPORT-7Z.md`.
- Sources, ressources d’origine, maquettes, historiques, Gradle et variantes téléphone/voiture inclus.
- Version 11.14.243 / 443 ; SDK 36, minimum 26, package inchangé.

Les 35 scénarios SQLite/canonique passent avec de vrais fichiers de test et des adaptateurs JVM. Android natif, compilation complète et matériel ne sont pas validés. Aucun APK 243 n’est fourni ; l’APK 197 conservé est historique.

L’import avancé sait lire un SQLite compatible déjà extrait. La chaîne de téléchargement 7z n’effectue pas encore l’extraction ni l’activation automatique : les deux archives et les cinq bibliothèques demandées manquent pour terminer cette étape.

**🔴 INTERVENTION OBLIGATOIRE — MAINTENANT POUR LE 7z :** joindre les sept fichiers indiqués dans la liste. La compilation et l’essai Xiaomi seront regroupés à la fin du lot.
