# ROADGUARD 11.14.276 — inscription administrateur simplifiée

Le serveur communautaire version 12 a été publié. Pour le numéro propriétaire déclaré, l’inscription et la connexion contrôlent désormais le mot de passe administrateur avec une empreinte PBKDF2 salée conservée dans les secrets du serveur. Le mot de passe n’est écrit ni dans les sources du site ni dans l’APK.

Au premier lancement, ouvrir Mon compte, choisir Créer un compte et saisir le numéro propriétaire au format international, son mot de passe et un pseudonyme. Si le compte a déjà été créé avec ce même mot de passe, choisir Se connecter. Le serveur attribue alors les droits administrateur à ce compte. Un mot de passe erroné sur ce numéro bloque la création et n’enregistre aucun compte. La gestion quotidienne des numéros reste dans Mon compte > Gérer les numéros autorisés.

La possession du numéro n’est pas prouvée par SMS. La sécurité de cet accès repose sur le secret du mot de passe et sur la protection du téléphone. Le mot de passe communiqué dans une conversation devrait être changé ensuite ; un parcours de changement sécurisé reste à ajouter.

Vérifications : 38 groupes de tests API, inventaire, TypeScript et construction du site. La compilation Android n’a pas été possible ici. Le correctif serveur fonctionne aussi avec l’application 11.14.275 déjà installée.

🔴 INTERVENTION OBLIGATOIRE : essayer la création ou la connexion avec ton numéro et ton mot de passe sur le téléphone, puis ouvrir Mon compte > Gérer les numéros autorisés. Ne me renvoie pas le mot de passe. Si tu utilises le nouveau projet Android, compile-le dans CodeAssist ; aucun APK compilé n’est inclus.
