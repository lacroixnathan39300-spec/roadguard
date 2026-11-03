# ROADGUARD 11.14.285 — interface, trajet, recherche et imports

Projet complet, version Android 11.14.285 / code 485. Contient les développements des versions précédentes, dont les trois modes de carte Auto / En ligne / Hors ligne et le crédit compact. Les essais sur téléphone restent regroupés à la fin.

## Écran de conduite et paysage

L'alerte est maintenant dans la pile verticale des informations du haut, avant la manœuvre. Elle ne dépend plus d'une marge fixe calculée indépendamment du guidage. En paysage suffisamment large, les informations du haut et le panneau du bas utilisent deux colonnes. Le panneau supérieur peut défiler lorsque son contenu dépasse la hauteur disponible.

La vitesse flottante et les commandes sont placées dans des rectangles libres mesurés. Lorsqu'aucun emplacement ne convient, la vitesse devient une valeur textuelle dans le panneau supérieur. Ce mode est aussi utilisé avec les caractères agrandis au-delà de 130 %, pour éviter le texte coupé dans le petit cercle. Les zooms peuvent disparaître du rail lorsque l'espace manque ; Recentrer, Zoom avant et Zoom arrière restent accessibles dans ⋯.

La poignée et le bouton de recherche ont une cible minimale de 44 dp ; la flèche de manœuvre est décorative pour l'accessibilité. La valeur de vitesse possède un libellé accessible actualisé. Ces changements restent une revue ciblée : ils ne valent pas vérification complète TalkBack ni validation visuelle sur Xiaomi.

## Rotation et caméra

La vue explorée, le texte de recherche, le niveau du panneau et le curseur de progression du trajet sont conservés lors de la recréation de l'activité. Le curseur est restauré seulement si le trajet enregistré correspond à la signature conservée. Le brouillon de recherche peut aussi survivre à une reconstruction de la carte tant que la destination n'a pas changé.

Leaflet et MapLibre utilisent maintenant les marges mesurées du haut et du bas pour placer le GPS dans l'intervalle visible. Les marges natives identiques ne sont pas réenvoyées à chaque mise en page. L'exploration manuelle garde la maîtrise de la caméra. Les coordonnées GPS ne sont pas modifiées par ce placement.

## Continuité du trajet et alertes

Les positions GPS consécutives sont rapprochées d'une fenêtre de progression bornée par le temps, le déplacement et la vitesse disponible. Cela évite qu'un petit écart de GPS accroche une branche proche située beaucoup plus loin sur un trajet en boucle. Les mesures anciennes ou contradictoires au même instant sont rejetées. Une interruption supérieure à 30 secondes permet une nouvelle acquisition après le curseur courant.

Le résumé, la carte de manœuvre et la voix du téléphone utilisent ce même suivi. Le service de bulle dispose du même modèle. La navigation voiture réutilise la politique de continuité, mais Android Auto reste reporté pour sa validation et ses autres travaux.

Quand le corridor de trajet est applicable, la distance d'alerte du téléphone et de la bulle est mesurée le long du parcours. Un radar situé à quelques mètres à vol d'oiseau sur le retour d'une boucle mais à plusieurs kilomètres par la route n'entre pas prématurément dans l'alerte à 1 km. Les distances du bandeau, des bips et des annonces suivent ce choix. Hors corridor, la politique géométrique de proximité reste applicable.

La projection d'un segment qui traverse l'antiméridien ne doit plus créer un faux segment traversant Greenwich. Une position hors trajet ne produit plus une heure d'arrivée précise dans le pied de guidage.

Ces protections ne prouvent pas le sens contrôlé d'un radar et ne permettent pas d'identifier avec certitude deux routes parallèles très proches. La première acquisition à une intersection ambiguë et la reprise après une longue coupure restent des cas à examiner sur appareil. Les orientations de source ne sont pas utilisées arbitrairement pour supprimer une alerte.

## Recherche de destination

Favoris et destinations récentes sont proposés immédiatement, y compris avec Internet. Les résultats en ligne sont ajoutés sans répéter les mêmes coordonnées proches. Une panne distante conserve les résultats locaux.

Une nouvelle saisie, un changement d'écran, une sélection ou la destruction de l'activité invalide la recherche et ferme sa connexion HTTP, y compris lorsqu'elle attend les en-têtes. Les réponses restent limitées à 256 Kio, sans suivre de redirection. Le service de géocodage Android n'est utilisé qu'en secours d'une recherche manuelle sans résultat local ; son appel système ne dispose pas ici du même mécanisme d'annulation que HTTP.

Les coordonnées acceptent notamment `46.75, 5.9`, `46,75 ; 5,9` et `46,75 5,9`. Les valeurs hors limites, infinies ou ambiguës sont refusées.

## Import et suppression de tuiles

L'import ZIP prépare un dossier distinct et conserve la carte précédente jusqu'au remplacement final. Le remplacement utilise des renommages atomiques avec sauvegarde et restauration après échec. Au redémarrage, une sauvegarde est remise en place si le dossier actif manque. Un import plus récent invalide l'ancien ; une suppression invalide aussi les imports en cours, pour éviter de recréer une carte supprimée.

Les limites sont 20 000 tuiles, 25 000 entrées ZIP, 2 Mio par image et 100 Mio de données décompressées au total. Les coordonnées XYZ et les signatures PNG/JPEG sont contrôlées. Les fichiers non retenus comptent également dans la limite de décompression. Ce contrôle de signature ne constitue pas un décodage complet de l'image. Les coordonnées avec zéros initiaux sont normalisées et les doublons XYZ sont refusés.

La suppression et le nettoyage des anciens dossiers s'effectuent en arrière-plan. Le nettoyage distingue les dossiers abandonnés de ceux d'un import actif, y compris après une rotation. Les interruptions de processus et les renommages restent à confirmer sur le stockage Android réel. Aucun pack régional supplémentaire n'est ajouté par cette version.

## Anciens chemins radar

Les anciens écrans d'import et les interrupteurs d'emplacements historiques demandent désormais le rechargement en arrière-plan au lieu de relire les bases sur le fil de l'interface. Le chemin synchrone conservé pour compatibilité utilise aussi la sélection commune. Les points des anciens suppléments sont identifiés comme points de catalogue, afin de pouvoir être remplacés au rechargement. Un radar personnel d'un type différent n'est plus écarté seulement parce qu'un autre radar est voisin.

Le choix manuel jour/nuit désactive le thème automatique pour que le choix explicite soit conservé au prochain démarrage.

## Vérification

Voir verification/VALIDATION-11.14.285.md et verification/interaction-11.14.285.log. Contrôles Java 8 et scripts ciblés réussis. Les tests de frontières injectées ne constituent pas une exécution Android, un rendu WebGL ou un trajet réel. Quatre tests natifs d'interface supplémentaires sont fournis mais non exécutés ici. La session finale reste nécessaire pour compiler, installer et examiner l'interface, le GPS, les sons et les interruptions sur le téléphone.

Aucun fichier de base radar, CSV/DB/SQLite, n'est remplacé ou enrichi artificiellement par ce lot. Les tâches globales A6, D7 et U4 restent ouvertes : leurs sous-parties avancent, sans créditer une tâche entière ou inventer une validation matérielle.
