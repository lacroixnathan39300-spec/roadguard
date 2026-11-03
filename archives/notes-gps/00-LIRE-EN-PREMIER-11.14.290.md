# ROADGUARD 11.14.290 — chargement local de la carte

Projet complet, versionCode 490, daté du 24 octobre 2026. Aucun APK inclus.

Une carte téléchargée, terminée et couvrant la zone doit s’afficher sans Internet. Il n’est pas demandé de retélécharger les packs, de nettoyer le stockage ou de désinstaller ROADGUARD.

## Changement de ce lot

Les correctifs 288/289 portaient sur le choix des fichiers et la couverture du cache ; ils n’ont pas résolu le rendu observé sur Xiaomi. Un autre problème de compatibilité existe dans le chargement : les pages de carte étaient ouvertes depuis file:///android_asset/ alors que MapLibre demande les tuiles et les polices à une origine HTTPS locale. Les réglages WebView désactivent les accès universels depuis les fichiers. La documentation Android/Chromium déconseille cette combinaison pour les API Fetch/XHR et recommande une origine HTTP(S) virtuelle pour le contenu embarqué.

Les trois pages de carte et leurs scripts, styles et images sont désormais servis depuis https://roadguard.local/assets/. Les tuiles et polices restent sur https://roadguard.local/pmtiles/ et /fonts/. Ce nom est intercepté par l’application ; il n’exige ni DNS, ni serveur Internet, ni téléchargement. Le routage utilise AssetManager et les réponses natives de WebView, sans nouvelle dépendance AndroidX pour CodeAssist. Les réglages d’accès aux fichiers arbitraires et de contenu mixte restent désactivés. La passerelle native reconnaît seulement les trois pages autorisées. Les ressources manquantes, chemins traversants et adresses non autorisées n’ouvrent aucun fichier arbitraire.

Les ressources embarquées sont renvoyées sans stockage en cache HTTP pour éviter de conserver un ancien script après une mise à jour. Le diagnostic de la carte indique l’origine chargée, les packs vectoriels détectés, le nombre de ressources servies et de tuiles effectivement lues dans la session, ainsi que les dernières erreurs locales. Ces compteurs ne sont pas une preuve de pixels dessinés par le GPU.

Les packs et les fichiers de radars restent inchangés. Les correctifs de doublons, Spotify et les autres fonctions de 289 sont conservés.

## Résultats et limites

Les corps réels des gestionnaires de requêtes MainActivity ont été compilés et exécutés avec des adaptateurs URI/réponse WebView : 98 contrôles passent, avec les vrais HTML/JS/CSS embarqués et des octets vectoriels extraits d’une archive PMTiles synthétique. Les trois pages, leurs dépendances relatives, le chemin mosaïque et le chemin direct de tuile sont exercés. L’ancien gestionnaire ne fournit pas cette nouvelle route HTTPS et répond 404 ; ce contrôle avant/après vérifie le nouveau contrat de routage, pas une reproduction de l’écran Xiaomi.

49 contrôles de politique d’origine et de passerelle passent avec des adaptateurs de réglages. Les tests de découverte des packs terminés et les régressions JS des cartes passent. Syntaxe Java vérifiée pour les fichiers modifiés.

La cause exacte sur le téléphone n’est pas démontrée par un journal WebView de cet appareil. Ce lot corrige un problème concret de compatibilité et fournit des diagnostics supplémentaires ; il ne vaut pas validation matérielle. Le SDK Android et un navigateur Chromium exécutable ne sont pas disponibles ici. Aucun build APK, test Robolectric API26/36, rendu WebGL/WebView matériel, essai GPS ou essai sur Xiaomi n’a été exécuté. Un test natif d’origine HTTPS a été ajouté pour la prochaine session Android.

## 🔴 INTERVENTION OBLIGATOIRE — après compilation

1. Compiler le projet 11.14.290 dans CodeAssist, puis installer en mise à jour avec la signature habituelle. Conserver les données et les cartes existantes.
2. Ouvrir une zone couverte par un pack déjà téléchargé, sélectionner Hors ligne, puis activer le mode avion. Fermer et rouvrir ROADGUARD pour vérifier aussi le démarrage sans réseau.
3. Si l’écran reste vide, ouvrir « … → État et diagnostic → État GPS et radars → Copier diagnostic ». Le texte contient maintenant les noms des packs, l’origine et les tuiles lues : l’envoyer permettra de distinguer lecture des fichiers, requêtes du moteur et rendu. Il n’est pas nécessaire de retélécharger les cartes pour ce diagnostic.

Le suivi complet est dans AVANCEMENT-11.14.290.md. Les pourcentages historiques ne sont pas augmentés : global développement 85,0000 %, validation 44,0000 %, deltas +0,0000 point. Fréquence communautaire maintenue à deux minutes. Android Auto reste reporté.

Références officielles consultées le 24/10/2026 :
- https://developer.android.com/develop/ui/views/layout/webapps/load-local-content
- https://chromium.googlesource.com/chromium/src/+/HEAD/android_webview/docs/cors-and-webview-api.md
