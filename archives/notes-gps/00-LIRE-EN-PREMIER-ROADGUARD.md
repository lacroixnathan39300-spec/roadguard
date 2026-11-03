# ROADGUARD 11.14.246 — ZIP complet

Sources, ressources, données, dépendances, notices et deux APK debug sont réunis dans ce projet. Commencer par `WORK-11.14.246.md` ; tableau complet dans `AVANCEMENT-11.14.246.md`.

Les deux APK courants sont dans LIVRAISON, avec TELEPHONE ou ANDROID-AUTO dans leur nom. Ce sont deux variantes du même package ; Android Auto inclut aussi le téléphone. Pour conserver les données de l’application déjà installée, reconstruire les sources avec sa signature CodeAssist habituelle si la clé diffère. Les APK 197 et les notes précédentes sont historiques.

Pack intégré : **Apparence → Pack graphique local → Graphite / Lisibilité / Couleurs OSM**. Il change les couleurs des tuiles ; les zones restent fournies par le réseau et le cache temporaire. Détails : `PACK-GRAPHIQUE-CARTE.md`.

Les archives FR/CH et toutes les bibliothèques nécessaires sont présentes. L’import 7z est isolé des anciennes classes d’Android 16. Les deux variantes compilent et 86 tests Robolectric passent. Les 59 ressources d’origine sont conservées. Les essais visuels, Xiaomi et autoradio restent à faire en fin de lot.
