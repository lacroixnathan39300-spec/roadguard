# ROADGUARD 11.14.142 — reprise après erreur Overpass

- Version source Android 11.14.142 / code 342. ZIP source, pas d’APK compilée.
- Le téléchargement radar OpenStreetMap sans clé essaie maintenant jusqu’à trois instances Overpass publiques, séquentiellement, après erreur HTTP ou réseau.
- Les demandes ne sont pas lancées en parallèle; l’annulation reste disponible et la base précédente n’est remplacée qu’après validation.
- Couverture OSM contributive (radars fixes cartographiés seulement); le service public peut être surchargé. Lufop reste facultatif. Les bases CH sont stockées mais ignorées par les chargeurs d’alertes.
- Tests Python et contrôles statiques à exécuter; compilation Android et essai Xiaomi restent à faire.
- Estimations non auditées : radars 89→90 %, global 78→79 %. Caméra reportée à la fin; aucun préchargement cartographique.
