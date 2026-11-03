# ROADGUARD 11.14.249 — pack visuel corrigé

Cette livraison contient **un seul APK téléphone** dans `LIVRAISON/`, le projet Android complet et les données conservées. Android Auto reste reporté ; aucun APK Android Auto n’est livré.

## Ce qui ne fonctionnait pas

Les anciens filtres jour/nuit et contraste possédaient des sélecteurs CSS plus prioritaires que les filtres du pack. Sur MapLibre et sur le canevas local, la commande du pack était absente ou écartée. Le bouton enregistrait donc le choix sans modifier les tuiles dans ces cas.

## Ce qui change

Depuis **Carte → ▧**, Graphite assombrit et désature les tuiles, Lisibilité accroît nettement le contraste et les couleurs, Couleurs OpenStreetMap affiche les tuiles sans filtre. Le choix s’applique aux cartes Leaflet, MapLibre et au canevas local ; il persiste. Cela recolore le fond de carte disponible et n’ajoute aucune nouvelle tuile. Si la zone ne charge aucune tuile, aucun pack ne peut dessiner les routes.

## Vérification

Voir `AVANCEMENT-11.14.249.md` et `verification/`. Les contrôles locaux ne remplacent pas un essai réel du rendu WebView sur Xiaomi.

🔴 **INTERVENTION OBLIGATOIRE — PLUS TARD** : compiler dans CodeAssist ou installer l’APK téléphone 11.14.249 sur le Xiaomi, ouvrir une zone dont les tuiles sont visibles et comparer les trois choix avec le bouton ▧. Si le fond reste vide ou identique, envoyer une capture de la carte et préciser si l’écran affiche une erreur de tuiles/réseau. Android Auto sera repris plus tard.
