# ROADGUARD 11.14.282 — lot regroupé carte, navigation et alertes

Cette version contient les modifications des versions 280 et 281, plus les développements ci-dessous. Les essais sur téléphone sont regroupés pour la fin, conformément à la demande de l'utilisateur. Aucun pourcentage de fiabilité physique n'est déduit des contrôles locaux.

## Alertes vocales

Les distances radar sont désormais réservées dans le registre partagé entre l'application et le service de la bulle, puis rendues disponibles si la synthèse refuse le message, échoue de façon asynchrone ou est interrompue. Une annonce terminée reste mémorisée ; le type est prononcé sur la première annonce, puis les distances seules. Une annulation ancienne ne peut pas effacer une étape plus récente. Une annonce déjà en attente de démarrage empêche l'empilement de distances supplémentaires. Si la voix n'est pas prête ou désactivée, elle ne consomme pas les étapes vocales radar ; les bips et l'affichage suivent leurs propres réglages.

La destruction ou la mise en veille des propriétaires d'alertes restitue leurs annonces inachevées. La disponibilité de la langue française est vérifiée lors de l'initialisation de la voix de l'application.

Un radar est réarmé après éloignement au-delà de 1,5 km, avec contrôle au plus toutes les 10 secondes sur position fraîche et suffisamment précise. Un retour peut ainsi déclencher une nouvelle première annonce sans attendre systématiquement 30 minutes. Ce comportement doit encore être éprouvé en conditions réelles.

## Filtre du trajet et bulle

Le calcul géométrique de projection sur le trajet utilise maintenant le même repère métrique aux deux extrémités des segments. L'ancien calcul appliquait la correction de longitude à une seule extrémité. Le cas de segments diagonaux et de latitudes différentes est couvert par les contrôles locaux.

Le service de la bulle recharge un instantané validé du trajet enregistré lorsque ce dernier ou la destination change. L'ancien corridor est écarté immédiatement ; une lecture devenue périmée est ignorée. Le service applique les mêmes largeurs de corridor 35/55/85 m et le même filtre de portion de trajet à venir que l'application. Le cap retenu provient du filtre GPS partagé.

Le corridor reste une approximation géométrique : il ne prouve ni la voie ni l'appartenance à une route parallèle très proche. Le trajet enregistré, les radars, la précision et le sens doivent être vérifiés lors des essais finaux.

## Carte déplacée manuellement

La vue d'exploration est conservée en mémoire pendant les changements de moteur cartographique. Elle est validée avant restauration et reste indépendante des nouvelles positions GPS. Le recentrage volontaire remet le suivi en route ; un changement explicite de destination réinitialise cette vue. Cette mémoire n'est pas enregistrée comme un historique de déplacement.

L'ordre des scripts de la carte Leaflet est corrigé : le code de restauration et de signal de démarrage est placé dans son propre bloc, afin de ne pas être ignoré à l'intérieur d'une balise de script externe. Les messages de vue identiques ne sont pas renvoyés à Android à chaque position GPS.

## Contrôles et limites

Voir verification/VALIDATION-11.14.282.md. Les contrôles ciblés du registre vocal, de la géométrie et des scripts sont réussis. Les appels Android réels, la lecture vocale, le volume, le focus audio, le Bluetooth, le comportement Xiaomi et le rendu WebGL ne sont pas validés ici. L'application entière n'a pas été compilée ; l'environnement ne dispose pas d'une distribution Gradle exploitable et ne peut pas télécharger les dépendances nécessaires.

## Session finale de tests, regroupée

Compiler une seule fois le dernier lot, puis vérifier ensemble :

1. Démarrage carte et éventuel repli 2D ; modes Auto/En ligne/Hors ligne ; coupure puis retour réseau ; conservation de la vue déplacée et recentrage ; crédit ⓘ.
2. Recherche en ligne, coordonnées et favoris/récents hors connexion ; Domicile/Travail ; annulation de calcul ; absence de réapparition d'un trajet arrêté ; reprise après redémarrage.
3. Alertes radar au premier plan puis dans la bulle ; première annonce avec type puis distances seules ; interruption volontaire d'une voix ; absence de doublon ; réarmement après éloignement ; comparaison du filtre de trajet.
4. Bips et musique, Bluetooth/voiture, GPS périmé et économie d'énergie Xiaomi. Les manipulations se font à l'arrêt ou par un passager.

Ne pas refaire une compilation entre chaque lot intermédiaire. Conserver les diagnostics et erreurs de compilation éventuels pour la session finale.
