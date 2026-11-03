# ROADGUARD 11.14.235 — Validation des coordonnées radar
- Refuse les latitudes hors [-90,90] et longitudes hors [-180,180], en plus des NaN/infinités.
- Empêche l'enregistrement d'étapes d'alerte pour des positions GPS impossibles.
- Préserve les seuils et distances valides, ainsi que toutes les bases radar.
- Contrôle ciblé seulement, pas de grosse vérification anticipée.
- Compilation Android et tests sur Xiaomi encore à réaliser.
