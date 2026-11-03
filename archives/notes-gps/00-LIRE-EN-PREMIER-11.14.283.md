# ROADGUARD 11.14.283 — lot transversal regroupé

Cette archive complète contient les lots 280–282 et les développements décrits ci-dessous. Les essais Android restent regroupés à la fin. Les contrôles locaux ne sont pas assimilés à une compilation ou à une validation Xiaomi.

## Navigation vocale et erreurs du moteur vocal

Les instructions ne sont plus ajoutées indéfiniment à la file de synthèse. Une instruction en attente ou une annonce radar active empêche d'ajouter une autre instruction de navigation. Une instruction interrompue ou refusée rend son seuil disponible tant que le trajet et la manœuvre sont encore actuels. Arrêter le guidage, modifier le trajet ou détruire l'activité invalide les anciens rappels. Les annonces radar conservent leur registre partagé.

Un délai de garde de 60 secondes libère une réservation vocale ou une instruction dont le moteur n'aurait jamais envoyé le callback de fin/erreur. Ce délai évite de bloquer le registre indéfiniment ; il ne prouve pas que le moteur Android produira un son. Les tâches de garde terminées ou annulées sont retirées du planificateur.

## GPS indisponible

Une position devenue périmée met la carte de guidage en attente et remplace l'heure d'arrivée par --:--. Les instructions de navigation en attente sont annulées. Une désactivation explicite du fournisseur GPS invalide immédiatement vitesse et cap, sans perdre la protection contre les mesures reçues dans le désordre. Désactiver un autre fournisseur ne produit plus à tort le message GPS désactivé.

Le dernier point connu est atténué/grisé sur Leaflet et MapLibre lorsque la position est périmée ; il reprend son apparence normale au retour d'une position fraîche. Ce signal visuel a été contrôlé au niveau du script et doit être examiné sur l'écran réel. Le service de bulle arrête aussi ses avertissements lorsque le fournisseur GPS est désactivé.

## Téléchargements en arrière-plan

Les annulations sans travailleur actif terminent le service. La destruction annule et interrompt le travailleur et ferme la connexion. Un ancien travailleur ne doit plus réinitialiser l'état global ni publier une notification à la place d'un nouveau téléchargement. Les transferts sont sérialisés autour de l'accès aux fichiers temporaires. Le contrôle d'annulation est répété après la vérification des en-têtes distants. Les fichiers installés ne sont pas supprimés par ces nouveaux contrôles.

Le cycle Android réel du service, les interruptions système et le réseau mobile/Wi-Fi restent à valider. Aucun nouveau fournisseur de cartes ni téléchargement de zone n'a été ajouté par ce lot.

## Diagnostic et écran de conduite

Dans ⋯, le diagnostic de l'appareil affiche à la demande les états de localisation précise, GPS, notifications, superposition, économie de batterie et exemption d'optimisation. Deux boutons ouvrent les pages Android correspondantes. Il ne change aucune autorisation ou exemption automatiquement. Ces états sont ajoutés au diagnostic copiable.

La poignée de recherche reste une petite flèche lorsque le panneau se met à jour : son ancien grand titre ne réapparaît plus. La hauteur des suggestions est limitée selon la hauteur disponible, notamment en paysage. Le symbole ⓘ reste à 16 pixels CSS ; seule sa zone d'appui transparente est portée à 44 × 44.

## Métadonnées des radars

L'orientation connue et la qualification de source sont conservées du stockage canonique vers les objets du catalogue et la fiche affichée. Le lecteur des bases embarquées conserve les orientations numériques valides, en laissant les autres inconnues. Les fiches affichent ces informations lorsqu'elles existent, avec échappement HTML.

L'orientation fournie n'est pas assimilée automatiquement au sens de circulation contrôlé : la convention de certaines sources reste ambiguë. Elle n'est donc pas utilisée pour supprimer arbitrairement une alerte. La fusion complète par sens et fiabilité, ainsi que l'identification certaine des routes parallèles proches, restent des chantiers ouverts.

## Suivi d'avancement

L'écran d'avancement lit désormais un fichier courant portant le numéro et le chantier du lot 283. Les poids historiques restent identiques : 85,0000 % de développement estimé et 44,0000 % de validation historique. Les changements partiels sont documentés sans créditer artificiellement une tâche globale ni un essai matériel. Les tâches ouvertes A6, D7 et U4 précisent les améliorations et limites actuelles.

## Contrôles réalisés et session finale

Voir verification/VALIDATION-11.14.283.md. Conserver ce projet complet comme point de sauvegarde. La liste regroupée des essais des versions 281–282 reste applicable ; ajouter : interruption des instructions, GPS désactivé/rétabli, couleur du point périmé, diagnostic de l'appareil, annulation d'un téléchargement et lecture des métadonnées de source. Les essais sur téléphone ne sont pas demandés entre les lots.
