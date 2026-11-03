# ROADGUARD 11.14.82 — Cockpit épuré

Base: 11.14.81 compilée sur Xiaomi selon captures utilisateur.

## Modifications réelles
- Compteur flottant: diamètre 96 -> 88 dp, chiffres 39 -> 35 sp.
- Barre supérieure: marges et bouton accueil réduits.
- Rail de commandes: diamètre 43/45 -> 42/44 dp.
- Panneau destination: poignée 31 dp, champ 40 dp, raccourcis 40 dp.
- Suppression de la grille de quatre actions du panneau déployé (doublons avec les onglets et commandes latérales).
- Navigation, GPS, WebView cartographique, zoom, moteur d'alertes, cache et caméra non modifiés.
- versionCode 282, versionName 11.14.82.

## Vérification et limites
- Archive ZIP vérifiée par testzip; pas de compilation APK Android dans cet environnement.
- À tester sur Xiaomi: panneau fermé, intermédiaire, ouvert, recherche, recentrage, compteur à l'arrêt et en mouvement.
- Les nombreuses voies ferrées visibles sur la carte correspondent à une zone ferroviaire; aucune donnée cartographique n'a été supprimée.
- Ne pas compter cette version comme correction du rendu des tuiles ou de la lisibilité de la carte.

## Suite
- Comparaison précise avec la maquette 10 écrans sur captures Xiaomi.
- Réduire la surcharge visuelle de la carte uniquement après vérification du fournisseur de tuiles et de ses options de style.
- Caméra à traiter en dernier.
