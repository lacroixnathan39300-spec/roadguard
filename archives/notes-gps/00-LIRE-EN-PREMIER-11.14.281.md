# ROADGUARD 11.14.281 — navigation et recherche

Cette archive complète inclut les correctifs cartographiques 11.14.280 et les travaux suivants.

- L'arrêt ou le remplacement du trajet invalide les calculs en cours. Un calcul local terminé tardivement ne peut plus réactiver le guidage arrêté. L'annulation ferme aussi la connexion du calcul routier en ligne ; le calcul local vérifie l'annulation pendant la construction et l'exploration du graphe.
- Une action « Annuler le calcul du trajet » apparaît dans ⋯ lorsqu'un calcul est en cours. Les favoris et la destination restent disponibles.
- La reprise du trajet enregistré valide géométrie, coordonnées et résumé dans des objets temporaires avant de remplacer le trajet courant. Elle ne rajoute plus les manœuvres aux anciennes lors de chargements répétés. Les alternatives invalides ne décalent plus silencieusement l'itinéraire sélectionné.
- Une nouvelle destination reçue par intention Android annule l'ancien calcul et actualise la carte immédiatement. Le calcul en ligne vérifie la fraîcheur du GPS et l'accès Internet avant de contacter le serveur. La perte de connexion ne supprime pas le trajet déjà enregistré.
- Hors connexion, la recherche reconnaît les noms des favoris et les 12 dernières destinations choisies, ainsi que les coordonnées déjà prises en charge. Cela ne crée pas un moteur de recherche d'adresses hors ligne complet.
- Les recherches d'adresse en ligne utilisent un seul travailleur et une seule requête en attente. Les requêtes périmées sont abandonnées avant les appels de secours et leurs réponses ne remplacent pas la recherche actuelle.
- Maison, Chez moi et Domicile sont reconnus comme le même raccourci. Un favori nommé identiquement est mis à jour ; deux noms différents à la même adresse peuvent être conservés. Le plafond de 30 favoris s'applique aussi aux raccourcis Domicile et Travail.

## État des vérifications

Analyse syntaxique Java de MainActivity et FavoriteNames réussie ; FavoriteNames compilé avec cible Java 8. Les nouveaux parcours complets Android, le service de recherche, le routage réel et les manipulations tactiles ne sont pas validés. Pas de compilation Android ni de nouveaux essais utilisateur demandés dans ce lot. Les résultats ciblés de la version 280 sont conservés et ne sont pas présentés comme une validation automatique des changements 281.

## Essais regroupés pour la fin

Quand la phase de développement est terminée, compiler puis contrôler ensemble : carte et Auto/En ligne/Hors ligne, recherche en ligne puis favoris/récents sans réseau, Domicile/Travail, annulation pendant calcul, arrêt puis attente d'une réponse tardive, reprise du trajet après redémarrage, et suppression de la destination. Ne pas compiler chaque lot intermédiaire.
