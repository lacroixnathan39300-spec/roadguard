# ROADGUARD 11.13.33 — Reprise PMTiles

## Travail effectif
- `MainActivity.java`: téléchargements PMTiles conservés dans `<id>.pmtiles.part` après coupure/annulation; identité URL/taille/SHA pour éviter la reprise depuis une autre source.
- Requête HTTP Range, vérification 206 + Content-Range, repli sur téléchargement complet si serveur répond 200.
- SHA-256 calculé sur le fichier final complet, y compris les octets récupérés d'une session antérieure.
- Aucune modification du GPS.

## Limites / BLOQUÉ
- Aucun fichier France + Corse vérifié et hébergé n'est fourni dans cette archive.
- Compilation APK et test réel Xiaomi non effectués.
- Pas de validation d'un serveur réel ni de l'affichage hors ligne.
- Progression historique 42 % non auditée; ne pas l'augmenter sans test de bout en bout.

## Suite prioritaire
1. Identifier une source de pack PMTiles France + Corse vérifiée, avec licence et hébergement HTTP Range.
2. Tester téléchargement et reprise sur serveur réel, y compris coupure et relance.
3. Compiler l'APK et tester en mode avion sur Xiaomi.
4. Ne demander une intervention utilisateur avec vignette rouge que lorsqu'elle est indispensable.
