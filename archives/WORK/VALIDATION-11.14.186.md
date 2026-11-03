# Validation 11.14.186

- Le panneau de guidage, affiché lorsque `navigationPoints.size()>1`, contient maintenant l'action « ✕ Arrêter »; la barre de recherche réservée à l'état sans trajet ne contient plus d'action inaccessible.
- L'arrêt invalide les réponses de calcul réseau en cours avant de vider le cache et les points de guidage. La destination reste sélectionnée.
- La bulle suit `onStop` pour l'affichage et `onStart` / `onResume` pour le masquage. Le service accepte une demande d'affichage pendant son initialisation et tente le fournisseur réseau si le GPS est indisponible.
- Compilation Gradle hors ligne : réussie. Vérification de l'APK signé : voir le compte rendu de livraison.
- Vérification réelle sur Xiaomi : à effectuer après installation (accueil et Waze, retour ROADGUARD, guidage puis arrêt).
