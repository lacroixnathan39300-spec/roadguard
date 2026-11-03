# ROADGUARD 11.14.268 — inscription et signalements

Cette archive contient **un seul projet Android Gradle complet** dans `app/` et le code du site communautaire dans `site-source/`. Importer le dossier racine dans CodeAssist ; ne pas importer les anciens ZIP à l'intérieur du projet.

## Modifications

- Le bouton Communauté ouvre dans l'application l'inscription par numéro international et code SMS. Le même numéro vérifié retrouve son compte et son essai initial ; les appareils possèdent chacun un jeton révocable. La saisie du prénom ou pseudonyme se fait dans l'application.
- L'administrateur seul dispose, sur le site, d'un formulaire pour accorder ou retirer l'accès illimité à un numéro. Les numéros sont identifiés sur le serveur par HMAC ; seuls les quatre derniers chiffres apparaissent dans la liste d'administration.
- L'écran principal « Signaler » propose le contrôle routier et demande explicitement si le point doit être partagé ou enregistré localement. Les membres non connectés peuvent créer leur compte depuis cet écran.
- La page du site reste réservée au propriétaire. Les lectures de signalements et de positions par l'API demandent désormais un compte ou un téléphone associé. Le fond radar statique existant reste publié séparément et exige une migration ultérieure pour être privé.
- Le menu Communauté n'envoie plus les membres vers l'interface du site réservée au propriétaire. Le bouton Paramètres n'est plus répété dans l'écran Paramètres.

## État réel

La version 7 du site est **enregistrée comme brouillon et non déployée**. La version publique précédente reste active. Il manque les identifiants d'un service Twilio Verify (`TWILIO_VERIFY_SERVICE_SID`, `TWILIO_API_KEY`, `TWILIO_API_SECRET`) pour envoyer les SMS ; `PHONE_HASH_SECRET` est déjà configuré sur le site. Ne déployer cette version et ne distribuer l'APK correspondant qu'après cette configuration et un essai réel du SMS. Aucun essai gratuit n'est encore limité dans le temps : la date de départ et l'exemption illimitée sont enregistrées, mais la durée d'essai n'a pas été définie.

Vérifications : 34 groupes de tests API/données, vérification TypeScript et construction du site réussies. Les contrôles ciblés Java et de la carte réussissent ; ils ne remplacent pas une compilation APK dans CodeAssist ni un test GPS, radar, Bluetooth ou entre deux téléphones.

🔴 **INTERVENTION OBLIGATOIRE** — Créer un service Twilio Verify et une clé API, puis renseigner **dans les secrets du site** les trois variables citées ci-dessus (ne pas les placer dans le ZIP ni dans un message). Après leur configuration, déployer la version 7 du site. Compiler ensuite ce dossier Gradle complet dans CodeAssist, installer l'APK sur deux téléphones et tester : inscription par SMS, signalement « Contrôle routier », réception sur l'autre téléphone et mise à jour d'un numéro illimité. Tant que ces étapes ne sont pas faites, l'inscription et le partage de cette nouvelle version ne sont pas opérationnels.
