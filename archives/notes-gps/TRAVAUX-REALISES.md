# ROADGUARD 11.14.197 — halo bleu persistant et bips radar

À l’approche d’un radar, un cadre bleu et un bandeau bleu restent fixes dès 500 mètres, jusqu’au passage ou à la sortie de la zone pertinente. Le cadre comporte une faible teinte transparente et une bordure bleue; il ne bloque pas les gestes. Une marge de 50 mètres limite les coupures visuelles dues aux petites variations GPS du téléphone. Une alerte routière plus proche ne masque plus ce radar. Aucun clignotement ajouté.

Sur Waze, la bulle ROADGUARD devient un panneau bleu fixe pendant l’approche, actualisé avec les nouvelles positions. Elle ne se replie plus au bout de 6,5 secondes et ne clignote plus. À la sortie de la zone, elle redevient la bulle compacte. L’arrêt du mode automatique ou la fermeture de la tâche continue à retirer toute la bulle.

Un bip discret est émis aux paliers 500, 250 et 100 mètres. Si la vitesse GPS brute dépasse la limitation connue du radar dans cette zone, l’alerte passe à deux bips nettement plus forts. Ce passage peut intervenir entre deux paliers, sans attendre le prochain. Au moins six secondes entre deux alertes fortes, avec déduplication commune téléphone/service/voiture. Le silence reste possible si les bips sont désactivés ou si le volume multimédia de l’appareil est coupé. L’application ne modifie pas le volume général.

Les niveaux du générateur sont 22/100 en approche et 95/100 en dépassement; ce ne sont pas des pourcentages de puissance acoustique ni une garantie de volume physique. Le niveau réellement entendu dépend du volume multimédia, du routage Bluetooth et de l’appareil. Les autres annonces vocales restent indépendantes.

La vitesse utilisée n’est pas la correction visuelle du compteur. Les limitations numériques existantes dans les bases France/Suisse sont désormais conservées jusqu’aux moteurs d’alerte. Une valeur absente, invalide ou contradictoire ne déclenche jamais une alerte forte. Un libellé explicitement renseigné en km/h peut également fournir la limitation pour les imports. Un signalement police seul ne fournit pas une limitation. Les données embarquées peuvent être anciennes; les routes parallèles et les changements de limitation restent une limite de précision.

Le cadre bleu est également dessiné dans la surface cartographique appartenant à ROADGUARD sur Android Auto. Aucun changement du moteur de carte ou de caméra, aucun téléchargement de carte France ou de pays ajouté. Le cache de 4 Gio, la mémoire des signalements, le mode automatique et les commandes Spotify de 196 restent conservés.

Accès : Paramètres → Alertes → Halo bleu radar / Bips de danger. Les réglages désactivés volontairement restent désactivés. Une permission de superposition est nécessaire pour la bulle au-dessus de Waze.

APK de vérification versionCode397, cibleAPI36, signée avec la même identité que191–196. Les essais sur Xiaomi et en voiture restent nécessaires pour juger du rendu et du volume réels. Voir WORK/VALIDATION-11.14.197.md pour les résultats de compilation et des tests effectués.

---

Historique conservé

# ROADGUARD 11.14.196 — validations, mode auto et Spotify

Chaque confirmation sur place prolonge la durée du signalement temporaire (24 heures par défaut, réglable 7 ou 30 jours). Les confirmations rapprochées renouvellent ce délai sans gonfler le nombre d'observations : 30 minutes entre deux observations manuelles comptées sur ce téléphone. Au troisième événement distinct, le lieu reste permanent. Un lieu ancien reste une zone possible, sans garantir une présence actuelle. GPS récent, précision suffisante et proximité de 100 mètres restent exigés pour confirmer.

## Mode automatique

Paramètres → Surveillance auto. Armez avec ou sans bulle. Après deux mesures GPS fiables au-dessus de 20 km/h, les alertes deviennent actives. Après une minute à 3 km/h maximum, elles s'arrêtent et la bulle disparaît. Une veille GPS avec notification reste armée pour détecter le prochain départ. Une perte GPS suspend les alertes et la bulle jusqu'à une nouvelle détection de conduite.

