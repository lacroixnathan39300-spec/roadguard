# Reprise ROADGUARD 11.14.119

## Passe de navigation
- Les réponses OSRM sont contrôlées avant remplacement de l’itinéraire courant.
- Chaque coordonnée de route doit être latitude/longitude valide.
- La géométrie doit contenir entre 2 et 15 000 points; aucune troncature silencieuse.
- Distance et durée doivent être finies, positives et sous les plafonds de sécurité existants.
- Les tests de politique ajoutent les bornes et cas invalides.

## Tableau d’avancement
Le panneau de suivi affiche maintenant l’avancement de chaque module, les points gagnés et une barre verte lorsqu’il progresse. La passe navigation apparaît à +5 points.

## Contraintes conservées
- Carte en ligne, cache Android/WebView normal des seules tuiles consultées; aucun préchargement.
- Pas de téléchargement permanent de carte France.
- Ne pas modifier la caméra avant la toute dernière étape.

## État
Navigation estimée 45 % (+5), global 58 % (+1) estimé et non audité. Version 11.14.119 / code 319. Aucun build Android possible ici (SDK 34/Gradle/wrapper absents). Prochaine validation : compiler dans CodeAssist, tester route réelle et reprise sur Xiaomi.
