# ROADGUARD 11.14.223 — correctif compilation

## Modifications
- `RadarDownloadActivity.java` : fermeture manquante de la branche ASC/CSV avant validation du fichier temporaire.
- Déplacement du contrôle de création du répertoire à l'intérieur du `try` pour que l'exception IOException soit gérée.
- Aucun changement des bases radar embarquées.

## Vérifications
- Contrôle de syntaxe Java avec javac sans SDK Android : pas de diagnostic de syntaxe identifié ; les symboles Android sont naturellement indisponibles hors SDK.
- Compilation Android CodeAssist non exécutée.

## À faire
- Compiler dans CodeAssist, transmettre les nouvelles erreurs si elles apparaissent.
- Vérifier les téléchargements et le rechargement des alertes après compilation.
- Le téléchargement `.7z` n'implique toujours pas sa conversion/activation.
