# ROADGUARD 11.14.210 — Correctif de lecture ZIP ASC

## Réalisé
- Corrigé `LufopAscZipImporter.convert` : lecture de chaque entrée ZIP en entier avant le décodage texte, pour éviter le préchargement inter-entrées par `BufferedReader`.
- Limite de 35 Mo décompressés conservée, ainsi que les contrôles de coordonnées, de nombre d'entrées et de points.
- Bases embarquées non modifiées.

## À faire
- Compiler l'application Android complète et tester l'import depuis l'écran Alertes sur Xiaomi.
- Tester activation, dédoublonnage et alertes pour tous les pays.
- Téléchargement automatique des bases RadarDroid non intégré ; nécessite une source autorisée accessible.

## Tests
- Compilation isolée Java et essai d'import du ZIP ASC utilisateur (si résultats consignés ci-dessous).
- Pas de compilation APK Android ni de test appareil.

## Version
- 11.14.210 (code source seulement)

## Résultat du test isolé
- `javac` : succès.
- Archive utilisateur Lufop ASC : 80 035 points convertis, 80 036 lignes CSV en comptant l’en-tête.
- 9 fichiers radar assets comparés avec 11.14.209, modifications : 0.
