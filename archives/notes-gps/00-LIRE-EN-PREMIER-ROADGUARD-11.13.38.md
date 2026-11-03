# ROADGUARD 11.13.38 — Contrôle de l'hébergement France

## Réalisé
- Nouveau `tools/verify_france_hosted.py` : contrôle d'un catalogue JSON HTTPS réel, entrée unique France, URL PMTiles HTTPS, taille et SHA-256 déclarés, requête Range `bytes=0-126`, statut 206, Content-Range exact, signature PMTiles v3 vectorielle.
- Cinq tests automatisés du contrôle distant réussis, incluant refus d'un serveur ignorant Range, d'une mauvaise taille, d'un raster et d'une URL HTTP.
- Aucun changement au code GPS, caméra et Android.

## Commande après publication effective
`python3 tools/verify_france_hosted.py https://VOTRE-HOTE/catalog.json`

## Limites
- Aucun hébergement public ni carte France+Corse réelle n'a été créé ici.
- Ce contrôle ne télécharge pas le pack entier, ne recalcule pas son SHA-256, ne vérifie pas la présence de chaque tuile et ne teste pas le rendu hors ligne.
- Aucun APK compilé ou testé sur Xiaomi.

## Priorité
1. Extraire une véritable archive France+Corse depuis une source Protomaps valide.
2. Publier archive + catalogue HTTPS avec prise en charge Range.
3. Exécuter le contrôle distant, compiler l'APK, tester sans Internet sur Xiaomi.
