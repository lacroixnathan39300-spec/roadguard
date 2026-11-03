# ROADGUARD 11.14.165 — correctif de compilation Lufop

Version source à importer dans CodeAssist. Aucun APK compilé.

## Correction

Dans l’import ZIP Lufop, le code utilisait `baseType` alors que seule la variable `type` avait été déclarée. Le filtre de fichiers emploie maintenant `type`, ce qui corrige l’erreur « baseType cannot be resolved to a variable ».

Le correctif de la carte libre de 11.14.164 est inclus : le point bleu continue à suivre le GPS, mais les nouveaux fixes ne recentrent plus la caméra après déplacement manuel; ⌖ reprend le suivi.

## État

- Global estimé : 94 % (inchangé).
- Carte : 89 % (inchangé; test Xiaomi requis).
- Navigation : 100 % côté code.
- Radars : 99 %.
- Compilation Android : non lancée; caméra toujours reportée à la fin.
