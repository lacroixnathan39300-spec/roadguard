# ROADGUARD 11.14.131 — préparation des bases radar par pays

- Source Android 11.14.131 / code 331; aucune APK compilée.
- Outil ajouté : `tools/build_radar_country_catalog.py`, transforme des CSV nationaux configurables et génère le catalogue signé par taille/SHA-256.
- Tests avec données synthétiques : valides conservés, non-radars et coordonnées invalides filtrés, liens HTTP refusés.
- Téléchargement par pays dans l’app de 11.14.130 reste prêt; les données doivent être redistribuables et hébergées sur HTTPS avant d’être proposées.
- Recherche inline du cockpit Carte conservée.
- Estimations : radars 76→77 % (+1), global stable à 71 %, non auditées.
- Compiler via CodeAssist et valider sur Xiaomi. Caméra à la fin; aucun préchargement.
