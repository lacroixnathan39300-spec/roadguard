# ROADGUARD 11.13.23 — passation

## Réalisé dans cette étape
- Corrigé trois textes trompeurs de l'écran « Hors ligne » : aucun pack France public n'est actuellement livré sans catalogue.
- Ajouté un bouton visible pour configurer la source HTTPS lorsque le catalogue n'est pas configuré.
- L'alerte « Carte France : source nécessaire » propose directement l'action « Configurer la source ».
- Aucun changement du code de localisation GPS ni des cartes déjà stockées.

## État réel
- GPS bleu : utilisateur indique qu'il fonctionne ; non retesté ici.
- France + Corse hors ligne : NON FONCTIONNELLE, aucune archive ni URL de pack vérifiée livrée.
- Source France : aucune archive complète hébergée/validée ; blocage principal.
- APK Android : NON COMPILÉE, NON TESTÉE sur Xiaomi.
- Avancement global : estimation antérieure 42 %, non augmentée.
- Intervention utilisateur : aucune pour cette correction source ; test sur téléphone nécessaire pour validation finale.

## À faire par ordre de priorité
1. Obtenir légalement une archive vectorielle France + Corse complète, vérifier format, licence, couches et emprise.
2. Héberger via HTTPS avec téléchargement par plages et reprise ; publier catalogue incluant URL, taille, SHA256, country=france, scope=country, format=pmtiles.
3. Compiler et installer APK ; tester téléchargement et carte en mode avion sur Xiaomi.
4. Contrôler style Protomaps/OpenMapTiles, affichage des routes et des noms, GPS, caméra et alertes.
5. Tester les autres pays, la persistance après mise à jour et la reprise des téléchargements.

## Tests réalisés
- Contrôle de présence des modifications source et de l'intégrité du ZIP.
- Aucun test de compilation ou test Android réel.
