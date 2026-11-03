# ROADGUARD 11.14.209 — Import ZIP ASC natif

## Réalisé
- Ajout de LufopAscZipImporter.java : reconnaissance des ZIP ASC Lufop, lecture par pays, conversion des coordonnées longitude/latitude et catégories en CSV ROADGUARD.
- RadarDownloadActivity accepte maintenant les ZIP ASC bruts en plus des CSV/ZIP au format ROADGUARD.
- Limites de sécurité : 2 000 entrées, 35 Mo de texte, 200 000 points ; validation des coordonnées ; fichier temporaire et bases embarquées inchangées.
- Progression de conversion par tranche de 5 000 points.

## À faire / limites
- Aucun téléchargement automatique depuis RadarDroid : source autorisée toujours nécessaire.
- Test de compilation Android CodeAssist et test réel Xiaomi non effectués.
- Le ZIP ASC local remplace l'overlay additionnel précédent après import réussi (les bases radar embarquées ne sont pas remplacées).
- Vérifier le parsing des différents fichiers ASC et l'actualisation des alertes après redémarrage navigation.
- Améliorer l'installation atomique et le suivi de progression octets pour import ASC.

## Validation
- Classe LufopAscZipImporter compilée isolément avec javac.
- Aucune compilation APK réalisée.
