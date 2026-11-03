# ROADGUARD 11.14.202 — Téléchargement radar, écran de diagnostic

## Réalisé
- Ajout de RadarDownloadActivity (interface autonome : pays FR/CH, URL HTTPS directe, SHA-256, barre d’octets, erreurs rouges).
- Téléchargement sur thread secondaire et réutilisation de AuthorizedRadarImporter.
- Activité déclarée non exportée dans le manifeste. Non liée au menu principal : écran encore inaccessible via l’interface ordinaire.
- Aucune modification des CSV embarqués ni du moteur de navigation.

## À faire
- Connecter l’écran à un bouton du menu ROADGUARD.
- Identifier une source autorisée de téléchargement RadarDroid/Lufop et son protocole réel. Aucun identifiant ni endpoint protégé n’a été copié.
- Étendre l’import à 7 pays (lecteur actuel : FR/CH seulement).
- Compiler et tester sur Xiaomi ; Android Auto non testé.

## Validation
- Inspection statique uniquement ; pas de compilation Android dans cet environnement.
- Version 11.14.201 utilisée comme base.
