# ROADGUARD 11.14.284 — catalogues et rechargement

Projet complet contenant les développements des versions précédentes : écran de carte allégé, modes Auto / En ligne / Hors ligne, crédit compact, navigation, GPS, voix et diagnostic. Les essais Android restent regroupés à la fin.

## Fusion des imports

Une information manquante de vitesse, d'orientation ou de qualification peut être complétée par un doublon compatible. L'identifiant, le type, la position et le statut historique de l'original restent conservés. Les sources compatibles sont réunies sans répétition. Les désaccords entre vitesses connues, orientations connues ou statuts historiques empêchent cet enrichissement ; aucun classement arbitraire de fiabilité n'est introduit. Les fichiers importés restent présents.

L'orientation de source ne prouve toujours pas le sens de circulation contrôlé. Elle ne supprime pas automatiquement une alerte. Le rapprochement exact utilise l'identité existante, pas une fusion large de points proches. Les radars de types différents à proximité restent distincts dans les anciens suppléments. Un pays explicitement différent ne disparaît plus comme doublon d'un autre pays ; une provenance communautaire sans pays garde la détection de doublons du même type.

Les fichiers canoniques endommagés conservent leur mécanisme de sauvegarde. Les avertissements de repli sont journalisés. Atteindre le plafond global de 300 000 points de surcouche n'annule plus les pays déjà assemblés : le pays qui dépasserait le plafond est ignoré, avec un avertissement. Ce cas de volume n'a pas été reproduit avec 300 000 enregistrements pendant ce lot.

## Même sélection dans les moteurs

InstalledRadarCatalog assemble les bases d'origine et les anciens pays installés pour le téléphone, le service de surveillance et les données voiture. Les limites de lecture, règles de quarantaine et règles de doublons ne sont plus réimplémentées séparément dans ces trois chemins. Les données communautaires et personnelles gardent leur cycle distinct.

Le service écoute les changements de réglages, de visibilité et de révision. Son contrôle périodique ne dépend plus de l'arrivée d'une position GPS. Un résultat construit pendant une modification est écarté et rechargé. Une erreur après un premier chargement conserve le catalogue précédent. Le repli officiel reste possible au premier démarrage. Les abonnements sont libérés à la destruction.

La révision inclut maintenant l'index des anciens pays installés et les caractéristiques de leurs fichiers. La voiture vérifie cette révision avant de publier son nouveau catalogue et conserve son état précédent après une erreur. Le service et la voiture écrivent un reçu seulement après adoption de leur catalogue, comme le téléphone. Le fonctionnement Android réel de ces chemins reste à valider ; aucune validation DHU ou voiture n'est revendiquée.

## Vérification et session finale

Voir verification/VALIDATION-11.14.284.md et verification/radar-merge-11.14.284.log. Les contrôles JVM ciblés sont réussis ; ils ne constituent pas une compilation Android. Trois tests Robolectric supplémentaires sont fournis mais non exécutés ici, faute d'environnement Android/Gradle utilisable.

Ajouter à la session finale déjà prévue : importer un point nouveau et un doublon compatible ; modifier un réglage pendant un chargement ; vérifier l'arrivée des points dans la bulle sans nouveau point GPS ; démarrer/arrêter le service ; comparer les pays installés entre téléphone et voiture lorsque cette partie reportée sera reprise. Conserver les essais des lots 280–283, notamment carte Auto / En ligne / Hors ligne, voix, perte GPS, téléchargement annulé, crédit compact et paysage.

La grille historique reste à 85,0000 % de développement estimé et 44,0000 % de validation estimée. Aucun crédit de tâche entière ni essai matériel n'est ajouté par ce lot.
