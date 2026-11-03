# WORK — ROADGUARD 11.14.196

## Réalisé


Chaque confirmation sur place prolonge la durée du signalement temporaire (24 heures par défaut, réglable 7 ou 30 jours). Les confirmations rapprochées renouvellent ce délai sans gonfler le nombre d'observations : 30 minutes entre deux observations manuelles comptées sur ce téléphone. Au troisième événement distinct, le lieu reste permanent. Un lieu ancien reste une zone possible, sans garantir une présence actuelle. GPS récent, précision suffisante et proximité de 100 mètres restent exigés pour confirmer.

## Mode automatique

Paramètres → Surveillance auto. Armez avec ou sans bulle. Après deux mesures GPS fiables au-dessus de 20 km/h, les alertes deviennent actives. Après une minute à 3 km/h maximum, elles s'arrêtent et la bulle disparaît. Une veille GPS avec notification reste armée pour détecter le prochain départ. Une perte GPS suspend les alertes et la bulle jusqu'à une nouvelle détection de conduite.

Passer sur Waze ou l'accueil conserve ce suivi. Fermer ROADGUARD par Retour depuis la carte ou le retirer des applications récentes retire la bulle et arrête le suivi. Rouvrir ROADGUARD réarme le mode choisi. Un arrêt forcé Android nécessite de rouvrir l'application. Le bouton Arrêter désarme le mode jusqu'à réactivation manuelle. L'affichage superposé, la localisation et les notifications sont demandés par Android lors de l'activation.

## Spotify

Paramètres → Spotify · commandes musicales. Autorisez l'accès aux notifications dans les réglages Android, ouvrez Spotify et démarrez une musique. Revenez dans ROADGUARD pour le titre/artiste, lecture/pause, précédent et suivant, selon les actions disponibles dans la session Spotify. Ces commandes utilisent la session locale Android; aucun mot de passe, clé Spotify ou compte développeur requis. ROADGUARD ne lit, ne stocke et ne transmet pas le contenu des notifications.

Ce panneau ne fournit pas une bibliothèque Spotify ni une connexion OAuth. Sur Android Auto, le lecteur de Spotify installé reste disponible séparément. Aucun essai avec une véritable session Spotify ou un téléphone connecté n'a été effectué ici.



## Build et tests

VersionCode396, compileSdk/targetSdk36, minSdk26, Gradle8.11.1/AGP8.10.1, JDK21 pour Robolectric4.16.1. Variante Android Auto activée par défaut. Compilation forcée finale avec assembleDebug, testDebugUnitTest et lintDebug.

300 tests Android simulés réussis, zéro échec/erreur/ignoré, sur API26/28/34/36 selon les suites. Mémoire54, mode auto28, commandes locales Spotify12, suites voiture99, cache52. Lint : 0 erreur bloquante, 55 avertissements, catégories {'UnusedAttribute': 1, 'SetJavaScriptEnabled': 1, 'ExportedService': 1, 'UsableSpace': 4, 'SetTextI18n': 48}. Aucun contournement de lint ou baseline ajouté. Les tests Spotify utilisent les sessions Android simulées; ils ne prouvent pas le comportement de Spotify réel.

Contrats Android Auto, scripts des13écrans et audit des46fichiers sur les tuiles réussis. La carte et le moteur caméra sont comparés au ZIP195 avant livraison. Les tests ont détecté les déclarations manquantes de la variante Auto; elles ont été ajoutées aux deux manifestes, avec permission système protégeant le service d’accès musical. Retour depuis la carte termine explicitement l’activité, y compris sur Android12.

## Reste et blocages

Voir TACHES-RESTANTES.md. Pas de Xiaomi/téléphone, Spotify réel, DHU/véhicule ou compte Play connecté. Aucune clé fournisseur fournie; aucun appel authentifié. La notification de veille GPS reste nécessaire pour redétecter un départ; arrêt forcé Android nécessite une réouverture. Les points permanents sont des lieux possibles, pas des présences garanties. Les estimations historiques globales restent95% (+0point), non auditées.

## Recette sur appareil

1. Installer l’APK par mise à jour compatible; conserver les données. Ouvrir Paramètres → Surveillance auto, armer avec la bulle et accorder les autorisations demandées.
2. Vérifier aucune bulle à l’arrêt. Sur trajet, dépasser20km/h puis passer sur Waze : bulle et alertes actives. S’arrêter plus de60secondes : bulle et alertes coupées, notification de veille présente. Repartir : réactivation.
3. Tester perte GPS, Retour depuis la carte et retrait des applications récentes : aucune bulle résiduelle. Rouvrir pour réarmer. Arrêter depuis la notification : mode désarmé.
4. Créer un radar mobile; valider à proximité plusieurs fois. Vérifier renouvellement du délai sans gonfler le compteur, puis permanence après trois observations distinctes.
5. Paramètres → Spotify : autoriser les commandes, démarrer une musique dans Spotify, revenir et essayer lecture/pause/précédent/suivant. Contrôler que les autres lecteurs ne sont pas ciblés.

Signature, alignement, empreinte et contenu APK consignés après construction finale ci-dessous.

APK finale : version396/11.14.196, cible36, signature v2/v3 et certificat identique aux versions191–195, alignement4octets vérifié. Assets livrés comparés au source; nouvelles classes et service voiture présents dans DEX. SHA256 : `6dadac652b7f98703fcea5a2f62088490a5163eee19b65005ee55d82cf748968`.
