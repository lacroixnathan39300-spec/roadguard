# ROADGUARD 11.14.274 — connexion dans l'application

L'écran Mon compte propose maintenant deux actions natives : « Se connecter » et « Créer un compte ». Le numéro au format +33… et un mot de passe de 12 à 128 caractères sont saisis dans ROADGUARD. L'inscription demande aussi un prénom ou pseudonyme. Aucun navigateur ni SMS n'est utilisé pour ce nouveau parcours.

Le serveur ROADGUARD version 10 (même origine HTTPS) est publié avec les routes `/api/v1/native-auth/register` et `/api/v1/native-auth/login`. Il conserve le numéro sous empreinte HMAC et le mot de passe sous dérivé PBKDF2 avec sel. Le téléphone garde seulement un jeton chiffré avec Android Keystore. Les signalements, la base communautaire et le droit d'accès utilisent ce jeton. La session est recontrôlée au retour au premier plan et une révocation verrouille l'application. Les cartes déjà téléchargées restent consultables hors réseau pour une session préalablement confirmée.

Le site propriétaire rejetait la requête `GET /?pair=…` avec un 404. La version 10 corrige le contrôle du propriétaire pour son adresse de compte d'hébergement vérifiée. L'application ne dépend plus de cette page pour la connexion.

**Limites à connaître :** aucun SMS n'est envoyé, donc le numéro déclaré n'est pas prouvé par un opérateur ; la récupération automatique du mot de passe n'est pas disponible. Un compte créé uniquement sur l'ancien site avec le même numéro ne possède pas encore de mot de passe natif : il faut une migration vérifiée par le propriétaire. Le simple fait de réinscrire ce numéro ne permet pas de prendre ce compte.

**Vérifications réalisées :** 36 groupes de tests API, test de cache/inventaire, contrôle TypeScript et construction du site, syntaxe de l'interface Android, archive ZIP intacte. Le nouveau site est déployé. La compilation Android n'a pas été possible localement car le wrapper Gradle 8.11.1 ne peut pas être téléchargé depuis cet environnement.

🔴 INTERVENTION OBLIGATOIRE : compiler l'application Android et essayer sur le téléphone l'inscription, la connexion, le retour hors réseau, la déconnexion et les messages d'erreur. Aucun nouvel APK compilé n'est inclus.
