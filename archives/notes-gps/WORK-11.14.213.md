# ROADGUARD 11.14.213 — Repli automatique des radars Lufop

## Réalisé
- Le catalogue Lufop Europe tente successivement la mise à jour locale, sa sauvegarde puis le CSV embarqué dans l’APK.
- Contrôle strict des lignes, coordonnées et vitesses avant publication du catalogue en mémoire.
- Si un fichier local est endommagé, il est ignoré au profit d’une source intacte ; les ressources de l’APK restent immuables.
- Journalisation des erreurs pour diagnostic.

## À faire
- Télécharger des ZIP depuis une source autorisée documentée, sans usurper RadarDroid.
- Tests de compilation Android et d’alertes GPS sur Xiaomi.
- Rechargement automatique du service après import et mise à jour de l’interface.

## Validation
- Vérification statique du code et de la conservation des fichiers radar embarqués.
- Pas de compilation Android ni de test sur appareil.
