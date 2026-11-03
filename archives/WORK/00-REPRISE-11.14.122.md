# Reprise ROADGUARD 11.14.122

Version Android 11.14.122 / code 322; archive source, sans APK.

## Réalisé
- `DangerSpatialIndex.java`: index géographique en cellules 0,10° avec longitudes cycliques, zone de requête conservative pour un rayon donné.
- Alertes en avant-plan et service arrière-plan ne mesurent que les points candidats sous 5 km.
- Correction du chargement CSV arrière-plan: latitude/longitude dans c[2]/c[3] du CSV embarqué.
- Le chargement arrière-plan insère puis indexe les 3 309 points officiels sans comparaison exhaustive entre chaque paire.
- Dédoublonnage spatial des points importés/personnels; identifiant des étapes basé sur coordonnées plutôt que position dans la liste.

## Tests
25 000 positions simulées couvrent le rayon; tests dataset, visuels, fonctionnels, GPS, carte et préchargement passent. Le SDK/Gradle/wrapper manque; compilation et test Xiaomi impossibles ici.

## Estimations
Radars 55→60 % (+5); global 63→64 % (+1), non audités.

## Suite
Compiler avec CodeAssist; tester performances et annonces premier plan/arrière-plan sur Xiaomi. Caméra reste dernière étape; cache sans préchargement.
