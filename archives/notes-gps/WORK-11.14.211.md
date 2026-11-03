# ROADGUARD 11.14.211 — Installation ASC avec sauvegarde

## Fait
- Validation intégrale de l’overlay temporaire avant installation (en-tête, colonnes, coordonnées, vitesse, limites).
- Sauvegarde de la base locale active avant remplacement, restauration en cas d’échec du renommage.
- Affichage du nombre de points installés.
- Les sept bases radar d’origine et l’overlay Europe embarqué restent inchangés.

## À faire
- Tester le flux sur Android/Xiaomi, en particulier interruptions, stockage plein et permissions.
- Finaliser téléchargement direct depuis une source autorisée et fusion fonctionnelle RadarDroid.
- Vérifier le rechargement à chaud des alertes.

## Tests
- Contrôle statique de la modification. Pas de compilation Android complète ni de test sur appareil.

## Version
11.14.211 — correctif de robustesse; ne pas considérer le téléchargement RadarDroid comme opérationnel.
