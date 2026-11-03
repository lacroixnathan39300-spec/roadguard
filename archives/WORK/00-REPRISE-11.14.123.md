# Reprise ROADGUARD 11.14.123

Version Android 11.14.123 / code 323. Source ZIP, pas d’APK.

## Réalisé
- Carte Leaflet conserve tous les dangers reçus mais ne crée les icônes que pour les points présents dans la fenêtre (bounds + 12 %). Rafraîchissement sur moveend/zoomend.
- Popups indiquent « Base officielle · data.gouv.fr » ou « Point local ».
- Le parseur foreground indexe d’abord les imports puis évite de réajouter le radar officiel à moins de 40 m.
- Alertes background et foreground gardent l’index de proximité, dataset officiel et IDs stables.
- Aucune modification de camera/heading, GPS bleu, navigation ou préchargement.

## Avancement estimé
Radars 60→65 % (+5); global 64→65 % (+1), non audités.

## Tests et prochaine étape
Tests dataset, index 25k positions, rendu, passation, GPS/carte et JS. SDK 34/Gradle/wrapper absents : compiler via CodeAssist, tester la fluidité et alertes sur Xiaomi. Caméra en dernier.
