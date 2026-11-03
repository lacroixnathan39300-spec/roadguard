# ROADGUARD 11.14.254 — liste des cartes et coupure visuelle

La liste de téléchargement reprend l'interface des radars : écran sombre pleine hauteur, grand titre, recherche, cartes par pays/région avec icône, nom, taille et bouton à droite ; bouton rouge Télécharger ou bouton vert Installée. Chaque archive PMTiles est conservée indépendamment et toutes sont sélectionnées automatiquement pour le mode local.

La coupure verticale de la capture du 21 octobre près de Ney provenait de la superposition de deux fonds incompatibles : raster OpenStreetMap en ligne à gauche, vectoriel régional à droite. Avec réseau disponible, l'application montre désormais le même fond raster sur toute la zone visible, avec les palettes choisies ; les cartes téléchargées restent installées et prennent le relais automatiquement lors de la coupure réseau. Hors réseau, la mosaïque PMTiles affiche toutes les régions téléchargées. En dehors de ces régions, une zone jamais téléchargée ne peut pas être inventée sans réseau. Le bandeau d'erreur JavaScript n'affirme plus que la carte est indisponible si le moteur affiche déjà une carte ; l'erreur reste consignée dans les diagnostics.

Contrôles du lot : test JavaScript des six palettes et de la bascule de visibilité réseau/local ; syntaxe JavaScript ; 9 tests d'en-tête PMTiles. Aucune compilation APK ni validation visuelle Xiaomi dans cet environnement. Android Auto reporté.

🔴 INTERVENTION OBLIGATOIRE — PLUS TARD : compiler 11.14.254 dans CodeAssist ; comparer le même point près de Ney avec réseau et en mode avion, ouvrir France Sud-Est et Suisse, vérifier la liste rouge/verte et signaler toute zone vide ou bandeau persistant.
