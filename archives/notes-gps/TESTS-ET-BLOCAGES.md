# WORK — ROADGUARD 11.14.197

## Réalisé


À l’approche d’un radar, un cadre bleu et un bandeau bleu restent fixes dès 500 mètres, jusqu’au passage ou à la sortie de la zone pertinente. Le cadre comporte une faible teinte transparente et une bordure bleue; il ne bloque pas les gestes. Une marge de 50 mètres limite les coupures visuelles dues aux petites variations GPS du téléphone. Une alerte routière plus proche ne masque plus ce radar. Aucun clignotement ajouté.

Sur Waze, la bulle ROADGUARD devient un panneau bleu fixe pendant l’approche, actualisé avec les nouvelles positions. Elle ne se replie plus au bout de 6,5 secondes et ne clignote plus. À la sortie de la zone, elle redevient la bulle compacte. L’arrêt du mode automatique ou la fermeture de la tâche continue à retirer toute la bulle.

Un bip discret est émis aux paliers 500, 250 et 100 mètres. Si la vitesse GPS brute dépasse la limitation connue du radar dans cette zone, l’alerte passe à deux bips nettement plus forts. Ce passage peut intervenir entre deux paliers, sans attendre le prochain. Au moins six secondes entre deux alertes fortes, avec déduplication commune téléphone/service/voiture. Le silence reste possible si les bips sont désactivés ou si le volume multimédia de l’appareil est coupé. L’application ne modifie pas le volume général.

Les niveaux du générateur sont 22/100 en approche et 95/100 en dépassement; ce ne sont pas des pourcentages de puissance acoustique ni une garantie de volume physique. Le niveau réellement entendu dépend du volume multimédia, du routage Bluetooth et de l’appareil. Les autres annonces vocales restent indépendantes.

La vitesse utilisée n’est pas la correction visuelle du compteur. Les limitations numériques existantes dans les bases France/Suisse sont désormais conservées jusqu’aux moteurs d’alerte. Une valeur absente, invalide ou contradictoire ne déclenche jamais une alerte forte. Un libellé explicitement renseigné en km/h peut également fournir la limitation pour les imports. Un signalement police seul ne fournit pas une limitation. Les données embarquées peuvent être anciennes; les routes parallèles et les changements de limitation restent une limite de précision.

Le cadre bleu est également dessiné dans la surface cartographique appartenant à ROADGUARD sur Android Auto. Aucun changement du moteur de carte ou de caméra, aucun téléchargement de carte France ou de pays ajouté. Le cache de 4 Gio, la mémoire des signalements, le mode automatique et les commandes Spotify de 196 restent conservés.

Accès : Paramètres → Alertes → Halo bleu radar / Bips de danger. Les réglages désactivés volontairement restent désactivés. Une permission de superposition est nécessaire pour la bulle au-dessus de Waze.



## Build / tests

VersionCode397, compileSdk/targetSdk36, minSdk26. Gradle8.11.1/AGP8.10.1/JDK21/Robolectric4.16.1. Variante Android Auto activée par défaut. Assemblage, ensemble des tests et lint exécutés avec --rerun-tasks après les changements finaux.

326 tests Android simulés réussis, zéro échec/erreur/ignoré, API26/28/34/36 selon les suites. Dont26 tests d’alertes radar API26/36 : limites connues/absentes/contradictoires, escalade entre deux paliers, seuil strict, vitesse périmée, doublons entre propriétaires, intervalle6s, nouvelle approche après30min, maintien visuel au-delà de l’ancien délai, arrêt et priorité radar devant un autre danger. Les suites voiture existantes restent99tests.

Lint0erreur bloquante, 55avertissements, catégories{'UnusedAttribute': 1, 'SetJavaScriptEnabled': 1, 'ExportedService': 1, 'UsableSpace': 4, 'SetTextI18n': 48}. Aucune baseline ou suppression globale ajoutée. Contrats Android Auto et précision radar, scripts13écrans et audit48fichiers tuiles réussis. map.html et camera_motion.js comparés octet pour octet auZIP196, identiques. Seul le dessin du cadre d’alerte a été ajouté à la surface voiture, sans changer sa projection ni son moteur.

## Reste / recette physique


- Xiaomi : vérifier cadre et bandeau bleus, continuité lors d’un changement d’écran et disparition après passage/perteGPS/arrêt du suivi. Tester l’affichage superposé sur Waze et le retour à la bulle compacte.
- Vérifier le bip discret et le niveau multimédia, puis son routage Bluetooth. Le bip fort est testé automatiquement avec des vitesses simulées; ne pas dépasser la limitation pour faire un essai.
- Limites numériques présentes pour5441points France actifs hors historique et1171points Suisse; autres points sans limite connue, sans alerte forte. Données anciennes et routes parallèles restent des limites. Ne remplace pas la signalisation réelle.
- Android Auto : rendu cadre et audio sur DHU/véhicule, GPS/reconnexion/appels/musique; distribution Play non validée.
- Fournisseurs : aucune clé OpenWeb Ninja/WazeAPI fournie, aucun appel authentifié. Spotify réel et compte communautaire restant à essayer.
- Carte en ligne/cache4Gio conservés; aucun téléchargement de carte pays ni préchargement de zone ajouté.


Estimations historiques inchangées95% (+0point). Le volume réel n’est pas mesuré ici et les données radar ne garantissent pas la limitation actuelle de la route. Carte/caméra et fonctionnalités196 conservées. Signature et empreinte APK ajoutées après construction finale ci-dessous.

APK finale397/11.14.197, cible36, signature v2/v3 et certificat identique191–196, alignement4octets vérifié. Assets comparés au source; moteurs d’alertes et service voiture présents dans DEX. SHA256 : `6b04b404b2a7671e88c2742b080248e87735c8eede1440dbd8f4ebe81386a6e2`.
