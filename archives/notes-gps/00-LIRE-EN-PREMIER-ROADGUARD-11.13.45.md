# ROADGUARD 11.13.45 — zones hors ligne (interface et sélection sécurisée)

## Réalisé
- Dans Cartes et données, bouton **Afficher la carte avec Internet** qui bascule sur la carte en ligne déjà existante (Leaflet + OSM).
- Bouton **Télécharger une région / un département** qui présente les packs `region`, `department`, `country` présents dans un catalogue validé, avec taille affichée et confirmation avant téléchargement.
- Les fiches non HTTPS, sans empreinte SHA-256, ou de taille incorrecte sont exclues.
- Sans pack autorisé, un message clair remplace tout téléchargement fictif.
- GPS bleu, caméra et fichiers cartographiques inchangés.
- Test `python -m unittest discover -s tools -p test_offline_region_picker.py -v`.

## Non réalisé / blocages
- **Aucun serveur autorisé ni archive France/Corse disponible** : le bouton de téléchargement ne peut pas encore fournir une carte réelle.
- La carte en ligne repose sur le serveur de tuiles public OSM : affichage interactif seulement, pas de téléchargement massif pour usage hors ligne. Il faudra un fournisseur explicitement autorisé, ou un pack PMTiles produit et hébergé.
- Pas d'APK compilée ni testée sur Xiaomi dans cet environnement.
- Le sélecteur ne définit pas encore une zone libre dessinée sur la carte.

## Prochaine priorité
1. Fournisseur de cartes compatible avec téléchargement hors ligne et droit de redistribution, puis téléchargement effectif.
2. Construction/compilation de l'APK, installation et recette Xiaomi.
3. Téléchargement de zones libres, contrôle du quota, mise à jour et affichage hors ligne.

Ne pas désinstaller l'APK existante ni effacer les 2,76 Go avant vérification.
