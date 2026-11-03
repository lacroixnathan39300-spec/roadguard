# Signature des APK de vérification

La clé `verification-debug.keystore` est une identité de test historique, avec les paramètres Android debug conventionnels. Elle n’est pas une clé de publication. Le script Gradle actuel ne force pas l’emploi de cette clé.

CodeAssist et Gradle conservent leur propre signature habituelle : aucune configuration de signature existante n’a été remplacée. Une APK signée avec cette clé de vérification ne peut pas remplacer une APK signée avec une autre clé. Pour conserver les données de l’installation actuelle, reconstruire les sources avec la même configuration CodeAssist qu’auparavant.

Les deux APK 11.14.246 de LIVRAISON ont été signés par la clé debug générée pour cette compilation Gradle ; elle est distincte de la clé historique ci-dessus. Ils portent la même empreinte de certificat, consignée dans verification/apk-11.14.246.json. La compatibilité de signature avec l’application déjà présente sur le téléphone n’a pas été testée. Une différence de clé se règle en recompilant avec la signature habituelle ; ne pas effacer les données pour forcer l’installation.
