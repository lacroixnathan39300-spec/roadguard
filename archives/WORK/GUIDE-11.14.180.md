# ROADGUARD 11.14.180 — finitions et vérification Android

## Changements

- Les 24 429 emplacements France et les 1 777 Suisse de la 179 restent disponibles. Les lieux historiques restent distingués des permanents et peuvent être désactivés. Aucune réinitialisation des choix de la 179 n’est effectuée.
- Paramètres : l’interrupteur « Annonces vocales » contrôle aussi le guidage, et un réglage distinct « Guidage vocal » permet de choisir les instructions de direction. Si la voix n’est pas prête ou si une annonce échoue, elle n’est plus marquée comme déjà diffusée. Sur Android Auto, une perte de focus audio interrompt la voix et permet une nouvelle tentative.
- Les 30 favoris du téléphone sont accessibles dans Android Auto, par pages de six, dans le même écran pour éviter un empilement de menus.
- Android Auto fournit maintenant la direction de la manœuvre à l’hôte. Le numéro de sortie de rond-point n’est utilisé que lorsqu’il est présent; aucun numéro n’est inventé.
- Un bouton permet de recalculer vers la destination enregistrée. Si le recalcul automatique est activé, une déviation de plus de 200 m est confirmée sur trois fixes distincts et au moins huit secondes; précision requise 35 m, véhicule en mouvement, délai de 90 secondes entre demandes. L’ancien trajet est conservé en cas d’échec. Hors itinéraire, les anciennes instructions de virage ne sont plus envoyées au combiné comme si le véhicule suivait encore la route.
- Les sauts de position invraisemblables sont rejetés aussi dans le GPS d’Android Auto.
- La flèche Leaflet conserve une rotation courte même après plusieurs tours complets autour du nord. Le zoom animé et les réglages de compteur de la 179 sont conservés.
- Cache : validation du PNG complet, des dimensions et des CRC avant sauvegarde. Une réponse tronquée ou corrompue ne remplace plus une tuile valide. Le téléphone et Android Auto partagent les verrous d’accès à leurs mêmes fichiers afin d’éviter les téléchargements concurrents du même emplacement.
- Le panneau de réglage GPS est défilant sur petit écran.
- Le chargement initial des données locales est effectué en arrière-plan. L’actualisation des signalements ne relit plus tout le catalogue radar; elle conserve l’abonnement GPS. Le démarrage différé ne relance pas le GPS si l’activité a été mise en pause.

## Vérifications

Les tests exécutent les composants Android via Robolectric et les composants officiels AndroidX Car App Testing. Ils couvrent Android API26, API28 et API34 : 13 écrans natifs, maintien de l’abonnement GPS entre pages, interruptions et reprises, faux zéros, correction du compteur séparée de la mesure, contrôle vocal, templates de voiture, manœuvres, arrivée et arrêt, invalidation d’un trajet, favoris, péremption et accès concurrents aux caches. Ce n’est pas un essai sur le Xiaomi de Nathan ni sur un véhicule.

L’essai limité d’une APK intermédiaire sur un émulateur logiciel est consigné dans WORK/VALIDATION-11.14.180.md. Un essai logiciel ou sur émulateur n’établit pas la précision GPS et la fluidité de conduite réelles.

## Points restant à valider

- Sur Xiaomi : trajet réel, rotation et zoom, passage en veille, voix française disponible, coupure/reprise du réseau et du GPS, correction du compteur choisie à l’arrêt.
- Android Auto : DHU et voiture réelle, reconnecter/déconnecter le câble, musique/appels et interruptions audio, permissions, variantes de taille d’écran et règles de qualité Google.
- Le fonctionnement dans un vrai véhicule nécessite une installation de confiance (par exemple partage interne Google Play); le seul réglage « sources inconnues » ne suffit pas à une app Car App Library. Aucun compte de publication ni lien Play n’a été créé ici.
- Le projet conserve compileSdk34 / targetSdk28 pour le chantier CodeAssist existant. Modernisation de cible Android et validation réglementaire/qualité Play nécessaires avant diffusion publique. Cette livraison ne prétend pas être certifiée Google Play.
- Site/compte : association et publication à vérifier avec un compte réel. Le site existant n’est pas republié par cette livraison.
- Le cache conserve les tuiles consultées; sa couverture n’est pas un disque garanti de 50 km. Aucun téléchargement de pays complet ni préchargement massif depuis OSM public n’est ajouté.

## Préservation des données

🔴 INTERVENTION OBLIGATOIRE : installer comme mise à jour avec la même signature. L’APK livrée reprend le certificat de vérification des 178/179, qui peut différer de CodeAssist. Si Android signale un conflit, garder l’installation actuelle et compiler le ZIP avec sa signature habituelle. Ne pas désinstaller les données pour contourner cette différence.

Le passage à cette version ne demande aucune nouvelle clé API pour les bases incluses. Le choix de couleur, de curseur, de voix et de fond illustré est conservé.

Sources officielles consultées pour l’outillage de test :
- https://developer.android.com/reference/androidx/car/app/testing/TestCarContext
- https://robolectric.org/getting-started/
- https://developer.android.com/studio/run/emulator-commandline
- https://developer.android.com/training/cars/testing
