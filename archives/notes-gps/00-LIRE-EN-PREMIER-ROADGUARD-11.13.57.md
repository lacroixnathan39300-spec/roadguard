# ROADGUARD 11.13.57 — Passation prioritaire

## État réel
Global historique estimé : **42 %** (non audité). Ce ZIP est du **code source**, pas une APK.

## Fait dans 11.13.57
- `OfflineCellStore.importVerified(id, InputStream, sha256, maxBytes)` : import en flux avec fichier temporaire, SHA-256, limite de taille, synchronisation disque, protection de l'ancienne version en cas d'échec.
- `OfflineCellStore.registeredBytes()` : total déclaré dans le registre.
- `MainActivity.java` : affiche aussi les Mio enregistrés dans la bibliothèque.
- `app/build.gradle` : version 11.13.57, versionCode 186.
- Tests CLI Java : import correct, mauvais SHA, dépassement de taille, conservation de l'ancien pack et redémarrage du registre.

## Limites à ne pas masquer
- AUCUN téléchargement automatique des secteurs GPS 50 km ; aucun fournisseur ou hébergement compatible raccordé.
- Les identifiants de grille de 5 km ne sont pas encore liés à de véritables packs PMTiles hébergés.
- Le test utilise des octets factices, pas un PMTiles valide : il teste l'intégrité du stockage, pas le rendu.
- Pas de build APK ni de test Xiaomi. GPS et caméra non modifiés fonctionnellement.
- Pas de carte France+Corse fournie.

## Prochaines tâches
1. Concevoir un catalogue réel de packs PMTiles hébergés légalement avec IDs de régions, emprises, SHA et URL HTTPS.
2. Relier les régions disponibles à la grille GPS et éviter les recouvrements et re-téléchargements.
3. Intégrer téléchargement Android reprenable en arrière-plan, puis appel à `importVerified`.
4. Raccorder les packs au rendu MapLibre hors ligne et tester sans réseau.
5. Compiler APK, valider GPS/caméra, bibliothèque persistante et tests Xiaomi.

## Commande de vérification locale
`javac -d classes app/src/main/java/fr/roadguard/app/{OfflineCellStore,NearbyOfflinePlanner}.java` (Linux shell).
