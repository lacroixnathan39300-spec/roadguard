# ROADGUARD 11.14.176 — communauté site + Android

Base conservée : 11.14.175, version 37 du projet enregistré. Nouveau site existant : https://roadguard-community.lacroixnathan39300.chatgpt.site. Audience publique conservée. Aucun projet paintball modifié.

## Réalisé

- Client natif CommunitySync et limites CommunityPolicy; mêmes API que le site, sans WebView distante dotée du pont JavaScript.
- Association : navigateur avec connexion ChatGPT du site, code à comparer, approbation explicite, puis contrôle au retour dans l’application. Le serveur ne conserve qu’une empreinte du jeton; le téléphone le chiffre avec Android Keystore AES-GCM.
- Inventaire permanent versionné préembarqué avec SHA256, puis vérifié au démarrage. Seule une version différente est téléchargée. Aucune carte de pays n’est téléchargée.
- Instantané des signalements paginé, validé puis remplacé atomiquement; délai de démarrage de 15 s maximum avant repli local explicite. Sur premier lancement sans position autorisée, les signalements arrivent dès le premier secteur disponible.
- Rafraîchissement au premier plan, lectures publiques de membres approximatifs, expiration locale sans réseau. Les comptes n’envoient leur position qu’après activation distincte.
- Bail de visibilité : désactivation depuis le site ou un autre appareil invalide les renouvellements précédents. Un téléphone périmé ne peut pas réactiver le partage tout seul.
- Publications nouvelles explicites; ancien historique privé non envoyé. Confirmations, abus, retrait de ses contributions et révocation des appareils.
- Couleurs, thèmes et curseurs; voix locales françaises disponibles, sans promesse sur le timbre/genre ni voix identiques entre plateformes.
- Optimisations SQL et recherche web limitée globalement; configuration d’un fournisseur de tuiles HTTPS commune au site et à l’application.

## Tests et limites

Consulter validation-11.14.176.json et VALIDATION-11.14.176.md pour les résultats finaux. Les scripts de rendu sont exercés dans une VM JavaScript; ils ne prouvent pas le rendu visuel Android. Les fixtures de 500 000 positions sont strictement synthétiques et locales. Aucun essai public de charge n’a été lancé. Le profil de charge est préparé, pas exécuté.

La compilation, la production DEX, l’alignement et la signature de l’APK sont contrôlés. Aucun téléphone ni émulateur accessible. L’association avec un vrai compte, le GPS, les voix, l’écran éteint et les couleurs sur Xiaomi restent à valider.

Le mode hébergé actuel utilise une seule D1. Il ne répond pas à une certification de 500 000 connexions simultanées. Il faut provisionner une infrastructure dédiée, des données et des fournisseurs autorisés, des générateurs de charge et une supervision, puis exécuter un essai convenu. La proposition Render n’atteste pas sa connexion ni un achat.

Le cache cartographique reste celui des tuiles consultées. Ni rayon de 50 km garanti, ni bibliothèque France permanente. Les fonctions de trafic, satellite/3D et péages non fournies ne sont pas présentées comme opérationnelles.

## Actions utilisateur, à la fin uniquement

🔴 INTERVENTION OBLIGATOIRE — installer l’APK si la signature convient, ou compiler le ZIP avec la signature CodeAssist existante. Ne pas désinstaller l’application actuelle pour contourner un conflit de signature : conserver ses données et utiliser le projet source.

Dans ROADGUARD : Menu → Communauté → Connecter mon compte. Comparer le code dans le navigateur, autoriser le téléphone, puis revenir dans l’application. Le partage de position se choisit séparément.

Tester un nouveau signalement, le voir dans le site, le retirer depuis le site, vérifier qu’il disparaît après rafraîchissement. Activer puis masquer la visibilité et vérifier l’absence de réactivation automatique.
