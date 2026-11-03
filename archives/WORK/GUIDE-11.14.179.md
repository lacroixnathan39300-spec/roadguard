# ROADGUARD 11.14.179 — les deux demandes réunies

Cette version reprend la 11.14.178 puis ajoute la France complète, une interface Android Auto et les corrections GPS/vitesse/cap/zoom.

## Ce qui change

- France : 24 429 emplacements disponibles, dont 7 325 permanents et 17 104 lieux historiques possibles. Une migration unique les active; le bouton « Activer les 24 429 emplacements France » permet de restaurer cette sélection. Le bouton ne nécessite ni fichier ni clé. Le filtre historique peut ensuite être désactivé volontairement.
- Suisse : 1 777 emplacements, dont 194 historiques. Les fichiers ne sont pas remplacés par les seules mises à jour permanentes de la communauté.
- Les listes affichent jusqu’à 200 points proches dans un rayon de 50 km; ce plafond d’affichage ne supprime aucun fichier. Un lieu historique n’est pas une présence confirmée.
- Un point réseau ne remet plus la vitesse et le cap GNSS à zéro. Une mesure absente est conservée brièvement (6 s au maximum), puis affichée « -- ». Un zéro isolé après une vitesse supérieure à 8 km/h doit être confirmé sur un second point, au moins 700 ms après; un arrêt confirmé reste affiché à zéro.
- Le cap de la voiture vient du GPS en roulant à plus de 10 km/h et avec une précision suffisante. À basse vitesse, le dernier cap est conservé. La flèche n’est plus tournée deux fois quand la carte suit la direction. Le passage 359° → 1° ne déclenche plus un tour complet du curseur.
- Paramètres → GPS, correction de vitesse et direction : écart compteur entre -15 et +15 km/h, aperçu GPS/brut corrigé, remise à zéro et réinitialisation du cap. Seul le compteur est modifié, pas la position, la vitesse utilisée pour les alertes ni le zoom. À l’arrêt, même un écart positif conserve zéro.
- Un GPS ne dispose pas d’une calibration magnétique. Le cap se réacquiert en roulant; les mouvements en huit concernent uniquement la boussole du téléphone. Aucun faux bouton de calibration satellite n’est proposé.
- Le zoom Leaflet change avec une animation de 1,35 s, une hystérésis et une attente de 3,5 s. Les changements sont espacés de 12 s. Une vitesse manquante ne provoque pas de changement. Les gestes manuels gardent la priorité.
- Le changement de page ne stoppe plus puis ne réabonne plus l’acquisition GPS. La pause de l’activité continue à libérer le GPS; les services dédiés restent responsables de l’arrière-plan et d’Android Auto.

## Android Auto

Le projet contient maintenant un CarAppService de navigation avec validation des hôtes Google, une carte native, les tuiles réellement visibles et leur cache, le trajet enregistré, les favoris, les indications, les intentions geo/google.navigation, une lecture des signalements publics toutes les 120 s et les annonces vocales avec gestion du focus audio. La simulation demandée par l’hôte n’envoie aucune position. Les préférences de couleur, curseur et correction du compteur sont reprises.

L’interface est compilée, mais n’a pas été exécutée dans un DHU ni un véhicule ici. La compatibilité opérationnelle et la certification ne sont donc pas acquises. La vérification de qualité automobile, les cas de reconnexion, l’audio avec musique/appel, les variantes d’écran, l’orientation et les permissions doivent encore être essayés.

Pour un vrai véhicule, Google impose une installation de confiance, par exemple le partage interne Play ou une piste de test. L’option « sources inconnues » ne suffit pas aux applications Car App Library. La présente APK peut être proposée au partage interne par le propriétaire du compte Play; aucun compte, lien de partage ni publication n’a été créé ici. Play re-signe les APK du partage interne. Cela peut empêcher une mise à jour d’une application signée avec une autre clé : préserver les données avant tout changement de circuit de distribution.

Le projet conserve compileSdk 34 / targetSdk 28 pour le chantier CodeAssist existant. Une diffusion Play actuelle exige une modernisation de la cible et des comportements Android associés; cette version n’est pas déclarée prête pour publication publique. Android Auto projeté est le périmètre ajouté; Android Automotive OS embarqué et l’écran instrument du véhicule ne sont pas déclarés.

Sources officielles vérifiées le 17/10/2026 :
- https://developer.android.com/training/cars/apps/navigation
- https://developer.android.com/training/cars/testing
- https://support.google.com/googleplay/android-developer/answer/9844679

## Installation et préservation des données

🔴 INTERVENTION OBLIGATOIRE : utiliser la mise à jour 11.14.179 avec la même signature que l’installation actuelle. L’APK fournie porte la même clé de vérification que la livraison 11.14.178, qui peut différer de CodeAssist. En cas de conflit de signature, conserver l’application actuelle et compiler les sources avec sa signature habituelle; ne pas désinstaller pour contourner le conflit.

Contrôles sur téléphone : ouvrir Bases radar (24 429 France activés), régler l’écart de compteur à l’arrêt, vérifier un trajet avec alternance de vitesses, retour du signal et passage du nord. Les essais du conducteur se font sans manipuler le téléphone en roulant.

Le Site communautaire existant n’est pas republié par cette livraison. Les réglages d’apparence et le fond de menu de la 11.14.178 sont conservés. La capacité de 500 000 connexions simultanées et le préchargement automatique 50 km ne sont toujours pas validés/fournis par l’infrastructure actuelle.
