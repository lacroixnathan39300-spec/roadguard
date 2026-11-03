# ROADGUARD 11.14.232 — Alerte visuelle cohérente

- `MainActivity.checkDangers()` : suppression du Toast redondant pour les radars lorsque la bannière dédiée est affichée. Les avertissements vocaux restent pilotés par les seuils existants.
- La distance de la bannière reste la distance GPS actualisée ; le message vocal reste une annonce de franchissement de seuil. L'écart ponctuel entre une annonce vocale et la bannière est donc possible mais n'est plus dupliqué à l'écran.
- Suppression de l'affichage de la vitesse sur les radars de feu rouge et de passage à niveau, sans toucher aux données sources.
- Libellé du réglage de halo aligné sur le halo rouge existant.
- Les bases radar CSV/JSON/ASC ne sont pas modifiées.
- Tests : vérification des remplacements et intégrité ZIP ; compilation Android et test sur route non réalisés.
- Suivi : aucune augmentation chiffrée de progression sans test de compilation et essai réel.
