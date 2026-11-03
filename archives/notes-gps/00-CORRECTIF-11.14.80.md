# ROADGUARD 11.14.80 — correction de typage du compteur

Erreur signalée : MainActivity.java:430, FrameLayout ne peut être affecté à LinearLayout.

Correction : champ `FrameLayout speedHudFrame` et références cohérentes, sans conversion en LinearLayout ; compteur `speedDial` conservé en FrameLayout.

Source de base : ZIP 11.14.79-CORRECTIF-COMPILATION. Numéro de version incrémenté pour distinguer clairement les imports CodeAssist.

Vérifications : intégrité ZIP, déclaration et affectation vérifiées statiquement. Compilation Android non exécutée ici.

Dans CodeAssist, supprimer l'ancien projet importé (ou importer comme nouveau projet), puis compiler la version 11.14.80. Ne pas compiler un projet 11.14.79 resté ouvert.
