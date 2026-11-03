# ROADGUARD 11.14.135 — recherche automatique et import catalogue radar

- Source Android 11.14.135 / code 335; aucune APK compilée.
- Recherche de destination lancée automatiquement après une pause de 750 ms, sans envoyer vers un autre écran ni fermer le clavier. Les réponses tardives sont ignorées et les résultats sont classés selon les mots saisis.
- Écran Alertes : import d’un catalogue JSON via le sélecteur de fichiers; même validation des liens HTTPS, tailles, types, SHA-256, provenance et licences.
- La recherche française utilise l’API d’autocomplétion Géoplateforme IGN (avec biais GPS); le géocodeur Android sert de secours et pour les résultats internationaux. La qualité réelle reste à valider sur le téléphone. Aucun pack radar international n’est fourni.
- Estimations navigation 48→50 % (+2), radars 80→81 % (+1), global 72→73 % (+1), non auditées.
- Six écrans à 100 % côté implémentation; rendu Xiaomi à confirmer. Aucun préchargement; caméra reportée à la fin.
