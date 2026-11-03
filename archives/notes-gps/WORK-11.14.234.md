# ROADGUARD 11.14.234 — Protection distances GPS invalides
- AlertMilestones.stage() refuse les distances NaN, infinies et négatives (étape 0).
- pending() et claim() refusent les étapes invalides et les coordonnées non finies : pas d'alerte/état enregistré à partir d'une mesure GPS corrompue.
- Seuils et comportement des distances valides inchangés.
- Correction ciblée uniquement : ce n'est pas la grosse vérification horaire.
- Bases radar originales inchangées ; archive ZIP vérifiée.
- Compilation Android et test sur appareil non réalisés.
