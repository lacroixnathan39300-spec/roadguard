# ROADGUARD 11.14.229 — Affichage Android 15/16 et états honnêtes
- RadarDownloadActivity : application des insets réels des barres système et des découpes via WindowInsets, en conservant 12 dp de marge.
- Résout le chevauchement du titre sous la barre d'état et des boutons sous la barre de navigation observé sur Xiaomi (validation visuelle encore requise).
- « Archive reçue » ne signifie pas « radars actifs ». Les téléchargements 7z ne sont toujours pas convertis en SQLite exploitable par le moteur.
- Les liens pays sont proposés, pas vérifiés pays par pays : statut explicite « disponibilité à vérifier ».
- Actualisation de l'état des boutons après une tentative de téléchargement, succès ou échec.
- Aucun fichier de données radar ni moteur d'alerte modifié.
- Compilation Android complète non exécutée. Contrôle ZIP OK.
