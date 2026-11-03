# Demande et changements — 11.14.197

Demande : flash/halo bleu persistant à la venue d’un radar, bip faible avant le radar, bip nettement plus fort si dépassement de sa limitation.

Défauts constatés dans196 : style d’alerte dépendant de l’accent rouge, absence de cadre bleu; bulle avec trois impulsions puis repli après6,5secondes; même niveau sonore pour toutes les approches; colonne speed_kmh perdue lors de la lecture des points embarqués.

Réalisé : cadre+bandeau bleus fixes, panneau flottant bleu sans temporisation; priorité visuelle au radar proche même devant un autre danger; acheminement des limites non contradictoires, moteur sonore commun avec trois paliers discrets, escalade indépendante des anciens jalons et intervalle6secondes entre alertes fortes. Nettoyage au retrait de la tâche, pause, arrêt et perte de GPS. Moteurs carte/caméra préservés.

Compilation, 326 tests simulés et lint réussis; résultats définitifs dans VALIDATION-11.14.197.md. Volume et rendu Xiaomi/voiture non testés physiquement ici. Aucun appel fournisseur authentifié ni dépôt Play.
