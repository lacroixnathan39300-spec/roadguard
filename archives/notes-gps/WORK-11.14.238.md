# ROADGUARD 11.14.238 — Annulation des téléchargements groupés
- Le bouton « Tout mettre à jour » devient une commande d'annulation lorsqu'un lot est en cours.
- L'annulation stoppe la lecture réseau au prochain bloc, supprime le fichier partiel et n'entame pas le pays suivant.
- Les archives téléchargées précédemment restent conservées ; le bilan distingue l'interruption des échecs réseau.
- Le lot reste limité aux 7 sources déjà configurées ; disponibilité serveur non vérifiée.
- Les fichiers 7z reçus ne sont toujours pas convertis en radars actifs.
- Aucune base radar embarquée modifiée. Contrôle ZIP effectué ; compilation Android non réalisée.
- Correctif ciblé, pas de grosse vérification horaire anticipée.
