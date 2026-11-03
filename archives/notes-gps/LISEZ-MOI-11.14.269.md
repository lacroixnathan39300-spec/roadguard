# ROADGUARD 11.14.269 — compte sans SMS

L'application Android et les sources du site sont réunies dans cette archive.

## Inscription

1. Dans l'application : Communauté → Connecter mon compte gratuitement.
2. Un code d'association est affiché ; ouvrir le site proposé.
3. Se connecter sur le site, déclarer son numéro au format international et choisir un pseudonyme.
4. Comparer le code puis autoriser le téléphone. Aucun SMS n'est envoyé.

Le site garde une empreinte HMAC du numéro et ses quatre derniers chiffres. Un numéro et un compte ne peuvent être associés qu'une fois. Le numéro est **déclaré, pas vérifié** : le dispositif ne prouve pas la propriété du numéro et n'empêche pas de créer d'autres comptes avec d'autres numéros. La récupération du compte passe par la connexion au site ; aucun accès n'est accordé à partir du seul numéro déclaré. Un compte déjà inscrit dans la version SMS garde son enregistrement.

L'administrateur peut accorder un accès illimité à un numéro enregistré depuis le site. Cette opération ne crée pas un compte et n'envoie aucun SMS.

La version du site associée à cette archive est la version 8. Elle utilise le secret PHONE_HASH_SECRET déjà configuré côté site ; aucune clé Twilio n'est requise. L'application Android n'a pas pu être compilée dans cet environnement, faute de distribution Gradle accessible. Les tests API (34 groupes), le contrôle TypeScript et la compilation du site ont réussi. Une compilation Android dans CodeAssist et un essai sur téléphone restent nécessaires.
