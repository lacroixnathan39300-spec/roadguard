# ROADGUARD 11.14.141 — téléchargement radar sans clé API

- Version source Android 11.14.141 / code 341. ZIP source, pas d’APK compilée.
- Depuis Alertes → Bases radar par pays, télécharge les radars fixes OpenStreetMap d’un pays directement, sans compte ni clé.
- Requête Overpass limitée aux objets cartographiés highway=speed_camera; les données peuvent être incomplètes et n’incluent pas forcément les radars mobiles. Les serveurs publics peuvent être momentanément indisponibles ou limiter les requêtes.
- Attribution et licence affichées/enregistrées : © OpenStreetMap contributors · ODbL 1.0.
- Lufop reste une source facultative demandant une clé. Les bases CH sont stockées mais ignorées par les chargeurs carte/alertes.
- Tests Python ciblés réussis; requête Android réelle, compilation et essai Xiaomi restent à faire.
- Estimations non auditées : radars 88→89 %, global 77→78 %. Caméra reportée à la fin; aucun préchargement cartographique.
