# ROADGUARD 11.14.208 — import local d'overlay
- Ajout d'un sélecteur de fichier Android pour importer un CSV ou ZIP contenant `lufop_europe_asc_overlay.csv` au format normalisé ROADGUARD.
- Validation des colonnes, coordonnées, taille et nombre de points; écriture temporaire puis remplacement atomique.
- Overlay installé dans le stockage privé, séparément des ressources APK; invalidation du cache après import.
- Les archives ASC brutes Lufop ne sont PAS encore converties par cet importateur. Les données Lufop déjà embarquées restent disponibles sans import.
- Aucun serveur RadarDroid ni téléchargement automatique connecté.
- Compilation et test Android NON réalisés; import d'un ZIP non testé sur téléphone.
- Prochaine étape : convertisseur ASC brut vers overlay, test CodeAssist et tests des alertes.