Passer sur Waze ou l'accueil conserve ce suivi. Fermer ROADGUARD par Retour depuis la carte ou le retirer des applications récentes retire la bulle et arrête le suivi. Rouvrir ROADGUARD réarme le mode choisi. Un arrêt forcé Android nécessite de rouvrir l'application. Le bouton Arrêter désarme le mode jusqu'à réactivation manuelle. L'affichage superposé, la localisation et les notifications sont demandés par Android lors de l'activation.

## Spotify

Paramètres → Spotify · commandes musicales. Autorisez l'accès aux notifications dans les réglages Android, ouvrez Spotify et démarrez une musique. Revenez dans ROADGUARD pour le titre/artiste, lecture/pause, précédent et suivant, selon les actions disponibles dans la session Spotify. Ces commandes utilisent la session locale Android; aucun mot de passe, clé Spotify ou compte développeur requis. ROADGUARD ne lit, ne stocke et ne transmet pas le contenu des notifications.

Ce panneau ne fournit pas une bibliothèque Spotify ni une connexion OAuth. Sur Android Auto, le lecteur de Spotify installé reste disponible séparément. Aucun essai avec une véritable session Spotify ou un téléphone connecté n'a été effectué ici.

## Livraison et limites

APK directe de vérification signée avec la même clé que 191–195, versionCode 396, cible API 36. Ne pas désinstaller une application portant une autre signature pour forcer la mise à jour sans sauvegarde. Carte, caméra et plafond de cache de 4 Gio conservés.

Les connecteurs WazeAPI/OpenWeb Ninja restent optionnels et nécessitent une clé personnelle : aucune connexion authentifiée testée. Aucun essai Xiaomi, DHU/véhicule ni dépôt Google Play dans cette version. La veille GPS dépend aussi des autorisations et de la gestion d'énergie du téléphone. Voir WORK/VALIDATION-11.14.196.md pour les résultats exacts et la recette sur appareil.

---

Historique des versions précédentes

# ROADGUARD11.14.195 — contrôles mémorisés

La mémoire des lieux de contrôles police/gendarmerie et radars mobiles est intégrée. Un lieu devient permanent à la troisième observation distincte. Avant ce seuil il expire24heures après son dernier signalement, sauf réglage7 ou30jours. Les relectures API ne sont pas de nouvelles confirmations. Les événements de moins de2minutes au même endroit ne sont pas cumulés; les UUID Waze communs aux deux fournisseurs sont dédupliqués. Regroupement40m, ancré sur le premier emplacement.

Un lieu permanent ancien est affiché comme zone de contrôle possible, sans garantir une présence actuelle. Confirmation manuelle disponible à moins de100m, avec GPS récent et précis; délai30minutes entre confirmations manuelles sur ce téléphone. Il s'agit d'observations distinctes, pas de trois identités de conducteurs vérifiées. Une suppression empêche la relecture des anciens signalements de recréer le point; un nouveau signalement ultérieur peut créer un nouveau point temporaire.

Accès : Paramètres → Contrôles mémorisés, également depuis l'historique des signalements. Le bouton «Recharger la liste» actualise les compteurs et les50lieux affichés. La mémoire locale est partagée avec la carte, la surveillance en arrière-plan et Android Auto. Plafond3000lieux, sans éviction automatique des lieux permanents. Les signalements privés «Radar mobile» alimentent la mémoire; les autres catégories ne l'alimentent pas.

Collecte externe optionnelle : choisissez OpenWeb Ninja ou WazeAPI, configurez une clé personnelle puis activez la collecte. Les clés sont chiffrées sur le téléphone et ne sont pas incluses dans cette livraison. Sans clé, aucune récupération externe n'est effectuée. La fréquence initiale est2minutes, réglable1/2/5/10minutes. Les quotas et frais du fournisseur s'appliquent;100requêtes ne suffisent pas à un usage quotidien régulier. La zone de recherche autour du GPS est transmise au fournisseur choisi. Les accidents et bouchons reçus sont ignorés.

