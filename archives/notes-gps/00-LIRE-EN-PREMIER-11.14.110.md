# ROADGUARD 11.14.110 — plafond du cache temporaire

- Version source : 11.14.110 / versionCode 310.
- Réglage du cache : plafond temporaire modifiable de 1 à 1024 Mo, valeur prudente 95 Mo par défaut. Le plafond est appliqué aux nouveaux chargements et à l’entretien de la réserve.
- Stadia Maps : plafond local ramené au plus à 100 MB par appareil; sa FAQ conditionne ce cache à un abonnement actif. La configuration n’ajoute aucun compte ni clé API.
- Ce plafond n’atteste pas le droit de précharger chez un autre fournisseur; l’utilisateur doit confirmer les conditions applicables à la source qu’il saisit.
- Avancement estimé : global 48→49 (+1), interface 68→69 (+1), architecture visuelle 37→38 (+1), planification GPS 70→71 (+1). Le cache fonctionnel reste à 0 % tant qu’une source n’est pas configurée.
- Tests statiques/Python : voir `TESTS-ET-BLOCAGES.md`. Le test Java de politique est ajouté, mais non exécuté (pas de `javac` dans l’environnement). APK non compilée et non vérifiée sur Xiaomi.
