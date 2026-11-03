# ROADGUARD 11.14.286 — projet complet

Ce dossier reprend intégralement la livraison 285, avec les modifications du lot 286. Le sélecteur Auto / En ligne / Hors ligne et le crédit cartographique compact des lots précédents sont conservés.

- Sons : un propriétaire audio par processus, priorité radar > danger > guidage > aperçu > bips ; priorité Android transitoire avec atténuation gérée par le système. Les sons ne changent pas le volume multimédia mémorisé. Les demandes refusées ou interrompues libèrent les annonces radar et jalons de guidage pour une nouvelle tentative. Les anciens retours TTS ne peuvent plus annuler une annonce plus récente.
- Bulle : masquée pendant l’utilisation d’une page ROADGUARD, lors du verrouillage ou de l’extinction de l’écran. Une transition de 300 ms évite le clignotement pendant une rotation. Position conservée proportionnellement à l’écran ; alerte agrandie et bouton + restent dans les marges disponibles. Si le bouton ne tient pas, appui long pour signaler. Les fenêtres et dialogues sont fermés au masquage. Retour quitte la page tout en conservant une surveillance expressément armée ; Arrêter ou retirer la tâche arrête cette surveillance.
- Recalcul automatique : option conservée, Internet validé sur téléphone, trois positions distinctes et au moins 8 secondes au-delà de 200 m, retour sous 120 m réinitialisant la confirmation, précision requise et délai de 90 secondes entre demandes. Une mesure périmée ne déclenche rien. Conversion km/h → m/s explicite pour le suivi du trajet sur téléphone, service et Car.
- Menu de carte : Conduite et guidage / Apparence / Bulle radar et surveillance / État et diagnostic. Les fonctions restent accessibles sans empiler des boutons permanents.

Contrôles exécutés et limites : verification/VALIDATION-11.14.286.md. Suivi pondéré : AVANCEMENT-11.14.286.md. Les chiffres sont des estimations historiques conservées ; les vérifications locales ne prouvent pas le rendu, le son ou la compilation Android.

## Session Android finale, sans désinstaller l’application

1. Ouvrir ce projet avec l’environnement Android habituel et compiler la variante téléphone ; conserver la signature habituelle pour préserver les données. Ne pas remplacer ce projet par les sources d’une ancienne archive.
2. Installer en mise à jour, vérifier portrait/paysage, grande taille de texte, marges système et accès aux quatre catégories du menu.
3. Sur téléphone, contrôler musique puis annonces : guidage, radar, interruption par appel, retour de musique, arrêt des voix/bips dans les réglages. Répéter sur Bluetooth.
4. Armer la surveillance ; quitter avec Retour, ouvrir une autre page ROADGUARD, verrouiller/déverrouiller, tourner l’écran, déplacer la bulle dans les coins, tester + et appui long ; arrêter depuis la notification puis retirer la tâche.
5. Vérifier le recalcul opté-in, un GPS imprécis, une coupure réseau, la conservation de l’ancien trajet en cas d’échec et les modes Auto / En ligne / Hors ligne.
6. Android Auto reste reporté : compilation de la variante séparée, DHU et véhicule nécessaires avant validation.

Aucune manipulation du téléphone n’est nécessaire pour conserver cette livraison. Aucun APK 286, son réel, Bluetooth ni test Android/Robolectric de ce lot n’a été exécuté dans cet environnement.