Aucune clé n'a été fournie : aucune connexion authentifiée testée, aucun abonnement et aucune donnée de démonstration installée. France/Suisse, zones transfrontalières, disponibilité et droits de conservation des données externes restent à valider. Android Auto est activé par défaut; les essais véhicule/DHU et la distribution Play restent à effectuer.

APK de vérification signée avec la même clé que191–194. Ne pas désinstaller une ancienne application ayant une signature différente pour forcer la mise à jour sans sauvegarde. Cache4Go et moteur carte/caméra conservés. Gradle8.11.1/AGP8.10.1, SDK36; JDK21 pour les tests. Voir WORK/VALIDATION-11.14.195.md et les rapports associés pour les résultats exacts.

---

Historique des versions précédentes

# Mise à jour11.14.194

Cette version conserve le cache commun téléphone/voiture de4Go et son moteur de carte/caméra. Elle restreint les pages WebView et leurs accès, bloque les réponses tardives d’un écran remplacé et conserve la navigation locale. Android Auto utilise une liste explicite de six associations hôte/certificat issue de la bibliothèque1.4.0, sans autoriser tous les hôtes.

La bulle et la poignée exposent une action accessible; alignements RTL, icône monochrome et compatibilité des catégories de notification sont corrigés. Une association communautaire n'est plus déclarée réussie si la sauvegarde locale des identifiants échoue.

APK de vérification directement dans LIVRAISON, signée avec la même clé que191–193. Cette clé n'est pas une clé de publication Play. Si une ancienne installation utilise une autre clé, ne pas la désinstaller pour forcer la mise à jour sans sauvegarder ses données.

Compilation : Gradle8.11.1/AGP8.10.1/SDK36; JDK21 pour les tests API36. Android Auto est activé par défaut. Résultats détaillés : WORK/VALIDATION-11.14.194.md.

Restent : essais DHU et voiture, installation depuis un canal Play approuvé, trajet Xiaomi, association/publication communautaire réelle. Aucun compte Play, téléphone ou véhicule connecté ici. Les tests simulés ne garantissent pas le fonctionnement à100%. Préchargement50km toujours conditionné à une source l'autorisant; aucun téléchargement intégral de pays ajouté.

---

Historique conservé ci-dessous.

# Livraison11.14.193 — cache4Go

Plafond commun4Gio, réserve512Mio, éviction selon dernière consultation et budget partagé en64bits. Comptage conservé entre écritures et interface actualisée.188 tests Android simulés réussis, dont52 cache; voir WORK/VALIDATION-11.14.193.md.

# Travaux de la livraison 11.14.192

- Migration compileSdk/targetSdk36, AGP8.10.1, wrapper Gradle8.11.1 contrôlé par SHA256; test API36 avec JDK21 et Robolectric4.16.1.
- Recherche native Android Auto, coordonnées directes, résultats limités/dédoublonnés, réponses obsolètes rejetées, récupération après erreur réseau et arrêt du traitement à la fermeture.
- Permissions modernes, demande de notifications lors de l'activation du suivi, reprise après refus, accès aux réglages de notifications. Sauvegarde de l'action en attente lors de la recréation de l'activité.
- Marges Android16 pour barres système, encoche et clavier; gestion du retour via le dispatcher moderne.
- Services GPS explicitement typés, refus de démarrage Android gérés sans crash; retrait d'autorisation libérant le GPS voiture.
- GPS périmé : suppression de la manœuvre envoyée à l'hôte et état de chargement à la place d'une instruction périmée.
- Surfaces graphiques relâchées au remplacement et à l'arrêt; callbacks détachés; trajet sauvegardé conservé entre sessions.
- Tests ajoutés sur recherche, surfaces, permissions et intégration Android16. Les modifications n'altèrent pas le moteur de caméra existant et n'ajoutent aucun téléchargement intégral de pays.

Voir WORK/VALIDATION-11.14.192.md pour les vérifications effectives, les limites et les empreintes de livraison.
