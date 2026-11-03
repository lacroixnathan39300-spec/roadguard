# ROADGUARD 11.14.224

- Correction de l’erreur CodeAssist ligne 177 : suppression de la référence locale `tmp` dans le bloc catch, remplacement par un chemin de nettoyage explicite.
- Le fichier temporaire est supprimé en cas d’échec, sans toucher aux bases intégrées.
- Avertissement targetSdk 36 edge-to-edge non bloquant, adaptation insets à tester.
- Compilation Android complète non effectuée ici.
