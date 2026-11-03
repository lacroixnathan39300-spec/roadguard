# WORK — ROADGUARD 11.14.220

## Modifications
- Import ZIP ASC Lufop : plafond de 4 000 000 octets **par fichier ASC** avant accumulation en mémoire, en plus du plafond total décompressé de 35 000 000 octets.
- Le dépassement interrompt l'import et supprime le fichier temporaire ; aucune base embarquée n'est modifiée.

## Vérifications
- Compilation Java isolée de LufopAscZipImporter.java et conversion de l'archive Lufop Europe fournie par l'utilisateur.
- Comparaison SHA-256 des fichiers radar embarqués avec la version 11.14.219.
- Contrôle d'intégrité du ZIP livré.

## Restant à faire
- Téléchargement automatique par pays depuis des sources autorisées, sans URL manuelle.
- Vérification du rechargement en navigation, tests Android Auto et compilation Android complète.

## Version / blocages
- 11.14.220, pas d'APK compilé ; ne pas considérer l'intégration RadarDroid terminée.
- Progression globale historique : 48,0000 % (non auditée), variation globale 0,0000 point ; amélioration ciblée sans réestimation arbitraire.
