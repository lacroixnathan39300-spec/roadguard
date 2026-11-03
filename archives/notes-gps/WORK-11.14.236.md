# ROADGUARD 11.14.236 — Téléchargement groupé
- « Tout mettre à jour » lance désormais une file séquentielle pour les 7 pays avec sources préconfigurées (FR, CH, DE, BE, ES, IT, LU).
- Une seule connexion réseau à la fois ; progression par pays et bilan succès/échecs.
- Échec d'un pays : tentative du suivant ; conservation des bases embarquées et des archives précédemment valides.
- Le téléchargement ne signifie PAS activation : la décompression 7z et l'import SQLite restent à développer.
- Aucun lien n'a été validé en HTTP depuis ce conteneur et aucun test Android complet n'a été exécuté.
- Modification ciblée, sans grosse vérification horaire anticipée.
