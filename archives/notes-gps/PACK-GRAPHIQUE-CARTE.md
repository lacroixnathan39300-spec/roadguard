# Pack graphique local de la carte

Le pack est intégré dans l’application : ouvrir **Apparence → Pack graphique local**.

| Choix | Effet |
|---|---|
| Graphite | Style sombre ROADGUARD conservé, avec les réglages Jour/Nuit existants |
| Lisibilité | Contraste renforcé et saturation réduite des tuiles, selon le mode Jour/Nuit |
| Couleurs OSM | Couleurs d’origine des images OpenStreetMap, sans filtre |

Le choix reste enregistré sur ce téléphone et est renvoyé à la carte lorsqu’elle s’ouvre. Aucun fichier supplémentaire à télécharger pour ces trois styles. Les réglages du curseur, de l’itinéraire et des couleurs de l’interface restent disponibles dans Apparence.

Ce pack applique des filtres aux images de la carte en ligne. Il ne redessine pas séparément les catégories de routes ou leurs libellés et ne fournit pas de nouvelles zones géographiques. La carte conserve son cache temporaire et sa reprise des tuiles en échec ; aucun téléchargement permanent de carte France n’est ajouté.

Les tests JavaScript contrôlent les trois identifiants et leur raccordement. Le test Android `MapAppearanceRuntimeTest` contrôle la préférence et les commandes envoyées à la carte ; il ne contrôle pas les pixels rendus par un vrai WebView. Le rendu, les petits libellés et le contraste sur Xiaomi restent à essayer en fin de lot.

Les APK Waze ou Sygic ne sont pas nécessaires à ce pack. Les services de signalements externes optionnels restent distincts des styles de carte et des bases radar intégrées.
