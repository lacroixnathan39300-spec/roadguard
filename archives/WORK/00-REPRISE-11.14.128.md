# Reprise ROADGUARD 11.14.128

Version 11.14.128 / code 328. Source ZIP sans APK.

## Correctif
Les imports `java.io.BufferedReader` et `java.io.InputStreamReader` sont ajoutés dans `BackgroundAlertService.java`. Ils règlent les trois symboles introuvables signalés par CodeAssist à la ligne 47.

## Vérification
Tests statiques de package et suite ciblée exécutés; compilation Android réelle non disponible ici. Aucun changement fonctionnel ni progression annoncée.

## Suite
Relancer le build CodeAssist avec cette source. Si le build passe, tester sur Xiaomi. Caméra à la fin; aucun préchargement.
