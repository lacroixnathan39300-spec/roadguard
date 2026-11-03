# ROADGUARD 11.14.288 — corrections des captures et Spotify

Cette archive contient le projet complet. Les sept inventaires embarqués et les autres fichiers de données radar restent inchangés. Version Android : 11.14.288, code 488.

## Doublons observés à Champagnole

Les deux points des captures ont été retrouvés dans les vrais fichiers embarqués : le radar fixe vers 46,74826 / 5,92218 et la zone historique vers 46,74612 / 5,92494. Leur observation Lufop supplémentaire avait une coordonnée légèrement décalée ou un libellé différent.

Le complément Lufop est maintenant rapproché, dans un rayon maximal de 35 m, d’une observation déjà attribuée à Lufop. Le pays, la famille du point, le statut historique, les limitations connues et les orientations connues doivent être compatibles. En cas de plusieurs candidats possibles, aucune correspondance n’est choisie arbitrairement. Les coordonnées, l’identifiant et les informations de l’observation d’origine sont conservés ; les provenances compatibles sont regroupées. Les zones anciennes de danger restent historiques, avec le même contrôle d’affichage que leurs équivalents embarqués. Les observations indépendantes et les types différents ne sont pas supprimés seulement parce qu’ils sont proches.

Le test avec les vrais fichiers ne produit plus les deux copies des deux emplacements et conserve tous les identifiants FR d’origine. Un groupe peut toujours contenir deux points réellement distincts ; ce n’est pas nécessairement un doublon.

## Carte hors ligne

Le sélecteur Auto / En ligne / Hors ligne ne recrée plus une carte déjà chargée. Il conserve le centre, le zoom, les tuiles affichées et le suivi. Le dernier cadre est conservé localement pour rouvrir la carte au niveau de zoom déjà consulté, sans transformer le suivi GPS en exploration manuelle.

Les requêtes raster utilisent le cache natif ; en mode hors ligne, un fichier XYZ PNG/JPEG importé peut aussi répondre si le cache HTTP manque. Un fond raster en cache reste sous les cartes vectorielles locales, y compris en dehors de leurs zones couvertes. Le choix de charger un autre moteur local ne se fait qu’après un écran raster effectivement sans tuiles et si une archive vectorielle couvre cet emplacement. Une zone absente est indiquée explicitement au lieu de laisser un fond vide sans explication.

Le cache n’invente pas les zones jamais consultées. La persistance peut être affectée par le nettoyage du cache Android ou le manque de stockage. Les détections de couverture et la bascule réelle du sélecteur nécessitent encore la vérification sur Xiaomi ; les tests locaux de stockage et de scripts ne constituent pas cet essai.

## Icône du contrôle routier

Une icône bleue représentant un agent avec un bouclier remplace l’icône de radar fixe pour Contrôle routier et les contrôles mémorisés. Elle est distincte dans Signaler et dans les pictogrammes partagés des cartes téléphone. Les catégories radar existantes gardent leurs dessins.

## Spotify

Le panneau natif propose une pochette lorsqu’elle est fournie par la session Spotify, le titre, l’artiste, un état de lecture et une ligne de temps. Les trois commandes utilisent des dessins locaux et des libellés accessibles ; aucun texte « Précédent » n’est comprimé dans un petit bouton. Le panneau défile sur les petits écrans et les grands caractères. Une seconde ouverture ne crée pas un second panneau.

Le déplacement dans le morceau est activé uniquement si Spotify autorise la commande et fournit une durée et une position. Une nouvelle session ou un changement de morceau pendant le geste ne reçoit pas une ancienne demande de déplacement. La progression utilise l’horloge de lecture ; une position inconnue n’est pas inventée et une lecture en chargement n’avance pas artificiellement. Une session Spotify en lecture est préférée à une ancienne session en pause. Les images sont celles fournies localement : aucune pochette distante n’est téléchargée par ce panneau.

Les commandes nécessitent l’autorisation Android d’accès aux notifications pour ROADGUARD. Aucun identifiant Spotify ni service de contrôle distant n’a été ajouté. Les commandes proposées restent celles de la session de Spotify sur le téléphone.

## INTERVENTION OBLIGATOIRE — après compilation, sur le téléphone

1. Importer le projet 11.14.288 dans CodeAssist et compiler la variante téléphone avec la signature habituelle ; installer en mise à jour pour conserver les données.
2. Ouvrir Spotify dans les réglages ROADGUARD, toucher Activer les commandes, puis Réglages Android ; autoriser l’accès aux notifications de ROADGUARD si ce n’est pas déjà fait. Ouvrir Spotify, lancer un morceau puis revenir au panneau.
3. Vérifier les deux emplacements des captures, l’icône Contrôle routier et les boutons Spotify.
4. Afficher une zone de carte en Auto, passer Hors ligne sans bouger le zoom, puis rouvrir la carte hors ligne. Tester aussi un redémarrage sans réseau et un déplacement dans une zone absente du cache ; relever le diagnostic en cas d’écran vide persistant.

Cette archive ne contient pas d’APK 288. Le rendu Android, la compilation SDK, les sessions Spotify et le Bluetooth n’ont pas été exécutés ici. Résultats précis : verification/VALIDATION-11.14.288.md. Fréquence communautaire conservée à deux minutes. Android Auto reste reporté.
