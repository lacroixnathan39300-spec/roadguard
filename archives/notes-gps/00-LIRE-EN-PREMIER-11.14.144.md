# ROADGUARD 11.14.144 — bases radar MapAtlas multi-pays

- Version Android source 11.14.144 / code 344. Archive source, pas d’APK compilée.
- Nouveau téléchargement intégré MapAtlas : catalogue public de 106 pays/régions puis GeoJSON du pays choisi, converti et stocké localement hors connexion.
- Catégories importées : radars fixes, feux rouges, contrôles de vitesse moyenne et radars à vitesse variable. La base officielle France, OpenStreetMap et Lufop restent séparés.
- Licence CC BY 4.0 et attribution MapAtlas enregistrées avec la base. Les données sont communautaires, non officielles, et leur snapshot déclaré est daté du 21/05/2024; l’exhaustivité n’est pas garantie, notamment pour les contrôles mobiles.
- Mise à jour et suppression des bases MapAtlas disponibles dans le même sélecteur. En cas d’échec de téléchargement, le CSV déjà installé est conservé.
- Estimations non auditées : Radars 91→92 %, global 80→81 %.
- Compiler avec CodeAssist puis tester le téléchargement France et un autre pays, les catégories, les alertes et le mode hors connexion.
