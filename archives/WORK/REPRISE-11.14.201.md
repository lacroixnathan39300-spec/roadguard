# ROADGUARD 11.14.201 — PROGRESSION TELECHARGEMENT

## Modifications réelles
- `AuthorizedRadarImporter` conserve son ancienne signature, et ajoute une surcharge avec `ProgressListener`.
- Émet octets téléchargés / taille annoncée (ou -1 si inconnue) depuis un thread de travail.
- Rejette les réponses HTML et les fichiers annoncés trop volumineux.
- Conserve les contrôles HTTPS, SHA-256, CSV et remplacement atomique.
- Aucune base radar embarquée modifiée. Aucune source RadarDroid privée ou contournement ajouté.

## Analyse du vrai RadarDroid
- APK `com.ventel.android.radardroid2` : les noms `downloadDb`, `downloadAndUnzip`, `DBProviderPickerDialogFragment` et `EXTRA_DOWNLOAD_PROGRESS` sont des pistes de recherche, pas encore des implémentations décompilées.
- Le serveur Lufop refuse explicitement l'accès hors RadarDroid. Aucun accès direct ROADGUARD autorisé n'a été confirmé.

## À faire
- Décompiler les méthodes et leurs appels pour documenter le format et les transitions d'état.
- Relier le callback de progression à l'écran de gestionnaire radar, et ajouter des mises à jour programmées.
- Identifier une source de téléchargement qui autorise explicitement ROADGUARD, puis créer un adaptateur de format.
- Tester compilation, téléchargement, interruption réseau, échec SHA, restauration et Android Auto.

## Validation
- Vérification structurelle de l'archive : effectuée.
- Compilation et tests sur téléphone : NON EFFECTUÉS.
- Chiffres d'avancement antérieurs : estimations non auditées ; ne pas les présenter comme validation.
- INTERVENTION OBLIGATOIRE : aucune à ce stade ; compiler uniquement à la fin des modifications.
