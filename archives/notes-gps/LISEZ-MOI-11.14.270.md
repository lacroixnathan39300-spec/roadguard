# ROADGUARD 11.14.270 — repère d'appareil sans SMS

Cette archive contient l'application Android et les sources du site ROADGUARD.

L'inscription reste gratuite et sans SMS : compte sur le site, numéro déclaré, code d'association affiché par le téléphone. Le numéro n'est pas vérifié par un opérateur. L'adresse IP sert uniquement à limiter les demandes répétées.

Avec cette version, l'application transmet l'identifiant Android au moment de l'association, après l'explication affichée à l'écran. Le site n'en conserve qu'une empreinte HMAC. Si cette empreinte a été vue sur plusieurs comptes, l'administrateur voit un indicateur dans « Accès des membres ». Aucun compte n'est bloqué automatiquement et la durée de l'essai n'est pas encore appliquée.

Il s'agit d'un indice technique, pas d'une identité certaine : cet identifiant peut changer ou être falsifié. Un téléphone partagé peut légitimement apparaître sur plusieurs comptes. Play Integrity et Device Recall ne sont pas intégrés à cet APK.

Vérifications effectuées : 35 groupes de tests API, TypeScript et compilation du site. Compilation Android non effectuée ici : la distribution Gradle est inaccessible dans cet environnement. Importer cette archive dans CodeAssist et compiler une seule fois pour tester sur téléphone.
