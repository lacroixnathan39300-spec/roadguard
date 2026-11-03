# ROADGUARD 11.14.197 — halo bleu persistant et bips radar

À l’approche d’un radar, un cadre bleu et un bandeau bleu restent fixes dès 500 mètres, jusqu’au passage ou à la sortie de la zone pertinente. Le cadre comporte une faible teinte transparente et une bordure bleue; il ne bloque pas les gestes. Une marge de 50 mètres limite les coupures visuelles dues aux petites variations GPS du téléphone. Une alerte routière plus proche ne masque plus ce radar. Aucun clignotement ajouté.

Sur Waze, la bulle ROADGUARD devient un panneau bleu fixe pendant l’approche, actualisé avec les nouvelles positions. Elle ne se replie plus au bout de 6,5 secondes et ne clignote plus. À la sortie de la zone, elle redevient la bulle compacte. L’arrêt du mode automatique ou la fermeture de la tâche continue à retirer toute la bulle.

Un bip discret est émis aux paliers 500, 250 et 100 mètres. Si la vitesse GPS brute dépasse la limitation connue du radar dans cette zone, l’alerte passe à deux bips nettement plus forts. Ce passage peut intervenir entre deux paliers, sans attendre le prochain. Au moins six secondes entre deux alertes fortes, avec déduplication commune téléphone/service/voiture. Le silence reste possible si les bips sont désactivés ou si le volume multimédia de l’appareil est coupé. L’application ne modifie pas le volume général.

Les niveaux du générateur sont 22/100 en approche et 95/100 en dépassement; ce ne sont pas des pourcentages de puissance acoustique ni une garantie de volume physique. Le niveau réellement entendu dépend du volume multimédia, du routage Bluetooth et de l’appareil. Les autres annonces vocales restent indépendantes.

La vitesse utilisée n’est pas la correction visuelle du compteur. Les limitations numériques existantes dans les bases France/Suisse sont désormais conservées jusqu’aux moteurs d’alerte. Une valeur absente, invalide ou contradictoire ne déclenche jamais une alerte forte. Un libellé explicitement renseigné en km/h peut également fournir la limitation pour les imports. Un signalement police seul ne fournit pas une limitation. Les données embarquées peuvent être anciennes; les routes parallèles et les changements de limitation restent une limite de précision.

Le cadre bleu est également dessiné dans la surface cartographique appartenant à ROADGUARD sur Android Auto. Aucun changement du moteur de carte ou de caméra, aucun téléchargement de carte France ou de pays ajouté. Le cache de 4 Gio, la mémoire des signalements, le mode automatique et les commandes Spotify de 196 restent conservés.

Accès : Paramètres → Alertes → Halo bleu radar / Bips de danger. Les réglages désactivés volontairement restent désactivés. Une permission de superposition est nécessaire pour la bulle au-dessus de Waze.

APK de vérification versionCode397, cibleAPI36, signée avec la même identité que191–196. Les essais sur Xiaomi et en voiture restent nécessaires pour juger du rendu et du volume réels. Voir WORK/VALIDATION-11.14.197.md pour les résultats de compilation et des tests effectués.
