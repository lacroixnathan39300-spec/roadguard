# ROADGUARD 11.14.287 — projet complet

Cette livraison reprend intégralement 286, y compris les modes Auto / En ligne / Hors ligne, l’interface allégée, le crédit cartographique compact et la coordination audio.

## Changements du lot

- Alertes téléphone et service de la bulle : le corridor du trajet ne dispense plus du filtre avant/arrière lorsque le véhicule roule dans le sens opposé au tracé enregistré. Un cap fiable compatible avec le segment local est nécessaire quand le filtrage directionnel est activé. Les tangentes des segments voisins sont acceptées seulement dans les 30 m d’une jonction pour ne pas couper les alertes au milieu d’un virage. Si le corridor n’est pas qualifié, le filtre géométrique avant/arrière existant reprend la décision. L’option désactivant volontairement le filtrage directionnel reste respectée.
- Calcul téléphone : client HTTP annulable enregistré avant l’attente des en-têtes, fermeture à l’annulation et rejet des résultats d’un ancien calcul. Redirections refusées, serveur OSRM HTTPS contrôlé, réponse JSON plafonnée à 1,5 Mo et limites de temps par connexion/lecture conservées. Les données de requête restent celles produites par RouteRequestPolicy.
- Perte d’Internet validé ou passage Hors ligne : seul un calcul en ligne est interrompu ; l’itinéraire enregistré n’est pas effacé. Le calcul local éventuel reste indépendant. La reprise en ligne demande une relance explicite ou une nouvelle confirmation du recalcul automatique ; aucune réponse périmée ne doit écraser le trajet. La disponibilité du recalcul dépend désormais du mode réseau choisi, plutôt que du moteur utilisé pour afficher la carte.
- Un calcul en ligne exige une position GPS récente vérifiée sur les horloges système et monotone.

## Limites qui restent ouvertes

Deux chaussées parallèles très proches ne peuvent pas être identifiées avec certitude avec la géométrie et la précision GPS seules. Une direction fournie par une source n’est pas interprétée comme un sens de circulation sans convention documentée. Le lot corrige une incohérence de sens du trajet, pas toute la tâche A6. Android Auto reste reporté et son filtre actuel n’est pas présenté comme aligné par ce lot.

## Essais Android à ajouter à la session finale

1. Même itinéraire conservé : déplacement dans le bon sens, retour en sens opposé, virage, absence de cap fiable ; comparer téléphone puis surveillance en arrière-plan.
2. Lancer un calcul puis passer Hors ligne ; vérifier la fin de l’attente et la conservation du trajet. Revenir en Auto puis relancer.
3. Répéter pendant une coupure réseau, un changement de destination et une annulation. Vérifier qu’un ancien résultat n’écrase pas le nouveau choix.
4. Vérifier qu’un calcul local en cours n’est pas annulé par le choix Hors ligne.

Conserver la signature habituelle et installer en mise à jour, sans désinstaller. Les étapes générales de la session Android restent dans 00-LIRE-EN-PREMIER-11.14.286.md. Aucun nouvel essai téléphone n’est requis pour conserver ce projet. Bilan exécuté : verification/VALIDATION-11.14.287.md.
