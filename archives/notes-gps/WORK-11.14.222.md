# ROADGUARD 11.14.222 — boutons de téléchargement par pays

## Modifications
- Sept boutons FR CH DE BE ES IT LU dans RadarDownloadActivity.
- Téléchargement HTTPS avec redirections limitées, contrôle HTTP, limite 100 Mo, signature 7z, fichier temporaire et progression.
- Les archives sont stockées dans le dossier privé radar_downloads; aucune base embarquée ni base active modifiée.

## Limites et tests à faire
- URLs GitHub SpeedAlert de la publication europe-202606 à vérifier en conditions réelles; téléchargement non testé sur réseau.
- Les fichiers 7z/SQLite ne sont PAS encore convertis ni activés; nécessite un parseur et la validation du schéma.
- Pas de compilation Android ni de test sur Xiaomi.
- Vérifier licence et politique de redistribution avant diffusion.

## Version précédente
11.14.221.
