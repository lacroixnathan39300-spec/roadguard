# ROADGUARD 11.14.178 — correctif MapAtlas

Base conservée : 11.14.177, projet enregistré version 39. Captures utilisateur : écran Bases radar visible sur Xiaomi; téléchargement MapAtlas en échec avec « Cannot set request property after connection is made ». Les bases OpenStreetMap affichées sont déjà installées; leur installation récente n’est pas déduite des captures.

## Cause et correctif

secureConnection lisait le code HTTP et ouvrait donc la connexion. MapAtlas tentait ensuite d’ajouter le User-Agent et de régler ses délais. Android refuse l’ajout d’en-têtes après connexion.

SecureHttp configure méthode, délais, redirections et User-Agent avant la première lecture de réponse. MapAtlas passe ses délais 20/60 secondes au moment de créer la requête. Les autres appels à secureConnection délèguent au même helper (15/30 secondes). Les erreurs libèrent la connexion. Le téléchargement existant garde sa limite 32 Mo, sa validation de pays/licence/points, son fichier temporaire et la conservation de la base précédente en cas d’échec.

## Sources clarifiées

- France et Suisse : base ROADGUARD incluse, vérification directe.
- MapAtlas et OpenStreetMap : accessibles sans clé API Lufop.
- Catalogue ROADGUARD supplémentaire : visible uniquement si une fiche existe pour ce pays.
- Lufop API : option avancée nécessitant une clé personnelle; non obligatoire.
- Import ZIP suisse : conservé. Aucun nouvel import non pris en charge proposé pour les autres pays.

## Vérifications

Test Java strict : l’ancien ordre reproduit le message Android; l’ordre corrigé permet de lire les données. Contrôles de délais/en-têtes, erreurs 301/403/404/429/500, erreur de transport, libération et refus HTTP/identifiants URL. Téléchargements de vérification FR/CH depuis cet environnement consignés dans mapatlas-network-11.14.178.json : ce ne sont pas des essais sur Xiaomi.

Résultats définitifs : VALIDATION-11.14.178.md et validation-11.14.178.json. Aucun changement du site V3, du moteur cartographique, de la caméra, des couleurs, du curseur ou des fréquences de partage. Aucun essai de capacité de 500 000 utilisateurs simultanés.

## Actions finales

🔴 INTERVENTION OBLIGATOIRE — installer l’APK si sa signature convient, sinon compiler les sources avec la signature CodeAssist habituelle. Ne pas désinstaller l’application actuelle pour contourner un conflit de signature.

Relancer MapAtlas via Menu → Installer les radars → autre pays, ou Toutes les sources → France/Suisse → MapAtlas. Il n’est pas nécessaire de créer une clé Lufop. Vérifier la nouvelle base dans la liste, puis tester sans réseau que les bases déjà installées restent disponibles.
