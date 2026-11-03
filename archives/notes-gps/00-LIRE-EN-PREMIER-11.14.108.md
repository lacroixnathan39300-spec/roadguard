# ROADGUARD 11.14.108 — état du cache cartographique

- Version source : 11.14.108 / versionCode 308.
- L’écran Cache présente un état réel : source active ou en attente, couverture mesurée si la position/source le permet, tuiles/Mio stockées et dernière activité.
- Quand la mesure n’est pas possible, le pourcentage est remplacé par un tiret et l’écran précise pourquoi.
- Les détails techniques sont repliables; le rappel précise que le rayon de 50 km est progressif et non garanti.
- Aucune source fournisseur n’est ajoutée ou présumée autorisée. Le cache automatique reste inactif sans source explicitement permise.
- Progression estimée du code : global 46→47 (+1); architecture visuelle 34→35 (+1). Le chantier cache fonctionnel reste à 0 % tant qu’il n’est pas configuré et vérifié.
- Tests statiques et suites Python : voir `TESTS-ET-BLOCAGES.md`. APK non compilée; contrôle Xiaomi encore à faire.
