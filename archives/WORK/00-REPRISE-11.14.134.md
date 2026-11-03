# Reprise ROADGUARD 11.14.134

Source Android / code 334. Aucun APK.

## Fait
- Le bouton Annuler ferme immédiatement la connexion HTTP radar actuellement ouverte au lieu d’attendre le prochain bloc reçu.
- L’exception réseau consécutive à cette fermeture est reconnue comme une annulation; le CSV partiel est supprimé et l’ancienne base est conservée.
- Progression, protection contre téléchargements concurrents, validation SHA-256 et rejet des fiches catalogue obsolètes conservés.
- Aucun préchargement cartographique. Caméra laissée intacte et reportée à la fin.

## Estimations
Radars 79→80 % (+1); global 72→72 % (+0), non audités. Six écrans estimés à 100 % côté code, sans build ni vérification Xiaomi.

## Suite
Compiler dans CodeAssist; vérifier téléchargement et annulation réelle avec réseau lent, puis poursuivre l’intégration d’un catalogue radar autorisé.
