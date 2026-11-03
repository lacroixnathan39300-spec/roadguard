# ROADGUARD 11.14.196 — validations, mode auto et Spotify

Chaque confirmation sur place prolonge la durée du signalement temporaire (24 heures par défaut, réglable 7 ou 30 jours). Les confirmations rapprochées renouvellent ce délai sans gonfler le nombre d'observations : 30 minutes entre deux observations manuelles comptées sur ce téléphone. Au troisième événement distinct, le lieu reste permanent. Un lieu ancien reste une zone possible, sans garantir une présence actuelle. GPS récent, précision suffisante et proximité de 100 mètres restent exigés pour confirmer.

## Mode automatique

Paramètres → Surveillance auto. Armez avec ou sans bulle. Après deux mesures GPS fiables au-dessus de 20 km/h, les alertes deviennent actives. Après une minute à 3 km/h maximum, elles s'arrêtent et la bulle disparaît. Une veille GPS avec notification reste armée pour détecter le prochain départ. Une perte GPS suspend les alertes et la bulle jusqu'à une nouvelle détection de conduite.

Passer sur Waze ou l'accueil conserve ce suivi. Fermer ROADGUARD par Retour depuis la carte ou le retirer des applications récentes retire la bulle et arrête le suivi. Rouvrir ROADGUARD réarme le mode choisi. Un arrêt forcé Android nécessite de rouvrir l'application. Le bouton Arrêter désarme le mode jusqu'à réactivation manuelle. L'affichage superposé, la localisation et les notifications sont demandés par Android lors de l'activation.

## Spotify

Paramètres → Spotify · commandes musicales. Autorisez l'accès aux notifications dans les réglages Android, ouvrez Spotify et démarrez une musique. Revenez dans ROADGUARD pour le titre/artiste, lecture/pause, précédent et suivant, selon les actions disponibles dans la session Spotify. Ces commandes utilisent la session locale Android; aucun mot de passe, clé Spotify ou compte développeur requis. ROADGUARD ne lit, ne stocke et ne transmet pas le contenu des notifications.

Ce panneau ne fournit pas une bibliothèque Spotify ni une connexion OAuth. Sur Android Auto, le lecteur de Spotify installé reste disponible séparément. Aucun essai avec une véritable session Spotify ou un téléphone connecté n'a été effectué ici.

## Livraison et limites

APK directe de vérification signée avec la même clé que 191–195, versionCode 396, cible API 36. Ne pas désinstaller une application portant une autre signature pour forcer la mise à jour sans sauvegarde. Carte, caméra et plafond de cache de 4 Gio conservés.

Les connecteurs WazeAPI/OpenWeb Ninja restent optionnels et nécessitent une clé personnelle : aucune connexion authentifiée testée. Aucun essai Xiaomi, DHU/véhicule ni dépôt Google Play dans cette version. La veille GPS dépend aussi des autorisations et de la gestion d'énergie du téléphone. Voir WORK/VALIDATION-11.14.196.md pour les résultats exacts et la recette sur appareil.
