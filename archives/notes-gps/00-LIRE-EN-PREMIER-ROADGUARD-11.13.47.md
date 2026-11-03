# ROADGUARD 11.13.47 — robustesse téléchargement des cartes

## Réalisé
- Contrôle strict de Content-Range (début, fin, longueur totale) sur les réponses 206.
- Contrôle du Content-Length sur réponse HTTP 200, lorsque déclaré.
- Réinitialisation explicite d'un téléchargement partiel si le serveur répond 200 à une tentative de reprise.
- Gestion de l'annulation avant lecture des données.
- Version Android 11.13.47 / versionCode 177.
- Aucun changement du code GPS bleu ou de la caméra.

## Vérifications
- `python tools/test_pmtiles_http_range_111347.py` : 7 contrôles statiques.
- `python tools/test_range_java_111347.py` : 13 cas Java exécutés avec javac.
- Intégrité ZIP à vérifier à l'assemblage.

## Non réalisé / blocages
- Aucune source autorisée de téléchargement de carte France + Corse n'est encore configurée.
- Aucun serveur d'archives France + Corse hébergé.
- Aucune APK compilée, aucun test réel sur Xiaomi.
- Le téléchargement intégré ne peut pas encore fonctionner sans source cartographique autorisée.
- Le contrôle Content-Range vérifie la cohérence de la plage mais pas l'identité du fichier distant entre deux requêtes. Les archives catalogue sont protégées par SHA-256 final.

## Suite prioritaire
1. Choisir/héberger une source régionale autorisée de PMTiles, avec catalogue HTTPS et hash.
2. Tester téléchargement, interruption, reprise et installation sur un serveur réel.
3. Compiler une APK et tester sur Xiaomi, puis rendu MapLibre en mode avion.
4. Évoluer vers un sélecteur de zone cartographique et téléchargement direct sans saisie d'URL.

Ne pas annoncer que la carte France est fonctionnelle. Préserver l'application installée et les données existantes.
