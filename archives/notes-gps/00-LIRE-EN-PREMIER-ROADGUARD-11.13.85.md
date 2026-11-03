# ROADGUARD 11.13.85 — Configuration et diagnostic tampon 20 km

## Changements réalisés
- Onglet Hors ligne : configuration du fournisseur HTTPS XYZ PNG et consentement explicite sur ses droits de préchargement.
- Bouton Désactiver conservant la configuration.
- État visible : activation, nombre de fichiers .tile, taille et statistiques du dernier lot.
- Statistiques écrites après chaque lot (tentatives et tuiles enregistrées).
- Conservation du GPS, de la carte et des radars existants sans modifications fonctionnelles volontaires.

## Limitations
- Aucun fournisseur autorisé livré par défaut. Pas de préchargement sans source configurée et confirmation.
- 20 km = planification limitée aux zooms 9–11, 240 tuiles maximum par plan, 12 tentatives/minute : couverture non garantie, pas de détails complets de rue.
- Statistiques du dernier lot : les erreurs réseau ne sont pas détaillées; nombre de fichiers peut inclure des fichiers expirés non encore purgés.
- Les conditions de licence doivent être vérifiées par l'utilisateur; la validation technique de l'URL ne prouve pas les droits.
- Aucune compilation Android ni test Xiaomi réalisés dans cet environnement.

## Prochaines étapes
1. Choisir une source de tuiles autorisant explicitement le préchargement et le stockage temporaire.
2. Tester le flux avec et sans réseau, et vérifier le rendu MapLibre/Leaflet sur Xiaomi.
3. Améliorer couverture, budget data, progression des lots, reprise, gestion erreurs et nettoyage.
