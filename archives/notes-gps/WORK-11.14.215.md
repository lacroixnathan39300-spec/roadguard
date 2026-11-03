# WORK — ROADGUARD 11.14.215

## Fait
- Invalidation automatique du cache Lufop Europe si les fichiers locaux changent (taille/date), même si le catalogue a déjà été lu.
- Invalidation explicite après installation conservée.
- Message d'installation clarifié.
- Bases radar d'origine et overlay embarqué inchangés.

## Limites / À faire
- La mise à jour du catalogue n'implique pas à elle seule un rafraîchissement immédiat d'une liste déjà conservée par un service GPS : ce point doit être audité.
- Téléchargement automatique façon RadarDroid non disponible ; aucune source distante autorisée intégrée.
- Compilation Android et tests Xiaomi non réalisés.
- Continuer gestion ZIP, rechargement du moteur et sources autorisées.

## Validation
- Contrôle textuel des changements et empreintes SHA-256 des données embarquées.
- Pas de compilation complète, pas de test sur appareil.
