# ROADGUARD 11.14.227 — Correction du véritable point d'entrée
- L'action « Choisir un pays · téléchargement direct sans clé » appelle openRadarCountryPicker(), et non openSimpleRadarCountryPicker().
- openRadarCountryPicker() ouvre maintenant RadarDownloadActivity, interface sombre avec drapeaux, recherche et états rouges/verts.
- L'ancienne méthode est conservée sous openRadarCountryPickerLegacy() pour ne pas perdre son implémentation.
- Aucune base radar embarquée modifiée. L'intégrité ZIP est contrôlée.
- Compilation Android et contrôle visuel sur appareil encore requis.
- Les archives .7z téléchargées ne sont pas encore converties/activées en alertes.
