# ROADGUARD 11.14.171 — rapprochement de la carte avec la maquette

Base 11.14.170, versionCode 371. L’utilisateur confirme le fonctionnement sur Xiaomi et a envoyé deux captures de 11.14.170. La comparaison avec `design/reference-validee/ROADGUARD-MAQUETTE-PREMIUM-10-ECRANS.png` montre une carte moins présente, un haut d’écran chargé et une action « Signaler » peu mise en valeur.

Sur l’écran Carte, la grande barre supérieure est retirée. Le premier onglet devient « Menu » et ouvre le tiroir; le tiroir conserve l’accès aux barres d’avancement. « Signaler » forme l’action rouge centrale. La poignée du panneau de recherche est plus compacte quand il est replié. Quand un itinéraire est calculé, son tracé devient bleu sur Leaflet, MapLibre et la carte locale, comme la référence jour/nuit. Le point GPS bleu, les alertes radar rouges, les données FR/CH et les commandes de suivi restent en place.

Les captures montrent une position sans trajet actif : la maquette montre une route guidée, une manœuvre et une limitation. Ces éléments ne peuvent s’afficher que si des données réelles existent; aucune route ni vitesse légale n’est inventée pour imiter l’image. Le fond de carte reste une vraie carte consultable dont le rendu dépend de ses données.

Les vérifications de source et les limites sont dans WORK/VALIDATION-11.14.171.md. Aucun APK 11.14.171 construit ou testé sur le Xiaomi. Les pourcentages historiques ont été réévalués : interface 100→85 % (−15 pts), global 95→94 % (−1 pt), parce que les photos montrent encore un écart de fidélité. Les autres valeurs restent stables.

**INTERVENTION OBLIGATOIRE — APRÈS LA LIVRAISON :** importer le ZIP 11.14.171 dans CodeAssist, compiler et vérifier sur Xiaomi l’écran Carte sans trajet, puis avec un trajet réel. Contrôler Menu, Avancement, Signaler, recherche, GPS bleu, alertes et tracé bleu. Comparer à la maquette jour/nuit.
