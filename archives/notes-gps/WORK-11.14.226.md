# ROADGUARD 11.14.226 — Raccordement du véritable écran
- MainActivity.openSimpleRadarCountryPicker() ouvrait l’ancien AlertDialog « Installer les radars · autres pays ».
- Cette méthode lance désormais RadarDownloadActivity (écran graphite/rouge, recherche, drapeaux et boutons).
- Bases radar intégrées inchangées. Téléchargement 7z ≠ installation active.
- Compilation et vérification sur téléphone non effectuées.
