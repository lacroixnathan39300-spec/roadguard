# ROADGUARD 11.13.22 — passation vérifiable

## Modification réelle
- Téléchargement PMTiles : fichier temporaire déplacé du cache vers le même répertoire que le fichier final (`files/pmtiles`). Le renommage vers le fichier final se fait ainsi sur le même système de fichiers.
- Précontrôle de stockage : PMTiles nécessite désormais taille annoncée + 128 Mio de réserve, au lieu de trois fois la taille. Les packs XYZ ZIP conservent la marge x3 pour extraction.
- Précontrôle du candidat France public harmonisé avec cette estimation.
- Aucun changement au GPS, à la caméra, au rendu MapLibre ni à la version affichée dans l'APK.

## Limitations et risques
- Le serveur historique map.comptoir.net reste indisponible; aucun pack France+Corse vérifié ni URL hébergée n'est fourni. Ce correctif ne rend pas la carte hors ligne disponible.
- Pas de reprise HTTP des téléchargements interrompus. Le téléchargement doit recommencer si interrompu.
- Pas de compilation APK ni de test Xiaomi; aucun progrès de validation fonctionnelle démontré.
- Le code source comporte toujours un candidat public historique; ne pas présenter cette URL comme une source fiable.
- La vérification d'en-tête/boîte englobante PMTiles ne garantit pas à elle seule la présence de toutes les tuiles.

## Avancement
42 % : ancienne estimation provisoire, NON recalculée ni auditée. Ne pas l'augmenter sans test utilisateur ou recette réelle. Pas d'intervention obligatoire immédiate pour ce changement de code.

## Suite prioritaire
1. Obtenir un pack France+Corse vérifiable (couverture réelle, licence, format, style) et URL HTTPS stable.
2. Intégrer reprise HTTP Range et contrôle SHA256 imposé.
3. Tester affichage réel hors ligne et bascule mode avion.
4. Compiler APK, test GPS bleu et cartes sur Xiaomi.
